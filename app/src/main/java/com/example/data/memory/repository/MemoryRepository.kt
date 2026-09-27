package com.example.data.memory.repository

import com.example.data.memory.model.MemoryEntry
import com.example.data.memory.model.MemoryType
import kotlinx.coroutines.flow.Flow

/**
 * MemoryRepository — the single entry point for companion memory (P3C-5 §2).
 *
 * Contract rules:
 * - Storage is the existing SQLDelight chat DB (`memory_entry` table) — no
 *   Room, no SharedPreferences, no second database file.
 * - [saveMemory] is idempotent per `characterId + sourceRefId`: saving the
 *   same source message twice never inserts a duplicate row.
 * - [getMemoriesForPrompt] is a pure rule-based recall (Float service order,
 *   embedding skipped): CORE first, then importance desc, then updatedAt
 *   desc, capped at `limit` (default 20).
 * - Pure Kotlin + kotlinx Flow only — no Android types (P3C-3 §14 rule).
 */
interface MemoryRepository {

    /** Emits the character's memories (newest first), re-emitting on every write. */
    fun observeMemories(characterId: String): Flow<List<MemoryEntry>>

    /**
     * Saves one memory. Idempotent per `characterId + sourceRefId`.
     *
     * @param sourceAppId which app saved it — chat uses `"chat"`.
     * @param sourceRefId origin turn/variant id, used for dedupe.
     */
    fun saveMemory(
        characterId: String,
        content: String,
        sourceAppId: String,
        sourceRefId: String? = null,
        type: MemoryType = MemoryType.LONG_TERM,
        importance: Double = 0.7,
    )

    /** Deletes one memory by id. No-op when unknown. */
    fun deleteMemory(id: String)

    /**
     * Recall for prompt injection (P3C-5 §3): CORE → importance → updatedAt,
     * at most [limit] entries. No embedding, no scoring.
     */
    fun getMemoriesForPrompt(characterId: String, limit: Int = 20): List<MemoryEntry>
}
