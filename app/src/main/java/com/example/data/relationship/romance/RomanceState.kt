package com.example.data.relationship.romance

import kotlinx.serialization.Serializable

/** Internal evidence-based relationship state. Never a UI score or an import of legacy bond levels. */
@Serializable
data class RomanceState(
    val familiarity: Float = 0f,
    val trust: Float = 0f,
    val attraction: Float = 0f,
    val intimacy: Float = 0f,
    val tension: Float = 0f,
)

enum class RomanceStage { STRANGER, FAMILIAR, CLOSE, AMBIGUOUS, ROMANTIC, COMMITTED }

enum class RomanceEventType {
    IMPORTANT_MEMORY, SUSTAINED_CONVERSATION, USER_CALL, SHARED_STORY, LETTER,
    CONFLICT, CONFLICT_RESOLVED, ANNIVERSARY, MUTUAL_AFFECTION, MUTUAL_COMMITMENT,
    BOUNDARY_RESET, GOOD_EVENT,
}

/** Only producers with real interaction evidence may create these events; model prose alone is not evidence. */
data class RomanceEvent(
    val id: String,
    val type: RomanceEventType,
    val occurredAtEpochMs: Long,
    val fingerprint: String = id,
)

@Serializable
data class RomanceConflict(
    val eventId: String,
    val occurredAtEpochMs: Long,
    val resolvedAtEpochMs: Long? = null,
)

@Serializable
data class ConversationEvidence(
    val startedAtEpochMs: Long = 0L,
    val lastAtEpochMs: Long = 0L,
    val uniqueExchangeFingerprints: Set<String> = emptySet(),
    val rewarded: Boolean = false,
)

@Serializable
data class RomanceRecord(
    val characterId: String,
    val state: RomanceState = RomanceState(),
    val processedEventIds: Set<String> = emptySet(),
    val rewardedFingerprints: Set<String> = emptySet(),
    val rewardDay: Long = -1,
    val dailyTypeCounts: Map<String, Int> = emptyMap(),
    val recentConflict: RomanceConflict? = null,
    val romanticConfirmedAtEpochMs: Long? = null,
    val commitmentConfirmedAtEpochMs: Long? = null,
    val romanceDeclined: Boolean = false,
    val lastMeaningfulAtEpochMs: Long? = null,
    /** Prevents late delivery from resurrecting a conflict or overruling a later user boundary. */
    val lastAppliedAtEpochMs: Long = 0L,
    val lastGoodEventAtEpochMs: Long? = null,
    val lastUserInteractionAtEpochMs: Long? = null,
    val conversation: ConversationEvidence = ConversationEvidence(),
    val processedExchangeIds: Set<String> = emptySet(),
)

data class RomanceTriggerFacts(
    val recentConflict: Boolean = false,
    val conflictEventId: String? = null,
    val conflictAtEpochMs: Long? = null,
    val readyToReconnect: Boolean = false,
    val recentGoodEvent: Boolean = false,
    val recentGoodEventAtEpochMs: Long? = null,
    val lastMeaningfulAtEpochMs: Long? = null,
)

data class RecentUserInteraction(val characterId: String, val occurredAtEpochMs: Long)
