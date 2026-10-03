package com.example

import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.engine.ScheduledActionType
import com.example.data.engine.ScheduledWorldAction
import com.example.data.model.sortedChronologically
import com.example.data.model.virtualMinuteRank
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LifeEventLedgerTest
 *
 * P3B-1 LifeEvent ledger contract:
 * - legacy JSON stays readable after the additive schema extension
 * - same-minute events are only deduped by event id
 * - query APIs: character / source / type / latest
 * - unified chronological ordering on virtual world time
 * - scheduled actions record their declared LifeEventType (MOMENT stays MOMENT, DIARY stays DIARY)
 */
class LifeEventLedgerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun event(
        id: String,
        characterId: String = "mira",
        time: String = "12:00",
        type: LifeEventType = LifeEventType.THOUGHT,
        worldDateLabel: String = "9月25日",
        worldMinutesOfDay: Int = 12 * 60,
        sourceAppId: String = "world",
        relatedCharacterIds: List<String> = emptyList(),
        title: String = "title_$id"
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = type,
        title = title,
        description = "desc_$id",
        worldDateLabel = worldDateLabel,
        worldMinutesOfDay = worldMinutesOfDay,
        sourceAppId = sourceAppId,
        relatedCharacterIds = relatedCharacterIds
    )

    private fun scheduledAction(
        id: String,
        type: ScheduledActionType,
        lifeEventType: LifeEventType = LifeEventType.THOUGHT
    ) = ScheduledWorldAction(
        id = id,
        triggerTimeMinutes = 20 * 60,
        triggerTimeString = "20:00",
        type = type,
        characterId = "ledger_schedule_$id",
        payloadId = "payload_$id",
        title = "title_$id",
        description = "description_$id",
        lifeEventType = lifeEventType
    )

    // === Backward compatibility ===

    @Test
    fun legacyLifeEventJsonStillDeserializes() {
        val legacyJson = """
            {
                "id": "legacy_1",
                "characterId": "mira",
                "time": "07:42",
                "type": "WAKE_UP",
                "title": "晨光初醒",
                "description": "薄雾推开晨曦。",
                "location": "青石街23号",
                "visibility": "PUBLIC",
                "relatedCharacterIds": ["yuna"],
                "imageReference": null
            }
        """.trimIndent()

        val decoded = json.decodeFromString(LifeEvent.serializer(), legacyJson)

        assertEquals("legacy_1", decoded.id)
        assertEquals(LifeEventType.WAKE_UP, decoded.type)
        assertEquals(listOf("yuna"), decoded.relatedCharacterIds)
        // additive fields fall back to schema defaults
        assertEquals(1, decoded.schemaVersion)
        assertEquals("", decoded.worldDateLabel)
        assertEquals(-1, decoded.worldMinutesOfDay)
        assertEquals("world", decoded.sourceAppId)
        assertNull(decoded.sourceRefId)
        assertTrue(decoded.metadata.isEmpty())
    }

    @Test
    fun newFieldsRoundTrip() {
        val original = event(
            id = "roundtrip_1",
            type = LifeEventType.MOMENT,
            sourceAppId = "moments"
        ).copy(
            sourceRefId = "post_9",
            metadata = mapOf("search_query" to "伯爵红茶怎么泡")
        )

        val encoded = json.encodeToString(LifeEvent.serializer(), original)
        val decoded = json.decodeFromString(LifeEvent.serializer(), encoded)

        assertEquals(original, decoded)
    }

    // === Identity: id is the only dedupe authority ===

    @Test
    fun sameMinuteEventsAreBothKept() {
        val suffix = System.nanoTime()
        val first = event(id = "same_minute_a_$suffix", time = "21:40", worldMinutesOfDay = 21 * 60 + 40)
        val second = event(id = "same_minute_b_$suffix", time = "21:40", worldMinutesOfDay = 21 * 60 + 40)

        WorldStateRepository.appendLifeEvent(first)
        WorldStateRepository.appendLifeEvent(second)

        val stored = WorldStateRepository.events.value.filter {
            it.id == first.id || it.id == second.id
        }
        assertEquals(2, stored.size)
    }

    @Test
    fun duplicateEventIdIsIgnoredAndReturnsExisting() {
        val suffix = System.nanoTime()
        val id = "dedupe_$suffix"
        val original = event(id = id, title = "original")
        val duplicate = event(id = id, title = "duplicate")

        val storedFirst = WorldStateRepository.appendLifeEvent(original)
        val storedSecond = WorldStateRepository.appendLifeEvent(duplicate)

        assertEquals("original", storedFirst.title)
        assertEquals("original", storedSecond.title)
        assertEquals(1, WorldStateRepository.events.value.count { it.id == id })
    }

    // === Query APIs ===

    @Test
    fun characterQueryIsStrictUnlessRelatedRequested() {
        val suffix = System.nanoTime()
        val own = event(id = "char_own_$suffix", characterId = "yuna", type = LifeEventType.MOMENT)
        val related = event(
            id = "char_rel_$suffix",
            characterId = "mira",
            relatedCharacterIds = listOf("yuna")
        )
        WorldStateRepository.appendLifeEvent(own)
        WorldStateRepository.appendLifeEvent(related)

        val strict = WorldStateRepository.eventsForCharacter("yuna")
        assertTrue(strict.any { it.id == own.id })
        assertTrue(strict.none { it.id == related.id })

        val withRelated = WorldStateRepository.eventsForCharacter("yuna", includeRelated = true)
        assertTrue(withRelated.any { it.id == related.id })

        val both = WorldStateRepository.eventsForCharacterAndType("yuna", LifeEventType.THOUGHT, includeRelated = true)
        assertTrue(both.any { it.id == related.id })
        assertTrue(both.none { it.id == own.id })
    }

    @Test
    fun sourceQueryGroupsByProducer() {
        val suffix = System.nanoTime()
        val heartbeatEvent = event(id = "src_hb_$suffix", sourceAppId = "heartbeat")
        val mailboxEvent = event(id = "src_mb_$suffix", sourceAppId = "mailbox")
        WorldStateRepository.appendLifeEvent(heartbeatEvent)
        WorldStateRepository.appendLifeEvent(mailboxEvent)

        val heartbeatEvents = WorldStateRepository.eventsForSource("heartbeat")
        assertTrue(heartbeatEvents.any { it.id == heartbeatEvent.id })
        assertTrue(heartbeatEvents.none { it.id == mailboxEvent.id })

        val mailboxEvents = WorldStateRepository.eventsForSource("mailbox")
        assertTrue(mailboxEvents.any { it.id == mailboxEvent.id })
    }

    @Test
    fun latestForCharacterReturnsNewestChronological() {
        val suffix = System.nanoTime()
        val characterId = "ledger_order_$suffix"
        // Isolate the query from authored seed events and append in reverse time
        // order so this also catches implementations returning the last insertion.
        WorldStateRepository.appendLifeEvent(
            event(id = "latest_b_$suffix", characterId = characterId, time = "23:10", worldMinutesOfDay = 23 * 60 + 10)
        )
        WorldStateRepository.appendLifeEvent(
            event(id = "latest_a_$suffix", characterId = characterId, time = "08:00", worldMinutesOfDay = 8 * 60)
        )

        val latest = WorldStateRepository.latestForCharacter(characterId)
        assertNotNull(latest)
        assertEquals("latest_b_$suffix", latest?.id)
    }

    // === Ordering ===

    @Test
    fun chronologicalSortUsesVirtualWorldTime() {
        val eBlankDate = LifeEvent(
            id = "sort_blank",
            characterId = "mira",
            time = "23:00",
            type = LifeEventType.THOUGHT,
            title = "t",
            description = "d"
        )
        val eSep25Morning = event(id = "sort_0800", time = "08:00", worldMinutesOfDay = 8 * 60)
        val eSep25Night = event(id = "sort_2100", time = "21:00", worldMinutesOfDay = 21 * 60)
        val eSep26 = event(id = "sort_next_day", time = "07:30", worldDateLabel = "9月26日", worldMinutesOfDay = 7 * 60 + 30)

        val sorted = listOf(eSep26, eSep25Night, eBlankDate, eSep25Morning).sortedChronologically()

        assertEquals(
            listOf("sort_blank", "sort_0800", "sort_2100", "sort_next_day"),
            sorted.map { it.id }
        )
    }

    @Test
    fun sameMinuteSortIsDeterministicById() {
        val a = event(id = "tie_b", time = "10:30", worldMinutesOfDay = 10 * 60 + 30)
        val b = event(id = "tie_a", time = "10:30", worldMinutesOfDay = 10 * 60 + 30)

        val sortedOnce = listOf(a, b).sortedChronologically()
        val sortedTwice = listOf(b, a).sortedChronologically()

        assertEquals(listOf("tie_a", "tie_b"), sortedOnce.map { it.id })
        assertEquals(sortedOnce, sortedTwice)
    }

    @Test
    fun minuteRankFallsBackToDisplayTime() {
        val event = LifeEvent(
            id = "fallback_time",
            characterId = "mira",
            time = "14:05",
            type = LifeEventType.THOUGHT,
            title = "t",
            description = "d",
            worldDateLabel = "9月25日",
            worldMinutesOfDay = -1
        )
        assertEquals(14 * 60 + 5, event.virtualMinuteRank())
    }

    // === Append normalization ===

    @Test
    fun appendFillsMissingWorldTimeFromClockAndDisplayTime() {
        val suffix = System.nanoTime()
        val raw = LifeEvent(
            id = "normalize_$suffix",
            characterId = "mira",
            time = "14:05",
            type = LifeEventType.SOCIAL,
            title = "t",
            description = "d"
        )

        val stored = WorldStateRepository.appendLifeEvent(raw)

        assertEquals(14 * 60 + 5, stored.worldMinutesOfDay)
        assertTrue(stored.worldDateLabel.isNotBlank())
        // and the stored instance is the one in the flow
        assertEquals(stored, WorldStateRepository.events.value.firstOrNull { it.id == stored.id })
    }

    // === Heartbeat scheduling: data-driven LifeEventType ===

    @Test
    fun momentScheduleIsNotRecordedAsThought() {
        val action = scheduledAction(
            id = "sched_test_moment_${System.nanoTime()}",
            type = ScheduledActionType.MOMENT,
            lifeEventType = LifeEventType.THOUGHT // even if unset, MOMENT schedule must stay MOMENT
        )

        assertEquals(LifeEventType.MOMENT, WorldHeartbeatEngine.lifeEventTypeFor(action))

        WorldHeartbeatEngine.executeAction(action)
        val stored = WorldStateRepository.events.value.firstOrNull { it.id == "pulse_sched_${action.id}" }
        assertNotNull(stored)
        assertEquals(LifeEventType.MOMENT, stored?.type)
        assertEquals("heartbeat", stored?.sourceAppId)
        assertEquals(action.id, stored?.sourceRefId)
    }

    @Test
    fun diaryScheduleKeepsDiaryType() {
        val action = scheduledAction(
            id = "sched_test_diary_${System.nanoTime()}",
            type = ScheduledActionType.LIFE_EVENT,
            lifeEventType = LifeEventType.DIARY
        )

        assertEquals(LifeEventType.DIARY, WorldHeartbeatEngine.lifeEventTypeFor(action))

        WorldHeartbeatEngine.executeAction(action)
        val stored = WorldStateRepository.events.value.firstOrNull { it.id == "pulse_sched_${action.id}" }
        assertNotNull(stored)
        assertEquals(LifeEventType.DIARY, stored?.type)
    }

    @Test
    fun lifeEventScheduleKeepsDeclaredTypeAndLocationScheduleStaysLocation() {
        val meal = scheduledAction(
            id = "sched_test_meal_${System.nanoTime()}",
            type = ScheduledActionType.LIFE_EVENT,
            lifeEventType = LifeEventType.MEAL
        )
        assertEquals(LifeEventType.MEAL, WorldHeartbeatEngine.lifeEventTypeFor(meal))

        val location = scheduledAction(
            id = "sched_test_loc_${System.nanoTime()}",
            type = ScheduledActionType.LOCATION_CHANGE
        )
        assertEquals(LifeEventType.LOCATION_CHANGE, WorldHeartbeatEngine.lifeEventTypeFor(location))

        WorldHeartbeatEngine.executeAction(location)
        val stored = WorldStateRepository.events.value.firstOrNull { it.id == "pulse_loc_${location.id}" }
        assertNotNull(stored)
        assertEquals(LifeEventType.LOCATION_CHANGE, stored?.type)
        assertEquals(20 * 60, stored?.worldMinutesOfDay)
    }

    // === Batch append ===

    @Test
    fun appendLifeEventsDedupesInBatchAndAgainstLedger() {
        val suffix = System.nanoTime()
        val seedId = "batch_seed_$suffix"
        WorldStateRepository.appendLifeEvent(event(id = seedId))

        val batch = listOf(
            event(id = seedId, title = "should_be_ignored"),
            event(id = "batch_a_$suffix"),
            event(id = "batch_a_$suffix", title = "in_batch_duplicate")
        )
        val stored = WorldStateRepository.appendLifeEvents(batch)

        // one authoritative event echoed per input; only two unique ids survive
        assertEquals(3, stored.size)
        assertEquals(2, stored.distinctBy { it.id }.size)
        assertEquals("title_$seedId", WorldStateRepository.events.value.first { it.id == seedId }.title)
        assertEquals(1, WorldStateRepository.events.value.count { it.id == "batch_a_$suffix" })
        assertEquals("title_batch_a_$suffix", stored.first { it.id == "batch_a_$suffix" }.title)
    }
}
