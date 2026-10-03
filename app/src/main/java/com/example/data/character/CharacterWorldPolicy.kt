package com.example.data.character

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.initiative.CharacterInitiativeQuota
import com.example.data.engine.ScheduledActionType
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction

/** Applies initiative preferences to new candidate plans only; stored facts are never rewritten. */
object CharacterWorldPolicy {
    fun select(
        candidates: List<PlannedWorldAction>,
        existing: List<PlannedWorldAction>,
        events: List<LifeEvent>,
        card: (String) -> CharacterCardData?,
    ): List<PlannedWorldAction> {
        val accepted = mutableListOf<PlannedWorldAction>()
        for (action in candidates) {
            val runtime = card(action.characterId)?.let(CharacterRuntimeResolver::resolve) ?: CharacterRuntimeResolver.resolve(action.characterId)
            val kind = CharacterInitiativeQuota.kind(action)
            if (kind == null) {
                accepted.add(action)
                continue
            }
            if (CharacterBehaviorRuntime.isSleeping(runtime, action.triggerMinutes)) continue
            val plannedCount = (existing + accepted).count {
                it.characterId == action.characterId && it.triggerWorldDate == action.triggerWorldDate && CharacterInitiativeQuota.kind(it) == kind
            }
            val eventCount = CharacterInitiativeQuota.used(action.characterId, action.triggerWorldDate, kind, events)
            if (plannedCount + eventCount < CharacterInitiativeQuota.cap(runtime, kind)) accepted.add(action)
        }
        return accepted
    }

}
