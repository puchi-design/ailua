package com.example.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.onboarding.ProviderSetup
import com.example.data.ai.repository.ProviderGraph
import com.example.data.ai.runtime.ActiveProfileProviderResolver
import com.example.data.ai.runtime.ChatGenerationRuntime
import com.example.data.ai.runtime.SendResult
import com.example.data.ai.runtime.WorldChatPromptContext
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.GroupMessage
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.memory.repository.MemoryGraph
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GroupUiMessage(val id: String, val speakerId: String?, val text: String, val failed: Boolean = false, val time: String = "")
data class GroupChatUiState(
    val messages: List<GroupUiMessage> = emptyList(),
    val streamingSpeakerId: String? = null,
    val streamingText: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

class GroupChatViewModel(
    private val repository: ChatRepository,
    private val runtime: ChatGenerationRuntime,
    private val driver: SqlDriver,
) : ViewModel() {
    companion object {
        const val GROUP_ID = "rain_tea"
        val PARTICIPANTS = listOf("mira", "yuna", "noa")

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val driver = ChatDriverFactory(context.applicationContext).createDriver()
                val repository = SqlDelightChatRepository(ChatDatabase(driver), UuidIdGenerator(), SystemEpochClock())
                val runtime = ChatGenerationRuntime(repository, ActiveProfileProviderResolver(ProviderGraph.repository),
                    WorldChatPromptContext, MemoryGraph.repository)
                GroupChatViewModel(repository, runtime, driver)
            }
        }
    }

    private val mutable = MutableStateFlow(GroupChatUiState())
    val state = mutable.asStateFlow()
    private var generation: Job? = null

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val session = repository.getOrCreateGroupSession(GROUP_ID, PARTICIPANTS)
                repository.recoverInterruptedVariants(session.id)
                session.id
            }.let { sessionId ->
                repository.observeResolvedTurns(sessionId).collect { turns ->
                    mutable.update { old -> old.copy(messages = turns.mapNotNull { turn ->
                        val variant = turn.activeVariant ?: return@mapNotNull null
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(turn.createdAtEpochMs))
                        when (turn.role) {
                            ChatTurnRole.USER -> GroupUiMessage(turn.id, null, variant.content, time = time)
                            ChatTurnRole.ASSISTANT -> {
                                val reply = GroupMessage.decode(variant.content, PARTICIPANTS)
                                if (reply != null) GroupUiMessage(turn.id, reply.characterId, reply.content, time = time)
                                else if (variant.status == VariantStatus.FAILED || variant.status == VariantStatus.CANCELLED)
                                    GroupUiMessage(turn.id, GroupMessage.speaker(variant.content, PARTICIPANTS), "本轮回复未完成", failed = true, time = time)
                                else null
                            }
                        }
                    }) }
                }
            }
        }
        viewModelScope.launch {
            runtime.streaming.collect { stream ->
                mutable.update { it.copy(streamingSpeakerId = stream?.speakerId, streamingText = stream?.text) }
            }
        }
    }

    fun send(text: String) {
        if (text.isBlank() || generation?.isActive == true) return
        generation = viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try { showResult(withContext(Dispatchers.IO) { runtime.sendGroup(GROUP_ID, PARTICIPANTS, text.trim()) }) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(error = "发送失败，请重试") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }

    fun regenerate() {
        if (generation?.isActive == true) return
        generation = viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try { showResult(withContext(Dispatchers.IO) { runtime.regenerateGroup(GROUP_ID, PARTICIPANTS) }) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(error = "重试失败，请稍后再试") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }

    fun cancel() { generation?.cancel() }
    fun dismissError() { mutable.update { it.copy(error = null) } }

    private fun showResult(result: SendResult) {
        val message = when (result) {
            SendResult.NotConfigured -> "还没有连接 AI，请先配置连接"
            SendResult.NoCharacter -> "群聊角色资料暂不可用"
            SendResult.NothingToRegenerate -> "还没有可重试的回复"
            is SendResult.Failed -> ProviderSetup.friendly(result.error)
            SendResult.Cancelled -> "已停止生成"
            SendResult.Completed -> null
        }
        mutable.update { it.copy(error = message) }
    }

    override fun onCleared() {
        generation?.cancel()
        driver.close()
    }
}
