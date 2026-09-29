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
                    (action.type == ScheduledActionType.LOCATION_CHANGE && action.lifeEventType != LifeEventType.LOCATION_CHANGE) ||
                    (action.triggerMinutes in 0..359 && action.type == ScheduledActionType.INCOMING_CALL) ||
                    (action.triggerMinutes in 0..299 && action.title.contains("午餐")) ||
                    (action.triggerMinutes in 120..299 && (action.title.contains("商场") || action.title.contains("购物")))
            }) return false
        if (candidateTypes.any { (date, counts) -> counts.any { (type, count) -> count + (if (date == clock.dateLabel) currentDayTypes[type] ?: 0 else 0) > 4 } }) return false
        val perCharacter = (existing + plan.actions).groupBy { it.triggerWorldDate to it.characterId }
        if (perCharacter.any { (key, actions) ->
                val fired = events.filter { it.worldDateLabel == key.first && it.characterId == key.second && it.sourceAppId == "heartbeat" }
                fired.count { it.type == LifeEventType.DIARY } + actions.count { it.lifeEventType == LifeEventType.DIARY } > 2 ||
                    fired.count { it.type == LifeEventType.MOMENT } + actions.count { it.lifeEventType == LifeEventType.MOMENT } > 2
            }) return false
        val byCharacter = (existing + plan.actions).groupBy { it.characterId }
        if (byCharacter.values.any { actions ->
                val sorted = actions.sortedBy { (dayOffset(clock.dateLabel, it.triggerWorldDate) ?: 0) * 1440 + it.triggerMinutes }
                sorted.zipWithNext().any { (first, second) ->
                    val gap = (dayOffset(clock.dateLabel, second.triggerWorldDate) ?: 0) * 1440 + second.triggerMinutes -
                        (dayOffset(clock.dateLabel, first.triggerWorldDate) ?: 0) * 1440 - first.triggerMinutes
                    gap < 20 || (gap < 90 && first.lifeEventType == second.lifeEventType && first.lifeEventType in setOf(LifeEventType.MOMENT, LifeEventType.DIARY, LifeEventType.MESSAGE))
                }
            }) return false
        val allContacts = (existing + plan.actions).filter {
            it.type == ScheduledActionType.INCOMING_CALL || it.type == ScheduledActionType.LETTER_DELIVERY ||
                it.lifeEventType == LifeEventType.MESSAGE || "user" in it.relatedCharacterIds
        }.sortedBy { (dayOffset(clock.dateLabel, it.triggerWorldDate) ?: 0) * 1440 + it.triggerMinutes }
        if (allContacts.zipWithNext().any { (first, second) ->
                (dayOffset(clock.dateLabel, second.triggerWorldDate) ?: 0) * 1440 + second.triggerMinutes -
                    (dayOffset(clock.dateLabel, first.triggerWorldDate) ?: 0) * 1440 - first.triggerMinutes < 90
            }) return false
        val lastContact = events.filter(UserContactCooldown::isContact).filter { it.worldMinutesOfDay in 0..1439 }
        if (plan.actions.any { action -> action in allContacts && lastContact.any { event ->
                    val offset = dayOffset(event.worldDateLabel, action.triggerWorldDate) ?: return@any false
                    offset * 1440 + action.triggerMinutes - event.worldMinutesOfDay in 0..89
                } }) return false
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
