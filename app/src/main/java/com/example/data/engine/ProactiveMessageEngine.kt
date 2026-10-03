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
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.character.runtime.InitiativeTrigger
import com.example.data.character.initiative.CharacterInitiativeQuota
import com.example.data.character.initiative.CharacterInitiativeRuntime
import com.example.data.character.initiative.InitiativeEvidence
import com.example.data.character.initiative.InitiativeDecision
import com.example.data.character.initiative.InitiativeFacts
import com.example.data.character.initiative.ProactiveContentType
import com.example.data.character.initiative.projectInitiativeLetters
import com.example.data.model.WorldClock
import com.example.data.model.isUserActivity
import com.example.data.relationship.romance.RomanceRepository
import com.example.data.repository.MailboxRepository
import com.example.data.systemui.notification.NotificationCategory
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
    private val recentWorldEvents: () -> List<LifeEvent> = { WorldStateRepository.events.value },
    private val postNotification: suspend (VirtualNotification) -> Unit = { VirtualNotificationGraph.post(it) },
    private val activeCharacterId: () -> String = { CharacterContext.currentId() },
    private val resolveRuntime: (String) -> CharacterRuntimeProfile = CharacterRuntimeResolver::resolve,
    private val worldClock: () -> WorldClock = { WorldHeartbeatEngine.worldClock.value },
    private val sample: (String) -> Double = CharacterInitiativeRuntime::stableSample,
    private val evidenceOverride: ((CharacterRuntimeProfile, Long, WorldClock) -> List<InitiativeEvidence>)? = null,
) {

    private val inFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Failed provider requests do not consume quota; wait for the next opportunity window before retrying.
    private var attemptedWindow: String? = null

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
    suspend fun fireIfDue(force: Boolean = false, forceContentType: ProactiveContentType = ProactiveContentType.TEXT): Boolean {
        val now = clock.nowEpochMs()
        val characterId = activeCharacterId()
        val runtime = resolveRuntime(characterId)
        val settings = CharacterInitiativeRuntime.contactSettings(loadSettings(), runtime)
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = now }
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = this@ProactiveMessageEngine.timeZone }.format(Date(now))
        val state = loadState()
        val world = worldClock()
        val events = recentWorldEvents()
        if (!force && !ProactiveRules.shouldFire(settings, state, now, minuteOfDay, today)) return false
        if (!force && CharacterBehaviorRuntime.isSleeping(runtime, world.minutesOfDay)) return false
        if (!force && UserContactCooldown.recentlyContacted(world, events)) return false
        if (!force && CallStateEngine.currentCall.value != null) return false
        val reality = realityContext()
        if (!force && reality?.contains("电量较低") == true) return false
        val resolved = providerResolver.resolve() ?: return false
        val character = CharacterRegistry.getCharacter(characterId)
        val session = chatRepository.getOrCreatePrivateSession(characterId)
        val romance = RomanceRepository.triggerFacts(characterId, now)
        if (!force && romance.recentConflict && !romance.readyToReconnect) return false
        val memories = memoryRepository.getMemoriesForPrompt(characterId, limit = 5)
        val importantMemory = memories.firstOrNull { it.importance >= .6 }
        val facts = InitiativeFacts(
            lastUserAtEpochMs = chatRepository.getResolvedTurns(session.id).lastOrNull { it.role == ChatTurnRole.USER }?.createdAtEpochMs,
            importantMemoryId = importantMemory?.id, importantMemory = importantMemory?.content,
            conflictEventId = romance.conflictEventId, readyToReconnect = romance.readyToReconnect,
            recentGoodEventAtEpochMs = romance.recentGoodEventAtEpochMs,
        )
        val evidence = evidenceOverride?.invoke(runtime, now, world)
            ?: CharacterInitiativeRuntime.evidence(runtime, now, today, world, facts, events)
        val decision = if (force) InitiativeDecision(forceContentType,
            InitiativeEvidence(InitiativeTrigger.SHARED_MEMORY, "developer:$now", "这是用户主动触发的测试联系，不编造共同记忆。"), "developer:$characterId:$now")
        else CharacterInitiativeRuntime.decide(runtime, now, evidence, events, world.dateLabel, sample) ?: return false
        if (!force && attemptedWindow == decision.windowKey) return false
        attemptedWindow = decision.windowKey
        val relationshipRecord = RomanceRepository.record(characterId)
        val relationshipInstructions = RomanceRepository.promptInstructions(characterId, now)
        val content = completeOnce(resolved, buildPrompt(character, runtime, reality, decision, relationshipInstructions))?.trim()
        if (content.isNullOrEmpty()) return false

        return withContext(NonCancellable) {
            val notification = synchronized(CharacterInitiativeQuota) {
                val commitNow = clock.nowEpochMs()
                val commitCalendar = Calendar.getInstance(timeZone).apply { timeInMillis = commitNow }
                val commitMinute = commitCalendar.get(Calendar.HOUR_OF_DAY) * 60 + commitCalendar.get(Calendar.MINUTE)
                val commitDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = this@ProactiveMessageEngine.timeZone }.format(Date(commitNow))
                val commitWorld = worldClock()
                val commitRuntime = resolveRuntime(characterId)
                val commitEvents = recentWorldEvents()
                val commitState = loadState()
                val commitRomance = RomanceRepository.triggerFacts(characterId, commitNow)
                // Timing bypass never authorizes delivery generated against a superseded boundary/relationship.
                if (RomanceRepository.record(characterId) != relationshipRecord ||
                    RomanceRepository.promptInstructions(characterId, commitNow) != relationshipInstructions ||
                    (!force && commitRomance.recentConflict && !commitRomance.readyToReconnect)) return@synchronized null
                if (!force && (!ProactiveRules.shouldFire(CharacterInitiativeRuntime.contactSettings(loadSettings(), commitRuntime), commitState, commitNow, commitMinute, commitDate) ||
                    CharacterBehaviorRuntime.isSleeping(commitRuntime, commitWorld.minutesOfDay) ||
                    !CharacterInitiativeQuota.available(commitRuntime, commitWorld.dateLabel, CharacterInitiativeQuota.kind(decision.type), commitEvents) ||
                    (CharacterInitiativeRuntime.contentWeights(commitRuntime)[decision.type] ?: 0.0) <= 0.0 ||
                    UserContactCooldown.recentlyContacted(commitWorld, commitEvents) ||
                    CallStateEngine.currentCall.value != null ||
                    commitEvents.any { it.characterId == characterId && (it.metadata["initiative_window"] == decision.windowKey || it.metadata["initiative_evidence"] == decision.evidence.key) })) return@synchronized null
                val turns = if (decision.type == ProactiveContentType.TEXT || decision.type == ProactiveContentType.CALL_INVITE) {
                    val chunks = if (decision.type == ProactiveContentType.TEXT) CharacterInitiativeRuntime.splitText(content, runtime.initiative.maxTextBurst) else listOf(content)
                    chunks.map { chunk -> chatRepository.appendAssistantTurn(sessionId = session.id, content = chunk,
                        status = VariantStatus.COMPLETE, providerProfileId = resolved.profileId, model = resolved.model) }
                } else emptyList()
                val event = WorldStateRepository.appendLifeEvent(CharacterInitiativeRuntime.fact(commitRuntime, decision, content, commitNow, commitWorld))
                if (decision.type == ProactiveContentType.LETTER) MailboxRepository.syncRuntimeLetters(WorldStateRepository.events.value)
                // One provider completion/burst consumes one contact, regardless of projected media or bubble count.
                saveState(ProactiveRules.withSuccess(commitState, commitNow, commitDate))
                when (decision.type) {
                    ProactiveContentType.TEXT, ProactiveContentType.CALL_INVITE -> NotificationEvents.proactiveMessage(
                        sessionId = session.id, turnId = turns.first().id, characterId = characterId,
                        characterName = character.name, content = content, timestampEpochMs = now)
                    ProactiveContentType.LETTER -> NotificationEvents.mailDelivered(projectInitiativeLetters(listOf(event)).single(), now)
                    else -> VirtualNotification(sourceKey = "initiative:${event.id}", sourceAppId = event.sourceAppId,
                        title = event.title, body = content, characterId = characterId, timestampEpochMs = now,
                        category = NotificationCategory.WORLD, route = if (decision.type == ProactiveContentType.PHOTO) "gallery" else "moments")
                }
            } ?: return@withContext false
            postNotification(notification)
            true
        }
    }

    private fun buildPrompt(character: CharacterProfile, runtime: CharacterRuntimeProfile, reality: String?, decision: InitiativeDecision, relationshipInstructions: String): List<AiMessage> {
        val characterId = runtime.characterId
        val clockValue = worldClock()
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

        val actionInstruction = when (decision.type) {
            ProactiveContentType.TEXT -> if (runtime.initiative.maxTextBurst > 1) "主动发最多${runtime.initiative.maxTextBurst}条很短的意群，用换行分隔，合计一次联系。" else "主动发一条自然短消息，长度遵循角色节奏。"
            ProactiveContentType.PHOTO -> "给一张生活照片写简短配文。照片目前使用角色素材槽位；不声称已生成具体人物新照片。"
            ProactiveContentType.CALL_INVITE -> "只发一句询问对方现在是否方便通话的邀请，允许拒绝或稍后；并没有开始通话。"
            ProactiveContentType.LETTER -> "写一封有具体内容的短信，遵循角色口吻，不署系统名。"
            ProactiveContentType.MOMENT -> "写一条公开的、属于你自己生活的动态。不要透露用户私聊、私人记忆、关系状态和触发证据中的私人内容。"
        }
        val system = "你是${character.name}。现在是${clockValue.dateLabel} ${clockValue.timeFormatted}，${weather}，${phase}。" +
            actionInstruction + (if (decision.type == ProactiveContentType.MOMENT) "只描述角色自己的生活，不引用私聊或用户身份。"
                else "触发事实：${decision.evidence.description}。结合真实记忆和对话，不重复上次主动联系。") +
            "不要干巴巴的问候，不要提到你是 AI 或模型。只输出内容，不要 JSON、引号和技术说明。"

        val context = if (decision.type == ProactiveContentType.MOMENT) {
            val publicFacts = recentWorldEvents().filter { it.characterId == characterId && it.visibility != "PRIVATE" && !it.isUserActivity() &&
                it.sourceAppId != "memory" && it.sourceAppId != "chat" && "user" !in it.relatedCharacterIds }
                .take(4).joinToString("\n") { "${it.time} ${it.title.take(80)} ${it.description.take(160)}" }
            "【角色自己的公开生活】\n$publicFacts"
        } else buildList {
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
            PromptAssembler.assemble(PromptAssemblyInput(character = card.data, relationshipInstructions = if (decision.type == ProactiveContentType.MOMENT) null else relationshipInstructions)).messages
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
