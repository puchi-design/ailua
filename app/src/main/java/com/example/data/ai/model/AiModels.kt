package com.example.data.ai.model

/**
 * P3C-1 AI provider contract.
 *
 * Deliberately minimal: roles, text content, streaming flag, sampling knobs.
 * tools / tool_calls / vision / audio / reasoning / response_format are NOT here —
 * they get added only when a real capability lands (spec §11 HARD RULE: minimal contract).
 *
 * AiStreamEvent terminal states:
 * - Completed(text): the model produced a full reply
 * - Failed(error): HTTP / network / protocol failure — NEVER a fabricated character reply
 * - Cancelled: the underlying call was cancelled while the collection coroutine was still alive
 */
enum class AiRole {
    SYSTEM,
    USER,
    ASSISTANT,
}

data class AiMessage(
    val role: AiRole,
    val content: String,
)

data class AiChatRequest(
    val model: String,
    val messages: List<AiMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
)

data class AiUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)

sealed interface AiStreamEvent {
    data object Started : AiStreamEvent

    data class Delta(val text: String) : AiStreamEvent

    data class Usage(val usage: AiUsage) : AiStreamEvent

    data class Completed(val text: String) : AiStreamEvent

    data class Failed(val error: AiProviderError) : AiStreamEvent

    data object Cancelled : AiStreamEvent
}
