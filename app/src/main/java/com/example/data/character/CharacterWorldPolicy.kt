package com.example.data.character

import com.example.data.codec.AiluaCharacterExtensionCodec
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
            val extension = card(action.characterId)?.let(AiluaCharacterExtensionCodec::readOrNull)
            val kind = kind(action)
            if (kind == null) {
                accepted.add(action)
                continue
            }
            if (CharacterBehaviorRuntime.isSleeping(extension, action.triggerMinutes)) continue
            val plannedCount = (existing + accepted).count {
                it.characterId == action.characterId && it.triggerWorldDate == action.triggerWorldDate && kind(it) == kind
            }
            val eventCount = events.count {
                it.characterId == action.characterId && it.worldDateLabel == action.triggerWorldDate && when (kind) {
                    "photo" -> it.type == LifeEventType.PHOTO
                    "moment" -> it.type == LifeEventType.MOMENT
                    // The plan's ringing fact is the autonomous contact. Its later call-summary
                    // fact belongs to the same call and must not consume a second quota slot.
                    else -> it.id.startsWith("pulse_call_plan_")
                }
            }
            if (plannedCount + eventCount < CharacterBehaviorRuntime.dailyActionLimit(extension, kind)) accepted.add(action)
        }
        return accepted
    }

    private fun kind(action: PlannedWorldAction): String? = when {
        action.type == ScheduledActionType.INCOMING_CALL -> "call"
        action.type == ScheduledActionType.MOMENT || action.lifeEventType == LifeEventType.MOMENT -> "moment"
        action.lifeEventType == LifeEventType.PHOTO -> "photo"
        else -> null
    }
}
