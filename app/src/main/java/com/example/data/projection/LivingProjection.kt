package com.example.data.projection

import com.example.data.model.CharacterProfile
import com.example.data.model.LifeEvent
import com.example.data.model.TimelineEvent
import com.example.data.model.sortedChronologically

/**
 * LivingProjection
 *
 * Pure function projecting the LifeEvent ledger plus seed world data into the
 * Living screen view state. No Compose, no Context, no storage.
 *
 * Rules:
 * - runtime facts (events appended after seed load) win over seed data
 * - seed timeline entries are initial world data, preserved as-is (including flags)
 * - the effective current state is the latest runtime fact; static profile fields
 *   are only a fallback when no runtime fact exists yet
 * - timeline merge dedupes by TimelineEvent.id (never by time), so two events in
 *   the same minute both survive
 */
data class LivingProjection(
    val currentActivity: String,
    val currentLocation: String,
    val currentEvent: LifeEvent?,
    val timeline: List<TimelineEvent>
)

fun projectLiving(
    character: CharacterProfile,
    seedTimeline: List<TimelineEvent>,
    runtimeEvents: List<LifeEvent>,
    seedEventIds: Set<String> = emptySet()
): LivingProjection {
    val ownEvents = runtimeEvents.filter { it.characterId == character.id }
    val seedIds = seedTimeline.mapTo(HashSet()) { it.id } + seedEventIds

    val runtimeMapped = ownEvents.map { lifeEvent ->
        TimelineEvent(
            id = lifeEvent.id,
            time = lifeEvent.time,
            title = lifeEvent.title,
            description = lifeEvent.description,
            location = lifeEvent.location ?: character.location,
            mood = character.mood,
            relatedCharacterIds = lifeEvent.relatedCharacterIds
        )
    }

    val merged = (seedTimeline + runtimeMapped).distinctBy { it.id }

    val latestRuntime = ownEvents
        .filterNot { it.id in seedIds }
        .sortedChronologically()
        .lastOrNull()

    val timeline = if (latestRuntime != null) {
        merged.map { it.copy(isCurrent = it.id == latestRuntime.id) }
    } else {
        merged
    }

    return LivingProjection(
        currentActivity = latestRuntime?.title ?: character.currentActivity,
        currentLocation = latestRuntime?.location ?: character.location,
        currentEvent = latestRuntime,
        timeline = timeline
    )
}
