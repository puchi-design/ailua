package com.example.data.codec

import com.example.data.model.CharacterCard
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

data class CharacterCardValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

object CharacterCardJsonCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val storageJson = Json(json) { prettyPrint = false }

    /**
     * Decodes Character Card V2 JSON string into CharacterCard model.
     * Extension objects retain arbitrary JSON values; the book serializer also preserves
     * unsupported V2 book/entry properties. Unmodeled root/data properties are ignored.
     */
    fun decode(jsonString: String): CharacterCard {
        return json.decodeFromString(CharacterCard.serializer(), jsonString)
    }

    /**
     * Encodes CharacterCard model into standard V2 formatted JSON string.
     */
    fun encode(card: CharacterCard): String {
        return json.encodeToString(CharacterCard.serializer(), card)
    }

    /** For editable JSON previews: retain numeric lexemes before the user edits a field. */
    fun encodeJsonElement(value: JsonElement): String =
        json.encodeToString(LosslessJsonElementSerializer, value)

    /** A malformed saved card must not prevent unrelated cards from loading. */
    fun decodeList(jsonString: String): List<CharacterCard> {
        val values = storageJson.parseToJsonElement(jsonString) as? JsonArray
            ?: throw SerializationException("Saved character cards must be a JSON array")
        return values.mapNotNull { value ->
            runCatching { storageJson.decodeFromJsonElement(CharacterCard.serializer(), value) }.getOrNull()
        }
    }

    fun encodeList(cards: List<CharacterCard>): String =
        storageJson.encodeToString(ListSerializer(CharacterCard.serializer()), cards)

    /**
     * Validates CharacterCard data contract.
     */
    fun validate(card: CharacterCard): CharacterCardValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (card.data.name.isBlank()) {
            errors.add("角色名称不能为空 (data.name is required)")
        }

        if (card.data.firstMessage.isBlank()) {
            warnings.add("初次见面台词 (first_mes) 建议填写，以便伴生初见唤醒")
        }

        if (card.spec != "chara_card_v2") {
            warnings.add("规格标头 '${card.spec}' 非标准 chara_card_v2，已按 V2 结构兼容映射")
        }

        return CharacterCardValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
