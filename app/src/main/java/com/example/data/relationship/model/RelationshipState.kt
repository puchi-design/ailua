package com.example.data.relationship.model

import kotlinx.serialization.Serializable

@Serializable
enum class RelationshipStage { STRANGER, ACQUAINTANCE, FAMILIAR, CLOSE, INTIMATE, STRAINED }

@Serializable
data class RelationshipState(
    val fromCharacterId: String,
    val toCharacterId: String,
    val affinity: Int = 0,
    val trust: Int = 0,
    val familiarity: Int = 0,
    val tension: Int = 0,
    val lastMeaningfulInteractionAt: String? = null,
    val interactionCount: Int = 0,
    val sharedMemoryCount: Int = 0,
    val stage: RelationshipStage = RelationshipStage.STRANGER,
    val updatedAt: String = "",
    val recentInteraction: String? = null,
    val sharedMemory: String? = null,
    val processedEventIds: Set<String> = emptySet(),
)

data class RelationshipDelta(
    val affinity: Int = 0,
    val trust: Int = 0,
    val familiarity: Int = 0,
    val tension: Int = 0,
    val meaningful: Boolean = false,
    val sharedMemory: Boolean = false,
)
