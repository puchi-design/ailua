package com.example.ui.creator

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterCardData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.UUID

/** Both editor modes retain the complete loaded card. Untouched wire data survives Save and Export. */
data class CreatorDraft(
    val card: CharacterCard,
    val jsonEdits: Map<String, String> = emptyMap()
) {
    fun editData(change: (CharacterCardData) -> CharacterCardData): CreatorDraft = copy(card = card.copy(data = change(card.data)))

    fun editExtension(change: (AiluaCharacterExtension) -> AiluaCharacterExtension): CreatorDraft {
        require(AiluaCharacterExtensionCodec.canEdit(card.data)) {
            "这张卡的 AILUA 行为格式暂不支持编辑，请在高级模式保留原始扩展。"
        }
        val before = AiluaCharacterExtensionCodec.read(card.data)
        val after = change(before)
        return editData { it.copy(extensions = AiluaCharacterExtensionCodec.writeChanges(it.extensions, before, after)) }
    }

    fun editJson(field: String, text: String): CreatorDraft {
        require(field in JSON_FIELDS)
        return copy(jsonEdits = jsonEdits + (field to text))
    }

    fun jsonText(field: String): String = jsonEdits[field] ?: run {
        val value = Json.parseToJsonElement(CharacterCardJsonCodec.encode(card)).jsonObject["data"]!!.jsonObject[field]
        if (value == null || value == JsonNull) "" else CharacterCardJsonCodec.encodeJsonElement(value)
    }

    /** Invalid raw JSON remains visible in Advanced mode and never silently erases stored fields. */
    fun build(): CharacterCard {
        if (jsonEdits.isEmpty()) return card
        val root = Json.parseToJsonElement(CharacterCardJsonCodec.encode(card)).jsonObject
        val data = root.getValue("data").jsonObject.toMutableMap()
        jsonEdits.forEach { (field, text) ->
            val value = if (text.isBlank()) when (field) {
                "character_book" -> JsonNull
                "alternate_greetings" -> JsonArray(emptyList())
                else -> JsonObject(emptyMap())
            } else Json.parseToJsonElement(text)
            when (field) {
                "extensions" -> require(value is JsonObject) { "Extensions 必须是 JSON object" }
                "character_book" -> require(value == JsonNull || value is JsonObject) { "Lorebook 必须是 JSON object 或留空" }
                "alternate_greetings" -> require(value is JsonArray && value.all { it is JsonPrimitive && it.isString }) {
                    "Alternate Greetings 必须是字符串数组"
                }
            }
            data[field] = value
        }
        return CharacterCardJsonCodec.decode(JsonObject(root + ("data" to JsonObject(data))).toString())
    }

    fun applyJsonEdits(): CreatorDraft = CreatorDraft(build())

    fun duplicate(): CreatorDraft {
        val source = build()
        return CreatorDraft(source.copy(data = source.data.copy(
            id = "${source.data.id.ifBlank { "custom" }}_copy_${UUID.randomUUID()}",
            name = "${source.data.name} (副本)"
        )))
    }

    companion object {
        private val JSON_FIELDS = setOf("character_book", "extensions", "alternate_greetings")

        /** Raw, possibly unfinished JSON is saved separately from the last valid card. */
        val Saver: Saver<CreatorDraft, Any> = listSaver<CreatorDraft, String>(
            save = { draft ->
                listOf(CharacterCardJsonCodec.encode(draft.card)) +
                    draft.jsonEdits.flatMap { (field, text) -> listOf(field, text) }
            },
            restore = { saved ->
                CreatorDraft(
                    card = CharacterCardJsonCodec.decode(saved.first()),
                    jsonEdits = saved.drop(1).chunked(2).associate { it[0] to it[1] }
                )
            }
        )

        fun fresh() = CreatorDraft(CharacterCard(data = CharacterCardData(
            id = "custom_${UUID.randomUUID()}", name = "", avatarReference = "yan"
        )))
    }
}

/** SAF may recreate the Activity before returning; retain the exact pending card snapshot. */
internal val CreatorCardSaver: Saver<CharacterCard?, String> = Saver(
    save = { card -> card?.let(CharacterCardJsonCodec::encode) },
    restore = CharacterCardJsonCodec::decode
)
