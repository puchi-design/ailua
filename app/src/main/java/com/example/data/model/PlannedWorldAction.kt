package com.example.data.model

import com.example.data.engine.ScheduledActionType
import kotlinx.serialization.Serializable

/** A proposed future action; it becomes a LifeEvent only when the heartbeat fires it. */
@Serializable
data class PlannedWorldAction(
    val id: String,
    val characterId: String,
    val triggerWorldDate: String,
    val triggerMinutes: Int,
    val type: ScheduledActionType = ScheduledActionType.LIFE_EVENT,
    val lifeEventType: LifeEventType,
    val title: String,
    val description: String,
    val location: String,
    val metadata: Map<String, String> = emptyMap(),
    val imageReference: String? = null,
    val relatedCharacterIds: List<String> = emptyList(),
)
