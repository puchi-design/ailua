package com.example.data.memory.model

/**
 * MemoryType — the two buckets Memory V1 keeps (Float `memory-types.ts` shape,
 * AGPL rewritten in Kotlin): long-term facts and core memories.
 */
enum class MemoryType {
    LONG_TERM,
    CORE,
}

/**
 * MemoryEntry — one persisted companion memory (P3C-5 §1).
 *
 * Deliberately minimal: no embedding, no emotion/confidence, no metadata map.
 * `sourceRefId` is the origin turn/variant in the source app — saving the same
 * `characterId + sourceRefId` twice never inserts a duplicate row.
 */
data class MemoryEntry(
    val id: String,
    val characterId: String,
    val type: MemoryType,
    val content: String,
    /** 0.0 – 1.0; chat saves use 0.7. */
    val importance: Double,
    val sourceAppId: String,
    val sourceRefId: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
