package com.example.data.notes

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Serializable
data class Memo(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class MemoDraft(
    val editingId: String?,
    val title: String,
    val body: String,
)

/** A small, private memo store. A failed read or write never replaces the user's saved notes. */
class MemoRepository(
    private val preferences: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val initial = readStored()
    private val _notes = MutableStateFlow(initial.notes)
    val notes = _notes.asStateFlow()
    private val _storageError = MutableStateFlow(initial.error)
    val storageError = _storageError.asStateFlow()
    private val draftMutex = Mutex()
    private val pendingDraft = AtomicReference(DraftVersion(0L, readDraft()))
    private val draftSignal = Channel<Unit>(Channel.CONFLATED)
    private val _draftWriteError = MutableStateFlow(false)
    val draftWriteError = _draftWriteError.asStateFlow()
    private val draftWriter = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        for (ignored in draftSignal) {
            delay(180)
            flushDraft()
        }
    }

    suspend fun save(id: String?, title: String, body: String): Memo? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (title.isBlank() && body.isBlank()) return@withLock null
            val stored = readStored()
            if (stored.error) {
                _storageError.value = true
                return@withLock null
            }
            val previous = id?.let { selected -> stored.notes.firstOrNull { it.id == selected } }
            if (id != null && previous == null) return@withLock null
            val timestamp = now()
            val note = Memo(
                id = previous?.id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                body = body.trimEnd(),
                createdAt = previous?.createdAt ?: timestamp,
                updatedAt = timestamp,
            )
            val updated = listOf(note) + stored.notes.filterNot { it.id == note.id }
            if (!writeStored(updated)) return@withLock null
            _notes.value = updated
            note
        }
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val stored = readStored()
            if (stored.error) {
                _storageError.value = true
                return@withLock false
            }
            val updated = stored.notes.filterNot { it.id == id }
            if (updated.size == stored.notes.size || !writeStored(updated)) return@withLock false
            _notes.value = updated
            true
        }
    }

    fun search(query: String): List<Memo> {
        val term = query.trim()
        return if (term.isEmpty()) notes.value else notes.value.filter {
            it.title.contains(term, ignoreCase = true) || it.body.contains(term, ignoreCase = true)
        }
    }

    /** Read the committed draft. A new repository after process death starts from this value. */
    fun readDraft(): MemoDraft? = runCatching {
        preferences.getString(KEY_DRAFT, null)?.let { json.decodeFromString<MemoDraft>(it) }
    }.getOrNull()

    /** Latest input remains available in memory across rotation before its disk write completes. */
    fun currentDraft(): MemoDraft? = pendingDraft.get().draft

    fun submitDraft(draft: MemoDraft?) {
        pendingDraft.updateAndGet { old -> DraftVersion(old.version + 1, draft) }
        draftSignal.trySend(Unit)
    }

    /** Commit the latest version on IO; a concurrent update forces another pass. */
    suspend fun flushDraft(): Boolean = withContext(Dispatchers.IO) {
        var latest = false
        while (!latest) {
            val (committed, isLatest) = draftMutex.withLock {
                val snapshot = pendingDraft.get()
                val success = runCatching {
                    val edit = preferences.edit()
                    if (snapshot.draft == null) edit.remove(KEY_DRAFT)
                    else edit.putString(KEY_DRAFT, json.encodeToString(snapshot.draft))
                    edit.commit()
                }.getOrDefault(false)
                _draftWriteError.value = !success
                success to (pendingDraft.get().version == snapshot.version)
            }
            if (!committed) return@withContext false
            latest = isLatest
        }
        true
    }

    private fun writeStored(updated: List<Memo>): Boolean = runCatching {
        val current = preferences.getString(KEY_NOTES, null)
        val edit = preferences.edit().putString(KEY_NOTES, json.encodeToString(updated))
        if (current != null) edit.putString(KEY_BACKUP, current)
        edit.commit()
    }.getOrDefault(false)

    private fun readStored(): Stored {
        val raw = runCatching { preferences.getString(KEY_NOTES, null) }
            .getOrElse { return Stored(emptyList(), true) } ?: return Stored(emptyList(), false)
        return runCatching {
            val notes = json.decodeFromString<List<Memo>>(raw)
            if (notes.any { it.id.isBlank() }) Stored(emptyList(), true) else Stored(notes, false)
        }.getOrElse { Stored(emptyList(), true) }
    }

    private data class Stored(val notes: List<Memo>, val error: Boolean)
    private data class DraftVersion(val version: Long, val draft: MemoDraft?)

    companion object {
        private const val FILE = "ailua_personal_memos"
        private const val KEY_NOTES = "notes_v1"
        private const val KEY_BACKUP = "notes_v1_previous"
        private const val KEY_DRAFT = "draft_v1"

        @Volatile private var appRepository: MemoRepository? = null

        fun from(context: Context): MemoRepository = appRepository ?: synchronized(this) {
            appRepository ?: MemoRepository(
                context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            ).also { appRepository = it }
        }
    }
}
