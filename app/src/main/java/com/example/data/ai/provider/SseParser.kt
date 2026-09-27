package com.example.data.ai.provider

import com.example.data.ai.model.AiUsage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * One parsed SSE event: the joined `data:` payload of a single message event.
 * Multi-line `data:` fields are joined with "\n" per the SSE specification.
 */
data class SseEvent(val data: String)

/**
 * Pure Kotlin SSE state machine (correction #3).
 *
 * Fed with ARBITRARY chunks — partial chunks, several events in one chunk, LF /
 * CRLF / CR line endings and a CR split across two chunks all parse to the same
 * events. It never throws on malformed input: framing only decides where events
 * end, JSON validity is decided later by [SseDataDecoder].
 *
 * Special handling:
 * - ":" comment lines are ignored
 * - a line without ":" is a field with an empty value
 * - one leading space after ":" is stripped (SSE rule)
 * - [flush] at EOF emits a final event even when the stream ends without "\n\n"
 *   (HTTP body EOF without `data: [DONE]` must not lose the last delta)
 */
class SseParser {

    private val line = StringBuilder()
    private val dataLines = mutableListOf<String>()
    private var swallowLf = false

    /** Feeds [chunk] and returns every SSE event completed inside it. */
    fun feed(chunk: String): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        for (char in chunk) {
            if (swallowLf) {
                swallowLf = false
                if (char == '\n') continue
            }
            when (char) {
                '\n' -> endLine(events)
                '\r' -> {
                    swallowLf = true
                    endLine(events)
                }
                else -> line.append(char)
            }
        }
        return events
    }

    /** Call at EOF: returns an event for unfinished trailing data, if any. */
    fun flush(): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        if (line.isNotEmpty()) endLine(events)
        if (dataLines.isNotEmpty()) {
            events += SseEvent(dataLines.joinToString("\n"))
            dataLines.clear()
        }
        return events
    }

    private fun endLine(events: MutableList<SseEvent>) {
        val raw = line.toString()
        line.setLength(0)
        if (raw.isEmpty()) {
            if (dataLines.isNotEmpty()) {
                events += SseEvent(dataLines.joinToString("\n"))
                dataLines.clear()
            }
            return
        }
        if (raw.startsWith(":")) return
        val colon = raw.indexOf(':')
        val field = if (colon >= 0) raw.substring(0, colon) else raw
        var value = if (colon >= 0) raw.substring(colon + 1) else ""
        if (value.startsWith(" ")) value = value.substring(1)
        when (field) {
            "data" -> dataLines.add(value)
            // "event" / "id" / "retry" are transport metadata — ignored for OpenAI-compatible streams.
        }
    }
}

/** Result of decoding one SSE `data:` payload. */
sealed interface SseData {
    /** A content-bearing chunk (may also carry usage). `content` is null when absent. */
    data class Chunk(val content: String?, val usage: AiUsage?) : SseData

    /** `data: [DONE]` */
    data object Done : SseData

    /** Empty payload (keep-alive / `data:` with no value) — silently ignored. */
    data object Blank : SseData

    /** Unparseable or protocol-violating payload. MUST NOT crash and MUST NOT pass as a normal completion. */
    data class Malformed(val reason: String) : SseData
}

/**
 * Decodes a single SSE data payload of an OpenAI-compatible `/chat/completions`
 * stream. Never throws.
 */
object SseDataDecoder {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(data: String): SseData {
        val trimmed = data.trim()
        if (trimmed.isEmpty()) return SseData.Blank
        if (trimmed == DONE_MARKER) return SseData.Done

        val root = try {
            json.parseToJsonElement(trimmed)
        } catch (_: Exception) {
            return SseData.Malformed("invalid JSON payload")
        }
        if (root !is JsonObject) return SseData.Malformed("payload is not a JSON object")

        val error = root["error"]
        if (error != null && error !is JsonNull) {
            return SseData.Malformed("provider error: ${error.toString().take(ERROR_SNIPPET)}")
        }

        val usage = (root["usage"] as? JsonObject)?.let { usageObject -> parseUsage(usageObject) }
        val firstChoice = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
        val messageObject = (firstChoice?.get("delta") as? JsonObject)
            ?: (firstChoice?.get("message") as? JsonObject)
        val contentElement: JsonElement? = messageObject?.get("content")
        val content: String? = when {
            contentElement == null || contentElement is JsonNull -> null
            contentElement is JsonPrimitive -> contentElement.content
            else -> return SseData.Malformed("unsupported content payload")
        }
        return SseData.Chunk(content, usage)
    }

    private fun parseUsage(usageObject: JsonObject): AiUsage? {
        val prompt = intOrNull(usageObject, "prompt_tokens")
        val completion = intOrNull(usageObject, "completion_tokens")
        val total = intOrNull(usageObject, "total_tokens")
        if (prompt == null && completion == null && total == null) return null
        return AiUsage(promptTokens = prompt, completionTokens = completion, totalTokens = total)
    }

    private fun intOrNull(element: JsonObject, key: String): Int? {
        val primitive = element[key] as? JsonPrimitive ?: return null
        return primitive.content.toIntOrNull()
    }

    private const val DONE_MARKER = "[DONE]"
    private const val ERROR_SNIPPET = 200
}
