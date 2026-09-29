package com.example.data.engine

import android.content.Context
import com.example.data.ai.repository.ProviderGraph
import com.example.data.ai.runtime.ActiveProfileProviderResolver
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldClock
import com.example.data.model.WorldPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WorldPlanRuntime {
    private var planner: WorldActionPlanner? = null
    private var hasProvider: () -> Boolean = { false }
    private var generating = false
    private var lastFailedAt: Pair<String, Int>? = null

    fun init(context: Context) {
        if (planner != null) return
        val resolver = ActiveProfileProviderResolver(ProviderGraph.repository)
        hasProvider = { resolver.resolve() != null }
        planner = WorldActionPlanner(resolver, MemoryGraph.repository)
    }

    suspend fun maybePlan(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val engine = WorldHeartbeatEngine
        val clock = engine.worldClock.value
        val future = engine.futureActions().filter {
            val offset = WorldPlanValidator.dayOffset(clock.dateLabel, it.triggerWorldDate)
            offset != null && offset * 1440 + it.triggerMinutes > clock.minutesOfDay
        }
        val planDate = AiluaLocalStore.savedWorldPlan.value?.createdWorldDate
        if (!force && !needsPlan(clock, future, planDate)) return@withContext true
        if (!force && lastFailedAt?.first == clock.dateLabel && clock.minutesOfDay - (lastFailedAt?.second ?: 0) in 0..29) return@withContext false
        if (generating) return@withContext false
        generating = true
        try {
            val activePlanner = planner ?: return@withContext false
            val plan = if (hasProvider()) activePlanner.generate(clock, WorldStateRepository.latestEvents(12), future)
                else if (future.size < 2) fallback(clock) else null
            val installed = plan != null && engine.installPlan(plan)
            if (installed) lastFailedAt = null else lastFailedAt = clock.dateLabel to clock.minutesOfDay
            installed
        } finally { generating = false }
    }

    fun needsPlan(clock: WorldClock, future: List<PlannedWorldAction>, lastPlanDate: String?): Boolean =
        future.size < 2 || lastPlanDate != clock.dateLabel

    private fun fallback(clock: WorldClock): WorldPlan {
        val ideas = listOf(
            Triple("mira", LifeEventType.THOUGHT, "小弥整理桌上的手记"),
            Triple("yuna", LifeEventType.MEAL, "悠奈准备一份简餐"),
            Triple("noa", LifeEventType.PHOTO, "诺亚拍下书阁窗边的光影"),
            Triple("mira", LifeEventType.DIARY, "小弥写下今天的日记"),
        )
        val actions = ideas.mapIndexed { index, (character, kind, title) ->
            val advanced = WorldTimeAdvancer.advance(clock.minutesOfDay, clock.dateLabel, (index + 1) * 75)
            PlannedWorldAction(
                id = "fallback_${clock.dateLabel}_${clock.minutesOfDay}_$index",
                characterId = character, triggerWorldDate = advanced.newDateLabel,
                triggerMinutes = advanced.newMinutes, lifeEventType = kind,
                title = title, description = title, location = when (character) {
                    "yuna" -> "街角全家便利店"
                    "noa" -> "月光书阁"
                    else -> "青石街23号"
                },
            )
        }
        return WorldPlan(clock.dateLabel, clock.minutesOfDay, actions)
    }
}
