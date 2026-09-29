package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WorldPlan(
    val createdWorldDate: String,
    val createdMinutes: Int,
    val actions: List<PlannedWorldAction>,
)
