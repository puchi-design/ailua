package com.example.data.chat.repository

import com.example.data.chat.model.ChatSession
import com.example.data.chat.model.GroupChatIdentity
import com.example.data.chat.model.ChatTurn
import com.example.data.chat.model.ChatVariant
import com.example.data.chat.model.ResolvedChatTurn
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.flow.Flow

/**
 * ChatRepository — the single entry point for chat persistence (P3C-3 §7).
 *
 * Contract rules:
 * - Every write method is ATOMIC: session update + turn + variant commit or
 *   roll back as one transaction (P3C-3 §10) — no half-written orphans.
 * - Regeneration is [appendVariant] on the EXISTING assistant turn; it never
 *   creates a new logical turn and it becomes the active variant (P3C-3 §11).
 * - Reads observe stable ordering: turns by per-session `position`, variants by
 *   per-turn `variantIndex` — never by timestamp.
 * - No driver types leak out of this interface: no SqlDriver, no generated
 *   Queries, no Cursor, no Android Context, no DAO (P3C-3 §7).
 * - Pure Kotlin + kotlinx Flow only — KMP-ready as-is (P3C-3 §14).
 */
interface ChatRepository {
    /** Emits the session or `null`, re-emitting on every session write. */
    fun observeSession(sessionId: String): Flow<ChatSession?>

    /** Emits turns of the session ordered by stable `position`. */
    fun observeTurns(sessionId: String): Flow<List<ChatTurn>>

    /** Emits variants of the turn ordered by stable `variantIndex`. */
    fun observeVariants(turnId: String): Flow<List<ChatVariant>>

    /** Synchronous single read; `null` when unknown. */
    fun getSession(sessionId: String): ChatSession?

    /**
     * Returns the earliest existing private session for [characterId], or
     * creates one (P3C-3 §13 — one canonical session per character).
     */
    fun getOrCreatePrivateSession(characterId: String): ChatSession

    /** Persists group membership in the canonical session's existing identity field. */
    fun getOrCreateGroupSession(groupId: String, participants: List<String>): ChatSession =
        getOrCreatePrivateSession(GroupChatIdentity.key(groupId, participants))

    /** Appends a USER turn with its single variant (index 0, [VariantStatus.COMPLETE]). */
    fun appendUserTurn(sessionId: String, content: String): ChatTurn

    /**
     * Appends an ASSISTANT turn whose first variant carries [content]/[status]
     * and becomes the turn's active variant — created atomically (P3C-3 §10).
     */
    fun appendAssistantTurn(
        sessionId: String,
        content: String,
        status: VariantStatus = VariantStatus.COMPLETE,
        providerProfileId: String? = null,
        model: String? = null,
        errorType: String? = null,
        errorMessage: String? = null,
    ): ChatTurn

    /**
     * Regeneration path: appends a NEW variant (`variantIndex = max + 1`) to the
     * existing [turnId], makes it active, and touches the owning session —
     * all in one transaction. Does NOT create a new turn (P3C-3 §11).
     */
    fun appendVariant(
        turnId: String,
        content: String,
        status: VariantStatus = VariantStatus.COMPLETE,
        providerProfileId: String? = null,
        model: String? = null,
        errorType: String? = null,
        errorMessage: String? = null,
    ): ChatVariant

    /**
     * Switches `activeVariantId` to [variantId] (future swipe-between-variants).
     * Throws [IllegalArgumentException] when the variant does not belong to [turnId].
     */
    fun selectVariant(turnId: String, variantId: String)

    /**
     * Deletes the session and everything under it — turns and variants must
     * cascade with zero orphan rows (P3C-3 §12).
     */
    fun clearSession(sessionId: String)

    /**
     * One-shot read of turns JOINed with their active variant (P3C-4 §7),
     * ordered by stable `position`. Never fans out into per-turn queries.
     */
    fun getResolvedTurns(sessionId: String): List<ResolvedChatTurn>

    /**
     * Same as [getResolvedTurns] but reactive: re-emits on every turn/variant
     * write under the session (P3C-4 §7).
     */
    fun observeResolvedTurns(sessionId: String): Flow<List<ResolvedChatTurn>>

    /** All variants of [turnId] ordered by stable `variantIndex` (variant picker). */
    fun getVariants(turnId: String): List<ChatVariant>

    /**
     * Single-row content/status update used by streaming persistence
     * (P3C-4 §8): the provider writes each variant exactly once on terminal
     * (COMPLETE / FAILED / CANCELLED with the accumulated partial text).
     * Touches the owning session alongside the variant row.
     */
    fun updateVariant(
        variantId: String,
        content: String,
        status: VariantStatus,
        errorType: String? = null,
        errorMessage: String? = null,
    )

    /**
     * Stale-streaming recovery (P3C-4 §9): marks every STREAMING variant of
     * [sessionId] as [VariantStatus.CANCELLED] with `errorType = "INTERRUPTED"`
     * so an app killed mid-generation never leaves a permanent "typing" row.
     *
     * @return how many variants were recovered.
     */
    fun recoverInterruptedVariants(sessionId: String): Int
}
