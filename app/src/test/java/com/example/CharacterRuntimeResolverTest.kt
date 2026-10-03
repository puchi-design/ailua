package com.example

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.runtime.InitiativeTrigger
import com.example.data.character.runtime.RuntimeSource
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.OfficialCharacters
import com.example.data.model.CharacterCardData
import com.example.data.registry.CharacterRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class CharacterRuntimeResolverTest {
    @Test fun standardCardGetsConservativePolicyWithoutInventingGenderOrRomance() {
        val data = CharacterCardData(id = "standard", name = "Friend", personality = "gentle",
            scenario = "family meeting", tags = listOf("romance", "male", "quiet"))
        val runtime = CharacterRuntimeResolver.resolve(data)
        assertEquals(RuntimeSource.STANDARD, runtime.source)
        assertEquals("", runtime.identity.gender)
        assertEquals("companion", runtime.relationship.routeType)
        assertFalse(runtime.relationship.romanceEnabled)
        assertEquals("medium", runtime.initiative.messageFrequency)
        assertEquals("low", runtime.initiative.callFrequency)
        assertEquals("low", runtime.initiative.photoFrequency)
        assertEquals("00:00-08:00", runtime.life.sleepWindow)
        assertEquals(data.tags, runtime.speech.tone)
        assertFalse(InitiativeTrigger.RECENT_CONFLICT in runtime.initiative.triggerWeights)
    }

    @Test fun arbitraryGenderRoutesAndUnknownJsonSurviveProjection() {
        for (route in listOf("romance", "friendship", "family", "companion", "my_custom_route")) {
            val card = CharacterCardJsonCodec.decode("""{"data":{"id":"custom","name":"Custom",
                "extensions":{"third_party":{"number":123456789012345678901234567890},
                "ailua":{"schema":1,"identity":{"gender":"nonbinary"},"relationship":{"route_type":"$route"}}}}}""")
            val original = CharacterCardJsonCodec.encode(card)
            val runtime = CharacterRuntimeResolver.resolve(card.data)
            assertEquals("nonbinary", runtime.identity.gender)
            assertEquals(route, runtime.relationship.routeType)
            assertEquals(route == "romance", runtime.relationship.romanceEnabled)
            assertEquals(original, CharacterCardJsonCodec.encode(card))
        }
    }

    @Test fun futureSchemaDoesNotLeakInstructionsAndIsNotModified() {
        val data = CharacterCardData(id = "future", name = "Future", extensions = Json.parseToJsonElement(
            """{"ailua":{"schema":9,"identity":{"gender":"secret"},"initiative":{"message_frequency":"high"}},"vendor":[1,true]}"""
        ).jsonObject)
        val before = data.extensions.toString()
        val runtime = CharacterRuntimeResolver.resolve(data)
        assertEquals(RuntimeSource.FUTURE, runtime.source)
        assertEquals("", runtime.identity.gender)
        assertEquals("medium", runtime.initiative.messageFrequency)
        assertEquals(before, data.extensions.toString())
    }

    @Test fun currentSavedOverrideWinsWithoutCachedOfficialPolicy() {
        val prior = AiluaLocalStore.customCards.value.firstOrNull { it.data.id == "yan" }
        val card = OfficialCharacters.cardYan
        try {
            val changed = card.copy(data = card.data.copy(name = "Edited Yan", extensions = Json.parseToJsonElement(
                """{"ailua":{"schema":1,"identity":{"gender":"custom"},"relationship":{"route_type":"friendship"},"initiative":{"message_frequency":"none"}}}"""
            ).jsonObject))
            CharacterRegistry.saveCharacterCard(changed)
            val runtime = CharacterRuntimeResolver.resolve("yan")
            assertEquals("Edited Yan", runtime.name)
            assertEquals(RuntimeSource.IMPORTED, runtime.source)
            assertEquals("custom", runtime.identity.gender)
            assertFalse(runtime.relationship.romanceEnabled)
            assertEquals(0.0, runtime.initiative.messageProbability, 0.0)
        } finally {
            AiluaLocalStore.deleteCustomCard("yan")
            prior?.let(AiluaLocalStore::saveCustomCard)
            CharacterRegistry.refresh()
        }
    }

    @Test fun malformedFieldsAreBoundedAndOnlyKnownTriggerKeysAreUsed() {
        val data = CharacterCardData(id = "bounded", name = "Bounded", extensions = Json.parseToJsonElement(
            """{"ailua":{"schema":1,"life":{"sleep_window":"99:99-25:80"},
            "initiative":{"max_text_burst":100,"call_frequency":"garbage","trigger_weights":{"rain":0.7,"morning":0,"unknown":1,"birthday":5}}}}"""
        ).jsonObject)
        val runtime = CharacterRuntimeResolver.resolve(data)
        assertEquals("00:00-08:00", runtime.life.sleepWindow)
        assertEquals(3, runtime.initiative.maxTextBurst)
        assertEquals("low", runtime.initiative.callFrequency)
        assertEquals(mapOf(InitiativeTrigger.RAIN to .7, InitiativeTrigger.MORNING to 0.0), runtime.initiative.triggerWeights)
    }

    @Test fun officialCardsAndMissingIdResolveWithoutSelectingAnotherCompanion() {
        OfficialCharacters.cards.forEach { assertEquals(RuntimeSource.OFFICIAL, CharacterRuntimeResolver.resolve(it.data).source) }
        val missing = CharacterRuntimeResolver.resolve("not-installed-character")
        assertEquals("not-installed-character", missing.characterId)
        assertEquals(RuntimeSource.STANDARD, missing.source)
        assertFalse(missing.relationship.romanceEnabled)
    }
}
