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
import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.character.CharacterWorldPolicy
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.registry.CharacterRegistry
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
                try { activePlanner.generate(clock, WorldStateRepository.events.value, future) }
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

    internal fun fallback(
        clock: WorldClock,
        existing: List<PlannedWorldAction>,
        events: List<com.example.data.model.LifeEvent>,
        activeCharacterId: String = com.example.data.context.CharacterContext.currentId(),
    ): WorldPlan? {
        // Keep the selected companion active even when the official catalog grows.
        // Give the remaining slots to characters with fewer recorded/planned events.
        val activityCounts = (events.map { it.characterId } + existing.map { it.characterId }).groupingBy { it }.eachCount()
        val characters = CharacterRegistry.getAllCharacters().sortedWith(
            compareBy({ it.id != activeCharacterId }, { activityCounts[it.id] ?: 0 }),
        ).take(3)
        if (characters.isEmpty()) return null
        val kinds = listOf(LifeEventType.THOUGHT, LifeEventType.MEAL, LifeEventType.PHOTO,
            LifeEventType.TRAVEL, LifeEventType.SOCIAL, LifeEventType.MEMORY, LifeEventType.SLEEP)
        for (shift in listOf(75, 105, 135, 165)) for (rotation in kinds.indices) {
            val actions = (0..2).map { index ->
                val character = characters[index % characters.size]
                val advanced = WorldTimeAdvancer.advance(clock.minutesOfDay, clock.dateLabel, shift + index * 75)
                val runtime = CharacterRuntimeResolver.resolve(character.id)
                val preferences = CharacterBehaviorRuntime.fallbackKinds(runtime)
                val kind = if (CharacterBehaviorRuntime.isSleeping(runtime, advanced.newMinutes)) LifeEventType.SLEEP
                    else preferences[(rotation + index) % preferences.size]
                val title = when (kind) {
                    LifeEventType.THOUGHT -> "${character.name}整理手记"
                    LifeEventType.MEAL -> "${character.name}准备简餐"
                    LifeEventType.PHOTO -> "${character.name}拍下窗边光影"
                    LifeEventType.TRAVEL -> "${character.name}在街上散步"
                    LifeEventType.SOCIAL -> "${character.name}与朋友聊近况"
                    LifeEventType.MEMORY -> "${character.name}回想一段往事"
                    LifeEventType.MOMENT -> "${character.name}分享生活片段"
                    else -> "${character.name}休息片刻"
                }
                PlannedWorldAction(
                    id = "fallback_${clock.dateLabel}_${clock.minutesOfDay}_${shift}_${rotation}_$index",
                    characterId = character.id, triggerWorldDate = advanced.newDateLabel,
                    triggerMinutes = advanced.newMinutes, lifeEventType = kind,
                    title = title,
                    description = if (kind == LifeEventType.THOUGHT && runtime.life.hobbies.isNotEmpty())
                        "${character.name}继续${runtime.life.hobbies.first()}，暂时把手机放在一旁。" else title,
                    location = if (kind == LifeEventType.SLEEP) runtime.life.home.ifBlank { character.location }
                        else runtime.life.workplace.ifBlank { character.location },
                )
            }
            val candidate = WorldPlan(clock.dateLabel, clock.minutesOfDay,
                CharacterWorldPolicy.select(actions, existing, events) { CharacterRegistry.getCard(it)?.data })
            if (WorldPlanValidator.validate(candidate, clock, existing, events)) return candidate
        }
        return null
    }
}
