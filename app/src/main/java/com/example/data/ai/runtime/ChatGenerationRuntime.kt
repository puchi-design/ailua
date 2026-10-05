package com.example.data.ai.runtime

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptMemory
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.GroupMessage
import com.example.data.chat.model.GroupReply
import com.example.data.chat.model.GroupSpeakerPlanner
import com.example.data.chat.model.ResolvedChatTurn
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.rich.RichMessageParser
import com.example.data.chat.rich.RichMessagePrompt
import com.example.data.memory.repository.MemoryRepository
import com.example.data.memory.model.MemoryType
import com.example.data.firstsession.FirstSessionPolicy
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.registry.CharacterRegistry
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.relationship.romance.RomanceRepository
import com.example.data.model.CharacterCardData
import com.example.data.projection.CharacterPresence
import com.example.data.reality.RealityContextPolicy
import com.example.data.reality.RealityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Terminal outcome of one generation attempt (send or regenerate). */
sealed interface SendResult {
    /** No active provider / API key — "请先配置 AI 连接"; NO turn was written. */
    data object NotConfigured : SendResult

    /** Character card not found; NO turn was written. */
    data object NoCharacter : SendResult

    /** Generation ran and the provider reported a full [Completed] reply. */
    data object Completed : SendResult

    /** Provider reported a typed error; the variant is persisted as FAILED. */
    data class Failed(val error: AiProviderError) : SendResult

    /** Provider emitted a terminal Cancelled event; variant persisted as CANCELLED. */
    data object Cancelled : SendResult

    /** Regenerate called with no assistant turn to rebuild. */
    data object NothingToRegenerate : SendResult
}

/** Live streaming slice surfaced to the UI while a reply is being generated (P3C-4 §8). */
data class StreamingState(
    val sessionId: String,
    val turnId: String,
    val variantId: String,
    val text: String,
    val speakerId: String? = null,
)

/**
 * ChatGenerationRuntime — send / regenerate / cancel pipeline (P3C-4 §1).
 *
 * Flow (send): provider+character preflight → getOrCreatePrivateSession →
 * recoverInterruptedVariants → appendUserTurn → read resolved history →
 * build PromptAssemblyInput from REAL sources → PromptAssembler →
 * appendAssistantTurn(STREAMING) → provider stream → single terminal
 * updateVariant(COMplete/FAILED/CANCELLED with partial text).
 *
 * Flow (regenerate): same, but history stops BEFORE the latest assistant turn
 * and the result lands in [ChatRepository.appendVariant] on that existing
 * turn (P3C-3 §11 semantics — never a new turn).
 *
 * Rules locked by spec:
 * - Preflight failure (No provider / no character) writes NOTHING (§5).
 * - Streaming text goes to [streaming] StateFlow only — never per-token DB
 *   writes; exactly one updateVariant on terminal (§8).
 * - Cancellation = collector Job cancel → flow cancel → partial text
 *   persisted as CANCELLED under NonCancellable, then rethrow (§12).
 * - Failures persist typed error metadata and surface [SendResult.Failed] —
 *   there is no canned/fake fallback path anywhere (§13).
 */
class ChatGenerationRuntime(
    private val repository: ChatRepository,
    private val providerResolver: ProviderResolver,
    private val promptContext: ChatPromptContext,
    private val memoryRepository: MemoryRepository,
) {

    companion object {
        /** P3D-3: newest LifeEvent facts injected into one prompt. */
        internal const val MAX_PROMPT_LIFE_EVENTS = 12
    }

    private val _streaming = MutableStateFlow<StreamingState?>(null)

    /** Live streaming state for the active generation; `null` when idle. */
    val streaming: StateFlow<StreamingState?> = _streaming.asStateFlow()

    /** Group messages share the same SQLDelight repository, provider and prompt boundary. */
    suspend fun sendGroup(groupId: String, participants: List<String>, userContent: String): SendResult {
        if (userContent.isBlank()) return SendResult.NothingToRegenerate
        val resolved = providerResolver.resolve() ?: return SendResult.NotConfigured
        val session = repository.getOrCreateGroupSession(groupId, participants)
        repository.recoverInterruptedVariants(session.id)
        val userTurn = repository.appendUserTurn(session.id, userContent.trim())
        val names = participants.associateWith { CharacterRegistry.getCharacter(it).name }
        val previousCount = repository.getResolvedTurns(session.id).count { it.role == ChatTurnRole.ASSISTANT }
        val speakers = GroupSpeakerPlanner.choose(userContent, participants, names, previousCount)
        for ((index, speaker) in speakers.withIndex()) {
            val result = generateGroupReply(session.id, participants, speaker, resolved, null, userContent)
            if (result != SendResult.Completed) return result
            FirstSessionPolicy.firstMemory(userContent)?.let { fact ->
                memoryRepository.saveMemory(speaker, fact, "group_chat", userTurn.id, MemoryType.LONG_TERM, 0.7)
            }
            val clock = WorldHeartbeatEngine.worldClock.value
            WorldStateRepository.appendLifeEvent(LifeEvent(
                id = "group_user_${userTurn.id}_$speaker", characterId = speaker,
                time = clock.timeFormatted, type = LifeEventType.MESSAGE,
                title = "用户在群聊中与${names[speaker]}交流", description = userContent.take(120),
                worldDateLabel = clock.dateLabel, worldMinutesOfDay = clock.minutesOfDay,
                relatedCharacterIds = listOf("user"), sourceAppId = "group_chat", sourceRefId = userTurn.id,
                metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER),
            ))
            if (index > 0) {
                WorldStateRepository.appendLifeEvent(LifeEvent(
                    id = "group_exchange_${userTurn.id}_$speaker", characterId = speaker,
                    time = clock.timeFormatted, type = LifeEventType.MESSAGE,
                    title = "${names[speaker]}回应${names[speakers[index - 1]]}", description = "两人在群聊中交换了想法",
                    worldDateLabel = clock.dateLabel, worldMinutesOfDay = clock.minutesOfDay,
                    relatedCharacterIds = listOf(speakers[index - 1]), sourceAppId = "group_chat", sourceRefId = userTurn.id,
                ))
            }
        }
        return SendResult.Completed
    }

    suspend fun regenerateGroup(groupId: String, participants: List<String>): SendResult {
        val resolved = providerResolver.resolve() ?: return SendResult.NotConfigured
        val session = repository.getOrCreateGroupSession(groupId, participants)
        val turns = repository.getResolvedTurns(session.id)
        val target = turns.lastOrNull { it.role == ChatTurnRole.ASSISTANT } ?: return SendResult.NothingToRegenerate
        val speaker = GroupMessage.speaker(target.activeVariant?.content.orEmpty(), participants)
            ?: return SendResult.NoCharacter
        val lastUserText = turns.lastOrNull { it.role == ChatTurnRole.USER }?.activeVariant?.content.orEmpty()
        return generateGroupReply(session.id, participants, speaker, resolved, target.id, lastUserText)
    }

    private suspend fun generateGroupReply(
        sessionId: String, participants: List<String>, speaker: String,
        resolved: ResolvedProvider, targetTurnId: String?, recentUserText: String,
    ): SendResult {
        if (speaker !in participants) return SendResult.NoCharacter
        val card = promptContext.characterCard(speaker) ?: return SendResult.NoCharacter
        val turns = repository.getResolvedTurns(sessionId)
        val history = turns.filter { targetTurnId == null || it.id != targetTurnId }.takeLast(10).mapNotNull { turn ->
            val variant = turn.activeVariant?.takeIf { it.status == VariantStatus.COMPLETE } ?: return@mapNotNull null
            if (turn.role == ChatTurnRole.USER) AiMessage(AiRole.USER, variant.content.take(400))
            else GroupMessage.decode(variant.content, participants)?.let { reply ->
                AiMessage(AiRole.ASSISTANT, "${CharacterRegistry.getCharacter(reply.characterId).name}：${reply.content.take(400)}")
            }
        }
        val presence = promptContext.presence(speaker)
        val (date, time) = promptContext.temporal()
        val relationships = RelationshipStateRepository.states.value.filter { state ->
            speaker in listOf(state.fromCharacterId, state.toCharacterId) &&
                state.fromCharacterId != "user" && state.toCharacterId != "user" &&
                state.fromCharacterId in participants && state.toCharacterId in participants
        }.take(4).joinToString("；") { "${it.fromCharacterId}与${it.toCharacterId}:${it.stage.name}" }
        val groupRule = "这是群聊，成员：${participants.joinToString("、") { CharacterRegistry.getCharacter(it).name }}。" +
            "本轮只由${CharacterRegistry.getCharacter(speaker).name}发言。你只能代表自己，不能替其他角色声明心理。" +
            "只输出该角色要说的消息正文，不要角色名或其他角色回复。关系：$relationships"
        val input = PromptAssemblyInput(
            character = card, worldState = presence,
            recentLifeEvents = promptContext.lifeEvents(speaker).takeLast(6),
            memories = memoryRepository.getMemoriesForPrompt(speaker, 4).map {
                PromptMemory(id = it.id, content = it.content, characterIds = listOf(speaker))
            },
            history = history, currentDate = date, currentTime = time,
            relationshipInstructions = RomanceRepository.groupPromptInstructions(speaker),
            activeLore = promptContext.activeLore(speaker, presence?.currentLocation, recentUserText, null).take(2),
        )
        val request = AiChatRequest(resolved.model, listOf(AiMessage(AiRole.SYSTEM, groupRule)) + PromptAssembler.assemble(input).messages)
        val variantId: String
        val turnId: String
        if (targetTurnId == null) {
            val turn = repository.appendAssistantTurn(sessionId, "$speaker\n", VariantStatus.STREAMING, resolved.profileId, resolved.model)
            turnId = turn.id
            variantId = turn.activeVariantId ?: error("group assistant variant missing")
        } else {
            turnId = targetTurnId
            variantId = repository.appendVariant(targetTurnId, "$speaker\n", VariantStatus.STREAMING, resolved.profileId, resolved.model).id
        }
        val partial = StringBuilder()
        _streaming.value = StreamingState(sessionId, turnId, variantId, "", speaker)
        var result: SendResult = SendResult.Failed(AiProviderError.Protocol("回复中断"))
        try {
            resolved.provider.streamChat(request).collect { event ->
                when (event) {
                    is AiStreamEvent.Delta -> {
                        partial.append(event.text)
                        _streaming.value = StreamingState(sessionId, turnId, variantId, partial.toString(), speaker)
                    }
                    is AiStreamEvent.Completed -> {
                        if (event.text.isNotBlank()) {
                            repository.updateVariant(variantId, GroupMessage.encode(GroupReply(speaker, event.text), participants), VariantStatus.COMPLETE)
                            result = SendResult.Completed
                        } else {
                            repository.updateVariant(variantId, "$speaker\n", VariantStatus.FAILED, "EMPTY", "回复为空")
                        }
                    }
                    is AiStreamEvent.Failed -> {
                        repository.updateVariant(variantId, "$speaker\n", VariantStatus.FAILED, event.error.typeName(), "生成失败")
                        result = SendResult.Failed(event.error)
                    }
                    AiStreamEvent.Cancelled -> {
                        repository.updateVariant(variantId, "$speaker\n", VariantStatus.CANCELLED, "CANCELLED", null)
                        result = SendResult.Cancelled
                    }
                    else -> Unit
                }
            }
            if (result is SendResult.Failed && repository.getVariants(turnId).lastOrNull()?.status == VariantStatus.STREAMING) {
                repository.updateVariant(variantId, "$speaker\n", VariantStatus.FAILED, "INTERRUPTED", "回复中断")
            }
            return result
        } catch (e: CancellationException) {
            withContext(NonCancellable) { repository.updateVariant(variantId, "$speaker\n", VariantStatus.CANCELLED, "CANCELLED", null) }
            throw e
        } finally {
            _streaming.value = null
        }
    }

    /** Sends one user message and generates the real assistant reply (§5). */
    suspend fun send(
        characterId: String,
        userContent: String,
        quoteMessageId: String? = null,
        quotePreview: String? = null,
    ): SendResult {
        val resolved = providerResolver.resolve() ?: return SendResult.NotConfigured
        val card = promptContext.characterCard(characterId) ?: return SendResult.NoCharacter

        val session = repository.getOrCreatePrivateSession(characterId)
        repository.recoverInterruptedVariants(session.id)
        val userTurn = repository.appendUserTurn(session.id, userContent, quoteMessageId, quotePreview)
        // A user's boundary needs no AI acknowledgement and survives a failed/cancelled generation.
        RomanceRepository.observeUserBoundary(characterId, userTurn.id, userContent, userTurn.createdAtEpochMs)

        val history = repository.getResolvedTurns(session.id)
        return generate(
            characterId = characterId,
            card = card,
            sessionId = session.id,
            history = history,
            targetTurnId = null,
            resolved = resolved,
        )
    }

    /** Regenerates the LATEST assistant turn as a new variant (§10). */
    suspend fun regenerate(characterId: String): SendResult {
        val resolved = providerResolver.resolve() ?: return SendResult.NotConfigured
        val card = promptContext.characterCard(characterId) ?: return SendResult.NoCharacter

        val session = repository.getOrCreatePrivateSession(characterId)
        repository.recoverInterruptedVariants(session.id)

        val turns = repository.getResolvedTurns(session.id)
        val target = turns.lastOrNull { it.role == ChatTurnRole.ASSISTANT }
            ?: return SendResult.NothingToRegenerate
        val history = turns.filter { it.position < target.position }

        return generate(
            characterId = characterId,
            card = card,
            sessionId = session.id,
            history = history,
            targetTurnId = target.id,
            resolved = resolved,
        )
    }

    private suspend fun generate(
        characterId: String,
        card: CharacterCardData,
        sessionId: String,
        history: List<ResolvedChatTurn>,
        targetTurnId: String?,
        resolved: ResolvedProvider,
    ): SendResult {
        val recentUserText = history.lastOrNull { it.role == ChatTurnRole.USER }
            ?.activeVariant?.content ?: ""
        // P3D-3: the ledger now also carries user app actions, so the prompt
        // keeps only the most recent facts instead of growing unbounded.
        val events = promptContext.lifeEvents(characterId).takeLast(MAX_PROMPT_LIFE_EVENTS)
        val presence = promptContext.presence(characterId)
        val (date, time) = promptContext.temporal()

        val input = PromptAssemblyInput(
            character = card,
            activeLore = promptContext.activeLore(
                characterId = characterId,
                locationId = presence?.currentLocation,
                recentUserText = recentUserText,
                lifeEventTitle = events.lastOrNull()?.title,
            ),
            worldState = presence,
            recentLifeEvents = events,
            memories = memoryRepository.getMemoriesForPrompt(characterId).map { memory ->
                PromptMemory(
                    id = memory.id,
                    content = memory.content,
                    characterIds = listOf(memory.characterId),
                )
            },
            history = history.toPromptHistory(),
            currentDate = date,
            currentTime = time,
            userName = promptContext.userName(),
            relationshipInstructions = RomanceRepository.promptInstructions(characterId),
            realityContext = RealityContextPolicy.context(RealityRepository.refresh(), RealityRepository.settings.value),
        )
        val assembly = PromptAssembler.assemble(input)
        val request = AiChatRequest(
            model = resolved.model,
            messages = assembly.messages.toMutableList().apply {
                add(minOf(1, size), AiMessage(AiRole.SYSTEM, RichMessagePrompt.INSTRUCTIONS))
            },
            stream = true,
        )

        // Create the terminal vehicle: new assistant turn (send) or new variant
        // on the existing turn (regenerate) — both start STREAMING (§8).
        val turnId: String
        val variantId: String
        if (targetTurnId == null) {
            val turn = repository.appendAssistantTurn(
                sessionId = sessionId,
                content = "",
                status = VariantStatus.STREAMING,
                providerProfileId = resolved.profileId,
                model = resolved.model,
            )
            turnId = turn.id
            variantId = turn.activeVariantId
                ?: error("assistant turn created without an active variant")
        } else {
            val variant = repository.appendVariant(
                turnId = targetTurnId,
                content = "",
                status = VariantStatus.STREAMING,
                providerProfileId = resolved.profileId,
                model = resolved.model,
            )
            turnId = targetTurnId
            variantId = variant.id
        }

        _streaming.value = StreamingState(sessionId, turnId, variantId, text = "")
        val partial = StringBuilder()
        var terminal = false
        var result: SendResult = SendResult.Failed(
            AiProviderError.Protocol("stream ended without a terminal event"),
        )

        try {
            resolved.provider.streamChat(request).collect { event ->
                when (event) {
                    AiStreamEvent.Started -> Unit
                    is AiStreamEvent.Delta -> {
                        partial.append(event.text)
                        _streaming.value = StreamingState(
                            sessionId = sessionId,
                            turnId = turnId,
                            variantId = variantId,
                            text = partial.toString(),
                        )
                    }
                    is AiStreamEvent.Usage -> Unit
                    is AiStreamEvent.Completed -> {
                        if (event.text.isBlank()) {
                            val protocolError = AiProviderError.Protocol("empty completion")
                            persistFailure(variantId, partial.toString(), protocolError)
                            terminal = true
                            result = SendResult.Failed(protocolError)
                        } else {
                            val parsed = RichMessageParser.parse(event.text)
                            repository.updateVariant(
                                variantId = variantId,
                                content = parsed.content,
                                status = VariantStatus.COMPLETE,
                                richPayloads = parsed.payloads,
                            )
                            terminal = true
                            result = SendResult.Completed
                            // Only a newly completed private reply establishes mutual evidence. Regeneration
                            // changes presentation of an existing exchange and must never advance a relationship.
                            if (targetTurnId == null) {
                                history.lastOrNull { it.role == ChatTurnRole.USER }?.let { userTurn ->
                                    runCatching { RomanceRepository.observeExchange(
                                        characterId, userTurn.id, recentUserText, parsed.content, System.currentTimeMillis(),
                                    ) }
                                }
                            }
                        }
                    }
                    is AiStreamEvent.Failed -> {
                        persistFailure(variantId, partial.toString(), event.error)
                        terminal = true
                        result = SendResult.Failed(event.error)
                    }
                    AiStreamEvent.Cancelled -> {
                        repository.updateVariant(
                            variantId = variantId,
                            content = partial.toString(),
                            status = VariantStatus.CANCELLED,
                            errorType = "CANCELLED",
                            errorMessage = null,
                        )
                        terminal = true
                        result = SendResult.Cancelled
                    }
                }
            }
            if (!terminal) {
                val protocolError = AiProviderError.Protocol("stream ended without a terminal event")
                persistFailure(variantId, partial.toString(), protocolError)
                result = SendResult.Failed(protocolError)
            }
            return result
        } catch (e: CancellationException) {
            // Job cancel mid-stream: keep the partial text (§8 cancel path),
            // persist under NonCancellable, then propagate the cancellation.
            withContext(NonCancellable) {
                repository.updateVariant(
                    variantId = variantId,
                    content = partial.toString(),
                    status = VariantStatus.CANCELLED,
                    errorType = "CANCELLED",
                    errorMessage = null,
                )
            }
            throw e
        } finally {
            _streaming.value = null
        }
    }

    private fun persistFailure(variantId: String, partialText: String, error: AiProviderError) {
        repository.updateVariant(
            variantId = variantId,
            content = partialText,
            status = VariantStatus.FAILED,
            errorType = error.typeName(),
            errorMessage = error.displayMessage(),
        )
    }

    /**
     * Prompt-history rule (§7): only COMPLETE turns enter the prompt, always
     * through the active variant; FAILED/CANCELLED/STREAMING never do.
     */
    private fun List<ResolvedChatTurn>.toPromptHistory(): List<AiMessage> =
        mapNotNull(RichMessagePrompt::history)

    private fun AiProviderError.typeName(): String = when (this) {
        AiProviderError.Unauthorized -> "UNAUTHORIZED"
        AiProviderError.Timeout -> "TIMEOUT"
        is AiProviderError.Network -> "NETWORK"
        AiProviderError.Cancelled -> "CANCELLED"
        is AiProviderError.Protocol -> "PROTOCOL"
        is AiProviderError.Http -> "HTTP"
    }

    private fun AiProviderError.displayMessage(): String = when (this) {
        AiProviderError.Unauthorized -> "API Key 无效"
        AiProviderError.Timeout -> "请求超时"
        is AiProviderError.Network -> message ?: "网络连接失败"
        AiProviderError.Cancelled -> "已取消"
        is AiProviderError.Protocol -> message
        is AiProviderError.Http -> "HTTP ${code}: ${message ?: ""}"
    }
}
