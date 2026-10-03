package com.example.data.relationship.romance

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.local.AiluaLocalStore
import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Separate from NPC relationships and old bond scores. Persistence contains evidence, never fabricated history. */
object RomanceRepository {
    private val mutableRecords = MutableStateFlow<List<RomanceRecord>>(emptyList())
    val records: StateFlow<List<RomanceRecord>> = mutableRecords.asStateFlow()

    @Synchronized fun restore() { mutableRecords.value = AiluaLocalStore.savedRomanceStates.value }
    fun record(characterId: String): RomanceRecord = mutableRecords.value.firstOrNull { it.characterId == characterId } ?: RomanceRecord(characterId)

    @Synchronized fun observeUserBoundary(characterId: String, userTurnId: String, userText: String, occurredAtEpochMs: Long) {
        val previous = record(characterId)
        val next = RomanceInteractionPolicy.observeUserBoundary(previous, CharacterRuntimeResolver.resolve(characterId), userTurnId, userText, occurredAtEpochMs)
        if (next != previous) save(next)
    }

    @Synchronized fun observeExchange(characterId: String, userTurnId: String, userText: String, assistantText: String, occurredAtEpochMs: Long) {
        val previous = record(characterId)
        val next = RomanceInteractionPolicy.observeExchange(previous, CharacterRuntimeResolver.resolve(characterId), userTurnId, userText, assistantText, occurredAtEpochMs)
        if (next != previous) save(next)
    }

    /** Only newly recorded, user-anchored memory facts have this producer timestamp. Old seeds are never replayed as romance. */
    fun observeLifeEvent(event: LifeEvent) {
        if (event.type != LifeEventType.MEMORY || event.sourceAppId != "memory" ||
            !event.id.startsWith("relationship_memory_") || event.metadata["actor"] != LIFE_EVENT_ACTOR_USER) return
        val occurredAt = event.metadata["interaction_epoch_ms"]?.toLongOrNull() ?: return
        if (event.description.trim().length < 12) return
        apply(event.characterId, RomanceEvent("memory:${event.id}", RomanceEventType.IMPORTANT_MEMORY, occurredAt,
            "memory:${RomanceInteractionPolicy.fingerprint(event.description)}"))
    }

    fun recordUserCall(characterId: String, callId: String, connectedDurationSeconds: Int, userInitiated: Boolean, nowEpochMs: Long = System.currentTimeMillis()) {
        if (!userInitiated || connectedDurationSeconds < 60) return
        apply(characterId, RomanceEvent("call:$callId", RomanceEventType.USER_CALL, nowEpochMs), userInteraction = true)
    }

    fun recordLetterRead(characterId: String, letterId: String, nowEpochMs: Long = System.currentTimeMillis()) {
        apply(characterId, RomanceEvent("letter:$letterId", RomanceEventType.LETTER, nowEpochMs), userInteraction = true)
    }

    fun recordSharedStory(characterId: String, storyId: String, nowEpochMs: Long = System.currentTimeMillis()) {
        if (characterId == "user" || characterId.isBlank()) return
        apply(characterId, RomanceEvent("story:$storyId", RomanceEventType.SHARED_STORY, nowEpochMs), userInteraction = true)
    }

    @Synchronized private fun apply(characterId: String, event: RomanceEvent, userInteraction: Boolean = false) {
        if (characterId.isBlank()) return
        val previous = record(characterId)
        if (event.id in previous.processedEventIds) return
        var next = RomanceReducer.apply(previous, event, CharacterRuntimeResolver.resolve(characterId))
        if (userInteraction && next != previous) next = next.copy(lastUserInteractionAtEpochMs = maxOf(next.lastUserInteractionAtEpochMs ?: 0L, event.occurredAtEpochMs))
        if (next != previous) save(next)
    }

    fun triggerFacts(characterId: String, nowEpochMs: Long = System.currentTimeMillis()): RomanceTriggerFacts =
        RomanceRuntime.triggerFacts(record(characterId), CharacterRuntimeResolver.resolve(characterId), nowEpochMs)

    fun promptInstructions(characterId: String, nowEpochMs: Long = System.currentTimeMillis()): String {
        val recentOther = mutableRecords.value.filter { it.characterId != characterId }.mapNotNull { other ->
            other.lastUserInteractionAtEpochMs?.let { RecentUserInteraction(other.characterId, it) }
        }.maxByOrNull { it.occurredAtEpochMs }
        return RomanceRuntime.promptInstructions(record(characterId), CharacterRuntimeResolver.resolve(characterId), nowEpochMs, recentOther)
    }

    fun groupPromptInstructions(characterId: String): String =
        RomanceRuntime.groupPromptInstructions(record(characterId), CharacterRuntimeResolver.resolve(characterId))

    private fun save(next: RomanceRecord) {
        val updated = mutableRecords.value.filterNot { it.characterId == next.characterId } + next
        AiluaLocalStore.saveRomanceStates(updated)
        mutableRecords.value = updated
    }
}
