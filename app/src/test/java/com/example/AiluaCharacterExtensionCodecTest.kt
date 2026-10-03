package com.example

import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.model.AiluaBehavior
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.AiluaIdentity
import com.example.data.model.AiluaInitiative
import com.example.data.model.AiluaLife
import com.example.data.model.AiluaRelationship
import com.example.data.model.AiluaSpeech
import com.example.data.model.AiluaVisual
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiluaCharacterExtensionCodecTest {
    private val codec = AiluaCharacterExtensionCodec

    @Test
    fun ordinaryCardsUseNeutralDefaultsWithoutInventingARomanceRoute() {
        val card = CharacterCardData(name = "Imported")
        assertNull(codec.readOrNull(card))
        assertEquals(AiluaCharacterExtension(), codec.read(card))
        assertEquals("", codec.read(card).identity.gender)
        assertEquals("", codec.read(card).relationship.routeType)
        assertTrue(codec.canEdit(card))
    }

    @Test
    fun allSevenGroupsRoundTripUsingSnakeCaseAndFreeGenderAndRouteValues() {
        val value = AiluaCharacterExtension(
            identity = AiluaIdentity("nonbinary", 30, "Researcher", 175, "03-12"),
            relationship = AiluaRelationship("custom_route", "colleague", "direct", "secure", 0.2, 0.1, "ask_first", 0.7),
            behavior = AiluaBehavior("Understand the world", listOf("stubborn"), listOf("overthinking"), listOf("ask first"), listOf("failure"), listOf("listen"), listOf("tease"), listOf("ask"), listOf("talk")),
            speech = AiluaSpeech("short", "rare", listOf("friend"), listOf("well"), listOf("never"), listOf("precise")),
            initiative = AiluaInitiative("medium", "low", "high", "low", listOf("weather_rain")),
            life = AiluaLife("Home", "Library", "00:30-07:30", listOf("reading"), listOf("yuna")),
            visual = AiluaVisual("future_pack", "daily", "avatar/a.webp", "portrait/a.webp", mapOf("smile" to "smile.webp")),
        )
        val extensions = codec.write(JsonObject(emptyMap()), value)
        val data = CharacterCardData(name = "Custom", extensions = extensions)
        assertEquals(value, codec.read(data))
        val root = extensions["ailua"]!!.jsonObject
        assertEquals(JsonPrimitive(175), root["identity"]!!.jsonObject["height_cm"])
        assertEquals(JsonPrimitive("custom_route"), root["relationship"]!!.jsonObject["route_type"])
        assertTrue("core_desire" in root["behavior"]!!.jsonObject)
        assertTrue("message_frequency" in root["initiative"]!!.jsonObject)
        val restored = CharacterCardJsonCodec.decode(CharacterCardJsonCodec.encode(CharacterCard(data = data)))
        assertEquals(value, codec.read(restored.data))
    }

    @Test
    fun malformedFieldsFallbackIndividuallyWithoutDiscardingOtherGroups() {
        val card = data("""{"ailua":{"schema":1,
          "identity":{"gender":"female","age":{"bad":true},"height_cm":-2},
          "relationship":{"route_type":"friendship","jealousy":8,"possessiveness":"NaN","confession_threshold":null},
          "behavior":{"care_patterns":["listen",null,2,"make tea"]},
          "speech":false,"initiative":{"photo_frequency":"high"},
          "life":{"home":"Custom home"},"visual":{"expressions":{"smile":"smile.webp","broken":[]}}
        }}""")
        val value = codec.read(card)
        assertEquals("female", value.identity.gender)
        assertNull(value.identity.age)
        assertNull(value.identity.heightCm)
        assertEquals("friendship", value.relationship.routeType)
        assertEquals(0.0, value.relationship.jealousy, 0.0)
        assertEquals(0.0, value.relationship.possessiveness, 0.0)
        assertEquals(0.5, value.relationship.confessionThreshold, 0.0)
        assertEquals(listOf("listen", "make tea"), value.behavior.carePatterns)
        assertEquals("high", value.initiative.photoFrequency)
        assertEquals("Custom home", value.life.home)
        assertEquals(mapOf("smile" to "smile.webp"), value.visual.expressions)
    }

    @Test
    fun editsPreserveThirdPartyAndUnknownAiluaNestedKeys() {
        val card = data("""{"third_party":{"array":[null,true,3]},"ailua":{"schema":1,
          "unknown_group":{"future":null},"identity":{"gender":"female","future_identity":{"x":3}},
          "behavior":{"care_patterns":["listen"],"future_behavior":[1,2]},
          "visual":{"expressions":{"future_expression":{"nested":true}}}
        }}""")
        val old = codec.read(card)
        val result = codec.write(card.extensions, old.copy(identity = old.identity.copy(occupation = "Painter")))
        assertEquals(card.extensions["third_party"], result["third_party"])
        val before = card.extensions["ailua"]!!.jsonObject
        val after = result["ailua"]!!.jsonObject
        assertEquals(before["unknown_group"], after["unknown_group"])
        assertEquals(before["identity"]!!.jsonObject["future_identity"], after["identity"]!!.jsonObject["future_identity"])
        assertEquals(before["behavior"]!!.jsonObject["future_behavior"], after["behavior"]!!.jsonObject["future_behavior"])
        assertEquals(before["visual"]!!.jsonObject["expressions"], after["visual"]!!.jsonObject["expressions"])
        assertEquals("Painter", codec.read(card.copy(extensions = result)).identity.occupation)
    }

    @Test
    fun futureSchemasAreNotInterpretedOrDowngradedOnWrite() {
        val card = data("""{"vendor":[false],"ailua":{"schema":4,"identity":{"gender":"future-value"},"future":[null,7]}}""")
        assertTrue(codec.hasFutureSchema(card))
        assertFalse(codec.canEdit(card))
        assertNull(codec.readOrNull(card))
        assertEquals(AiluaCharacterExtension(), codec.read(card))
        assertEquals(card.extensions, codec.write(card.extensions, AiluaCharacterExtension(identity = AiluaIdentity(gender = "male"))))
        assertEquals(card.extensions, CharacterCardJsonCodec.decode(CharacterCardJsonCodec.encode(CharacterCard(data = card))).data.extensions)
    }

    @Test
    fun malformedOrUnknownRootShapesArePreservedWhenEditingIsUnsupported() {
        listOf("""{"ailua":"legacy opaque value"}""", """{"ailua":{"schema":"unrecognized","raw":true}}""").forEach { raw ->
            val card = data(raw)
            assertFalse(codec.canEdit(card))
            assertNull(codec.readOrNull(card))
            assertEquals(card.extensions, codec.write(card.extensions, AiluaCharacterExtension()))
        }
    }

    @Test
    fun absentSchemaUsesV1DefaultsWithoutMutatingTheSource() {
        val card = data("""{"ailua":{"identity":{"age":26},"life":{"hobbies":["books"]}}}""")
        val original = card.extensions
        assertEquals(26, codec.read(card).identity.age)
        assertEquals(listOf("books"), codec.read(card).life.hobbies)
        assertTrue(codec.canEdit(card))
        assertEquals(original, card.extensions)
    }

    private fun data(extensions: String): CharacterCardData = CharacterCardData(
        name = "Imported", extensions = Json.parseToJsonElement(extensions).jsonObject,
    )
}
