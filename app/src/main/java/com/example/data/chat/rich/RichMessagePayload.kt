package com.example.data.chat.rich

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Content of one chat variant. A TEXT payload preserves its place beside cards. */
@Serializable
enum class RichMessageType { TEXT, QUOTE, STICKER, LOCATION, GIFT, RED_PACKET, TRANSFER }

@Serializable
enum class RichMessageStatus { PENDING, OPENED, RECEIVED, ACCEPTED, DECLINED, CANCELED }

/** Virtual story interactions only; [amount] never represents a spendable balance. */
@Serializable
data class RichMessagePayload(
    val type: RichMessageType,
    val label: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val status: RichMessageStatus? = null,
    val referenceMessageId: String? = null,
    val locationName: String? = null,
    val iconKey: String? = null,
)

/** Malformed or future payloads must never make a historical chat unreadable. */
object RichMessageCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(payloads: List<RichMessagePayload>): String =
        json.encodeToString(kotlinx.serialization.builtins.ListSerializer(RichMessagePayload.serializer()), payloads)

    fun decode(raw: String?): List<RichMessagePayload> = raw?.let {
        runCatching {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(RichMessagePayload.serializer()), it)
        }.getOrDefault(emptyList())
    } ?: emptyList()
}
