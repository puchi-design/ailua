package com.example.data.projection

import com.example.data.model.CharacterProfile
import com.example.data.model.LifeEvent
import com.example.data.model.sortedChronologically

/**
 * CharacterPresenceProjection
 *
 * Pure projection of a character's effective presence (what is happening right now)
 * derived from the LifeEvent ledger, with static profile fields only as fallback.
 * Shares its precedence rules with LivingProjection so every consumer (Living hero,
 * Home widgets, future consumers) reads presence the same way.
 */
data class CharacterPresence(
    val characterId: String,
    val currentActivity: String,
    val currentLocation: String,
    val currentEventId: String?
)

fun projectPresence(
    character: CharacterProfile,
    events: List<LifeEvent>,
    seedEventIds: Set<String> = emptySet()
): CharacterPresence {
    val latest = events
        .filter { it.characterId == character.id && it.id !in seedEventIds }
        .sortedChronologically()
        .lastOrNull()
    return CharacterPresence(
        characterId = character.id,
        currentActivity = latest?.title ?: character.currentActivity,
        currentLocation = latest?.location ?: character.location,
        currentEventId = latest?.id
    )
}
