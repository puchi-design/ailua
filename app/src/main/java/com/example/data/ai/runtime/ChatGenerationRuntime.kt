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
import com.example.data.chat.model.ResolvedChatTurn
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.model.CharacterCardData
import com.example.data.projection.CharacterPresence
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
) {

    private val _streaming = MutableStateFlow<StreamingState?>(null)

    /** Live streaming state for the active generation; `null` when idle. */
    val streaming: StateFlow<StreamingState?> = _streaming.asStateFlow()

    /** Sends one user message and generates the real assistant reply (§5). */
    suspend fun send(characterId: String, userContent: String): SendResult {
        val resolved = providerResolver.resolve() ?: return SendResult.NotConfigured
        val card = promptContext.characterCard(characterId) ?: return SendResult.NoCharacter

        val session = repository.getOrCreatePrivateSession(characterId)
        repository.recoverInterruptedVariants(session.id)
        repository.appendUserTurn(session.id, userContent)

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
        val events = promptContext.lifeEvents(characterId)
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
            memories = emptyList<PromptMemory>(),
            history = history.toPromptHistory(),
            currentDate = date,
            currentTime = time,
            userName = promptContext.userName(),
        )
        val assembly = PromptAssembler.assemble(input)
        val request = AiChatRequest(
            model = resolved.model,
            messages = assembly.messages,
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
                            repository.updateVariant(
                                variantId = variantId,
                                content = event.text,
                                status = VariantStatus.COMPLETE,
                            )
                            terminal = true
                            result = SendResult.Completed
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
        mapNotNull { turn ->
            val variant = turn.activeVariant ?: return@mapNotNull null
            if (variant.status != VariantStatus.COMPLETE) return@mapNotNull null
            when (turn.role) {
                ChatTurnRole.USER -> AiMessage(AiRole.USER, variant.content)
                ChatTurnRole.ASSISTANT -> AiMessage(AiRole.ASSISTANT, variant.content)
            }
        }

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
