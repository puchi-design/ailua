package com.example

import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import com.example.data.model.LoreActivationMode
import com.example.data.model.LoreEntry
import com.example.data.model.WorldBook
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCardCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun nestedThirdPartyValuesRetainTypesAndLargeNumberPrecision() {
        val extensions = json.parseToJsonElement(
            """{"vendor":{"array":["text",7,true,null,{"nested":false}],"integer":123456789012345678901234567890,"decimal":0.0100},"legacy":"string value"}""",
        ).jsonObject
        val card = decode(""""extensions":$extensions""")

        assertEquals(extensions, card.data.extensions)
        assertEquals(extensions, encodedData(card)["extensions"])
        assertEquals(extensions, CharacterCardJsonCodec.decode(CharacterCardJsonCodec.encode(card)).data.extensions)
    }

    @Test
    fun numericLexemesSurviveCodecDirectSerializerLocalListsAndEditablePreview() {
        val card = decode(""""extensions":$numericExtensions""")
        val directSerializer = ListSerializer(CharacterCard.serializer())
        val outputs = listOf(
            CharacterCardJsonCodec.encode(card),
            json.encodeToString(CharacterCard.serializer(), card),
            json.parseToJsonElement(json.encodeToString(directSerializer, listOf(card))).jsonArray.single().toString(),
            json.parseToJsonElement(CharacterCardJsonCodec.encodeList(listOf(card))).jsonArray.single().toString(),
        )
        outputs.forEach { output ->
            assertEquals(numericExtensions, json.parseToJsonElement(output).jsonObject["data"]!!.jsonObject["extensions"])
            assertEquals(numericExtensions, CharacterCardJsonCodec.decode(output).data.extensions)
        }
        assertEquals(numericExtensions, CharacterCardJsonCodec.decodeList(CharacterCardJsonCodec.encodeList(listOf(card))).single().data.extensions)
        assertEquals(numericExtensions, json.parseToJsonElement(CharacterCardJsonCodec.encodeJsonElement(card.data.extensions)))
    }

    @Test
    fun bookAndEntryNumericExtensionsSurviveUnrelatedLoreEditsAndPersistence() {
        val originalEntry = standardBook["entries"]!!.jsonArray.single().jsonObject
        val bookJson = JsonObject(standardBook + mapOf(
            "extensions" to numericExtensions,
            "future_numeric_field" to numericExtensions,
            "entries" to JsonArray(listOf(JsonObject(originalEntry + mapOf(
                "extensions" to numericExtensions,
                "future_numeric_field" to numericExtensions,
            )))),
        ))
        val card = decode(""""character_book":$bookJson""")
        assertEquals(bookJson, encodedData(card)["character_book"])
        val book = requireNotNull(card.data.characterBook)
        assertEquals(bookJson, json.parseToJsonElement(json.encodeToString(WorldBook.serializer(), book)))

        val changedEntry = book.entries.single().copy(content = "Only the lore text changed")
        val changedCard = card.copy(data = card.data.copy(characterBook = book.copy(entries = listOf(changedEntry))))
        val restored = CharacterCardJsonCodec.decodeList(CharacterCardJsonCodec.encodeList(listOf(changedCard))).single()
        val exported = encodedData(restored)["character_book"]!!.jsonObject
        assertEquals(numericExtensions, exported["extensions"])
        assertEquals(numericExtensions, exported["future_numeric_field"])
        val exportedEntry = exported["entries"]!!.jsonArray.single().jsonObject
        assertEquals(numericExtensions, exportedEntry["extensions"])
        assertEquals(numericExtensions, exportedEntry["future_numeric_field"])
        assertEquals(JsonPrimitive("Only the lore text changed"), exportedEntry["content"])
        assertEquals(numericExtensions, json.parseToJsonElement(json.encodeToString(LoreEntry.serializer(), changedEntry)).jsonObject["extensions"])
    }

    @Test
    fun ordinaryNameEditRetainsEveryOtherStandardV2Field() {
        val originalData = json.parseToJsonElement("""{
          "name":"Before","description":"Description","personality":"Personality","scenario":"Scenario",
          "first_mes":"Greeting","mes_example":"<START>\\n{{char}}: Example","creator_notes":"Author note",
          "system_prompt":"System","post_history_instructions":"History instruction",
          "alternate_greetings":["First","Second"],"character_book":$standardBook,
          "tags":["tag-a","tag-b"],"creator":"Third-party author","character_version":"2.7",
          "extensions":$numericExtensions
        }""").jsonObject
        val card = CharacterCardJsonCodec.decode("""{"spec":"chara_card_v2","spec_version":"2.0","data":$originalData}""")
        val changed = card.copy(data = card.data.copy(name = "After"))
        val saved = CharacterCardJsonCodec.decodeList(CharacterCardJsonCodec.encodeList(listOf(changed))).single()
        val output = encodedData(saved)
        originalData.filterKeys { it != "name" }.forEach { (key, value) -> assertEquals(key, value, output[key]) }
        assertEquals(JsonPrimitive("After"), output["name"])
        assertEquals("chara_card_v2", saved.spec)
        assertEquals("2.0", saved.specVersion)
    }

    @Test
    fun oldStringMapAndMissingExtensionsRemainReadable() {
        val old = decode(""""extensions":{"voice":"local","note":"old save"}""")
        assertEquals(JsonPrimitive("local"), old.data.extensions["voice"])
        val plain = CharacterCardJsonCodec.decode("""{"spec":"chara_card_v2","spec_version":"2.0","data":{"name":"Plain"}}""")
        assertTrue(plain.data.extensions.isEmpty())
    }

    @Test
    fun standardBookRetainsOptionalFieldsAndUnknownNestedDataExactly() {
        val card = decode(""""character_book":$standardBook""")
        val book = requireNotNull(card.data.characterBook)
        assertEquals(7, book.scanDepth)
        assertEquals(800, book.tokenBudget)
        assertEquals("42", book.entries.single().id)
        assertEquals(listOf("rain"), book.entries.single().keywords)
        assertEquals(listOf("window"), book.entries.single().secondaryKeywords)
        assertEquals(standardBook, encodedData(card)["character_book"])
    }

    @Test
    fun editingOneBookEntryOnlyChangesTheEditedStandardField() {
        val card = decode(""""character_book":$standardBook""")
        val book = requireNotNull(card.data.characterBook)
        val edited = card.copy(data = card.data.copy(characterBook = book.copy(
            entries = listOf(book.entries.single().copy(content = "Updated lore")),
        )))
        val originalEntry = standardBook["entries"]!!.jsonArray.single().jsonObject
        val expected = JsonObject(standardBook + ("entries" to JsonArray(listOf(
            JsonObject(originalEntry + ("content" to JsonPrimitive("Updated lore"))),
        ))))
        assertEquals(expected, encodedData(edited)["character_book"])
    }

    @Test
    fun optionalBookFieldsStayAbsentInsteadOfBecomingInternalDefaults() {
        val minimal = json.parseToJsonElement(
            """{"extensions":{"vendor":{"keep":null}},"entries":[{"keys":[],"content":"Fact","extensions":{},"enabled":true,"insertion_order":17}]}""",
        ).jsonObject
        val card = decode(""""character_book":$minimal""")
        assertEquals(minimal, encodedData(card)["character_book"])
    }

    @Test
    fun legacyInternalBookMigratesToV2WithoutLosingRuntimeMetadata() {
        val legacy = """{
          "id":"legacy-book","name":"Old book","description":"Description",
          "scanDepth":4,"tokenBudget":900,"recursiveScanning":true,
          "entries":[{"id":"entry-one","title":"Old entry","content":"Old lore",
            "keywords":["old"],"secondaryKeywords":["book"],"enabled":false,"priority":23,
            "characterIds":["custom"],"locationIds":["library"],"activationMode":"LOCATION",
            "category":"地点","notes":"Keep this note"}]
        }"""
        val card = decode(""""character_book":$legacy""")
        val encoded = encodedData(card)["character_book"]!!.jsonObject
        assertEquals(JsonPrimitive(4), encoded["scan_depth"])
        assertFalse("scanDepth" in encoded)
        assertFalse("id" in encoded)
        val entryJson = encoded["entries"]!!.jsonArray.single().jsonObject
        assertEquals(JsonArray(listOf(JsonPrimitive("old"))), entryJson["keys"])
        assertFalse("keywords" in entryJson)
        assertFalse("title" in entryJson)
        assertTrue("insertion_order" in entryJson)

        val restored = CharacterCardJsonCodec.decode(CharacterCardJsonCodec.encode(card)).data.characterBook!!
        assertEquals("legacy-book", restored.id)
        assertEquals(4, restored.scanDepth)
        assertEquals(900, restored.tokenBudget)
        assertTrue(restored.recursiveScanning)
        val entry = restored.entries.single()
        assertEquals("entry-one", entry.id)
        assertEquals("Old entry", entry.title)
        assertEquals(listOf("custom"), entry.characterIds)
        assertEquals(listOf("library"), entry.locationIds)
        assertEquals(LoreActivationMode.LOCATION, entry.activationMode)
        assertEquals("地点", entry.category)
        assertEquals("Keep this note", entry.notes)
        assertFalse(entry.enabled)
    }

    @Test
    fun newInternalBookExportsStandardRequiredFieldsAndRoundTripsItsIds() {
        val card = CharacterCard(data = CharacterCardData(name = "Custom", characterBook = WorldBook(
            id = "private-book", name = "Book", description = "",
            entries = listOf(LoreEntry(id = "private-entry", title = "Title", content = "Fact", activationMode = LoreActivationMode.ALWAYS)),
        )))
        val exported = encodedData(card)["character_book"]!!.jsonObject
        val entry = exported["entries"]!!.jsonArray.single().jsonObject
        assertTrue("keys" in entry)
        assertTrue("extensions" in entry)
        assertTrue("insertion_order" in entry)
        assertEquals(JsonPrimitive(true), entry["constant"])
        assertFalse("id" in entry)
        val restored = CharacterCardJsonCodec.decode(CharacterCardJsonCodec.encode(card))
        assertEquals("private-book", restored.data.characterBook!!.id)
        assertEquals("private-entry", restored.data.characterBook!!.entries.single().id)
        assertEquals(LoreActivationMode.ALWAYS, restored.data.characterBook!!.entries.single().activationMode)
    }

    @Test
    fun directGeneratedSerializerPersistenceAlsoPreservesExtensionsAndBook() {
        val card = decode(""""character_book":$standardBook,"extensions":{"ailua":{"schema":9,"future":[null,{"x":true}]},"vendor":{"n":2}}""")
        val serializer = ListSerializer(CharacterCard.serializer())
        val saved = json.encodeToString(serializer, listOf(card))
        val restored = json.decodeFromString(serializer, saved).single()
        assertEquals(card.data.extensions, restored.data.extensions)
        assertEquals(standardBook, encodedData(restored)["character_book"])
    }

    @Test
    fun localListLoadingIsolatesMalformedCardsAndRetainsValidNeighbors() {
        val first = decode(""""id":"first","extensions":{"vendor":{"retain":[1,false,null]}}""")
        val second = decode(""""id":"second","character_book":$standardBook""")
        val saved = "[${CharacterCardJsonCodec.encode(first)},{\"data\":{}},${CharacterCardJsonCodec.encode(second)}]"
        val restored = CharacterCardJsonCodec.decodeList(saved)
        assertEquals(listOf("first", "second"), restored.map { it.data.id })
        val roundTrip = CharacterCardJsonCodec.decodeList(CharacterCardJsonCodec.encodeList(restored))
        assertEquals(first.data.extensions, roundTrip.first().data.extensions)
        assertEquals(standardBook, encodedData(roundTrip.last())["character_book"])
    }

    private fun decode(fields: String): CharacterCard = CharacterCardJsonCodec.decode(
        """{"spec":"chara_card_v2","spec_version":"2.0","data":{"name":"Imported",$fields}}""",
    )

    private fun encodedData(card: CharacterCard): JsonObject =
        json.parseToJsonElement(CharacterCardJsonCodec.encode(card)).jsonObject["data"]!!.jsonObject

    private val numericExtensions = json.parseToJsonElement("""{
      "vendor": {
        "integer":123456789012345678901234567890,
        "decimal":0.0100,
        "nested":[-0,1E+09,1e400,-1e-400,{"fraction":-0.000,"text":"1e400","escaped":"quote\" and slash\\ and newline\n"}],
        "boolean":true,"empty":null
      },
      "legacy":"string value"
    }""").jsonObject

    private val standardBook = json.parseToJsonElement("""{
      "name":"Imported book","description":"A V2 book","scan_depth":7,"token_budget":800,
      "recursive_scanning":true,"extensions":{"vendor":{"nested":[true,null,3]}},
      "future_book":{"retain":"value"},
      "entries":[{"keys":["rain"],"content":"Rain at the window","extensions":{"third_party":[1,{"keep":false}]},
        "enabled":true,"insertion_order":12,"case_sensitive":true,"name":"Rain","priority":13,"id":42,
        "comment":"Author comment","selective":true,"secondary_keys":["window"],"constant":false,
        "position":"after_char","future_entry":{"retain":[null,0.25]}}]
    }""").jsonObject
}
