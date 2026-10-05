package com.example.data.chat.rich

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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

    fun decode(raw: String?): List<RichMessagePayload> {
        if (raw.isNullOrBlank()) return emptyList()
        val entries = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonArray
            ?: return listOf(unavailableMessage())
        return entries.map { entry ->
            runCatching { json.decodeFromJsonElement(RichMessagePayload.serializer(), entry) }
                .getOrElse {
                    val label = ((entry as? JsonObject)?.get("label") as? JsonPrimitive)
                        ?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)?.take(160)
                    unavailableMessage(label)
                }
        }
    }

    /** Change one known card without rewriting unknown siblings or future JSON fields. */
    fun withStatus(raw: String?, index: Int, status: RichMessageStatus): String? {
        val entries = runCatching { json.parseToJsonElement(raw ?: return null) }
            .getOrNull() as? JsonArray ?: return null
        val original = entries.getOrNull(index) as? JsonObject ?: return null
        val updated = JsonObject(original + ("status" to JsonPrimitive(status.name)))
        return json.encodeToString(JsonElement.serializer(), JsonArray(entries.mapIndexed { position, entry ->
            if (position == index) updated else entry
        }))
    }

    private fun unavailableMessage(label: String? = null) = RichMessagePayload(
        RichMessageType.TEXT,
        label = if (label == null) "消息卡片暂不可用" else "消息卡片暂不可用 · $label",
    )
}
