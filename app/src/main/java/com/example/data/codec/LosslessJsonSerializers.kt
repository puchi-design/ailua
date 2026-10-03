package com.example.data.codec

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/**
 * kotlinx.serialization 1.7.x's default JsonLiteral serializer converts parsed numeric text
 * through Long/ULong/Double. Keep arbitrary JSON numbers as validated literal text instead,
 * including values outside Double's range, trailing fractional zeros, and exponent spelling.
 */
object LosslessJsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): JsonElement = JsonElement.serializer().deserialize(decoder)

    override fun serialize(encoder: Encoder, value: JsonElement) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("Lossless JSON requires a JSON encoder")
        jsonEncoder.encodeJsonElement(preserveNumberLiterals(value))
    }
}

/** Property serializer also used by direct CharacterCard.serializer() persistence. */
object LosslessJsonObjectSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): JsonObject = JsonObject.serializer().deserialize(decoder)

    override fun serialize(encoder: Encoder, value: JsonObject) = LosslessJsonElementSerializer.serialize(encoder, value)
}

private val jsonNumber = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")

@OptIn(ExperimentalSerializationApi::class)
private fun preserveNumberLiterals(value: JsonElement): JsonElement = when (value) {
    is JsonObject -> JsonObject(value.mapValues { (_, nested) -> preserveNumberLiterals(nested) })
    is JsonArray -> JsonArray(value.map(::preserveNumberLiterals))
    is JsonPrimitive -> {
        // Only grammar-checked numbers use the raw API. Strings, null, booleans and lenient
        // parser tokens retain normal JSON escaping/encoding; no arbitrary text is injected.
        if (!value.isString && jsonNumber.matches(value.content)) JsonUnquotedLiteral(value.content) else value
    }
}
