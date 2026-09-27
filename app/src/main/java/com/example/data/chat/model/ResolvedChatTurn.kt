package com.example.data.chat.model

/**
 * ResolvedChatTurn — one logical turn JOINed with its active variant in a
 * single read (P3C-4 §7).
 *
 * Produced by `selectResolvedTurns` (one SQL LEFT JOIN per session) so the UI
 * and prompt assembly never fan out into N per-turn variant queries.
 *
 * [activeVariant] is `null` only when the turn has no active variant row
 * (defensive — appends always create one). [variantCount] is the turn's total
 * variant count for the ‹ 1 / n › picker; [activeVariant] carries only the
 * currently selected variant's content/status (always `activeVariantId`).
 */
data class ResolvedChatTurn(
    val id: String,
    val sessionId: String,
    val role: ChatTurnRole,
    val position: Int,
    val activeVariantId: String?,
    val createdAtEpochMs: Long,
    val activeVariant: ChatVariant?,
    val variantCount: Int,
)
