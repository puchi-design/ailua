package com.example

import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.character.CharacterWorldPolicy
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.engine.ProactiveRules
import com.example.data.engine.ScheduledActionType
import com.example.data.mock.OfficialCharacters
import com.example.data.model.AiluaBehavior
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.AiluaInitiative
import com.example.data.model.AiluaLife
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.ProactiveSettings
import com.example.data.model.ProactiveState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Behavior hints may narrow planning; they must not bypass user preferences or rewrite facts. */
class CharacterBehaviorRuntimeTest {
    private val runtime = CharacterBehaviorRuntime
    private val today = "10月3日"
    private val tomorrow = "10月4日"
    private val hero = "custom"

    @Test
    fun plainV2AndFutureSchemasUseConservativeRuntimeWithoutInterpretingFutureFields() {
        val plain = CharacterCardData(id = hero, name = "Imported", personality = "Original personality")
        val future = plain.copy(extensions = jsonObject("""{"ailua":{"schema":9,"behavior":{"core_desire":"FUTURE_SECRET"}}}"""))
        val baseline = PromptAssembler.assemble(PromptAssemblyInput(character = plain))
        for (data in listOf(plain, future)) {
            val profile = CharacterRuntimeResolver.resolve(data)
            assertFalse(profile.relationship.romanceEnabled)
            assertEquals("medium", profile.initiative.messageFrequency)
            assertTrue(runtime.prompt(profile).contains("当前未启用恋爱路线"))
            assertFalse(runtime.prompt(profile).contains("FUTURE_SECRET"))
            val assembled = PromptAssembler.assemble(PromptAssemblyInput(character = data))
            assertTrue(assembled.includedBlocks.any { it.id == "ailua_behavior" })
            assertEquals(baseline.messages, assembled.messages)
        }
    }

    @Test
    fun arbitraryVendorAndUnknownAiluaJsonNeverBecomePromptInstructions() {
        val data = CharacterCardData(id = hero, name = "Custom", extensions = jsonObject("""{
          "vendor":{"instruction":"VENDOR_SECRET","nested":[{"system":"NESTED_VENDOR_SECRET"}]},
          "ailua":{"schema":1,"behavior":{"core_desire":"Recognized desire","unknown":"UNKNOWN_BEHAVIOR_SECRET"},
            "unknown_group":{"prompt":"UNKNOWN_GROUP_SECRET"},"visual":{"portrait":"VISUAL_REFERENCE_SECRET"}}
        }"""))
        val assembled = PromptAssembler.assemble(PromptAssemblyInput(character = data))
        val text = assembled.messages.joinToString("\n") { it.content }
        assertTrue(text.contains("Recognized desire"))
        listOf("VENDOR_SECRET", "NESTED_VENDOR_SECRET", "UNKNOWN_BEHAVIOR_SECRET", "UNKNOWN_GROUP_SECRET", "VISUAL_REFERENCE_SECRET")
            .forEach { assertFalse("Unexpected JSON leakage: $it", text.contains(it)) }
        assertEquals(1, assembled.includedBlocks.count { it.id == "ailua_behavior" })
    }

    @Test
    fun threeOfficialCardsProduceDistinctIsolatedBehaviorPrompts() {
        val cards = OfficialCharacters.cards.map { it.data }
        assertEquals(setOf("yan", "yeo", "noa"), cards.map { it.id }.toSet())
        val desires = cards.associate { it.id to AiluaCharacterExtensionCodec.read(it).behavior.coreDesire }
        assertEquals(3, desires.values.toSet().size)
        val prompts = cards.map { data ->
            val extension = AiluaCharacterExtensionCodec.read(data)
            val result = PromptAssembler.assemble(PromptAssemblyInput(character = data))
            val behavior = result.includedBlocks.single { it.id == "ailua_behavior" }.content
            assertTrue(behavior.contains(desires.getValue(data.id)))
            assertTrue(behavior.contains(extension.behavior.flaws.first()))
            assertTrue(behavior.contains(extension.speech.sentenceLength))
            desires.filterKeys { it != data.id }.values.forEach { assertFalse(behavior.contains(it)) }
            behavior
        }
        assertEquals(3, prompts.toSet().size)
    }

    @Test
    fun officialFrequenciesAndSleepWindowsAreMachineReadable() {
        val frequencies = setOf("none", "never", "off", "very_low", "low", "low_medium", "medium", "medium_high", "high")
        OfficialCharacters.cards.forEach { card ->
            val extension = AiluaCharacterExtensionCodec.read(card.data)
            with(extension.initiative) {
                listOf(messageFrequency, callFrequency, photoFrequency, momentFrequency, letterFrequency).forEach {
                    assertTrue("${card.data.id} has an unsupported frequency: $it", it in frequencies)
                }
            }
            assertTrue("${card.data.id} should be sleeping at 03:00", runtime.isSleeping(extension, 3 * 60))
            assertFalse("${card.data.id} should be awake at 13:00", runtime.isSleeping(extension, 13 * 60))
            listOf("call", "photo", "moment").forEach {
                assertTrue("${card.data.id} unexpectedly uses unlimited $it fallback", runtime.dailyActionLimit(extension, it) < Int.MAX_VALUE)
            }
        }
    }

    @Test
    fun characterFrequencyCannotEnableDisabledUserMessages() {
        val settings = ProactiveSettings(enabled = false, intervalHours = 12, dailyLimit = 1)
        listOf("off", "low", "medium", "high", "custom_frequency").forEach { frequency ->
            val adjusted = runtime.proactiveSettings(settings, preferences(frequency))
            assertFalse(adjusted.enabled)
            assertFalse(shouldFire(adjusted, ProactiveState()))
        }
        assertFalse(settings.enabled)
    }

    @Test
    fun characterFrequencyRetainsQuietHoursAndCannotIncreaseUserQuotaOrSpeed() {
        val settings = ProactiveSettings(enabled = true, intervalHours = 30, quietStartMinute = 22 * 60, quietEndMinute = 9 * 60, dailyLimit = 1)
        listOf("low", "medium", "high", "custom_frequency").forEach { frequency ->
            val adjusted = runtime.proactiveSettings(settings, preferences(frequency))
            assertEquals(settings.quietStartMinute, adjusted.quietStartMinute)
            assertEquals(settings.quietEndMinute, adjusted.quietEndMinute)
            assertTrue(adjusted.intervalHours >= settings.intervalHours)
            assertTrue(adjusted.dailyLimit <= settings.dailyLimit)
            assertFalse(shouldFire(adjusted, ProactiveState(), minute = 23 * 60))
            assertFalse(shouldFire(adjusted, ProactiveState(), minute = 8 * 60))
            assertFalse(shouldFire(adjusted, ProactiveState(sentDate = "2026-10-03", sentCount = 1)))
            assertFalse(shouldFire(adjusted, ProactiveState(lastSuccessAtEpochMs = now - 29 * 3_600_000L)))
            assertTrue(shouldFire(adjusted, ProactiveState()))
        }
        assertEquals(1, settings.dailyLimit)
        assertEquals(30, settings.intervalHours)
    }

    @Test
    fun characterOptOutCanDisableAnEnabledUserPreference() {
        val settings = ProactiveSettings(enabled = true)
        listOf("off", "none", "never").forEach {
            assertFalse(runtime.proactiveSettings(settings, preferences(it)).enabled)
        }
        assertEquals(settings, runtime.proactiveSettings(settings, null))
    }

    @Test
    fun sleepWindowsWrapMidnightWithAnInclusiveStartAndExclusiveEnd() {
        val extension = preferences().copy(life = AiluaLife(sleepWindow = "23:00-07:00"))
        assertFalse(runtime.isSleeping(extension, 22 * 60 + 59))
        assertTrue(runtime.isSleeping(extension, 23 * 60))
        assertTrue(runtime.isSleeping(extension, 0))
        assertTrue(runtime.isSleeping(extension, 6 * 60 + 59))
        assertFalse(runtime.isSleeping(extension, 7 * 60))
        assertFalse(runtime.isSleeping(extension, 12 * 60))
        val nap = extension.copy(life = AiluaLife(sleepWindow = "13:00-14:00"))
        assertFalse(runtime.isSleeping(nap, 12 * 60 + 59))
        assertTrue(runtime.isSleeping(nap, 13 * 60))
        assertFalse(runtime.isSleeping(nap, 14 * 60))
    }

    @Test
    fun missingOrMalformedSleepWindowsUseConservativeDefaultSchedule() {
        assertFalse(runtime.isSleeping(null, 0))
        listOf("", "whenever", "25:00-07:00", "23:75-07:00", "23:00-23:00").forEach { window ->
            val extension = preferences().copy(life = AiluaLife(sleepWindow = window))
            val profile = CharacterRuntimeResolver.fromExtension(extension)
            assertEquals("00:00-08:00", profile.life.sleepWindow)
            assertTrue("Default sleep window: $window", runtime.isSleeping(profile, 0))
            assertFalse(runtime.isSleeping(profile, 12 * 60))
        }
    }

    @Test
    fun planSelectionEnforcesCallPhotoAndMomentCapsAcrossOneBatch() {
        val candidates = (0..7).flatMap { index -> listOf("call", "photo", "moment").map { action("$it-$index", it, minute = 9 * 60 + index * 30) } }
        val expected = mapOf("off" to listOf(0, 0, 0), "low" to listOf(1, 1, 1), "medium" to listOf(2, 3, 2), "high" to listOf(4, 6, 4))
        expected.forEach { (frequency, counts) ->
            val data = card(preferences(frequency))
            val selected = CharacterWorldPolicy.select(candidates, emptyList(), emptyList()) { data }
            assertEquals("$frequency call quota", counts[0], selected.count { it.type == ScheduledActionType.INCOMING_CALL })
            assertEquals("$frequency photo quota", counts[1], selected.count { it.lifeEventType == LifeEventType.PHOTO })
            assertEquals("$frequency moment quota", counts[2], selected.count { it.lifeEventType == LifeEventType.MOMENT })
        }
    }

    @Test
    fun dailyCapsIncludeBothPendingPlansAndAlreadyRecordedEvents() {
        val data = card(preferences("medium"))
        val existing = listOf(action("pending-call", "call"), action("pending-photo", "photo"))
        val events = listOf(event("past-call", "call"), event("past-photo", "photo"), event("past-moment", "moment"))
        val candidates = listOf(action("new-call", "call"), action("new-photo-1", "photo"), action("new-photo-2", "photo"), action("new-moment-1", "moment"), action("new-moment-2", "moment"))
        val selected = CharacterWorldPolicy.select(candidates, existing, events) { data }
        assertEquals(listOf("new-photo-1", "new-moment-1"), selected.map { it.id })
    }

    @Test
    fun dailyCapsAreScopedToTheCharacterAndWorldDate() {
        val data = card(preferences("low"))
        val existing = listOf(action("other-person", "photo", characterId = "other"), action("other-day", "photo", date = tomorrow))
        val events = listOf(event("other-person-event", "photo", characterId = "other"), event("other-day-event", "photo", date = tomorrow))
        val candidate = action("available-slot", "photo")
        assertEquals(listOf(candidate), CharacterWorldPolicy.select(listOf(candidate), existing, events) { data })
    }

    @Test
    fun oneScheduledCallAndItsCompletionUseOneCallSlot() {
        val data = card(preferences("medium"))
        val ringing = event("scheduled-call-one", "call", minute = 10 * 60)
        val completion = ringing.copy(
            id = "pulse_call_call-session-one", type = LifeEventType.SOCIAL,
            sourceAppId = "call", sourceRefId = "call-session-one",
        )
        val candidate = action("second-call", "call", minute = 14 * 60)
        assertEquals(listOf(candidate), CharacterWorldPolicy.select(listOf(candidate), emptyList(), listOf(ringing, completion)) { data })
    }

    @Test
    fun sleepFilteringPreservesOtherLifeEventsAndNeverMutatesInputRecords() {
        val data = card(preferences("high").copy(life = AiluaLife(sleepWindow = "23:00-07:00")))
        val candidates = mutableListOf(
            action("sleeping-call", "call", minute = 23 * 60),
            action("sleeping-photo", "photo", minute = 0),
            action("sleeping-moment", "moment", minute = 6 * 60),
            action("thought", "thought", minute = 0),
            action("awake-photo", "photo", minute = 7 * 60),
        )
        val existing = mutableListOf(action("already-planned", "photo", date = tomorrow))
        val events = mutableListOf(event("recorded-fact", "photo", date = tomorrow))
        val originalCandidates = candidates.toList()
        val originalExisting = existing.toList()
        val originalEvents = events.toList()
        val selected = CharacterWorldPolicy.select(candidates, existing, events) { data }
        assertEquals(listOf("thought", "awake-photo"), selected.map { it.id })
        assertEquals(originalCandidates, candidates)
        assertEquals(originalExisting, existing)
        assertEquals(originalEvents, events)
        selected.forEach { result -> assertSame(candidates.single { it.id == result.id }, result) }
    }

    @Test
    fun ordinaryAndFutureCardsApplyConservativePlanningLimitsWithoutUsingUnknownFields() {
        val plain = CharacterCardData(id = hero, name = "Plain")
        val future = plain.copy(extensions = jsonObject("""{"ailua":{"schema":7,"initiative":{"call_frequency":"off"},"life":{"sleep_window":"00:00-23:59"}}}"""))
        val candidates = (0..6).flatMap { index -> listOf("call", "photo", "moment").map { action("$it-$index", it, minute = 12 * 60 + index * 30) } }
        for (data in listOf(plain, future)) {
            val extension = AiluaCharacterExtensionCodec.readOrNull(data)
            assertNull(extension)
            val profile = CharacterRuntimeResolver.resolve(data)
            listOf("call", "photo", "moment").forEach { assertEquals(1, runtime.dailyActionLimit(profile, it)) }
            assertTrue(LifeEventType.MEAL in runtime.fallbackKinds(profile))
            assertTrue(LifeEventType.PHOTO in runtime.fallbackKinds(profile))
            assertEquals(listOf("call-0", "photo-0", "moment-0"), CharacterWorldPolicy.select(candidates, emptyList(), emptyList()) { data }.map { it.id })
            assertTrue(CharacterWorldPolicy.select(candidates.map { it.copy(triggerMinutes = 3 * 60) }, emptyList(), emptyList()) { data }.isEmpty())
        }
    }

    @Test
    fun photoOptOutRemovesPhotoFallbackWithoutRemovingOrdinaryLife() {
        val kinds = runtime.fallbackKinds(preferences("off"))
        assertFalse(LifeEventType.PHOTO in kinds)
        assertFalse(LifeEventType.MOMENT in kinds)
        assertTrue(LifeEventType.MEAL in kinds)
        assertTrue(LifeEventType.THOUGHT in kinds)
        assertTrue(LifeEventType.SOCIAL in kinds)
    }

    private fun preferences(frequency: String = "medium") = AiluaCharacterExtension(
        initiative = AiluaInitiative(messageFrequency = frequency, callFrequency = frequency, photoFrequency = frequency, momentFrequency = frequency),
        behavior = AiluaBehavior(coreDesire = "Keep an independent daily life"),
    )

    private fun card(extension: AiluaCharacterExtension): CharacterCardData = CharacterCardData(
        id = hero, name = "Custom", extensions = AiluaCharacterExtensionCodec.write(JsonObject(emptyMap()), extension),
    )

    private fun action(id: String, kind: String, minute: Int = 12 * 60, characterId: String = hero, date: String = today): PlannedWorldAction = PlannedWorldAction(
        id = id, characterId = characterId, triggerWorldDate = date, triggerMinutes = minute,
        type = when (kind) { "call" -> ScheduledActionType.INCOMING_CALL; "moment" -> ScheduledActionType.MOMENT; else -> ScheduledActionType.LIFE_EVENT },
        lifeEventType = when (kind) { "call" -> LifeEventType.MESSAGE; "photo" -> LifeEventType.PHOTO; "moment" -> LifeEventType.MOMENT; else -> LifeEventType.THOUGHT },
        title = id, description = "A future action", location = "Library",
        metadata = mapOf("note" to "Preserve this candidate"),
    )

    private fun event(id: String, kind: String, minute: Int = 9 * 60, characterId: String = hero, date: String = today): LifeEvent = LifeEvent(
        id = if (kind == "call") "pulse_call_plan_$id" else id,
        characterId = characterId, time = "%02d:%02d".format(minute / 60, minute % 60),
        type = when (kind) { "call" -> LifeEventType.MESSAGE; "photo" -> LifeEventType.PHOTO; "moment" -> LifeEventType.MOMENT; else -> LifeEventType.THOUGHT },
        title = id, description = "A recorded fact", worldDateLabel = date, worldMinutesOfDay = minute,
        sourceAppId = "heartbeat", sourceRefId = id,
    )

    private fun shouldFire(settings: ProactiveSettings, state: ProactiveState, minute: Int = 12 * 60): Boolean =
        ProactiveRules.shouldFire(settings, state, now, minute, "2026-10-03")

    private fun jsonObject(text: String): JsonObject = Json.parseToJsonElement(text) as JsonObject

    private val now = 1_791_000_000_000L
}
