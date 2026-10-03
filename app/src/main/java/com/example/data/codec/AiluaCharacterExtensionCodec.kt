package com.example.data.codec

import com.example.data.model.AiluaBehavior
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.AiluaIdentity
import com.example.data.model.AiluaInitiative
import com.example.data.model.AiluaLife
import com.example.data.model.AiluaRelationship
import com.example.data.model.AiluaSpeech
import com.example.data.model.AiluaVisual
import com.example.data.model.CharacterCardData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Reads a tolerant typed view while keeping the card's original JSON as the source of truth. */
object AiluaCharacterExtensionCodec {
    private val json = Json { encodeDefaults = true }

    fun read(data: CharacterCardData): AiluaCharacterExtension =
        readOrNull(data) ?: AiluaCharacterExtension()

    fun readOrNull(data: CharacterCardData): AiluaCharacterExtension? {
        val root = data.extensions["ailua"] as? JsonObject ?: return null
        if (!supports(root)) return null
        val identity = root["identity"] as? JsonObject
        val relationship = root["relationship"] as? JsonObject
        val behavior = root["behavior"] as? JsonObject
        val speech = root["speech"] as? JsonObject
        val initiative = root["initiative"] as? JsonObject
        val life = root["life"] as? JsonObject
        val visual = root["visual"] as? JsonObject
        return AiluaCharacterExtension(
            identity = AiluaIdentity(
                gender = identity.text("gender"),
                age = identity.integer("age")?.takeIf { it >= 0 },
                occupation = identity.text("occupation"),
                heightCm = identity.integer("height_cm")?.takeIf { it > 0 },
                birthday = identity.text("birthday"),
            ),
            relationship = AiluaRelationship(
                routeType = relationship.text("route_type"),
                initialRelation = relationship.text("initial_relation"),
                affectionStyle = relationship.text("affection_style"),
                attachmentStyle = relationship.text("attachment_style"),
                jealousy = relationship.ratio("jealousy", 0.0),
                possessiveness = relationship.ratio("possessiveness", 0.0),
                physicalDistance = relationship.text("physical_distance"),
                confessionThreshold = relationship.ratio("confession_threshold", 0.5),
                progressionStyle = relationship.text("progression_style").ifBlank { "balanced" },
            ),
            behavior = AiluaBehavior(
                coreDesire = behavior.text("core_desire"),
                flaws = behavior.strings("flaws"),
                blindSpots = behavior.strings("blind_spots"),
                boundaries = behavior.strings("boundaries"),
                vulnerabilities = behavior.strings("vulnerabilities"),
                carePatterns = behavior.strings("care_patterns"),
                flirtPatterns = behavior.strings("flirt_patterns"),
                jealousyPatterns = behavior.strings("jealousy_patterns"),
                conflictPatterns = behavior.strings("conflict_patterns"),
            ),
            speech = AiluaSpeech(
                sentenceLength = speech.text("sentence_length"),
                emojiFrequency = speech.text("emoji_frequency"),
                petNames = speech.strings("pet_names"),
                verbalTics = speech.strings("verbal_tics"),
                forbiddenPhrases = speech.strings("forbidden_phrases"),
                tone = speech.strings("tone"),
            ),
            initiative = AiluaInitiative(
                messageFrequency = initiative.text("message_frequency"),
                callFrequency = initiative.text("call_frequency"),
                photoFrequency = initiative.text("photo_frequency"),
                momentFrequency = initiative.text("moment_frequency"),
                preferredTriggers = initiative.strings("preferred_triggers"),
                letterFrequency = initiative.text("letter_frequency"),
                triggerWeights = (initiative?.get("trigger_weights") as? JsonObject).orEmpty().mapNotNull { (key, value) ->
                    (value as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() && it in 0.0..1.0 }?.let { key to it }
                }.toMap(),
                maxTextBurst = initiative.integer("max_text_burst")?.coerceIn(1, 3) ?: 1,
            ),
            life = AiluaLife(
                home = life.text("home"),
                workplace = life.text("workplace"),
                sleepWindow = life.text("sleep_window"),
                hobbies = life.strings("hobbies"),
                socialCircle = life.strings("social_circle"),
            ),
            visual = AiluaVisual(
                assetPack = visual.text("asset_pack"),
                defaultOutfit = visual.text("default_outfit"),
                avatar = visual.text("avatar"),
                portrait = visual.text("portrait"),
                expressions = (visual?.get("expressions") as? JsonObject).orEmpty()
                    .mapNotNull { (key, value) -> value.stringOrNull()?.let { key to it } }.toMap(),
            ),
        )
    }

    fun canEdit(data: CharacterCardData): Boolean = canEdit(data.extensions)

    fun hasFutureSchema(data: CharacterCardData): Boolean =
        ((data.extensions["ailua"] as? JsonObject)?.get("schema") as? JsonPrimitive)
            ?.intOrNull?.let { it > 1 } == true

    /** Unknown schemas and unsupported root shapes are deliberately left untouched. */
    fun write(extensions: JsonObject, value: AiluaCharacterExtension): JsonObject {
        if (!canEdit(extensions) || value.schema != 1) return extensions
        val encoded = json.encodeToJsonElement(AiluaCharacterExtension.serializer(), value) as JsonObject
        val previous = extensions["ailua"] as? JsonObject ?: JsonObject(emptyMap())
        return JsonObject(extensions + ("ailua" to merge(previous, encoded)))
    }

    /**
     * Ordinary editing applies only changed typed leaves to the original wire data.
     * A tolerant read may hide fields this version cannot understand; those raw values
     * must survive edits to unrelated fields, without being replaced by fallback defaults.
     */
    fun writeChanges(
        extensions: JsonObject,
        before: AiluaCharacterExtension,
        after: AiluaCharacterExtension,
    ): JsonObject {
        if (!canEdit(extensions) || before.schema != 1 || after.schema != 1 || before == after) return extensions
        val previous = extensions["ailua"] as? JsonObject
        val beforeJson = json.encodeToJsonElement(AiluaCharacterExtension.serializer(), before) as JsonObject
        val afterJson = json.encodeToJsonElement(AiluaCharacterExtension.serializer(), after) as JsonObject
        val changed = patchChanges(previous ?: JsonObject(emptyMap()), beforeJson, afterJson)
        val result = if (previous == null) JsonObject(changed + ("schema" to JsonPrimitive(1))) else changed
        return JsonObject(extensions + ("ailua" to result))
    }

    private fun patchChanges(original: JsonObject, before: JsonObject, after: JsonObject): JsonObject = JsonObject(
        original.toMutableMap().apply {
            (before.keys + after.keys).forEach { key ->
                val oldValue = before[key]
                val newValue = after[key]
                if (oldValue != newValue) {
                    when {
                        newValue == null -> remove(key)
                        oldValue is JsonObject && newValue is JsonObject -> put(
                            key, patchChanges(get(key) as? JsonObject ?: JsonObject(emptyMap()), oldValue, newValue),
                        )
                        else -> put(key, newValue)
                    }
                }
            }
        },
    )

    private fun canEdit(extensions: JsonObject): Boolean {
        val value = extensions["ailua"] ?: return true
        return value is JsonObject && supports(value)
    }

    private fun supports(value: JsonObject): Boolean =
        "schema" !in value || (value["schema"] as? JsonPrimitive)?.intOrNull == 1

    private fun merge(original: JsonObject, updates: JsonObject): JsonObject = JsonObject(
        original.toMutableMap().apply {
            updates.forEach { (key, update) ->
                val old = get(key)
                put(key, if (old is JsonObject && update is JsonObject) merge(old, update) else update)
            }
        },
    )

    private fun JsonElement.stringOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject?.text(key: String): String = this?.get(key)?.stringOrNull().orEmpty()

    private fun JsonObject?.integer(key: String): Int? = (this?.get(key) as? JsonPrimitive)?.intOrNull

    private fun JsonObject?.ratio(key: String, fallback: Double): Double =
        (this?.get(key) as? JsonPrimitive)?.doubleOrNull
            ?.takeIf { it.isFinite() && it in 0.0..1.0 } ?: fallback

    private fun JsonObject?.strings(key: String): List<String> =
        (this?.get(key) as? JsonArray)?.mapNotNull { it.stringOrNull() }.orEmpty()
}
