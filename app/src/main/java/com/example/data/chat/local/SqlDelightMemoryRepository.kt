package com.example.data.chat.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.data.chat.repository.EpochClock
import com.example.data.chat.repository.IdGenerator
import com.example.data.memory.model.MemoryEntry
import com.example.data.memory.model.MemoryType
import com.example.data.memory.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SqlDelightMemoryRepository — the only MemoryRepository implementation
 * (P3C-5 §2).
 *
 * Lives next to the generated SQLDelight code (same convention as
 * [SqlDelightChatRepository]); the repository contract stays driver-free.
 * Dedupe is enforced by the partial unique index on
 * `character_id + source_ref_id` with `INSERT OR IGNORE` — same behavior as
 * Float's keyed `put`, in one SQL statement.
 */
class SqlDelightMemoryRepository(
    private val database: ChatDatabase,
    private val idGenerator: IdGenerator,
    private val clock: EpochClock,
) : MemoryRepository {

    override fun observeMemories(characterId: String): Flow<List<MemoryEntry>> =
        database.memoryEntryQueries.selectMemoriesByCharacter(characterId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun saveMemory(
        characterId: String,
        content: String,
        sourceAppId: String,
        sourceRefId: String?,
        type: MemoryType,
        importance: Double,
    ) {
        val now = clock.nowEpochMs()
        database.memoryEntryQueries.insertMemory(
            id = idGenerator.newId(),
            character_id = characterId,
            type = type.name,
            content = content,
            importance = importance,
            source_app_id = sourceAppId,
            source_ref_id = sourceRefId,
            created_at_epoch_ms = now,
            updated_at_epoch_ms = now,
        )
    }

    override fun deleteMemory(id: String) {
        database.memoryEntryQueries.deleteMemoryById(id = id)
    }

    override fun getMemories(characterId: String): List<MemoryEntry> =
        database.memoryEntryQueries.selectMemoriesByCharacter(characterId)
            .executeAsList().map { it.toDomain() }

    override fun getMemoriesForPrompt(characterId: String, limit: Int): List<MemoryEntry> =
        database.memoryEntryQueries.selectMemoriesForPrompt(
            character_id = characterId,
            value_ = limit.toLong(),
        ).executeAsList().map { it.toDomain() }

    private fun Memory_entry.toDomain() = MemoryEntry(
        id = id,
        characterId = character_id,
        type = MemoryType.valueOf(type),
        content = content,
        importance = importance,
        sourceAppId = source_app_id,
        sourceRefId = source_ref_id,
        createdAtEpochMs = created_at_epoch_ms,
        updatedAtEpochMs = updated_at_epoch_ms,
    )
}
