package com.example

import com.example.data.character.initiative.*
import com.example.data.character.runtime.*
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.engine.UserContactCooldown
import com.example.data.engine.ScheduledWorldAction
import com.example.data.engine.ScheduledActionType
import com.example.data.character.CharacterWorldPolicy
import com.example.data.mock.OfficialCharacters
import com.example.data.model.*
import com.example.data.projection.projectGalleryAssets
import com.example.data.projection.projectMoments
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CharacterInitiativeRuntimeTest {
    private val now = 1_700_000_000_000L
    private val clock = WorldClock(dateLabel = "11月14日", minutesOfDay = 9 * 60, weather = WeatherState.RAIN)
    private val yan = CharacterRuntimeResolver.resolve(OfficialCharacters.cardYan.data)
    private val rain = InitiativeEvidence(InitiativeTrigger.RAIN, "rain:2023-11-14", "下雨")

    private fun always(type: ProactiveContentType): CharacterRuntimeProfile = yan.copy(initiative = yan.initiative.copy(
        messageProbability = if (type == ProactiveContentType.TEXT) 1.0 else 0.0,
        callProbability = if (type == ProactiveContentType.CALL_INVITE) 1.0 else 0.0,
        photoProbability = if (type == ProactiveContentType.PHOTO) 1.0 else 0.0,
        momentProbability = if (type == ProactiveContentType.MOMENT) 1.0 else 0.0,
        letterProbability = if (type == ProactiveContentType.LETTER) 1.0 else 0.0,
        triggerWeights = mapOf(InitiativeTrigger.RAIN to 1.0),
    ))

    @Test fun realEvidenceCoversNineTriggersWithoutInventingAbsenceOrMemories() {
        val moving = LifeEvent("move", "yan", "08:50", LifeEventType.LOCATION_CHANGE, "去了书店", "", worldDateLabel = clock.dateLabel, worldMinutesOfDay = 530)
        val facts = InitiativeFacts(now - 26 * 3_600_000L, "memory1", "用户提过喜欢旧书", "conflict1", true, now - 1_000)
        val profile = yan.copy(identity = yan.identity.copy(birthday = "11-14"))
        val morning = CharacterInitiativeRuntime.evidence(profile, now, "2023-11-14", clock, facts, listOf(moving))
        val night = CharacterInitiativeRuntime.evidence(profile, now, "2023-11-14", clock.copy(minutesOfDay = 23 * 60), facts, emptyList())
        assertEquals(InitiativeTrigger.entries.toSet(), (morning + night).map { it.trigger }.toSet())
        val empty = CharacterInitiativeRuntime.evidence(yan, now, "2023-11-14", clock.copy(weather = WeatherState.CLEAR, minutesOfDay = 14 * 60), InitiativeFacts(), emptyList())
        assertTrue(empty.isEmpty())
        assertTrue(morning.single { it.trigger == InitiativeTrigger.BIRTHDAY }.description.contains("角色自己的生日"))
    }

    @Test fun conflictMustActuallyBeReadyAndOldGoodEventsDoNotRecur() {
        val facts = InitiativeFacts(conflictEventId = "unresolved", readyToReconnect = false, recentGoodEventAtEpochMs = now - 48 * 3_600_000L)
        val evidence = CharacterInitiativeRuntime.evidence(yan, now, "2023-11-14", clock, facts, emptyList())
        assertFalse(evidence.any { it.trigger in setOf(InitiativeTrigger.RECENT_CONFLICT, InitiativeTrigger.RECENT_GOOD_EVENT) })
    }

    @Test fun repeatedTicksAndProcessRecreationUseIdenticalDraws() {
        val profile = always(ProactiveContentType.TEXT)
        val first = CharacterInitiativeRuntime.decide(profile, now, listOf(rain), emptyList())
        val boundary = now / CharacterInitiativeRuntime.WINDOW_MS * CharacterInitiativeRuntime.WINDOW_MS
        repeat(30) { index -> assertEquals(first, CharacterInitiativeRuntime.decide(profile, boundary + index * 1000, listOf(rain), emptyList())) }
        assertEquals(CharacterInitiativeRuntime.stableSample("persistent-key"), CharacterInitiativeRuntime.stableSample("persistent-key"), 0.0)
        assertNotEquals(CharacterInitiativeRuntime.stableSample("persistent-key"), CharacterInitiativeRuntime.stableSample("another-key"))
    }

    @Test fun zeroWeightTriggerAndZeroProbabilityChannelNeverFire() {
        val profile = always(ProactiveContentType.PHOTO)
        assertNull(CharacterInitiativeRuntime.decide(profile, now, listOf(rain.copy(trigger = InitiativeTrigger.MORNING)), emptyList()) { 0.0 })
        assertNull(CharacterInitiativeRuntime.decide(profile.copy(initiative = profile.initiative.copy(photoProbability = 0.0)), now, listOf(rain), emptyList()) { 0.0 })
    }

    @Test fun allFiveTypesAreSelectableFromRuntimeProbabilities() {
        ProactiveContentType.entries.forEach { type ->
            assertEquals(type, CharacterInitiativeRuntime.decide(always(type), now, listOf(rain), emptyList()) { 0.0 }?.type)
        }
    }

    @Test fun successfulWindowAndEvidenceAreDurablySuppressed() {
        val profile = always(ProactiveContentType.TEXT)
        val decision = CharacterInitiativeRuntime.decide(profile, now, listOf(rain), emptyList()) { 0.0 }!!
        val event = CharacterInitiativeRuntime.fact(profile, decision, "窗外正在下雨。", now, clock)
        val restored = Json.decodeFromString(LifeEvent.serializer(), Json.encodeToString(LifeEvent.serializer(), event))
        assertNull(CharacterInitiativeRuntime.decide(profile, now, listOf(rain.copy(key = "another-trigger")), listOf(restored)) { 0.0 })
        assertNull(CharacterInitiativeRuntime.decide(profile, now + CharacterInitiativeRuntime.WINDOW_MS, listOf(rain), listOf(restored)) { 0.0 })
    }

    @Test fun lowPhotoDailyCapCannotBeBypassedByANewTrigger() {
        val profile = always(ProactiveContentType.PHOTO).copy(initiative = always(ProactiveContentType.PHOTO).initiative.copy(photoFrequency = "low"))
        val decision = CharacterInitiativeRuntime.decide(profile, now, listOf(rain), emptyList()) { 0.0 }!!
        val event = CharacterInitiativeRuntime.fact(profile, decision, "书页", now, clock)
        assertNull(CharacterInitiativeRuntime.decide(profile, now + 3_600_000, listOf(rain.copy(key = "next-rain")), listOf(event)) { 0.0 })
        assertNotNull(CharacterInitiativeRuntime.decide(profile, now + 25 * 3_600_000, listOf(rain.copy(key = "next-day")), listOf(event)) { 0.0 })
    }

    @Test fun letterChannelWorksWhenTextIsDisabledAndNeverOverridesUserOptOut() {
        val profile = always(ProactiveContentType.LETTER).copy(initiative = always(ProactiveContentType.LETTER).initiative.copy(messageFrequency = "none", letterFrequency = "medium"))
        assertTrue(CharacterInitiativeRuntime.contactSettings(ProactiveSettings(enabled = true), profile).enabled)
        assertFalse(CharacterInitiativeRuntime.contactSettings(ProactiveSettings(enabled = false), profile).enabled)
        val settings = CharacterInitiativeRuntime.contactSettings(ProactiveSettings(enabled = true, dailyLimit = 1, intervalHours = 24), profile)
        assertEquals(1, settings.dailyLimit)
        assertEquals(24, settings.intervalHours)
    }

    @Test fun privatePhotoFlowsToGalleryWithAssetSlotAndStaysOutOfPublicMoments() {
        val profile = always(ProactiveContentType.PHOTO)
        val fact = CharacterInitiativeRuntime.fact(profile, InitiativeDecision(ProactiveContentType.PHOTO, rain, "window"), "今天的工作台", now, clock)
        val gallery = projectGalleryAssets(emptyList(), listOf(fact), mapOf("yan" to "沈砚")).single()
        assertEquals(fact.imageReference, gallery.visualReference)
        assertTrue(projectMoments(emptyList(), listOf(fact), authorNameOf = { "沈砚" }).isEmpty())
        assertEquals(fact.imageReference, projectMoments(emptyList(), listOf(fact.copy(visibility = "PUBLIC")), authorNameOf = { "沈砚" }).single().imageType)
        assertTrue(gallery.visualReference.startsWith("character_asset:"))
        assertTrue(UserContactCooldown.recentlyContacted(clock, listOf(fact)))
    }

    @Test fun letterRestoresReadStateAndBodyFromDurableFact() {
        val fact = CharacterInitiativeRuntime.fact(always(ProactiveContentType.LETTER), InitiativeDecision(ProactiveContentType.LETTER, rain, "letter-window"), "桌上的书替你留着。", now, clock)
        val restored = Json.decodeFromString(LifeEvent.serializer(), Json.encodeToString(LifeEvent.serializer(), fact))
        val letter = projectInitiativeLetters(listOf(restored)).single()
        assertEquals(fact.description, letter.body)
        assertEquals(LetterDeliveryState.DELIVERED, letter.deliveryState)
        assertEquals(LetterDeliveryState.OPENED, projectInitiativeLetters(listOf(restored), setOf(letter.id)).single().deliveryState)
    }

    @Test fun burstKeepsAllContentWithinOneToThreeBubbles() {
        assertEquals(listOf("一", "二", "三\n四"), CharacterInitiativeRuntime.splitText("一\n二\n三\n四", 3))
        assertEquals(listOf("一\n二"), CharacterInitiativeRuntime.splitText("一\n二", 1))
    }

    @Test fun officialCharactersHaveDifferentContactDistributions() {
        val profiles = OfficialCharacters.cards.map { CharacterRuntimeResolver.resolve(it.data) }
        val counts = profiles.associate { profile ->
            profile.characterId to (0 until 2000).mapNotNull { index ->
                val evidence = InitiativeTrigger.entries.map { InitiativeEvidence(it, "$index:${it.wireName}", "fixture fact") }
                CharacterInitiativeRuntime.decide(profile, now + index * CharacterInitiativeRuntime.WINDOW_MS, evidence, emptyList())?.type
            }.groupingBy { it }.eachCount()
        }
        val yeo = counts.getValue("zhoujianye"); val noa = counts.getValue("peixubai"); val yanCount = counts.getValue("hewenchuan")
        assertTrue(yeo.getOrDefault(ProactiveContentType.TEXT, 0) > noa.getOrDefault(ProactiveContentType.TEXT, 0))
        assertTrue(yeo.getOrDefault(ProactiveContentType.PHOTO, 0) > yanCount.getOrDefault(ProactiveContentType.PHOTO, 0))
        assertTrue(yeo.getOrDefault(ProactiveContentType.CALL_INVITE, 0) > noa.getOrDefault(ProactiveContentType.CALL_INVITE, 0))
        assertTrue(noa.getOrDefault(ProactiveContentType.LETTER, 0) > noa.getOrDefault(ProactiveContentType.TEXT, 0))
    }

    @Test fun worldPhotoConsumesTheSameSlotAsDirectPhotoSharing() {
        val profile = always(ProactiveContentType.PHOTO).let { it.copy(initiative = it.initiative.copy(photoFrequency = "low")) }
        val worldPhoto = LifeEvent("world-photo", "yan", "08:10", LifeEventType.PHOTO, "书页", "光透过纸页", worldDateLabel = clock.dateLabel, worldMinutesOfDay = 490)
        assertNull(CharacterInitiativeRuntime.decide(profile, now, listOf(rain), listOf(worldPhoto), clock.dateLabel) { 0.0 })
        assertNotNull(CharacterInitiativeRuntime.decide(profile, now, listOf(rain), listOf(worldPhoto), "11月15日") { 0.0 })
    }

    @Test fun savingCharactersPhotoDoesNotConsumeTheirAutonomousPhotoQuota() {
        val profile = CharacterRuntimeResolver.resolve(OfficialCharacters.cardNoa.data).let { it.copy(initiative = it.initiative.copy(photoFrequency = "low")) }
        val saved = LifeEvent("user_saved_photo", "noa", "09:00", LifeEventType.PHOTO, "用户收藏照片", "已收藏",
            worldDateLabel = clock.dateLabel, worldMinutesOfDay = 540, sourceAppId = "gallery", metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER))
        assertEquals(0, CharacterInitiativeQuota.used("noa", clock.dateLabel, "photo", listOf(saved)))
        assertTrue(CharacterInitiativeQuota.available(profile, clock.dateLabel, "photo", listOf(saved)))
        val actualPhoto = saved.copy(id = "noa_shared_photo", metadata = emptyMap())
        assertEquals(1, CharacterInitiativeQuota.used("noa", clock.dateLabel, "photo", listOf(saved, actualPhoto)))
        assertFalse(CharacterInitiativeQuota.available(profile, clock.dateLabel, "photo", listOf(saved, actualPhoto)))
    }

    @Test fun invitationAndRingingShareOneCallCapButCallCompletionDoesNotDoubleCount() {
        val profile = always(ProactiveContentType.CALL_INVITE).let { it.copy(initiative = it.initiative.copy(callFrequency = "low")) }
        val invite = CharacterInitiativeRuntime.fact(profile, InitiativeDecision(ProactiveContentType.CALL_INVITE, rain, "w"), "方便通话吗", now, clock)
        val candidate = PlannedWorldAction("candidate", "yan", clock.dateLabel, 12 * 60, ScheduledActionType.INCOMING_CALL, LifeEventType.MESSAGE, "通话", "想说句话", "书店")
        val card = OfficialCharacters.cardYan.data
        assertTrue(CharacterWorldPolicy.select(listOf(candidate), emptyList(), listOf(invite)) { card }.isEmpty())
        val completion = invite.copy(id = "finished-call", sourceAppId = "call", metadata = emptyMap())
        assertEquals(1, CharacterInitiativeQuota.used("yan", clock.dateLabel, "call", listOf(invite, completion)))
    }

    @Test fun plannedActionRechecksChangedPreferenceSleepAndQuotaAtExecution() {
        val profile = always(ProactiveContentType.PHOTO).let { it.copy(initiative = it.initiative.copy(photoFrequency = "low")) }
        val action = scheduled(ScheduledActionType.LIFE_EVENT, LifeEventType.PHOTO)
        assertTrue(eligible(action, profile))
        assertFalse(eligible(action, profile.copy(initiative = profile.initiative.copy(photoFrequency = "none"))))
        assertFalse(eligible(action.copy(triggerTimeMinutes = 2 * 60), profile))
        val alreadySent = CharacterInitiativeRuntime.fact(profile, InitiativeDecision(ProactiveContentType.PHOTO, rain, "w"), "书页", now, clock)
        assertFalse(eligible(action, profile, listOf(alreadySent)))
    }

    @Test fun scheduledCallsRespectOptOutQuietDailyLimitAndOngoingCalls() {
        val action = scheduled(ScheduledActionType.INCOMING_CALL, LifeEventType.MESSAGE)
        assertTrue(eligible(action, yan))
        assertFalse(eligible(action, yan, settings = ProactiveSettings(enabled = false)))
        assertFalse(eligible(action, yan, realMinute = 23 * 60))
        assertFalse(eligible(action, yan, state = ProactiveState(sentDate = "2023-11-14", sentCount = 3)))
        assertFalse(eligible(action, yan, hasCall = true))
        val invite = CharacterInitiativeRuntime.fact(yan, InitiativeDecision(ProactiveContentType.TEXT, rain, "w"), "来了条消息", now, clock.copy(minutesOfDay = 11 * 60 + 30))
        assertFalse(eligible(action, yan, listOf(invite)))
    }

    private fun scheduled(type: ScheduledActionType, eventType: LifeEventType) = ScheduledWorldAction(
        "schedule", 12 * 60, "12:00", type, "yan", "schedule", "题目", "内容", eventType, worldDate = clock.dateLabel,
    )
    private fun eligible(
        action: ScheduledWorldAction, profile: CharacterRuntimeProfile, events: List<LifeEvent> = emptyList(),
        settings: ProactiveSettings = ProactiveSettings(enabled = true), state: ProactiveState = ProactiveState(),
        realMinute: Int = 12 * 60, hasCall: Boolean = false,
    ) = CharacterInitiativeQuota.eligibleScheduled(action, profile, clock, events, settings, state, now, realMinute, "2023-11-14", hasCall)
}
