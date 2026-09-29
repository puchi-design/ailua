package com.example.data.relationship.engine

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.relationship.model.RelationshipDelta
import com.example.data.relationship.model.RelationshipStage
import com.example.data.relationship.model.RelationshipState

object RelationshipReducer {
    fun delta(event: LifeEvent): RelationshipDelta = when (event.type) {
        LifeEventType.MESSAGE -> RelationshipDelta(affinity = 1, familiarity = 1, meaningful = true)
        LifeEventType.SOCIAL, LifeEventType.MEMORY -> RelationshipDelta(affinity = 1, trust = 2, familiarity = 1, meaningful = true, sharedMemory = true)
        LifeEventType.SURPRISE -> RelationshipDelta(affinity = 1, trust = 1, familiarity = 1, meaningful = true, sharedMemory = true)
        else -> RelationshipDelta(familiarity = 1)
    }

    fun apply(state: RelationshipState, event: LifeEvent, delta: RelationshipDelta = delta(event)): RelationshipState {
        if (event.id in state.processedEventIds) return state
        val affinity = (state.affinity + delta.affinity.coerceIn(-3, 3)).coerceIn(0, 100)
        val trust = (state.trust + delta.trust.coerceIn(-3, 3)).coerceIn(0, 100)
        val familiarity = (state.familiarity + delta.familiarity.coerceIn(0, 2)).coerceIn(0, 100)
        val tension = (state.tension + delta.tension.coerceIn(-3, 3)).coerceIn(0, 100)
        val stage = when {
            tension >= 35 -> RelationshipStage.STRAINED
            affinity >= 80 && trust >= 70 && familiarity >= 70 -> RelationshipStage.INTIMATE
            affinity >= 55 && trust >= 40 && familiarity >= 40 -> RelationshipStage.CLOSE
            familiarity >= 20 && trust >= 15 -> RelationshipStage.FAMILIAR
            else -> RelationshipStage.ACQUAINTANCE
        }
        val dateTime = "${event.worldDateLabel} ${event.time}"
        return state.copy(
            affinity = affinity, trust = trust, familiarity = familiarity, tension = tension,
            lastMeaningfulInteractionAt = if (delta.meaningful) dateTime else state.lastMeaningfulInteractionAt,
            interactionCount = state.interactionCount + 1,
            sharedMemoryCount = state.sharedMemoryCount + if (delta.sharedMemory) 1 else 0,
            stage = stage, updatedAt = dateTime, recentInteraction = event.title,
            sharedMemory = if (delta.sharedMemory) event.description.take(160) else state.sharedMemory,
            processedEventIds = (state.processedEventIds + event.id).toList().takeLast(256).toSet(),
        )
    }
}
