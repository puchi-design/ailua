package com.example.data.engine

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldClock
import com.example.data.model.WorldPlan
import com.example.data.registry.CharacterRegistry

object WorldPlanValidator {
    private val phoneKeys = setOf("search_query", "draft", "note", "music_title", "music_artist", "music_cover", "music_duration", "browsing_query", "saved_item", "hidden_thought", "photo_title", "mood", "moodTag", "weather")

    fun validate(plan: WorldPlan, clock: WorldClock, existing: List<PlannedWorldAction>, events: List<LifeEvent>, characterIds: Set<String> = CharacterRegistry.allCards.value.keys): Boolean {
        if (plan.createdWorldDate != clock.dateLabel || plan.createdMinutes != clock.minutesOfDay || plan.actions.size !in 3..6) return false
        val ids = (existing.map { it.id } + plan.actions.map { it.id })
        if (ids.size != ids.toSet().size) return false
        if (plan.actions.any { action -> events.any { it.sourceRefId == action.id && it.sourceAppId == "heartbeat" } }) return false
        val currentDayTypes = events.filter { it.worldDateLabel == clock.dateLabel && it.sourceAppId == "heartbeat" }.groupingBy { it.type }.eachCount()
        val candidateTypes = (existing + plan.actions).groupBy { it.triggerWorldDate }.mapValues { (_, actions) -> actions.groupingBy { it.lifeEventType }.eachCount() }
        val contacts = plan.actions.count { it.type == ScheduledActionType.INCOMING_CALL || it.type == ScheduledActionType.LETTER_DELIVERY || it.lifeEventType == LifeEventType.MESSAGE || "user" in it.relatedCharacterIds }
        val existingContacts = existing.count { it.type == ScheduledActionType.INCOMING_CALL || it.type == ScheduledActionType.LETTER_DELIVERY || it.lifeEventType == LifeEventType.MESSAGE || "user" in it.relatedCharacterIds }
        val todayContacts = events.count { it.worldDateLabel == clock.dateLabel && it.sourceAppId == "heartbeat" && (it.type == LifeEventType.MESSAGE || "user" in it.relatedCharacterIds) }
        if (contacts + existingContacts + todayContacts > 2 || contacts * 2 >= plan.actions.size) return false
        if (plan.actions.any { action ->
                val offset = dayOffset(clock.dateLabel, action.triggerWorldDate) ?: return@any true
                val distance = offset * 1440 + action.triggerMinutes - clock.minutesOfDay
                action.id.isBlank() || action.id.length > 100 || action.characterId !in characterIds ||
                    action.relatedCharacterIds.any { it !in characterIds && it != "user" || it == action.characterId } ||
                    action.triggerMinutes !in 0..1439 || distance !in 1..720 ||
                    action.title.isBlank() || action.description.isBlank() || action.location.isBlank() ||
                    action.title.length > 120 || action.description.length > 600 || action.metadata.keys.any { it !in phoneKeys } ||
                    action.metadata.values.any { it.length > 300 } ||
                    (action.type == ScheduledActionType.INCOMING_CALL && action.lifeEventType != LifeEventType.MESSAGE) ||
                    (action.type == ScheduledActionType.GALLERY_ASSET || action.type == ScheduledActionType.LETTER_DELIVERY) ||
                    (action.type == ScheduledActionType.MOMENT && action.lifeEventType != LifeEventType.MOMENT) ||
                    (action.type == ScheduledActionType.LOCATION_CHANGE && action.lifeEventType != LifeEventType.LOCATION_CHANGE)
            }) return false
        if (candidateTypes.any { (date, counts) -> counts.any { (type, count) -> count + (if (date == clock.dateLabel) currentDayTypes[type] ?: 0 else 0) > 4 } }) return false
        val signatures = (existing + plan.actions).map { "${it.characterId}|${it.triggerWorldDate}|${it.triggerMinutes}|${it.lifeEventType}|${it.title.trim()}" }
        return signatures.size == signatures.toSet().size
    }

    fun dayOffset(from: String, to: String): Int? = when (to) {
        from -> 0
        WorldTimeAdvancer.advanceDateLabel(from, 1) -> 1
        else -> null
    }

    fun dayOffsetWithin(from: String, to: String, maxDays: Int): Int? =
        (0..maxDays).firstOrNull { WorldTimeAdvancer.advanceDateLabel(from, it) == to }
}
