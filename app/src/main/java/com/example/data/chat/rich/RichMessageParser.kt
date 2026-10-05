package com.example.data.chat.rich

/** Plain text remains in [content]; [payloads] preserves text/card order for the chat UI. */
data class RichMessageParseResult(
    val content: String,
    val payloads: List<RichMessagePayload>,
)

/** Parses a small, bounded text protocol without trusting model output as structured data. */
object RichMessageParser {
    private val directive = Regex("\\[[^\\[\\]\\r\\n]{1,180}]")
    val stickerKeys = setOf("偷看", "无语", "开心", "害羞", "生气", "晚安", "抱抱", "委屈")

    fun parse(raw: String): RichMessageParseResult {
        val plain = StringBuilder()
        val parts = mutableListOf<RichMessagePayload>()
        var cursor = 0
        var richCount = 0
        var financeCount = 0
        fun text(value: String) {
            if (value.isEmpty()) return
            plain.append(value)
            if (parts.lastOrNull()?.type == RichMessageType.TEXT) {
                val last = parts.removeAt(parts.lastIndex)
                parts += last.copy(label = last.label.orEmpty() + value)
            } else parts += RichMessagePayload(RichMessageType.TEXT, label = value)
        }
        for (match in directive.findAll(raw)) {
            text(raw.substring(cursor, match.range.first))
            cursor = match.range.last + 1
            val candidate = parseDirective(match.value.substring(1, match.value.length - 1))
            if (candidate == null) {
                text(match.value)
                continue
            }
            val finance = candidate.type == RichMessageType.RED_PACKET || candidate.type == RichMessageType.TRANSFER
            if (richCount >= 2 || (finance && financeCount >= 1)) {
                // Keep the model's words visible when a directive exceeds the per-turn limit.
                text(match.value)
                continue
            }
            parts += candidate
            richCount++
            if (finance) financeCount++
        }
        text(raw.substring(cursor))
        return RichMessageParseResult(
            content = if (richCount == 0) raw else plain.toString().trim(),
            payloads = if (richCount == 0) emptyList() else parts.toList(),
        )
    }

    private fun parseDirective(body: String): RichMessagePayload? {
        val fields = body.split(':', limit = 3)
        val kind = fields.firstOrNull()?.trim().orEmpty()
        val value = fields.getOrNull(1)?.trim().orEmpty()
        if (value.isEmpty() || value.length > 100) return null
        return when (kind) {
            "红包", "转账" -> {
                if (!Regex("(?:0|[1-9][0-9]{0,3})(?:\\.[0-9]{1,2})?").matches(value)) return null
                val amount = value.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 && it <= 9999.0 } ?: return null
                val label = fields.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() && it.length <= 100 }
                RichMessagePayload(
                    type = if (kind == "红包") RichMessageType.RED_PACKET else RichMessageType.TRANSFER,
                    label = label,
                    amount = amount,
                    currency = "¥",
                    status = RichMessageStatus.PENDING,
                )
            }
            "位置" -> RichMessagePayload(RichMessageType.LOCATION, label = value, locationName = value)
            "礼物" -> RichMessagePayload(RichMessageType.GIFT, label = value, status = RichMessageStatus.PENDING)
            "表情" -> if (value in stickerKeys) RichMessagePayload(RichMessageType.STICKER, label = value, iconKey = value) else null
            else -> null
        }
    }
}
