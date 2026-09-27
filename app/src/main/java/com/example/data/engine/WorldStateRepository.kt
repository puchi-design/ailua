package com.example.data.engine

import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.parseClockTimeToMinutes
import com.example.data.model.sortedChronologically
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WorldStateRepository
 *
 * Single source of truth for the living companion world events stream (the LifeEvent ledger).
 * Shared reactively across Home, Living, Moments, Diary, Check Phone, Gallery, and Mailbox.
 * Restores and persists events using AiluaLocalStore.
 *
 * Ordering contract:
 * - [events] flow: newest-first (presentation order, consumed by Home Bento / UI previews)
 * - query APIs ([eventsForCharacter], [eventsForType], [eventsForSource], ...):
 *   chronological ascending by virtual world time (see LifeEventOrder)
 * - identity: [LifeEvent.id] is the only dedupe authority
 */
object WorldStateRepository {

    private val _events = MutableStateFlow<List<LifeEvent>>(initialEvents())
    val events: StateFlow<List<LifeEvent>> = _events.asStateFlow()

    private fun initialEvents(): List<LifeEvent> {
        val saved = AiluaLocalStore.savedWorldEvents.value
        val base = if (saved.isNotEmpty()) {
            (saved + MockData.unifiedLifeEvents).distinctBy { it.id }
        } else {
            MockData.unifiedLifeEvents
        }
        // Stamp seed world facts with virtual world time so ledger queries sort them
        // chronologically against runtime events (seed dates stay their original day).
        return base.map { if (it.worldDateLabel.isBlank()) normalizeWorldTime(it) else it }
    }

    fun syncWithLocalStore() {
        val saved = AiluaLocalStore.savedWorldEvents.value
        if (saved.isNotEmpty()) {
            _events.value = (saved + _events.value).distinctBy { it.id }
        }
    }

    /**
     * Appends a runtime fact. Normalizes virtual world time fields from the current
     * world clock when the producer did not set them. Dedupes strictly by [LifeEvent.id];
     * a same-minute event with a different id is always kept.
     * Returns the authoritative stored event (the existing one when the id already exists).
     */
    fun appendLifeEvent(event: LifeEvent): LifeEvent {
        val existing = _events.value.firstOrNull { it.id == event.id }
        if (existing != null) return existing
        val normalized = normalizeWorldTime(event)
        _events.value = listOf(normalized) + _events.value
        AiluaLocalStore.appendWorldEvent(normalized)
        return normalized
    }

    /**
     * Appends a batch preserving input order; in-batch duplicate ids and ids that
     * already exist in the ledger are ignored.
     */
    fun appendLifeEvents(events: List<LifeEvent>): List<LifeEvent> = events.map { appendLifeEvent(it) }

    private fun normalizeWorldTime(event: LifeEvent): LifeEvent {
        val dateDone = event.worldDateLabel.isNotBlank()
        val minutesDone = event.worldMinutesOfDay in 0..1439
        if (dateDone && minutesDone) return event
        // Read the persisted virtual clock directly instead of WorldHeartbeatEngine:
        // appending can happen re-entrantly while the engine object is still initializing
        // (engine init -> MailboxRepository delivery -> append), so the engine must not be touched here.
        val clockDate = AiluaLocalStore.getVirtualDate()
        val clockMinutes = AiluaLocalStore.getVirtualMinutes()
        val parsedFromDisplayTime = parseClockTimeToMinutes(event.time)
        return event.copy(
            worldDateLabel = if (dateDone) event.worldDateLabel else clockDate,
            worldMinutesOfDay = if (minutesDone) {
                event.worldMinutesOfDay
            } else {
                parsedFromDisplayTime ?: clockMinutes
            }
        )
    }

    fun eventsForCharacter(characterId: String, includeRelated: Boolean = false): List<LifeEvent> {
        return _events.value.filter {
            it.characterId == characterId || (includeRelated && it.relatedCharacterIds.contains(characterId))
        }.sortedChronologically()
    }

    fun eventsForType(type: LifeEventType): List<LifeEvent> =
        _events.value.filter { it.type == type }.sortedChronologically()

    fun eventsForSource(sourceAppId: String): List<LifeEvent> =
        _events.value.filter { it.sourceAppId == sourceAppId }.sortedChronologically()

    fun eventsForCharacterAndType(
        characterId: String,
        type: LifeEventType,
        includeRelated: Boolean = false
    ): List<LifeEvent> = eventsForCharacter(characterId, includeRelated).filter { it.type == type }

    fun latestForCharacter(characterId: String, includeRelated: Boolean = false): LifeEvent? =
        eventsForCharacter(characterId, includeRelated).lastOrNull()

    fun eventsForPlace(locationName: String): List<LifeEvent> =
        _events.value.filter { it.location?.contains(locationName) == true }.sortedChronologically()

    /** Newest-first snapshot of the most recent [limit] events. */
    fun latestEvents(limit: Int = 10): List<LifeEvent> = _events.value.take(limit)

    fun eventsOfType(type: LifeEventType): List<LifeEvent> = eventsForType(type)
}
