package com.example.data.engine

import android.content.Context
import com.example.data.ai.repository.ProviderGraph
import com.example.data.ai.runtime.ActiveProfileProviderResolver
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph
import com.example.data.reality.RealityContextPolicy
import com.example.data.reality.RealityRepository
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldClock
import com.example.data.model.WorldPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object WorldPlanRuntime {
    private var planner: WorldActionPlanner? = null
    private var hasProvider: () -> Boolean = { false }
    private val generating = AtomicBoolean(false)
    private var lastFailedAt: Pair<String, Int>? = null

    fun init(context: Context) {
        if (planner != null) return
        val resolver = ActiveProfileProviderResolver(ProviderGraph.repository)
        hasProvider = { resolver.resolve() != null }
        val chat = SqlDelightChatRepository(
            ChatDatabase(ChatDriverFactory(context.applicationContext).createDriver()),
            UuidIdGenerator(), SystemEpochClock()
        )
        planner = WorldActionPlanner(resolver, MemoryGraph.repository, chat) {
            RealityContextPolicy.context(RealityRepository.refresh(), RealityRepository.settings.value)
        }
    }

    suspend fun maybePlan(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!generating.compareAndSet(false, true)) return@withContext false
        try {
            val engine = WorldHeartbeatEngine
            val clock = engine.worldClock.value
            val future = engine.futureActions().filter {
                val offset = WorldPlanValidator.dayOffset(clock.dateLabel, it.triggerWorldDate)
                offset != null && offset * 1440 + it.triggerMinutes > clock.minutesOfDay
            }
            if (!force && !needsPlan(clock, future)) return@withContext true
            if (!force && lastFailedAt?.first == clock.dateLabel && clock.minutesOfDay - (lastFailedAt?.second ?: 0) in 0..29) return@withContext false
            val activePlanner = planner ?: return@withContext false
            val proposed = if (hasProvider()) {
                try { activePlanner.generate(clock, WorldStateRepository.latestEvents(12), future) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { null }
            } else null
            val plan = proposed ?: if (future.size < 2) fallback(clock) else null
            val installed = plan != null && try { engine.installPlan(plan) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { false }
            if (installed) lastFailedAt = null else lastFailedAt = clock.dateLabel to clock.minutesOfDay
            installed
        } finally { generating.set(false) }
    }

    fun needsPlan(clock: WorldClock, future: List<PlannedWorldAction>, lastPlanDate: String?): Boolean =
        needsPlan(clock, future)

    fun needsPlan(clock: WorldClock, future: List<PlannedWorldAction>): Boolean = future.size < 2

    internal fun fallback(clock: WorldClock): WorldPlan {
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
