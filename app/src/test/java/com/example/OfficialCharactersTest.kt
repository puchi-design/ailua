package com.example

import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.engine.OfficialDemoWorldPlan
import com.example.data.engine.ScheduledActionType
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldPlanRuntime
import com.example.data.engine.WorldPlanValidator
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.mock.OfficialCharacters
import com.example.data.mock.WorldData
import com.example.data.model.LifeEventType
import com.example.data.model.WorldClock
import com.example.data.model.WorldPlan
import com.example.data.registry.CharacterRegistry
import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json

class OfficialCharactersTest {
    @Test
    fun officialSeedFactsAllPrecedeTheInitialWorldClock() {
        val initialMinutes = WorldClock().minutesOfDay
        assertEquals(initialMinutes, WorldHeartbeatEngine.DEFAULT_MINUTES_OF_DAY)
        OfficialCharacters.lifeEvents.forEach { event ->
            assertTrue("${event.id} must already have happened at first launch", event.worldMinutesOfDay in 0..initialMinutes)
            assertEquals(event.time, "%02d:%02d".format(event.worldMinutesOfDay / 60, event.worldMinutesOfDay % 60))
        }
    }

    @Test
    fun officialInitialPlanCarriesSavedDateAndSurvivesThePersistedFutureQueueContract() {
        val clock = WorldClock(dateLabel = "10月3日")
        val plan = OfficialDemoWorldPlan.createPlan(clock.dateLabel, clock.minutesOfDay)
        val stored = Json.encodeToString(WorldPlan.serializer(), plan)
        val restored = Json.decodeFromString(WorldPlan.serializer(), stored)
        assertEquals(plan, restored)
        assertEquals(clock.dateLabel, restored.createdWorldDate)
        assertEquals(clock.minutesOfDay, restored.createdMinutes)
        assertEquals(5, restored.actions.size)
        assertEquals(OfficialDemoWorldPlan.actions.map { it.id }, restored.actions.map { it.id })
        assertTrue(restored.actions.all { it.triggerWorldDate == clock.dateLabel && it.triggerMinutes > clock.minutesOfDay })
        assertTrue(WorldPlanValidator.validate(restored, clock, emptyList(), OfficialCharacters.lifeEvents))
        assertFalse(WorldPlanRuntime.needsPlan(clock, restored.actions))
        val call = restored.actions.single { it.type == ScheduledActionType.INCOMING_CALL }
        assertEquals("yeo", call.characterId)
        assertEquals(22 * 60 + 45, call.triggerMinutes)
        assertEquals(LifeEventType.MESSAGE, call.lifeEventType)
        assertEquals("河岸摄影工作室", call.location)
        assertEquals(setOf("yan", "yeo", "noa"), restored.actions.map { it.characterId }.toSet())
        assertEquals(LifeEventType.SLEEP, restored.actions.last().lifeEventType)
    }

    @Test
    fun lateInitialPlanDoesNotReplayMissedEventsOrMoveThemToTomorrow() {
        val date = "10月4日"
        val plan = OfficialDemoWorldPlan.createPlan(date, 22 * 60)
        assertEquals(listOf("official_sched_noa_note", "official_sched_yeo_call", "official_sched_yan_sleep"), plan.actions.map { it.id })
        assertTrue(plan.actions.all { it.triggerWorldDate == date && it.triggerMinutes > 22 * 60 })
        assertTrue(OfficialDemoWorldPlan.createPlan(date, 23 * 60 + 40).actions.isEmpty())
    }

    @Test
    fun rapidCopiesKeepCreatorNotesAndUnknownExtensionFieldsWithDistinctIds() {
        val sourceId = "copy_source_fixture"
        val source = OfficialCharacters.cardYan.copy(data = OfficialCharacters.cardYan.data.copy(
            id = sourceId, creatorNotes = "使用者原始创作说明",
            extensions = buildJsonObject {
                put("external_tool", buildJsonObject { put("unknown_property", "keep") })
            },
        ))
        val copies = mutableListOf<String>()
        val previous = AiluaLocalStore.customCards.value.firstOrNull { it.data.id == sourceId }
        try {
            CharacterRegistry.saveCharacterCard(source)
            repeat(25) {
                val copy = CharacterRegistry.duplicateCharacter(sourceId)
                copies += copy.data.id
                assertEquals(source.data.creatorNotes, copy.data.creatorNotes)
                assertEquals(source.data.extensions, copy.data.extensions)
                assertEquals(source.data.characterBook, copy.data.characterBook)
            }
            assertEquals(25, copies.toSet().size)
        } finally {
            copies.forEach(AiluaLocalStore::deleteCustomCard)
            AiluaLocalStore.deleteCustomCard(sourceId)
            previous?.let(AiluaLocalStore::saveCustomCard)
            CharacterRegistry.refresh()
        }
    }

    @Test
    fun officialRosterIsExplicitWhileLegacyAndCustomRemainSelectable() {
        assertEquals("yan", CharacterRegistry.DEFAULT_CHARACTER_ID)
        assertEquals(listOf("yan", "yeo", "noa"), CharacterRegistry.OFFICIAL_ROMANCE_IDS)
        assertEquals(listOf("yan", "yeo", "noa"), CharacterRegistry.getOfficialRomanceCharacters().map { it.id })
        assertEquals(listOf("yan", "yeo", "noa", "mira", "yuna"), CharacterRegistry.getAllCharacters().take(5).map { it.id })
        assertEquals("小弥", CharacterRegistry.getCharacter("mira").name)
        assertEquals("悠奈", CharacterRegistry.getCharacter("yuna").name)
        assertTrue(WorldData.cardMira.data.systemPrompt.contains("女孩"))
        assertTrue(WorldData.cardYuna.data.systemPrompt.contains("少女"))
        assertEquals("unregistered_identity", CharacterRegistry.getCharacter("unregistered_identity").id)
    }

    @Test
    fun standardV2ExportKeepsDistinctAdultIdentitiesAndRoleBooks() {
        val expected = mapOf("yan" to 26, "yeo" to 22, "noa" to 28)
        OfficialCharacters.cards.forEach { original ->
            val exported = CharacterCardJsonCodec.encode(original)
            val restored = CharacterCardJsonCodec.decode(exported)
            val data = restored.data
            val extension = AiluaCharacterExtensionCodec.read(data)
            assertEquals(original.data.id, data.id)
            assertEquals("chara_card_v2", restored.spec)
            assertEquals("male", extension.identity.gender)
            assertEquals(expected[data.id], extension.identity.age)
            assertEquals("romance", extension.relationship.routeType)
            assertEquals(original.data.description, data.description)
            assertEquals(original.data.exampleMessages, data.exampleMessages)
            assertEquals(original.data.systemPrompt, data.systemPrompt)
            assertEquals(original.data.extensions, data.extensions)
            assertTrue(data.personality.isNotBlank())
            assertTrue(data.scenario.isNotBlank())
            assertTrue(data.firstMessage.isNotBlank())
            assertTrue(data.creatorNotes.isNotBlank())
            assertTrue(data.postHistoryInstructions.isNotBlank())
            assertTrue(data.alternateGreetings.size >= 2)
            assertTrue(data.characterBook!!.entries.all { data.id in it.characterIds })
            assertEquals(original.data.characterBook!!.entries.map { it.id }, data.characterBook!!.entries.map { it.id })
            assertTrue(extension.behavior.flaws.isNotEmpty())
            assertTrue(extension.behavior.boundaries.isNotEmpty())
            assertTrue(extension.behavior.vulnerabilities.isNotEmpty())
            assertTrue(extension.behavior.conflictPatterns.isNotEmpty())
        }
        assertEquals(3, OfficialCharacters.cards.map { AiluaCharacterExtensionCodec.read(it.data).identity.occupation }.distinct().size)
        val registeredIds = WorldData.allCards.map { it.data.id }.toSet()
        OfficialCharacters.cards.forEach { card ->
            val peers = AiluaCharacterExtensionCodec.read(card.data).life.socialCircle
            assertTrue(peers.isNotEmpty())
            assertTrue(peers.all { it in registeredIds && it != card.data.id })
        }
    }

    @Test
    fun savedBuiltinCardOverridesAllProfileEntryPointsWithoutEnforcingGenderOrRoute() {
        val id = "yan"
        val previous = AiluaLocalStore.customCards.value.firstOrNull { it.data.id == id }
        val base = OfficialCharacters.cardYan
        val editedExtension = OfficialCharacters.yanExtension.copy(
            identity = OfficialCharacters.yanExtension.identity.copy(gender = "female", occupation = "测试编辑职业"),
            relationship = OfficialCharacters.yanExtension.relationship.copy(routeType = "friend", initialRelation = "朋友"),
            life = OfficialCharacters.yanExtension.life.copy(workplace = "编辑后的工作室"),
        )
        val edited = base.copy(data = base.data.copy(
            name = "保存后的角色", description = "使用者保存的角色说明", avatarReference = "user_portrait",
            extensions = AiluaCharacterExtensionCodec.write(base.data.extensions, editedExtension),
        ))
        try {
            val savedProfile = CharacterRegistry.saveCharacterCard(edited)
            listOf(savedProfile, CharacterRegistry.getCharacter(id),
                CharacterRegistry.getAllCharacters().first { it.id == id },
                CharacterRegistry.getOfficialRomanceCharacters().first { it.id == id }).forEach { profile ->
                assertEquals("保存后的角色", profile.name)
                assertEquals("使用者保存的角色说明", profile.bio)
                assertEquals("测试编辑职业", profile.title)
                assertEquals("编辑后的工作室", profile.location)
                assertEquals("朋友", profile.relationshipType)
                assertEquals("user_portrait", profile.avatarId)
            }
            val saved = AiluaCharacterExtensionCodec.read(CharacterRegistry.getCard(id)!!.data)
            assertEquals("female", saved.identity.gender)
            assertEquals("friend", saved.relationship.routeType)
        } finally {
            AiluaLocalStore.deleteCustomCard(id)
            previous?.let(AiluaLocalStore::saveCustomCard)
            CharacterRegistry.refresh()
        }
    }

    @Test
    fun newContentHasStableDistinctIdsAndValidCrossAppReferences() {
        val events = OfficialCharacters.lifeEvents
        val eventIds = events.map { it.id }.toSet()
        val diaries = OfficialCharacters.diaryEntries.associateBy { it.id }
        val placeIds = WorldData.virtualPlaces.map { it.id }.toSet()
        assertEquals(events.size, eventIds.size)
        assertTrue(eventIds.all { it.startsWith("official_") })
        assertEquals(MockData.unifiedLifeEvents.size, MockData.unifiedLifeEvents.distinctBy { it.id }.size)
        OfficialCharacters.romanceIds.forEach { id ->
            assertTrue(events.count { it.characterId == id } >= 5)
            assertTrue(OfficialCharacters.profiles.getValue(id).timeline.isNotEmpty())
            assertTrue(OfficialCharacters.diaryEntries.any { it.characterId == id })
            assertTrue(OfficialCharacters.galleryAssets.any { it.characterId == id })
            assertTrue(OfficialCharacters.letters.any { it.characterId == id })
            assertTrue(OfficialCharacters.checkPhoneByCharacter.getValue(id).notes.isNotEmpty())
        }
        events.filter { it.type == LifeEventType.DIARY }.forEach { event ->
            assertEquals(event.characterId, diaries.getValue(event.sourceRefId!!).characterId)
        }
        OfficialCharacters.galleryAssets.forEach { asset ->
            assertEquals(asset.characterId, events.single { it.id == asset.lifeEventId }.characterId)
            assertTrue(asset.locationId in placeIds)
        }
        OfficialCharacters.letters.forEach { letter ->
            assertEquals(letter.characterId, events.single { it.id == letter.relatedLifeEventId }.characterId)
        }
        WorldData.virtualPlaces.forEach { place -> assertTrue(place.connectedPlaceIds.all { it in placeIds }) }
        assertEquals(3, events.filter { it.type == LifeEventType.SOCIAL }.map { it.characterId }.distinct().size)
    }

    @Test
    fun originalLifeDiaryAndTheaterIdentitiesAreNotRelabeledAsNewMen() {
        assertEquals("mira", MockData.unifiedLifeEvents.single { it.id == "pulse_1" }.characterId)
        assertEquals("yuna", MockData.unifiedLifeEvents.single { it.id == "pulse_10" }.characterId)
        assertEquals("noa", MockData.unifiedLifeEvents.single { it.id == "pulse_11" }.characterId)
        assertTrue(MockData.unifiedLifeEvents.single { it.id == "pulse_11" }.description.contains("夜色是世界"))
        assertEquals("mira", MockData.diaryEntries.single { it.id == "diary_1" }.characterId)
        assertEquals("story_rainy_tea", WorldData.rainyNightStory.id)
        assertTrue("mira" in WorldData.rainyNightStory.characterIds)
        assertFalse("yan" in WorldData.rainyNightStory.characterIds)
        assertEquals("mn1", MockData.characterNoa.memories.single().id)
        assertTrue(OfficialCharacters.profiles.getValue("yan").memories.isEmpty())
        assertTrue(OfficialCharacters.profiles.getValue("yeo").memories.isEmpty())
    }
}
