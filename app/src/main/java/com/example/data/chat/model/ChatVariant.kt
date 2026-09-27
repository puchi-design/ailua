package com.example.data.chat.model

/** Lifecycle of a single generated reply variant (P3C-3 §5). */
enum class VariantStatus {
    STREAMING,
    COMPLETE,
    FAILED,
    CANCELLED,
}

/**
 * ChatVariant — one concrete rendering of a turn's content (P3C-3 §5).
 *
 * Every user turn carries exactly one variant (index 0); assistant turns start
 * with variant 0 and may gain 1..n through regeneration ([variantIndex] is a
 * stable per-turn counter assigned inside the writing transaction).
 *
 * [providerProfileId]/[model]/[errorType]/[errorMessage] are optional runtime
 * metadata — populated when a real provider runs (P3C-4), null this pass.
 */
data class ChatVariant(
    val id: String,
    val turnId: String,
    val variantIndex: Int,
    val content: String,
    val status: VariantStatus,
    val providerProfileId: String?,
    val model: String?,
    val errorType: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
