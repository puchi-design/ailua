package com.example.data.codec

import com.example.data.model.CharacterCard
import kotlinx.serialization.json.Json

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

    /**
     * Decodes Character Card V2 JSON string into CharacterCard model.
     * Uses lenient parser and ignores unknown extensions/properties for maximum interoperability.
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
