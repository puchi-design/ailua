package com.example.data.engine

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.runtime.ProviderResolver
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.repository.EpochClock
import com.example.data.memory.repository.MemoryRepository
import com.example.data.model.CharacterProfile
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.ProactiveSettings
import com.example.data.model.ProactiveState
import com.example.data.model.WeatherState
import com.example.data.registry.CharacterRegistry
import com.example.data.context.CharacterContext
import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.systemui.notification.NotificationEvents
import com.example.data.systemui.notification.VirtualNotification
import com.example.data.systemui.notification.VirtualNotificationGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pure rule evaluation for P3D-2 proactive messages — real device time only,
 * no Android types, fully unit-testable.
 */
object ProactiveRules {

    /** Wrap-aware quiet-window test: [minuteOfDay] in [start, end) across midnight. */
    fun isQuiet(minuteOfDay: Int, start: Int, end: Int): Boolean =
        if (start <= end) minuteOfDay in start until end else minuteOfDay >= start || minuteOfDay < end

    /** Whether the rules allow one proactive message right now. */
    fun shouldFire(
        settings: ProactiveSettings,
        state: ProactiveState,
        nowEpochMs: Long,
        minuteOfDay: Int,
        today: String,
    ): Boolean {
        if (!settings.enabled) return false
        val intervalMs = settings.intervalHours * 3_600_000L
        if (nowEpochMs - state.lastSuccessAtEpochMs < intervalMs) return false
        if (isQuiet(minuteOfDay, settings.quietStartMinute, settings.quietEndMinute)) return false
        if (state.sentDate == today && state.sentCount >= settings.dailyLimit) return false
        return true
    }

    /** State after one successful send: rolls the per-day counter on day change. */
    fun withSuccess(state: ProactiveState, nowEpochMs: Long, today: String): ProactiveState =
        if (state.sentDate == today) {
            state.copy(lastSuccessAtEpochMs = nowEpochMs, sentCount = state.sentCount + 1)
        } else {
            state.copy(lastSuccessAtEpochMs = nowEpochMs, sentDate = today, sentCount = 1)
        }
}

/**
 * ProactiveMessageEngine — P3D-2: on every heartbeat tick, fire ONE real
 * proactive message when the rules allow it: Character + World + Memory +
 * recent chat -> one non-streaming provider call -> append the assistant
 * turn to ChatRepository + record a LifeEvent.
 *
 * Contract:
 * - Reuses the existing heartbeat ticker (MainActivity RESUMED loop) — this
 *   class is NOT a second scheduler; [maybeFire] only checks rules and launches.
 * - Real device time for interval/quiet/daily (virtual clock freezes in bg).
 * - Provider unconfigured / call failure / empty reply -> write NOTHING,
 *   rules are retried on the next tick.
 * - [force] skips the timing rules (dev-console test button) but still
 *   performs a real call and real writes.
 */
class ProactiveMessageEngine(
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val providerResolver: ProviderResolver,
    private val clock: EpochClock,
    private val loadSettings: () -> ProactiveSettings,
    private val loadState: () -> ProactiveState,
    private val saveState: (ProactiveState) -> Unit,
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val realityContext: suspend () -> String? = { null },
    private val recentWorldEvents: () -> List<LifeEvent> = { WorldStateRepository.latestEvents(20) },
    private val postNotification: suspend (VirtualNotification) -> Unit = { VirtualNotificationGraph.post(it) },
    private val activeCharacterId: () -> String = { CharacterContext.currentId() },
) {

    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Non-blocking entry called from the heartbeat ticker. */
    fun maybeFire() {
        if (!inFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                fireIfDue()
            } finally {
                inFlight.set(false)
            }
        }
    }

    /** Dev-console entry: same pipeline, timing rules skipped. */
    fun forceFire() {
        if (!inFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                fireIfDue(force = true)
            } finally {
                inFlight.set(false)
            }
        }
    }

    /** One synchronous evaluation; returns true when a message was sent. */
    suspend fun fireIfDue(force: Boolean = false): Boolean {
        val now = clock.nowEpochMs()
        val characterId = activeCharacterId()
        val card = CharacterRegistry.getCard(characterId)
        val extension = card?.data?.let(AiluaCharacterExtensionCodec::readOrNull)
        val settings = CharacterBehaviorRuntime.proactiveSettings(loadSettings(), extension)
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = now }
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = this@ProactiveMessageEngine.timeZone }
            .format(Date(now))
        val state = loadState()

        if (!force && !ProactiveRules.shouldFire(settings, state, now, minuteOfDay, today)) return false
        if (!force && CharacterBehaviorRuntime.isSleeping(extension, WorldHeartbeatEngine.worldClock.value.minutesOfDay)) return false
        if (!force && UserContactCooldown.recentlyContacted(WorldHeartbeatEngine.worldClock.value, recentWorldEvents())) return false
        val reality = realityContext()
        if (!force && reality?.contains("电量较低") == true) return false
        val resolved = providerResolver.resolve() ?: return false

        val character = CharacterRegistry.getCharacter(characterId)
        val session = chatRepository.getOrCreatePrivateSession(characterId)

        val content = completeOnce(resolved, buildPrompt(character, characterId, reality))?.trim()
        if (content.isNullOrEmpty()) return false

        withContext(NonCancellable) {
            val turn = chatRepository.appendAssistantTurn(
                sessionId = session.id,
                content = content,
                status = VariantStatus.COMPLETE,
                providerProfileId = resolved.profileId,
                model = resolved.model,
            )
            WorldStateRepository.appendLifeEvent(
                LifeEvent(
                    id = "proactive_$now",
                    characterId = characterId,
                    time = WorldHeartbeatEngine.worldClock.value.timeFormatted,
                    type = LifeEventType.THOUGHT,
                    title = "${character.name}主动发来消息",
                    description = content,
                    location = character.location,
                    worldDateLabel = WorldHeartbeatEngine.worldClock.value.dateLabel,
                    worldMinutesOfDay = WorldHeartbeatEngine.worldClock.value.minutesOfDay,
                    sourceAppId = "heartbeat",
                    sourceRefId = "proactive_message",
                )
            )
            saveState(ProactiveRules.withSuccess(state, now, today))
            postNotification(
                NotificationEvents.proactiveMessage(
                    sessionId = session.id,
                    turnId = turn.id,
                    characterId = characterId,
                    characterName = character.name,
                    content = content,
                    timestampEpochMs = now,
                ),
            )
        }
        return true
    }

    private fun buildPrompt(character: CharacterProfile, characterId: String, reality: String?): List<AiMessage> {
        val clockValue = WorldHeartbeatEngine.worldClock.value
        val phase = WorldHeartbeatEngine.heartbeatState.value.currentPhase.label
        val weather = when (clockValue.weather) {
            WeatherState.RAIN -> "下着雨"
            WeatherState.CLOUDY -> "阴天"
            else -> "天色平静"
        }

        val memories = memoryRepository.getMemoriesForPrompt(characterId, limit = 5)
        val session = chatRepository.getOrCreatePrivateSession(characterId)
        val recent = chatRepository.getResolvedTurns(session.id)
            .takeLast(6)
            .mapNotNull { turn ->
                val variant = turn.activeVariant ?: return@mapNotNull null
                if (variant.status != VariantStatus.COMPLETE || variant.content.isBlank()) return@mapNotNull null
                when (turn.role) {
                    ChatTurnRole.USER -> "用户：${variant.content.trim()}"
                    ChatTurnRole.ASSISTANT -> "${character.name}：${variant.content.trim()}"
                }
            }

        val system = "你是${character.name}。现在是${clockValue.dateLabel} ${clockValue.timeFormatted}，${weather}，${phase}。" +
            "你主动给用户发 1 条消息：2-3 句，自然口语，结合你们最近的对话、你的记忆或当下情境；" +
            "不要干巴巴的问候，不要提到你是 AI 或模型。只输出消息本身，不要引号和署名。"

        val context = buildList {
            if (memories.isNotEmpty()) {
                add("【你的记忆】" + memories.joinToString("；") { it.content.trim() })
            }
            if (recent.isNotEmpty()) {
                add("【最近对话】\n" + recent.joinToString("\n"))
            } else {
                add("【最近对话】还没有对话，随意开启话题。")
            }
            if (reality != null) add(reality.take(300))
        }.joinToString("\n")

        val characterMessages = CharacterRegistry.getCard(characterId)?.let { card ->
            PromptAssembler.assemble(PromptAssemblyInput(character = card.data)).messages
        }.orEmpty()
        return characterMessages + listOf(AiMessage(AiRole.SYSTEM, system), AiMessage(AiRole.USER, context))
    }

    /** One non-streaming call; `null` on any failure (nothing is written). */
    private suspend fun completeOnce(
        resolved: ResolvedProvider,
        messages: List<AiMessage>,
    ): String? {
        val request = AiChatRequest(model = resolved.model, messages = messages, stream = false)
        var text: String? = null
        var failed = false
        try {
            resolved.provider.streamChat(request).collect { event ->
                when (event) {
                    is AiStreamEvent.Completed -> text = event.text
                    is AiStreamEvent.Failed -> failed = true
                    AiStreamEvent.Cancelled -> failed = true
                    else -> Unit
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return null
        }
        return if (failed) null else text?.takeIf { it.isNotBlank() }
    }
}
