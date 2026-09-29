package com.example.data.memory.auto

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.runtime.ProviderResolver
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.chat.local.MemoryExtractCursorQueries
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.repository.EpochClock
import com.example.data.memory.model.MemoryType
import com.example.data.memory.repository.MemoryRepository
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.relationship.repository.RelationshipStateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AutoMemoryExtractor — P3D-1 auto memory.
 *
 * After a completed chat reply, takes the new COMPLETE chat turns past the
 * per-character+session cursor, asks the active provider for structured
 * long-term memories in ONE non-streaming call, dedupes them against the
 * character's existing memories (exact/normalized content equality only —
 * no embeddings, no similarity), saves them and advances the cursor.
 *
 * Hard rules (P3D-1):
 * - Fewer than [MIN_NEW_TURNS] new COMPLETE non-blank turns -> no-op.
 * - The cursor advances ONLY after the model call succeeded AND its JSON
 *   parsed; model/JSON failures retry on the next completed reply.
 * - Every failure path is invisible to the chat experience — the caller
 *   wraps [run] in its own try/catch.
 */
class AutoMemoryExtractor(
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val cursorQueries: MemoryExtractCursorQueries,
    private val providerResolver: ProviderResolver,
    private val clock: EpochClock,
    private val characterName: String,
) {

    /**
     * Runs one extraction step for [characterId]. [sessionId] is the session
     * the caller just completed a reply in; when null (or not the character's
     * private session) the canonical private session is used instead.
     */
    suspend fun run(characterId: String, sessionId: String? = null) {
        val session = sessionId
            ?.takeIf { chatRepository.getSession(it)?.characterId == characterId }
            ?: chatRepository.getOrCreatePrivateSession(characterId).id
        val cursorKey = "$characterId:$session"

        val lastPosition = cursorQueries.selectMemoryExtractCursor(cursorKey)
            .executeAsOneOrNull()?.last_position ?: -1L

        val pending = chatRepository.getResolvedTurns(session).filter { turn ->
            turn.position > lastPosition &&
                turn.activeVariant?.status == VariantStatus.COMPLETE &&
                turn.activeVariant.content.isNotBlank()
        }
        if (pending.size < MIN_NEW_TURNS) return

        val batch = pending.take(BATCH_SIZE)
        val resolved = providerResolver.resolve() ?: return

        val transcript = batch.joinToString(separator = "\n") { turn ->
            val text = turn.activeVariant!!.content.trim()
            when (turn.role) {
                ChatTurnRole.USER -> "用户：$text"
                ChatTurnRole.ASSISTANT -> "$characterName：$text"
            }
        }

        val reply = completeOnce(resolved, transcript) ?: return
        val extracted = parseMemories(reply) ?: return

        // Commit phase: once the model succeeded, saves + cursor advance run
        // under NonCancellable so scope cancellation cannot half-apply a batch.
        // Saves are idempotent (normalized-content dedupe + unique
        // character_id+source_ref_id index), so a retry after any partial
        // commit re-processes the same batch without duplicating rows.
        withContext(NonCancellable) {
            val existing = memoryRepository.getMemories(characterId)
                .mapTo(HashSet()) { normalize(it.content) }
            extracted.forEachIndexed { index, item ->
                val content = item.content.trim()
                if (content.isEmpty()) return@forEachIndexed
                if (!existing.add(normalize(content))) return@forEachIndexed
                memoryRepository.saveMemory(
                    characterId = characterId,
                    content = content,
                    sourceAppId = SOURCE_APP_ID,
                    sourceRefId = "batch:${batch.first().id}:${batch.last().id}:$index",
                    type = MemoryType.LONG_TERM,
                    importance = item.importance.coerceIn(0.0, 1.0),
                )
                val worldClock = WorldHeartbeatEngine.worldClock.value
                RelationshipStateRepository.recordMemory(characterId, "batch:${batch.first().id}:${batch.last().id}:$index", content, worldClock.dateLabel, worldClock.timeFormatted)
            }
            cursorQueries.upsertMemoryExtractCursor(
                cursor_key = cursorKey,
                last_position = batch.last().position.toLong(),
                updated_at_epoch_ms = clock.nowEpochMs(),
            )
        }
    }

    /** One non-streaming call; `null` on any failure (retry next time). */
    private suspend fun completeOnce(
        resolved: ResolvedProvider,
        transcript: String,
    ): String? {
        val request = AiChatRequest(
            model = resolved.model,
            messages = listOf(
                AiMessage(AiRole.SYSTEM, EXTRACTION_SYSTEM_PROMPT),
                AiMessage(AiRole.USER, "$EXTRACTION_USER_PREFIX$transcript"),
            ),
            stream = false,
        )
        var text: String? = null
        var failed = false
        try {
            resolved.provider.streamChat(request).collect { event ->
                when (event) {
                    is AiStreamEvent.Completed -> text = event.text
                    is AiStreamEvent.Failed -> failed = true
                    AiStreamEvent.Cancelled -> failed = true
                    else -> Unit
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return null
        }
        return if (failed) null else text?.takeIf { it.isNotBlank() }
    }

    /** Parses the strict JSON payload; `null` when malformed (no cursor advance). */
    private fun parseMemories(reply: String): List<ExtractedMemory>? {
        val cleaned = reply.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        val payload = if (start in 0 until end) cleaned.substring(start, end + 1) else cleaned
        return try {
            JSON.decodeFromString<ExtractedMemoryBatch>(payload).memories
                .filter { it.content.isNotBlank() }
                .take(MAX_MEMORIES)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /** New COMPLETE turns required before any extraction runs. */
        const val MIN_NEW_TURNS = 12

        /** Turns taken per extraction call. */
        const val BATCH_SIZE = 12

        /** Never save more than this many memories per batch. */
        const val MAX_MEMORIES = 3

        /** sourceAppId for every auto-saved memory. */
        const val SOURCE_APP_ID = "chat-auto"

        private const val EXTRACTION_SYSTEM_PROMPT =
            "从下面对话中提取值得长期记住的用户事实、偏好、重要经历或双方共同约定。最多 3 条。" +
                "不要记录普通寒暄，不要编造。严格返回 JSON：\n" +
                "{\"memories\": [{\"content\": \"...\", \"importance\": 0.0-1.0}]}"

        private const val EXTRACTION_USER_PREFIX = "对话记录：\n"

        private val JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        /** Content equality for dedupe: lowercase, strip whitespace + punctuation. */
        private fun normalize(content: String): String =
            content.lowercase().replace(Regex("[\\s\\p{P}]"), "")
    }
}

/** Strict extraction payload — at most [AutoMemoryExtractor.MAX_MEMORIES] items. */
@Serializable
internal data class ExtractedMemory(
    val content: String = "",
    val importance: Double = 0.7,
)

@Serializable
internal data class ExtractedMemoryBatch(
    val memories: List<ExtractedMemory> = emptyList(),
)
