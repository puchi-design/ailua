package com.example.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.runtime.ActiveProfileProviderResolver
import com.example.data.ai.runtime.ChatGenerationRuntime
import com.example.data.ai.runtime.SendResult
import com.example.data.ai.runtime.WorldChatPromptContext
import com.example.data.ai.repository.ProviderGraph
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.ResolvedChatTurn
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichInteractionEvidence
import com.example.data.engine.UserActivityRecorder
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.firstsession.FirstSessionPolicy
import com.example.data.firstsession.FirstSessionStore
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.memory.auto.AutoMemoryExtractor
import com.example.data.memory.model.MemoryEntry
import com.example.data.memory.model.MemoryType
import com.example.data.memory.repository.MemoryGraph
import com.example.data.memory.repository.MemoryRepository
import com.example.data.model.ChatMessage
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ChatUiState — everything ChatScreen renders (P3C-4 §1).
 *
 * API keys, providers, and repository objects never appear here; only
 * display-safe values (messages, flags, user-facing error strings).
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    /** Live partial text while streaming; `null` when idle. */
    val streamingText: String? = null,
    /** User-facing error from the typed provider error (§13); null when none. */
    val errorMessage: String? = null,
    /** One-shot ask to open the AI connection sheet (preflight failed, §5). */
    val requestProviderConfig: Boolean = false,
)

/** Keep existing turn keys for first replies; regenerated replies need their own stable key. */
internal fun memorySourceRefId(message: ChatMessage): String =
    if (message.sender == MessageSender.CHARACTER && message.variantIndex > 0) {
        "${message.id}:variant:${message.variantIndex}"
    } else {
        message.id
    }

/** Resolves old turn-ID saves without collapsing a different regenerated reply into that row. */
internal fun memorySourceRefIdForSave(
    message: ChatMessage,
    existingMemories: List<MemoryEntry>,
): String? {
    fun atKey(key: String) = existingMemories.firstOrNull { it.sourceRefId == key }
    fun MemoryEntry.matchesMessage() = sourceAppId == "chat" && content == message.text

    val canonicalKey = memorySourceRefId(message)
    val existing = atKey(canonicalKey)
    if (existing != null) {
        if (existing.matchesMessage()) return null
        if (message.sender == MessageSender.CHARACTER && message.variantIndex == 0) {
            // A legacy save of variant 1+ can occupy the original reply's turn ID.
            val alternateKey = "${message.id}:variant:0"
            val alternate = atKey(alternateKey)
            if (alternate == null) return alternateKey
            if (alternate.matchesMessage()) return null
        }
        throw IllegalStateException("Memory source key already belongs to different content: $canonicalKey")
    }
    if (message.sender == MessageSender.CHARACTER && message.variantIndex > 0 &&
        atKey(message.id)?.matchesMessage() == true
    ) return null
    return canonicalKey
}

/**
 * ChatViewModel — thin UI-state layer over [ChatGenerationRuntime] (P3C-4 §1).
 *
 * Owns exactly one generation [Job] at a time; cancelling that Job is the
 * sanctioned cancel mechanism (§12): Job → Flow cancellation →
 * HttpStreamCall.cancel, with the runtime persisting the partial text as
 * CANCELLED before the cancellation propagates. `onCleared` (leaving the
 * chat screen) cancels the Job via viewModelScope and closes the SQL driver.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val characterId: String,
    private val character: CharacterProfile,
    private val repository: ChatRepository,
    private val runtime: ChatGenerationRuntime,
    private val memoryRepository: MemoryRepository,
    private val autoMemoryExtractor: AutoMemoryExtractor,
    private val driver: SqlDriver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val sessionState = MutableStateFlow<String?>(null)
    private var generationJob: Job? = null

    /** P3D-3: text of the in-flight user message, for the MESSAGE LifeEvent. */
    private var lastUserText: String? = null

    init {
        viewModelScope.launch {
            sessionState.filterNotNull()
                .flatMapLatest { sessionId -> repository.observeResolvedTurns(sessionId) }
                .collect { turns -> publishMessages(turns) }
        }
        viewModelScope.launch {
            runtime.streaming.collect { streaming ->
                _uiState.update {
                    it.copy(
                        isGenerating = streaming != null,
                        streamingText = streaming?.text,
                    )
                }
            }
        }
        viewModelScope.launch {
            val session = repository.getOrCreatePrivateSession(characterId)
            repository.recoverInterruptedVariants(session.id)
            sessionState.value = session.id
        }
    }

    fun send(
        userText: String,
        quoteMessageId: String? = null,
        quotePreview: String? = null,
        onPreflightRejected: () -> Unit = {},
    ) {
        if (userText.isBlank()) return
        if (generationJob?.isActive == true) return
        lastUserText = userText
        generationJob = viewModelScope.launch {
            val result = runtime.send(characterId, userText, quoteMessageId, quotePreview)
            if (result == SendResult.NotConfigured || result == SendResult.NoCharacter) {
                lastUserText = null
                onPreflightRejected()
            }
            handleResult(result)
        }
    }

    /** Uses the persisted variant as the source of truth; duplicate taps cannot repeat an action. */
    fun transitionRichMessage(message: ChatMessage, payloadIndex: Int, status: RichMessageStatus) {
        if (message.variantId.isBlank()) return
        viewModelScope.launch {
            val changed = try {
                repository.updateRichStatus(message.variantId, payloadIndex, status)
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "操作未保存，请重试") }
                return@launch
            }
            if (changed) {
                try {
                    message.richPayloads.getOrNull(payloadIndex)?.let { payload ->
                        RichInteractionEvidence.record(characterId, message.id, message.variantId, payloadIndex, payload, status)
                    }
                } catch (_: Exception) {
                    // The next repository emission or cold start reconciles this deterministic event.
                }
            }
        }
    }

    fun regenerate() {
        if (generationJob?.isActive == true) return
        generationJob = viewModelScope.launch {
            handleResult(runtime.regenerate(characterId))
        }
    }

    /** Cancel mechanism (§12): cancels the single generation Job. */
    fun cancelGeneration() {
        generationJob?.cancel()
    }

    /** P3C-5 chat save entry: persists [message] as a long-term memory. */
    fun saveMemory(message: ChatMessage): Boolean {
        val sourceRefId = memorySourceRefIdForSave(message, memoryRepository.getMemories(characterId))
            ?: return false
        memoryRepository.saveMemory(
            characterId = characterId,
            content = message.text,
            sourceAppId = "chat",
            sourceRefId = sourceRefId,
            type = MemoryType.LONG_TERM,
            importance = 0.7,
        )
        val worldClock = WorldHeartbeatEngine.worldClock.value
        RelationshipStateRepository.recordMemory(characterId, sourceRefId, message.text, worldClock.dateLabel, worldClock.timeFormatted)
        return true
    }

    fun switchVariant(turnId: String, direction: Int) {
        viewModelScope.launch {
            val variants = repository.getVariants(turnId)
            if (variants.size < 2) return@launch
            val current = _uiState.value.messages
                .firstOrNull { it.id == turnId }
                ?.variantIndex ?: 0
            val targetIndex = (current + direction + variants.size) % variants.size
            val target = variants.firstOrNull { it.variantIndex == targetIndex } ?: return@launch
            repository.selectVariant(turnId, target.id)
        }
    }

    fun clearSession() {
        generationJob?.cancel()
        viewModelScope.launch {
            val session = repository.getOrCreatePrivateSession(characterId)
            repository.clearSession(session.id)
            val fresh = repository.getOrCreatePrivateSession(characterId)
            repository.recoverInterruptedVariants(fresh.id)
            sessionState.value = fresh.id
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissProviderRequest() {
        _uiState.update { it.copy(requestProviderConfig = false) }
    }

    private suspend fun handleResult(result: SendResult) {
        when (result) {
            SendResult.NotConfigured -> _uiState.update {
                it.copy(errorMessage = "请先配置 AI 连接", requestProviderConfig = true)
            }
            SendResult.NoCharacter -> _uiState.update {
                it.copy(errorMessage = "角色数据缺失，无法生成回复")
            }
            SendResult.Cancelled -> _uiState.update { it.copy(errorMessage = "已取消") }
            is SendResult.Failed -> _uiState.update {
                it.copy(errorMessage = friendlyError(result.error))
            }
            SendResult.Completed -> {
                if (!FirstSessionStore.state.value.receivedFirstReply && lastUserText != null) {
                    try {
                        FirstSessionPolicy.firstMemory(lastUserText.orEmpty())?.let { fact ->
                            memoryRepository.saveMemory(characterId, fact, "chat", "first_session_name", MemoryType.LONG_TERM, 0.8)
                        }
                        FirstSessionStore.markSent()
                        FirstSessionStore.markFirstReply(characterId, character.name, character.location)
                    } catch (_: Exception) {
                        // First-session guidance is optional; the completed reply is authoritative.
                    }
                }
                recordChatActivity()
                launchAutoMemory()
            }
            SendResult.NothingToRegenerate -> Unit
        }
    }

    /**
     * P3D-3: one completed exchange becomes a MESSAGE fact in the ledger, so
     * other apps/characters see that the user was chatting (never affects UI).
     */
    private fun recordChatActivity() {
        val userText = lastUserText ?: return
        lastUserText = null
        try {
            UserActivityRecorder.recordChatMessage(
                characterId = characterId,
                characterName = character.name,
                userText = userText,
            )
        } catch (_: Exception) {
            // Continuity facts must never break the chat experience.
        }
    }

    /**
     * P3D-1 hook: auto-memory extraction after one real completed reply.
     * Sibling job on viewModelScope — it survives generation-job cancellation,
     * and any failure is swallowed so the chat experience never changes.
     */
    private fun launchAutoMemory() {
        val sessionId = sessionState.value
        viewModelScope.launch {
            try {
                autoMemoryExtractor.run(characterId, sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Auto-memory must never affect the chat experience.
            }
        }
    }

    private fun publishMessages(turns: List<ResolvedChatTurn>) {
        val messages = turns.mapNotNull { turn -> turn.toChatMessage() }
        _uiState.update { state -> state.copy(messages = messages) }
        reconcileRichInteractionEvidence(messages)
    }

    private fun reconcileRichInteractionEvidence(messages: List<ChatMessage>) {
        messages.asSequence().filter { it.sender == MessageSender.CHARACTER && it.variantId.isNotBlank() }
            .forEach { message ->
                message.richPayloads.forEachIndexed { index, payload ->
                    val status = payload.status ?: return@forEachIndexed
                    if (status == RichMessageStatus.PENDING || status == RichMessageStatus.CANCELED) return@forEachIndexed
                    try {
                        RichInteractionEvidence.record(characterId, message.id, message.variantId, index, payload, status)
                    } catch (_: Exception) {
                        // Best effort; another repository emission or a future cold start retries.
                    }
                }
            }
    }

    private fun ResolvedChatTurn.toChatMessage(): ChatMessage? {
        val variant = activeVariant ?: return null
        if (variant.status == VariantStatus.STREAMING) return null
        val epoch = when (role) {
            ChatTurnRole.USER -> variant.createdAtEpochMs
            ChatTurnRole.ASSISTANT -> createdAtEpochMs
        }
        val statusLabel = when (variant.status) {
            VariantStatus.FAILED -> "FAILED"
            VariantStatus.CANCELLED -> "CANCELLED"
            else -> null
        }
        return when (role) {
            ChatTurnRole.USER -> ChatMessage(
                id = id,
                sender = MessageSender.USER,
                senderCharacterId = character.avatarId,
                senderName = character.name,
                type = MessageType.TEXT,
                text = variant.content,
                timestamp = formatTime(epoch),
                variantId = variant.id,
                richPayloads = variant.richPayloads,
                quoteMessageId = variant.quoteMessageId,
                quotePreview = variant.quotePreview,
            )
            ChatTurnRole.ASSISTANT -> ChatMessage(
                id = id,
                sender = MessageSender.CHARACTER,
                senderCharacterId = character.avatarId,
                senderName = character.name,
                type = MessageType.TEXT,
                text = variant.content,
                timestamp = formatTime(epoch),
                variantIndex = variant.variantIndex,
                variantCount = variantCount,
                statusLabel = statusLabel,
                variantId = variant.id,
                richPayloads = variant.richPayloads,
                quoteMessageId = variant.quoteMessageId,
                quotePreview = variant.quotePreview,
            )
        }
    }

    private fun friendlyError(error: AiProviderError): String = when (error) {
        AiProviderError.Unauthorized -> "API Key 无效"
        AiProviderError.Timeout -> "请求超时"
        is AiProviderError.Network -> "网络连接失败"
        is AiProviderError.Http -> "服务请求失败"
        is AiProviderError.Protocol -> "返回格式异常"
        AiProviderError.Cancelled -> "已取消"
    }

    override fun onCleared() {
        driver.close()
    }

    companion object {
        private fun formatTime(epochMs: Long): String =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

        fun factory(context: Context, character: CharacterProfile): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val driver = ChatDriverFactory(context.applicationContext).createDriver()
                    val database = ChatDatabase(driver)
                    val clock = SystemEpochClock()
                    val repository = SqlDelightChatRepository(
                        database = database,
                        idGenerator = UuidIdGenerator(),
                        clock = clock,
                    )
                    val providerResolver = ActiveProfileProviderResolver(ProviderGraph.repository)
                    val memoryRepository = MemoryGraph.repository
                    val runtime = ChatGenerationRuntime(
                        repository = repository,
                        providerResolver = providerResolver,
                        promptContext = WorldChatPromptContext,
                        memoryRepository = memoryRepository,
                    )
                    val autoMemoryExtractor = AutoMemoryExtractor(
                        chatRepository = repository,
                        memoryRepository = memoryRepository,
                        cursorQueries = database.memoryExtractCursorQueries,
                        providerResolver = providerResolver,
                        clock = clock,
                        characterName = character.name,
                    )
                    ChatViewModel(
                        characterId = character.id,
                        character = character,
                        repository = repository,
                        runtime = runtime,
                        memoryRepository = memoryRepository,
                        autoMemoryExtractor = autoMemoryExtractor,
                        driver = driver,
                    )
                }
            }
    }
}
