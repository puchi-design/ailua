package com.example.data.chat.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.data.chat.model.ChatSession
import com.example.data.chat.model.ChatTurn
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.ChatVariant
import com.example.data.chat.model.ResolvedChatTurn
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.repository.EpochClock
import com.example.data.chat.repository.IdGenerator
import com.example.data.chat.rich.RichMessageCodec
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SqlDelightChatRepository — the only ChatRepository implementation (P3C-3 §7).
 *
 * Atomicity (P3C-3 §10): every append runs inside `transactionWithResult`, so
 * session touch + turn insert + variant insert + activeVariantId update commit
 * or roll back together. Ordering counters (`position`, `variantIndex`) are
 * read as `MAX + 1` inside the same transaction — never timestamps.
 *
 * Lives in the `local` package next to the generated SQLDelight code; the
 * repository CONTRACT in `data.chat.repository` stays driver-free (P3C-3 §14).
 * Flows dispatch on [Dispatchers.Default] — pure Kotlin, KMP-ready.
 */
class SqlDelightChatRepository(
    private val database: ChatDatabase,
    private val idGenerator: IdGenerator,
    private val clock: EpochClock,
) : ChatRepository {

    override fun observeSession(sessionId: String): Flow<ChatSession?> =
        database.chatSessionQueries.selectSessionById(sessionId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { row -> row?.toDomain() }

    override fun observeTurns(sessionId: String): Flow<List<ChatTurn>> =
        database.chatTurnQueries.selectTurnsBySession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeVariants(turnId: String): Flow<List<ChatVariant>> =
        database.chatVariantQueries.selectVariantsByTurn(turnId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getSession(sessionId: String): ChatSession? =
        database.chatSessionQueries.selectSessionById(sessionId).executeAsOneOrNull()?.toDomain()

    override fun getOrCreatePrivateSession(characterId: String): ChatSession =
        database.transactionWithResult {
            val existing = database.chatSessionQueries
                .selectCanonicalSessionByCharacter(characterId)
                .executeAsOneOrNull()
            if (existing != null) {
                return@transactionWithResult existing.toDomain()
            }
            val now = clock.nowEpochMs()
            val session = ChatSession(
                id = idGenerator.newId(),
                characterId = characterId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
            database.chatSessionQueries.insertSession(
                id = session.id,
                character_id = session.characterId,
                created_at_epoch_ms = session.createdAtEpochMs,
                updated_at_epoch_ms = session.updatedAtEpochMs,
            )
            session
        }

    override fun appendUserTurn(
        sessionId: String,
        content: String,
        quoteMessageId: String?,
        quotePreview: String?,
    ): ChatTurn =
        appendTurn(
            sessionId = sessionId,
            role = ChatTurnRole.USER,
            content = content,
            status = VariantStatus.COMPLETE,
            providerProfileId = null,
            model = null,
            errorType = null,
            errorMessage = null,
            quoteMessageId = quoteMessageId,
            quotePreview = quotePreview,
        )

    override fun appendAssistantTurn(
        sessionId: String,
        content: String,
        status: VariantStatus,
        providerProfileId: String?,
        model: String?,
        errorType: String?,
        errorMessage: String?,
    ): ChatTurn = appendTurn(
        sessionId = sessionId,
        role = ChatTurnRole.ASSISTANT,
        content = content,
        status = status,
        providerProfileId = providerProfileId,
        model = model,
        errorType = errorType,
        errorMessage = errorMessage,
        quoteMessageId = null,
        quotePreview = null,
    )

    private fun appendTurn(
        sessionId: String,
        role: ChatTurnRole,
        content: String,
        status: VariantStatus,
        providerProfileId: String?,
        model: String?,
        errorType: String?,
        errorMessage: String?,
        quoteMessageId: String?,
        quotePreview: String?,
    ): ChatTurn = database.transactionWithResult {
        val now = clock.nowEpochMs()
        val position = database.chatTurnQueries.selectMaxPosition(sessionId).executeAsOne() + 1
        val turnId = idGenerator.newId()
        val variantId = idGenerator.newId()
        val quotedTurn = quoteMessageId?.let { database.chatTurnQueries.selectTurnById(it).executeAsOneOrNull() }
            ?.takeIf { it.session_id == sessionId }
        val quotedVariant = quotedTurn?.active_variant_id?.let {
            database.chatVariantQueries.selectVariantById(it).executeAsOneOrNull()
        }
        val safeQuoteId = quotedTurn?.id
        val safeQuotePreview = if (safeQuoteId == null) null else
            (quotedVariant?.content?.takeIf { it.isNotBlank() } ?: quotePreview.orEmpty()).trim().take(160)
        database.chatTurnQueries.insertTurn(
            id = turnId,
            session_id = sessionId,
            role = role.name,
            position = position,
            active_variant_id = variantId,
            created_at_epoch_ms = now,
        )
        database.chatVariantQueries.insertVariant(
            id = variantId,
            turn_id = turnId,
            variant_index = 0,
            content = content,
            status = status.name,
            provider_profile_id = providerProfileId,
            model = model,
            error_type = errorType,
            error_message = errorMessage,
            created_at_epoch_ms = now,
            updated_at_epoch_ms = now,
            rich_payloads_json = null,
            quote_message_id = safeQuoteId,
            quote_preview = safeQuotePreview,
        )
        database.chatSessionQueries.touchSession(updated_at_epoch_ms = now, id = sessionId)
        ChatTurn(
            id = turnId,
            sessionId = sessionId,
            role = role,
            position = position.toInt(),
            activeVariantId = variantId,
            createdAtEpochMs = now,
        )
    }

    override fun appendVariant(
        turnId: String,
        content: String,
        status: VariantStatus,
        providerProfileId: String?,
        model: String?,
        errorType: String?,
        errorMessage: String?,
    ): ChatVariant = database.transactionWithResult {
        val turn = database.chatTurnQueries.selectTurnById(turnId).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Unknown turn: $turnId")
        val now = clock.nowEpochMs()
        val variantId = idGenerator.newId()
        val nextIndex = database.chatVariantQueries.selectMaxVariantIndex(turnId).executeAsOne() + 1
        database.chatVariantQueries.insertVariant(
            id = variantId,
            turn_id = turnId,
            variant_index = nextIndex,
            content = content,
            status = status.name,
            provider_profile_id = providerProfileId,
            model = model,
            error_type = errorType,
            error_message = errorMessage,
            created_at_epoch_ms = now,
            updated_at_epoch_ms = now,
            rich_payloads_json = null,
            quote_message_id = null,
            quote_preview = null,
        )
        database.chatTurnQueries.updateActiveVariant(active_variant_id = variantId, id = turnId)
        database.chatSessionQueries.touchSession(updated_at_epoch_ms = now, id = turn.session_id)
        ChatVariant(
            id = variantId,
            turnId = turnId,
            variantIndex = nextIndex.toInt(),
            content = content,
            status = status,
            providerProfileId = providerProfileId,
            model = model,
            errorType = errorType,
            errorMessage = errorMessage,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
    }

    override fun selectVariant(turnId: String, variantId: String) {
        database.transaction {
            val variant = database.chatVariantQueries.selectVariantById(variantId).executeAsOneOrNull()
            require(variant != null && variant.turn_id == turnId) {
                "Unknown variant $variantId for turn $turnId"
            }
            database.chatTurnQueries.updateActiveVariant(active_variant_id = variantId, id = turnId)
            val turn = database.chatTurnQueries.selectTurnById(turnId).executeAsOne()
            database.chatSessionQueries.touchSession(updated_at_epoch_ms = clock.nowEpochMs(), id = turn.session_id)
        }
    }

    override fun clearSession(sessionId: String) {
        database.transaction {
            // Explicit ordered deletes (variants -> turns -> session) keep the
            // zero-orphan guarantee even when a driver omits FK enforcement.
            database.chatVariantQueries.deleteVariantsBySession(session_id = sessionId)
            database.chatTurnQueries.deleteTurnsBySession(session_id = sessionId)
            database.chatSessionQueries.deleteSessionById(id = sessionId)
        }
    }

    override fun getResolvedTurns(sessionId: String): List<ResolvedChatTurn> =
        database.chatTurnQueries.selectResolvedTurns(sessionId).executeAsList().map { it.toDomain() }

    override fun observeResolvedTurns(sessionId: String): Flow<List<ResolvedChatTurn>> =
        database.chatTurnQueries.selectResolvedTurns(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getVariants(turnId: String): List<ChatVariant> =
        database.chatVariantQueries.selectVariantsByTurn(turnId).executeAsList().map { it.toDomain() }

    override fun updateVariant(
        variantId: String,
        content: String,
        status: VariantStatus,
        errorType: String?,
        errorMessage: String?,
        richPayloads: List<RichMessagePayload>?,
    ) {
        database.transaction {
            val variant = database.chatVariantQueries.selectVariantById(variantId).executeAsOneOrNull()
                ?: throw IllegalArgumentException("Unknown variant: $variantId")
            database.chatVariantQueries.updateVariant(
                content = content,
                status = status.name,
                error_type = errorType,
                error_message = errorMessage,
                updated_at_epoch_ms = clock.nowEpochMs(),
                value = richPayloads?.let(RichMessageCodec::encode),
                id = variantId,
            )
            val turn = database.chatTurnQueries.selectTurnById(variant.turn_id).executeAsOneOrNull()
            if (turn != null) {
                database.chatSessionQueries.touchSession(
                    updated_at_epoch_ms = clock.nowEpochMs(),
                    id = turn.session_id,
                )
            }
        }
    }

    override fun updateRichStatus(variantId: String, payloadIndex: Int, status: RichMessageStatus): Boolean =
        database.transactionWithResult {
            val variant = database.chatVariantQueries.selectVariantById(variantId).executeAsOneOrNull()
                ?: return@transactionWithResult false
            val payloads = RichMessageCodec.decode(variant.rich_payloads_json)
            val payload = payloads.getOrNull(payloadIndex) ?: return@transactionWithResult false
            if (payload.status != RichMessageStatus.PENDING) return@transactionWithResult false
            val allowed = when (payload.type) {
                RichMessageType.RED_PACKET -> status == RichMessageStatus.OPENED
                RichMessageType.TRANSFER -> status == RichMessageStatus.ACCEPTED || status == RichMessageStatus.DECLINED
                RichMessageType.GIFT -> status == RichMessageStatus.RECEIVED
                else -> false
            }
            if (!allowed) return@transactionWithResult false
            val now = clock.nowEpochMs()
            database.chatVariantQueries.updateRichPayloads(
                rich_payloads_json = RichMessageCodec.encode(payloads.toMutableList().also {
                    it[payloadIndex] = payload.copy(status = status)
                }),
                updated_at_epoch_ms = now,
                id = variantId,
            )
            val turn = database.chatTurnQueries.selectTurnById(variant.turn_id).executeAsOneOrNull()
            if (turn != null) database.chatSessionQueries.touchSession(updated_at_epoch_ms = now, id = turn.session_id)
            true
        }

    override fun recoverInterruptedVariants(sessionId: String): Int =
        database.transactionWithResult {
            val stale = database.chatVariantQueries
                .selectStreamingCountBySession(session_id = sessionId)
                .executeAsOne()
            if (stale > 0) {
                database.chatVariantQueries.recoverStreamingVariants(
                    updated_at_epoch_ms = clock.nowEpochMs(),
                    session_id = sessionId,
                )
            }
            stale.toInt()
        }

    private fun SelectResolvedTurns.toDomain() = ResolvedChatTurn(
        id = turn_id,
        sessionId = turn_session_id,
        role = ChatTurnRole.valueOf(turn_role),
        position = turn_position.toInt(),
        activeVariantId = turn_active_variant_id,
        createdAtEpochMs = turn_created_at_epoch_ms,
        activeVariant = variant_id?.let { id ->
            ChatVariant(
                id = id,
                turnId = variant_turn_id!!,
                variantIndex = variant_variant_index!!.toInt(),
                content = variant_content!!,
                status = VariantStatus.valueOf(variant_status!!),
                providerProfileId = variant_provider_profile_id,
                model = variant_model,
                errorType = variant_error_type,
                errorMessage = variant_error_message,
                createdAtEpochMs = variant_created_at_epoch_ms!!,
                updatedAtEpochMs = variant_updated_at_epoch_ms!!,
                richPayloads = RichMessageCodec.decode(variant_rich_payloads_json),
                quoteMessageId = variant_quote_message_id,
                quotePreview = variant_quote_preview,
            )
        },
        variantCount = variant_count.toInt(),
    )

    private fun Chat_session.toDomain() = ChatSession(
        id = id,
        characterId = character_id,
        createdAtEpochMs = created_at_epoch_ms,
        updatedAtEpochMs = updated_at_epoch_ms,
    )

    private fun Chat_turn.toDomain() = ChatTurn(
        id = id,
        sessionId = session_id,
        role = ChatTurnRole.valueOf(role),
        position = position.toInt(),
        activeVariantId = active_variant_id,
        createdAtEpochMs = created_at_epoch_ms,
    )

    private fun Chat_variant.toDomain() = ChatVariant(
        id = id,
        turnId = turn_id,
        variantIndex = variant_index.toInt(),
        content = content,
        status = VariantStatus.valueOf(status),
        providerProfileId = provider_profile_id,
        model = model,
        errorType = error_type,
        errorMessage = error_message,
        createdAtEpochMs = created_at_epoch_ms,
        updatedAtEpochMs = updated_at_epoch_ms,
        richPayloads = RichMessageCodec.decode(rich_payloads_json),
        quoteMessageId = quote_message_id,
        quotePreview = quote_preview,
    )
}
