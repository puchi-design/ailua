package com.example

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.context.CharacterContext
import com.example.data.context.CharacterSelectionPolicy
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.LegacyMockData
import com.example.data.mock.LegacyOfficialCharacters
import com.example.data.mock.MockData
import com.example.data.mock.OfficialCharacters
import com.example.data.mock.WorldData
import com.example.data.registry.CharacterIdentityAliases
import com.example.data.registry.CharacterRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SixCharacterCompatibilityTest {
    @Test fun publicSlugsNeverCreateDuplicateFemaleIdentitiesOrRemapSavedSelections() {
        assertEquals(mapOf("mira" to "suwanning", "yuna" to "xuchaoyan", "noa" to "songzhiwei"), CharacterIdentityAliases.publicSlugByLegacyId)
        assertEquals(6, OfficialCharacters.cards.size)
        assertEquals(OfficialCharacters.sixRosterIds, OfficialCharacters.cards.map { it.data.id })
        listOf("mira", "yuna", "noa", "yan", "yeo", "custom_saved").forEach { id ->
            assertEquals(id, CharacterSelectionPolicy.resolve(id, hasLegacyData = true))
        }
        CharacterIdentityAliases.publicSlugByLegacyId.forEach { (id, slug) ->
            assertNull(CharacterRegistry.getCard(slug))
            assertEquals(id, CharacterRegistry.getCharacter(id).id)
            assertTrue(CharacterRegistry.getCard(id)!!.data.avatarReference.contains("characters/$slug/avatar_main.webp"))
        }
    }

    @Test fun currentFemaleProfilesAndPromptHintsDoNotResolveToTheArchivedMalePersona() {
        val expected = mapOf("mira" to "苏晚宁", "yuna" to "许朝颜", "noa" to "宋知微")
        expected.forEach { (id, name) ->
            val profile = CharacterRegistry.getCharacter(id)
            val runtime = CharacterRuntimeResolver.resolve(id)
            assertEquals(name, profile.name)
            assertEquals(name, runtime.name)
            assertEquals("female", runtime.identity.gender)
            assertEquals("friendship", runtime.relationship.routeType)
            assertFalse(runtime.relationship.romanceEnabled)
            assertEquals(0.0, runtime.relationship.jealousy, 0.0)
            assertTrue(runtime.behavior.flirtPatterns.isEmpty())
            assertTrue(runtime.behavior.jealousyPatterns.isEmpty())
            assertEquals(runtime.identity.occupation, profile.title)
            assertEquals(runtime.life.workplace, profile.location)
            assertTrue(profile.memories.isEmpty())
        }
        assertFalse(CharacterRegistry.getCharacter("noa").title.contains("档案"))
        assertEquals("male", AiluaCharacterExtensionCodec.read(LegacyOfficialCharacters.cardNoa.data).identity.gender)
        assertEquals("noa", LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_11" }.characterId)
        assertEquals("mn1", LegacyMockData.characterNoa.memories.single().id)
    }

    @Test fun sameIdFemaleEditRetainsCustomNameRouteAssetAndUnknownExtensionData() {
        val id = "noa"
        val previous = AiluaLocalStore.customCards.value.lastOrNull { it.data.id == id }
        val base = OfficialCharacters.cardNoa
        val edited = base.copy(data = base.data.copy(name = "用户自定义角色", avatarReference = "content://user/portrait",
            extensions = Json.parseToJsonElement("""{"ailua":{"schema":1,"identity":{"gender":"arbitrary"},"relationship":{"route_type":"custom"},"speech":{"tone":["自己的语气"]}},"vendor":{"large":123456789012345678901234567890,"future":[true,null,{"keep":"verbatim"}]}}""").jsonObject))
        val wireBefore = CharacterCardJsonCodec.encode(edited)
        try {
            CharacterRegistry.saveCharacterCard(edited)
            val resolved = CharacterRegistry.getCharacter(id)
            assertEquals(id, resolved.id)
            assertEquals("用户自定义角色", resolved.name)
            assertEquals("content://user/portrait", resolved.avatarId)
            assertEquals("arbitrary", CharacterRuntimeResolver.resolve(id).identity.gender)
            assertEquals("custom", CharacterRuntimeResolver.resolve(id).relationship.routeType)
            assertEquals(wireBefore, CharacterCardJsonCodec.encode(CharacterRegistry.getCard(id)!!))
            assertFalse(CharacterRegistry.isUnmodifiedBuiltIn(id))
        } finally {
            AiluaLocalStore.deleteCustomCard(id)
            previous?.let(AiluaLocalStore::saveCustomCard)
            CharacterRegistry.refresh()
        }
    }

    @Test fun archiveIsDirectlyReadableAndOnlySavedArchiveSelectionEntersVisibleRoster() {
        val selection = CharacterContext.currentId()
        try {
            CharacterContext.select("hewenchuan")
            assertEquals(OfficialCharacters.sixRosterIds, CharacterRegistry.getAllCharacters().take(6).map { it.id })
            assertFalse(CharacterRegistry.getAllCards().any { it.data.id == "yan" || it.data.id == "yeo" })
            assertEquals(LegacyOfficialCharacters.cardYan, CharacterRegistry.getCard("yan"))
            assertEquals(LegacyOfficialCharacters.cardYeo, CharacterRegistry.getCard("yeo"))
            CharacterContext.select("yan")
            assertEquals("yan", CharacterContext.currentCharacter().id)
            assertTrue(CharacterRegistry.getAllCharacters().any { it.id == "yan" })
            assertFalse(CharacterRegistry.getAllCharacters().any { it.id == "yeo" })
        } finally {
            CharacterContext.select(selection)
            CharacterRegistry.refresh()
        }
    }

    @Test fun freshContentPoolContainsNoOldRomanceOrRelabeledHistoricalFacts() {
        assertTrue(MockData.unifiedLifeEvents.all { it.id.startsWith("six_") && it.characterId in OfficialCharacters.sixRosterIds })
        assertTrue(MockData.diaryEntries.all { it.id.startsWith("six_") })
        assertTrue(OfficialCharacters.profiles.values.all { it.memories.isEmpty() })
        assertTrue(WorldData.defaultWorldBook.entries.none { it.id.startsWith("official_") || it.id == "lore_silver_wrapper" })
        assertEquals("story_rainy_tea", WorldData.rainyNightStory.id)
        assertEquals("node_1", WorldData.rainyNightStory.initialNodeId)
        assertTrue(WorldData.rainyNightStory.nodes.containsKey("node_1"))
        assertEquals("six_story_street_open", WorldData.defaultTheaterStory.id)
        assertEquals(OfficialCharacters.sixRosterIds.toSet(), WorldData.defaultTheaterStory.characterIds.toSet())
        assertTrue(WorldData.defaultTheaterStory.nodes.values.flatMap { it.choices }.all { it.bondIncrease == 0 })
        assertEquals("mira", LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_1" }.characterId)
        assertEquals("yuna", LegacyMockData.unifiedLifeEvents.single { it.id == "pulse_10" }.characterId)
    }

    @Test fun sixExportedStandardCardsMatchRuntimeAndPreserveAllExtensionGroups() {
        val directory = listOf(File("../docs/characters"), File("docs/characters")).first { it.isDirectory }
        OfficialCharacters.cards.forEach { card ->
            val slug = CharacterIdentityAliases.publicSlug(card.data.id)
            val exported = CharacterCardJsonCodec.decode(File(directory, "$slug.json").readText())
            assertEquals(CharacterCardJsonCodec.encode(card), CharacterCardJsonCodec.encode(exported))
            assertEquals(8, Regex("<START>").findAll(exported.data.exampleMessages).count())
            val groups = exported.data.extensions["ailua"]!!.jsonObject.keys
            assertTrue(groups.containsAll(listOf("identity", "relationship", "behavior", "speech", "initiative", "life", "visual")))
            val wire = Json.parseToJsonElement(CharacterCardJsonCodec.encode(exported)).jsonObject["data"]!!.jsonObject
            assertTrue(wire["character_book"]!!.jsonObject.containsKey("scan_depth"))
            assertFalse(wire["character_book"]!!.jsonObject.containsKey("scanDepth"))
        }
    }
}
