package com.example

import com.example.data.context.CharacterContext
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.MockData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.projectCheckPhone
import com.example.data.projection.projectDiary
import com.example.data.projection.projectLiving
import com.example.data.projection.projectMoments
import com.example.data.projection.projectPresence
import com.example.data.registry.CharacterRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WorldFlowEndToEndTest
 *
 * P3B-5 cross-app end-to-end scenarios over the shared LifeEvent ledger:
 * one authored fact -> Living / Moments / Diary / CheckPhone / Home Bento all
 * read the same record through their projections, with no duplicate stores.
 */
class WorldFlowEndToEndTest {

    private val seedIds = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id }
    private val authorOf: (String) -> String = { CharacterRegistry.getCharacter(it).name }

    private fun fact(
        id: String,
        characterId: String = "mira",
        type: LifeEventType = LifeEventType.THOUGHT,
        title: String = "title_$id",
        description: String = "desc_$id",
        location: String? = null,
        imageReference: String? = null,
        date: String = "9月26日",
        minutes: Int = 12 * 60,
        time: String = "%02d:%02d".format(minutes / 60, minutes % 60),
        metadata: Map<String, String> = emptyMap()
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = type,
        title = title,
        description = description,
        location = location,
        imageReference = imageReference,
        worldDateLabel = date,
        worldMinutesOfDay = minutes,
        sourceAppId = "e2e",
        metadata = metadata
    )

    @Test
    fun seedWorldFactsAreStampedWithVirtualTime() {
        val seed = WorldStateRepository.events.value.firstOrNull { it.id == "six_mira_work" }
        checkNotNull(seed) { "seed ledger must contain six_mira_work" }
        assertTrue(seed.worldDateLabel.isNotBlank())
        assertTrue(seed.worldMinutesOfDay in 0..1439)
    }

    @Test
    fun ledgerStaysNewestFirstForHomeBento() {
        val stored = WorldStateRepository.appendLifeEvent(
            fact(id = "e2e_head_1", minutes = 8 * 60)
        )
        assertEquals("e2e_head_1", stored.id)
        assertEquals("e2e_head_1", WorldStateRepository.events.value.first().id)
        assertEquals(4, WorldStateRepository.latestEvents(4).size)
    }

    @Test
    fun momentFactFlowsFromLedgerIntoMomentsFeed() {
        WorldStateRepository.appendLifeEvent(
            fact(
                id = "e2e_moment_flow",
                type = LifeEventType.MOMENT,
                title = "新到的雏菊",
                description = "花瓶里换上了今天新买的雏菊",
                location = "客厅窗台",
                minutes = 13 * 60 + 7,
                metadata = mapOf("moodTag" to "小确幸")
            )
        )

        val feed = projectMoments(
            seedPosts = MockData.getMomentsFromLifeEvents(),
            runtimeEvents = WorldStateRepository.events.value,
            seedEventIds = seedIds,
            authorNameOf = authorOf
        )

        val post = feed.first { it.id == "moment_e2e_moment_flow" }
        assertEquals("花瓶里换上了今天新买的雏菊", post.content)
        assertEquals("客厅窗台", post.locationContext)
        assertEquals("小确幸", post.moodTag)
        assertEquals("苏晚宁", post.authorName)
        // runtime fact precedes authored seed fixtures in the feed
        assertTrue(feed.indexOf(post) < feed.indexOfFirst { it.id == "six_moment_six_mira_photo" })
    }

    @Test
    fun diaryFactFlowsFromLedgerIntoDiaryStream() {
        WorldStateRepository.appendLifeEvent(
            fact(
                id = "e2e_diary_flow",
                type = LifeEventType.DIARY,
                title = "窗台的夜",
                description = "今天把窗台收拾干净，等你来坐。",
                imageReference = "flowers",
                minutes = 21 * 60 + 30,
                metadata = mapOf("weather" to "夜风微凉", "mood" to "安宁")
            )
        )

        val entries = projectDiary(
            characterId = "mira",
            runtimeEvents = WorldStateRepository.events.value,
            seedEntries = MockData.diaryEntries,
            seedEventIds = seedIds,
            authorNameOf = authorOf
        )

        val mine = entries.first { it.id == "e2e_diary_flow" }
        assertEquals("窗台的夜", mine.title)
        assertEquals("9月26日", mine.date)
        assertEquals("夜风微凉", mine.weather)
        assertEquals("安宁", mine.mood)
        assertEquals("flowers", mine.imageReference)
        // runtime pages precede authored seed pages
        assertTrue(entries.indexOf(mine) < entries.indexOfFirst { it.id == "six_diary_mira_1" })
    }

    @Test
    fun photoFactFlowsFromLedgerIntoPrivateGallery() {
        WorldStateRepository.appendLifeEvent(
            fact(
                id = "e2e_photo_flow",
                type = LifeEventType.PHOTO,
                title = "雨后的窗",
                description = "玻璃上的水痕像一条条小河",
                imageReference = "rain_window",
                minutes = 16 * 60 + 20,
                metadata = mapOf("photo_title" to "雨后的窗")
            )
        )

        val phone = projectCheckPhone(
            characterId = "mira",
            runtimeEvents = WorldStateRepository.events.value,
            seedEventIds = seedIds
        )

        val photo = phone.privateGallery.first { it.title == "雨后的窗" }
        assertEquals("rain_window", photo.imageType)
        assertEquals("玻璃上的水痕像一条条小河", photo.note)
        // runtime gallery entries precede Mira's seed album
        assertTrue(phone.privateGallery.indexOf(photo) == 0)
    }

    @Test
    fun livingFactBecomesTheCurrentState() {
        WorldStateRepository.appendLifeEvent(
            fact(
                id = "e2e_living_flow",
                title = "正在给窗台换水",
                location = "客厅窗台",
                minutes = 22 * 60
            )
        )

        val mira = CharacterRegistry.getCharacter("mira")
        val projection = projectLiving(
            character = mira,
            seedTimeline = MockData.getTimelineForCharacter("mira"),
            runtimeEvents = WorldStateRepository.events.value,
            seedEventIds = seedIds
        )

        assertEquals("正在给窗台换水", projection.currentActivity)
        assertEquals("客厅窗台", projection.currentLocation)
        assertEquals("e2e_living_flow", projection.currentEvent?.id)
        assertTrue(projection.timeline.first { it.id == "e2e_living_flow" }.isCurrent)
    }

    @Test
    fun noaLocationUpdateIsVisibleForNoaButNotForMira() {
        WorldStateRepository.appendLifeEvent(
            fact(
                id = "e2e_noa_location",
                characterId = "noa",
                type = LifeEventType.LOCATION_CHANGE,
                title = "在北境灯塔写生",
                location = "北境灯塔",
                date = "9月27日",
                minutes = 9 * 60
            )
        )

        val noa = CharacterRegistry.getCharacter("noa")
        val mira = CharacterRegistry.getCharacter("mira")
        val events = WorldStateRepository.events.value

        val noaPresence = projectPresence(noa, events, seedIds)
        assertEquals("北境灯塔", noaPresence.currentLocation)
        assertEquals("e2e_noa_location", noaPresence.currentEventId)

        val miraPresence = projectPresence(mira, events, seedIds)
        assertNotEquals("北境灯塔", miraPresence.currentLocation)

        // query API agrees with the projection
        val latestNoa = WorldStateRepository.latestForCharacter("noa")
        assertEquals("e2e_noa_location", latestNoa?.id)
        assertEquals("北境灯塔", latestNoa?.location)
    }

    @Test
    fun singleFactIsVisibleAcrossAllConsumers() {
        WorldStateRepository.appendLifeEvent(
            fact(
                id = "e2e_shared_fact",
                type = LifeEventType.MOMENT,
                title = "共享事实",
                minutes = 18 * 60 + 45
            )
        )
        val events = WorldStateRepository.events.value
        val mira = CharacterRegistry.getCharacter("mira")

        // Home Bento (newest-first snapshot)
        assertEquals("e2e_shared_fact", WorldStateRepository.latestEvents(1).first().id)

        // Living timeline
        val living = projectLiving(mira, MockData.getTimelineForCharacter("mira"), events, seedIds)
        assertTrue(living.timeline.any { it.id == "e2e_shared_fact" })

        // Moments feed
        val feed = projectMoments(MockData.getMomentsFromLifeEvents(), events, seedIds, authorOf)
        assertTrue(feed.any { it.id == "moment_e2e_shared_fact" })

        // Diary stream is type-scoped: a MOMENT must NOT appear as a diary page
        val entries = projectDiary("mira", events, MockData.diaryEntries, seedIds, authorOf)
        assertTrue(entries.none { it.id == "e2e_shared_fact" })
    }

    @Test
    fun customCharacterScopesNeverInheritMiraContent() {
        try {
            CharacterContext.select("custom_e2e_starlight")
            val profile = CharacterContext.currentCharacter()
            assertEquals("custom_e2e_starlight", profile.id)

            val events = WorldStateRepository.events.value

            // no diary pages, no phone traces from Mira's world
            val entries = projectDiary(
                characterId = profile.id,
                runtimeEvents = events,
                seedEntries = MockData.diaryEntries,
                seedEventIds = seedIds,
                authorNameOf = authorOf
            )
            assertTrue(entries.isEmpty())

            val phone = projectCheckPhone(profile.id, events, seedIds)
            assertEquals(EMPTY_CHECK_PHONE_DATA, phone)

            // Living falls back to the custom profile's own fields, never Mira's
            val living = projectLiving(profile, emptyList(), events, seedIds)
            assertTrue(living.timeline.isEmpty())
            assertEquals(profile.currentActivity, living.currentActivity)
            assertNotEquals(
                CharacterRegistry.getCharacter("mira").currentActivity,
                living.currentActivity
            )
        } finally {
            CharacterContext.select(CharacterContext.DEFAULT_CHARACTER_ID)
        }
    }
}
