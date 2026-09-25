package com.example.data.engine

import com.example.data.mock.MockData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WorldStateRepository
 *
 * Single source of truth for the living companion world events stream.
 * Shared reactively across Home, Living, Moments, Gallery, and Mailbox.
 */
object WorldStateRepository {

    private val _events = MutableStateFlow<List<LifeEvent>>(MockData.unifiedLifeEvents)
    val events: StateFlow<List<LifeEvent>> = _events.asStateFlow()

    fun appendLifeEvent(event: LifeEvent) {
        val current = _events.value
        // Avoid duplicate ID
        if (current.none { it.id == event.id }) {
            _events.value = listOf(event) + current
        }
    }

    fun eventsForCharacter(characterId: String): List<LifeEvent> {
        return _events.value.filter {
            it.characterId == characterId || it.relatedCharacterIds.contains(characterId)
        }
    }

    fun eventsForPlace(locationName: String): List<LifeEvent> {
        return _events.value.filter { it.location?.contains(locationName) == true }
    }

    fun latestEvents(limit: Int = 10): List<LifeEvent> = _events.value.take(limit)

    fun eventsOfType(type: LifeEventType): List<LifeEvent> = _events.value.filter { it.type == type }
}
