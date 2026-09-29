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
            val plan = proposed ?: if (future.size < 2) fallback(clock, future, WorldStateRepository.events.value) else null
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

    internal fun fallback(clock: WorldClock): WorldPlan = fallback(clock, emptyList(), emptyList())!!

    internal fun fallback(clock: WorldClock, existing: List<PlannedWorldAction>, events: List<com.example.data.model.LifeEvent>): WorldPlan? {
        val characters = com.example.data.registry.CharacterRegistry.getAllCharacters().take(3)
        if (characters.isEmpty()) return null
        val kinds = listOf(LifeEventType.THOUGHT, LifeEventType.MEAL, LifeEventType.PHOTO,
            LifeEventType.TRAVEL, LifeEventType.SOCIAL, LifeEventType.MEMORY, LifeEventType.SLEEP)
        for (shift in listOf(75, 105, 135, 165)) for (rotation in kinds.indices) {
            val actions = (0..2).map { index ->
                val character = characters[index % characters.size]
                val kind = kinds[(rotation + index) % kinds.size]
                val advanced = WorldTimeAdvancer.advance(clock.minutesOfDay, clock.dateLabel, shift + index * 75)
                val title = when (kind) {
                    LifeEventType.THOUGHT -> "${character.name}整理手记"
                    LifeEventType.MEAL -> "${character.name}准备简餐"
                    LifeEventType.PHOTO -> "${character.name}拍下窗边光影"
                    LifeEventType.TRAVEL -> "${character.name}在街上散步"
                    LifeEventType.SOCIAL -> "${character.name}与朋友聊近况"
                    LifeEventType.MEMORY -> "${character.name}回想一段往事"
                    else -> "${character.name}休息片刻"
                }
                PlannedWorldAction(
                    id = "fallback_${clock.dateLabel}_${clock.minutesOfDay}_${shift}_${rotation}_$index",
                    characterId = character.id, triggerWorldDate = advanced.newDateLabel,
                    triggerMinutes = advanced.newMinutes, lifeEventType = kind,
                    title = title, description = title, location = character.location,
                )
            }
            val candidate = WorldPlan(clock.dateLabel, clock.minutesOfDay, actions)
            if (WorldPlanValidator.validate(candidate, clock, existing, events)) return candidate
        }
        return null
    }
}
