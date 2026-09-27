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
import com.example.data.model.ChatMessage
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import app.cash.sqldelight.db.SqlDriver
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
    private val driver: SqlDriver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val sessionState = MutableStateFlow<String?>(null)
    private var generationJob: Job? = null

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

    fun send(userText: String) {
        if (userText.isBlank()) return
        if (generationJob?.isActive == true) return
        generationJob = viewModelScope.launch {
            handleResult(runtime.send(characterId, userText))
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
            SendResult.Completed, SendResult.NothingToRegenerate -> Unit
        }
    }

    private fun publishMessages(turns: List<ResolvedChatTurn>) {
        _uiState.update { state ->
            state.copy(messages = turns.mapNotNull { turn -> turn.toChatMessage() })
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
                    val repository = SqlDelightChatRepository(
                        database = ChatDatabase(driver),
                        idGenerator = UuidIdGenerator(),
                        clock = SystemEpochClock(),
                    )
                    val runtime = ChatGenerationRuntime(
                        repository = repository,
                        providerResolver = ActiveProfileProviderResolver(ProviderGraph.repository),
                        promptContext = WorldChatPromptContext,
                    )
                    ChatViewModel(
                        characterId = character.id,
                        character = character,
                        repository = repository,
                        runtime = runtime,
                        driver = driver,
                    )
                }
            }
    }
}
