package com.example.data.chat.model

/** Speaker of a logical chat turn (P3C-3 §5). */
enum class ChatTurnRole {
    USER,
    ASSISTANT,
}

/**
 * ChatTurn — one logical message position inside a session (P3C-3 §5).
 *
 * [position] is a stable per-session counter starting at 0, assigned inside the
 * writing transaction and NEVER derived from timestamps, so ordering cannot be
 * disturbed when several rows share the same epoch millisecond.
 *
 * An assistant turn owns one or more [ChatVariant]s; [activeVariantId] points at
 * the currently selected one. Regeneration appends a variant to the existing
 * turn (P3C-3 §11) instead of creating a new turn.
 */
data class ChatTurn(
    val id: String,
    val sessionId: String,
    val role: ChatTurnRole,
    val position: Int,
    val activeVariantId: String?,
    val createdAtEpochMs: Long,
)
