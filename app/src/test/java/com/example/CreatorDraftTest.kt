package com.example

import androidx.compose.runtime.saveable.SaverScope
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.ui.creator.CreatorDraft
import com.example.ui.creator.CreatorCardSaver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorDraftTest {
    @Test fun advancedJsonRenderingRetainsNumericLexemes() {
        val original = numericCard()
        val draft = CreatorDraft(original)
        assertEquals(original.data.extensions, Json.parseToJsonElement(draft.jsonText("extensions")))
        assertEquals(wireData(original)["character_book"], Json.parseToJsonElement(draft.jsonText("character_book")))
        val applied = draft.editJson("extensions", draft.jsonText("extensions"))
            .editJson("character_book", draft.jsonText("character_book"))
            .applyJsonEdits().build()
        assertEquals(wireData(original), wireData(applied))
    }

    @Test fun draftSaverKeepsUnsavedFieldsAndUnfinishedJsonThroughRecreation() {
        val draft = CreatorDraft(numericCard())
            .editData { it.copy(name = "Unsaved name", avatarReference = "content://photos/unsaved") }
            .editJson("extensions", "{\n  \"vendor\": [123456789012345678901234567890, ")
            .editJson("alternate_greetings", "[\"new opening\"]\n")
            .editJson("character_book", "  ")
        val saved = with(CreatorDraft.Saver) { SAVE_SCOPE.save(draft) }!!
        val restored = CreatorDraft.Saver.restore(saved)!!
        assertEquals(wireData(draft.card), wireData(restored.card))
        assertEquals(draft.jsonEdits, restored.jsonEdits)
        assertEquals("Unsaved name", restored.card.data.name)
        assertEquals("content://photos/unsaved", restored.card.data.avatarReference)
        assertTrue(runCatching { restored.build() }.isFailure)
        val repaired = restored.editJson("extensions", CreatorDraft(draft.card).jsonText("extensions")).build()
        assertEquals(draft.card.data.extensions, repaired.data.extensions)
        assertEquals(listOf("new opening"), repaired.data.alternateGreetings)
        assertNull(repaired.data.characterBook)
    }

    @Test fun pendingSafSnapshotSaverPreservesFullCardAndAbsentSnapshot() {
        val card = numericCard()
        val saved = with(CreatorCardSaver) { SAVE_SCOPE.save(card) }!!
        val restored = CreatorCardSaver.restore(saved)!!
        assertEquals(wireData(card), wireData(restored))
        assertEquals(card.data.id, restored.data.id)
        assertEquals(card.data.extensions, restored.data.extensions)
        assertNull(with(CreatorCardSaver) { SAVE_SCOPE.save(null) })
    }

    @Test fun editingOccupationPreservesUntouchedMalformedKnownFieldsAfterExport() {
        val original = CharacterCardJsonCodec.decode(CARD).let { card -> card.copy(data = card.data.copy(
            extensions = Json.parseToJsonElement("""{
                "vendor":{"keep":[null,false,7]},
                "ailua":{"schema":1,
                    "identity":{"gender":"female","age":{"vendor":"adult"},"height_cm":-2},
                    "speech":false,"relationship":{"jealousy":8},
                    "behavior":{"care_patterns":["listen",null,{"future":true}]},"life":["opaque"]}
            }""").jsonObject
        )) }
        val edited = CreatorDraft(original).editExtension {
            it.copy(identity = it.identity.copy(occupation = "Painter"))
        }.build()
        val exported = CharacterCardJsonCodec.decode(CharacterCardJsonCodec.encode(edited))
        val expected = Json.parseToJsonElement("""{
            "vendor":{"keep":[null,false,7]},
            "ailua":{"schema":1,
                "identity":{"gender":"female","age":{"vendor":"adult"},"height_cm":-2,"occupation":"Painter"},
                "speech":false,"relationship":{"jealousy":8},
                "behavior":{"care_patterns":["listen",null,{"future":true}]},"life":["opaque"]}
        }""").jsonObject
        assertEquals(expected, exported.data.extensions)
        assertEquals(original.data.extensions, CreatorDraft(original).editExtension { it }.build().data.extensions)
    }

    @Test fun firstBehaviorEditAddsOnlyChangedLeafAndExplicitSchema() {
        val original = CharacterCardJsonCodec.decode(CARD)
        val edited = CreatorDraft(original).editExtension {
            it.copy(identity = it.identity.copy(occupation = "Painter"))
        }.build()
        assertEquals(JsonObject(original.data.extensions + ("ailua" to Json.parseToJsonElement("""{
            "schema":1,"identity":{"occupation":"Painter"}
        }"""))), edited.data.extensions)
    }

    @Test fun clearingKnownMapEntryKeepsOpaqueSiblings() {
        val original = CharacterCardJsonCodec.decode(CARD).let { card -> card.copy(data = card.data.copy(
            extensions = Json.parseToJsonElement("""{"ailua":{"schema":1,"visual":{
                "expressions":{"smile":"smile.webp","future":{"layers":[1,2]}}
            }}}""").jsonObject
        )) }
        val edited = CreatorDraft(original).editExtension {
            it.copy(visual = it.visual.copy(expressions = emptyMap()))
        }.build()
        assertEquals(Json.parseToJsonElement("""{"ailua":{"schema":1,"visual":{
            "expressions":{"future":{"layers":[1,2]}}
        }}}"""), edited.data.extensions)
    }

    @Test fun renameAndModeSwitchPreserveStandardFieldsBookAndThirdPartyExtensions() {
        val original = CharacterCardJsonCodec.decode(CARD)
        val edited = CreatorDraft(original).editData { it.copy(name = "Edited name") }.applyJsonEdits().build()
        val before = wireData(original)
        val after = wireData(edited)
        listOf("post_history_instructions", "alternate_greetings", "character_book", "extensions", "mes_example", "creator_notes").forEach {
            assertEquals(it, before[it], after[it])
        }
        assertEquals("Edited name", edited.data.name)
        assertFalse(edited.data.extensions.containsKey("ailua"))
    }

    @Test fun normalBehaviorEditKeepsThirdPartyAndUnknownAiluaKeys() {
        val original = CharacterCardJsonCodec.decode(CARD).let { card ->
            card.copy(data = card.data.copy(extensions = Json.parseToJsonElement("""{
                "other":{"nested":[true,4,{"key":"value"}]},
                "ailua":{"schema":1,"identity":{"gender":"female","future_identity":"keep"},
                    "speech":{"tone":["direct"],"future_speech":{"keep":1}},"future_group":[1,2]}
            }""").jsonObject))
        }
        val edited = CreatorDraft(original).editExtension {
            it.copy(identity = it.identity.copy(gender = "nonbinary"), relationship = it.relationship.copy(routeType = "custom_route"))
        }.build()
        assertEquals(original.data.extensions["other"], edited.data.extensions["other"])
        val before = original.data.extensions.getValue("ailua").jsonObject
        val after = edited.data.extensions.getValue("ailua").jsonObject
        assertEquals(before["future_group"], after["future_group"])
        assertEquals(before.getValue("identity").jsonObject["future_identity"], after.getValue("identity").jsonObject["future_identity"])
        assertEquals(before.getValue("speech").jsonObject["future_speech"], after.getValue("speech").jsonObject["future_speech"])
        assertEquals("nonbinary", AiluaCharacterExtensionCodec.read(edited.data).identity.gender)
        assertEquals("custom_route", AiluaCharacterExtensionCodec.read(edited.data).relationship.routeType)
    }

    @Test fun futureSchemaCanBeRenamedAndDuplicatedWithoutDowngrade() {
        val original = CharacterCardJsonCodec.decode(CARD).let { card -> card.copy(data = card.data.copy(
            extensions = Json.parseToJsonElement("""{"ailua":{"schema":9,"identity":{"unfamiliar":true}},"third":[false,3]}""").jsonObject
        )) }
        val draft = CreatorDraft(original).editData { it.copy(name = "Future card") }
        assertFalse(AiluaCharacterExtensionCodec.canEdit(draft.card.data))
        assertTrue(runCatching { draft.editExtension { it.copy(schema = 1) } }.isFailure)
        val before = AiluaCharacterExtensionCodec.read(original.data)
        assertEquals(original.data.extensions, AiluaCharacterExtensionCodec.writeChanges(
            original.data.extensions, before, before.copy(identity = before.identity.copy(occupation = "Painter")),
        ))
        assertEquals(original.data.extensions, draft.build().data.extensions)
        val copy = draft.duplicate().build()
        assertNotEquals(original.data.id, copy.data.id)
        assertEquals(original.data.extensions, copy.data.extensions)
        assertEquals(original.data.characterBook, copy.data.characterBook)
        assertEquals(original.data.postHistoryInstructions, copy.data.postHistoryInstructions)
    }

    @Test fun invalidAdvancedJsonStaysDraftAndCannotOverwriteOriginal() {
        val original = CharacterCardJsonCodec.decode(CARD)
        val invalid = CreatorDraft(original).editJson("extensions", "{ unfinished")
        assertTrue(runCatching { invalid.build() }.isFailure)
        assertEquals(original, invalid.card)
        assertEquals("{ unfinished", invalid.jsonText("extensions"))
        assertTrue(runCatching { CreatorDraft(original).editJson("alternate_greetings", "[1]").build() }.isFailure)
    }

    @Test fun advancedBookAndGreetingsEditsKeepUnrelatedFieldsAndExtensions() {
        val original = CharacterCardJsonCodec.decode(CARD)
        val book = """{"name":"Changed lore","entries":[],"extensions":{"nested":{"x":[true,2]}}}"""
        val edited = CreatorDraft(original)
            .editJson("character_book", book)
            .editJson("alternate_greetings", "[\"another opening\",\"second\"]")
            .applyJsonEdits().build()
        assertEquals(Json.parseToJsonElement(book), wireData(edited)["character_book"])
        assertEquals(listOf("another opening", "second"), edited.data.alternateGreetings)
        assertEquals(original.data.extensions, edited.data.extensions)
        assertEquals(original.data.postHistoryInstructions, edited.data.postHistoryInstructions)
    }

    private fun wireData(card: com.example.data.model.CharacterCard): JsonObject =
        Json.parseToJsonElement(CharacterCardJsonCodec.encode(card)).jsonObject.getValue("data").jsonObject

    private fun numericCard() = CharacterCardJsonCodec.decode(CARD.replace(
        "\"vendor_option\":{\"nested\":true}", "\"vendor_option\":$NUMERIC_VENDOR"
    )).let { card ->
        card.copy(data = card.data.copy(extensions = Json.parseToJsonElement(
            """{"vendor":$NUMERIC_VENDOR,"old_string":"legacy"}"""
        ).jsonObject))
    }

    private companion object {
        val SAVE_SCOPE = object : SaverScope {
            override fun canBeSaved(value: Any) = value is String
        }
        const val NUMERIC_VENDOR = """{"integer":123456789012345678901234567890,"decimal":0.0100,"array":[null,false,7]}"""
        val CARD = """{
            "spec":"chara_card_v2","spec_version":"2.0","data":{
                "id":"imported_card","name":"Original","description":"Description","personality":"Personality",
                "scenario":"Scene","first_mes":"Hello","mes_example":"Example","creator_notes":"Notes",
                "system_prompt":"System","post_history_instructions":"Preserve this PHI",
                "alternate_greetings":["Opening one","Opening two"],"tags":["custom"],"creator":"Tester","character_version":"7",
                "character_book":{"name":"External book","entries":[{"id":4,"keys":["rain"],"content":"Lore",
                    "enabled":true,"insertion_order":2,"extensions":{"opaque":[false,7]}}],"vendor_option":{"nested":true}},
                "extensions":{"vendor":{"nested":[true,3,{"x":"keep"}]},"old_string":"legacy"}
            }
        }"""
    }
}
