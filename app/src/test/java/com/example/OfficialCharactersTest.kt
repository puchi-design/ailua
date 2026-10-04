package com.example

import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.engine.OfficialDemoWorldPlan
import com.example.data.engine.ScheduledActionType
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldPlanRuntime
import com.example.data.engine.WorldPlanValidator
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.LegacyMockData
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
        assertEquals(6, restored.actions.size)
        assertEquals(OfficialDemoWorldPlan.actions.map { it.id }, restored.actions.map { it.id })
        assertTrue(restored.actions.all { it.triggerWorldDate == clock.dateLabel && it.triggerMinutes > clock.minutesOfDay })
        assertTrue(WorldPlanValidator.validate(restored, clock, emptyList(), OfficialCharacters.lifeEvents))
        assertFalse(WorldPlanRuntime.needsPlan(clock, restored.actions))
        val call = restored.actions.single { it.type == ScheduledActionType.INCOMING_CALL }
        assertEquals("zhoujianye", call.characterId)
        assertEquals(22 * 60 + 45, call.triggerMinutes)
        assertEquals(LifeEventType.MESSAGE, call.lifeEventType)
        assertEquals("回声排练室", call.location)
        assertEquals(OfficialCharacters.sixRosterIds.toSet(), restored.actions.map { it.characterId }.toSet())
        assertEquals(LifeEventType.SLEEP, restored.actions.last().lifeEventType)
    }

    @Test
    fun lateInitialPlanDoesNotReplayMissedEventsOrMoveThemToTomorrow() {
        val date = "10月4日"
        val plan = OfficialDemoWorldPlan.createPlan(date, 22 * 60)
        assertEquals(OfficialDemoWorldPlan.actions.filter { it.triggerTimeMinutes > 22 * 60 }.map { it.id }, plan.actions.map { it.id })
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
        assertEquals("hewenchuan", CharacterRegistry.DEFAULT_CHARACTER_ID)
        assertEquals(listOf("hewenchuan", "zhoujianye", "peixubai"), CharacterRegistry.OFFICIAL_ROMANCE_IDS)
        assertEquals(listOf("mira", "yuna", "noa"), CharacterRegistry.OFFICIAL_FRIENDSHIP_IDS)
        assertEquals(OfficialCharacters.romanceIds, CharacterRegistry.getOfficialRomanceCharacters().map { it.id })
        assertEquals(OfficialCharacters.friendshipIds, CharacterRegistry.getOfficialFriendshipCharacters().map { it.id })
        assertEquals(OfficialCharacters.sixRosterIds, CharacterRegistry.getAllCharacters().take(6).map { it.id })
        assertEquals("苏晚宁", CharacterRegistry.getCharacter("mira").name)
        assertEquals("许朝颜", CharacterRegistry.getCharacter("yuna").name)
        assertEquals("宋知微", CharacterRegistry.getCharacter("noa").name)
        assertEquals("friendship", AiluaCharacterExtensionCodec.read(WorldData.cardNoa.data).relationship.routeType)
        assertEquals("female", AiluaCharacterExtensionCodec.read(WorldData.cardNoa.data).identity.gender)
        assertEquals("yan", CharacterRegistry.getCard("yan")!!.data.id)
        assertEquals("yeo", CharacterRegistry.getCard("yeo")!!.data.id)
        assertEquals("unregistered_identity", CharacterRegistry.getCharacter("unregistered_identity").id)
    }

    @Test
    fun standardV2ExportKeepsDistinctAdultIdentitiesAndRoleBooks() {
        val expected = mapOf("hewenchuan" to 28, "zhoujianye" to 23, "peixubai" to 27, "mira" to 25, "yuna" to 24, "noa" to 26)
        OfficialCharacters.cards.forEach { original ->
            val exported = CharacterCardJsonCodec.encode(original)
            val restored = CharacterCardJsonCodec.decode(exported)
            val data = restored.data
            val extension = AiluaCharacterExtensionCodec.read(data)
            assertEquals(original.data.id, data.id)
            assertEquals("chara_card_v2", restored.spec)
            assertEquals(if (data.id in OfficialCharacters.romanceIds) "male" else "female", extension.identity.gender)
            assertEquals(expected[data.id], extension.identity.age)
            assertEquals(if (data.id in OfficialCharacters.romanceIds) "romance" else "friendship", extension.relationship.routeType)
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
        assertEquals(6, OfficialCharacters.cards.map { AiluaCharacterExtensionCodec.read(it.data).identity.occupation }.distinct().size)
        val registeredIds = WorldData.allCards.map { it.data.id }.toSet()
        OfficialCharacters.cards.forEach { card ->
            val peers = AiluaCharacterExtensionCodec.read(card.data).life.socialCircle
            assertTrue(peers.isNotEmpty())
            assertTrue(peers.all { it in registeredIds && it != card.data.id })
        }
    }

    @Test
    fun savedBuiltinCardOverridesAllProfileEntryPointsWithoutEnforcingGenderOrRoute() {
        val id = "hewenchuan"
        val previous = AiluaLocalStore.customCards.value.firstOrNull { it.data.id == id }
        val base = OfficialCharacters.cardHeWenchuan
        val originalExtension = AiluaCharacterExtensionCodec.read(base.data)
        val editedExtension = originalExtension.copy(
            identity = originalExtension.identity.copy(gender = "female", occupation = "测试编辑职业"),
            relationship = originalExtension.relationship.copy(routeType = "friend", initialRelation = "朋友"),
            life = originalExtension.life.copy(workplace = "编辑后的工作室"),
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
        assertTrue(eventIds.all { it.startsWith("six_") })
        assertEquals(MockData.unifiedLifeEvents.size, MockData.unifiedLifeEvents.distinctBy { it.id }.size)
        OfficialCharacters.sixRosterIds.forEach { id ->
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
        assertEquals(6, events.filter { it.type == LifeEventType.SOCIAL }.map { it.characterId }.distinct().size)
    }

    @Test
    fun originalLifeDiaryAndTheaterIdentitiesAreNotRelabeledAsNewMen() {
        assertEquals("mira", LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_1" }.characterId)
        assertEquals("yuna", LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_10" }.characterId)
        assertEquals("noa", LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_11" }.characterId)
        assertTrue(LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_11" }.description.contains("夜色是世界"))
        assertEquals("mira", LegacyMockData.diaryEntries.single { it.id == "diary_1" }.characterId)
        assertEquals("story_rainy_tea", WorldData.rainyNightStory.id)
        assertTrue("mira" in WorldData.rainyNightStory.characterIds)
        assertFalse("yan" in WorldData.rainyNightStory.characterIds)
        assertEquals("mn1", LegacyMockData.characterNoa.memories.single().id)
        assertTrue(OfficialCharacters.profiles.getValue("hewenchuan").memories.isEmpty())
        assertTrue(OfficialCharacters.profiles.getValue("zhoujianye").memories.isEmpty())
    }
}
