package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.engine.WorldStateRepository
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
import com.example.data.model.ChatMessage
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageSender
import com.example.data.projection.projectPresence
import com.example.ui.chat.components.ChatActionItem
import com.example.ui.chat.components.ChatComposer
import com.example.ui.chat.components.ChatJumpToLatest
import com.example.ui.chat.components.ChatMessageItem
import com.example.ui.chat.components.ChatQuickReplies
import com.example.ui.chat.components.ChatTopBar
import com.example.ui.chat.components.StreamingReplyBubble
import com.example.ui.components.AiConnectionSheet
import com.example.ui.components.ProactiveSettingsSheet
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

/** Rendering and actions over the existing provider → runtime → database pipeline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    character: CharacterProfile,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onGoHome: () -> Unit = onBackToHome,
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: ChatViewModel = viewModel(
        key = character.id,
        factory = ChatViewModel.factory(appContext, character),
    )
    val theme = LocalAiluaTheme.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val firstSession by FirstSessionStore.state.collectAsStateWithLifecycle()
    val bookmarkedMsgIds by AiluaLocalStore.bookmarkedMessageIds.collectAsStateWithLifecycle()
    val messages = uiState.messages
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val currentActivity = projectPresence(character, worldEvents).currentActivity
    val streamingText = uiState.streamingText

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showAiConnection by remember { mutableStateOf(false) }
    var showProactive by remember { mutableStateOf(false) }
    var memorySaveInProgress by remember(character.id) { mutableStateOf(false) }
    val savedMemorySourceRefIds = remember(character.id) { mutableSetOf<String>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val contentCount = messages.size + if (streamingText != null) 1 else 0
    val scrollTargetIndex = contentCount
    val showJumpToBottom by remember {
        derivedStateOf {
            listState.canScrollForward
        }
    }
    val lastAssistantId = remember(messages) {
        messages.lastOrNull { it.sender == MessageSender.CHARACTER }?.id
    }
    val latestSaveableMessage = remember(messages) { latestSaveableChatMessage(messages) }

    fun saveMessageToMemory(message: ChatMessage?) {
        when {
            message == null -> coroutineScope.launch {
                snackbarHostState.showSnackbar("还没有可保存的对话消息")
            }
            memorySaveInProgress -> Unit
            memorySourceRefId(message) in savedMemorySourceRefIds -> coroutineScope.launch {
                snackbarHostState.showSnackbar("这条消息已保存到记忆")
            }
            else -> {
                memorySaveInProgress = true
                try {
                    val newlySaved = viewModel.saveMemory(message)
                    savedMemorySourceRefIds.add(memorySourceRefId(message))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (newlySaved) "已保存到记忆" else "这条消息已保存到记忆")
                    }
                } catch (_: Exception) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("保存记忆失败，请重试") }
                } finally {
                    memorySaveInProgress = false
                }
            }
        }
    }
    val quickPrompts = if (!firstSession.sentFirstMessage) listOf(
        "你现在在做什么？", "今天过得怎么样？", "第一次见面，你想让我怎么称呼你？"
    ) else when (character.id.lowercase()) {
        "yan" -> listOf("今天修的是什么书？", "我想在店里坐一会儿", "你也会有想躲起来的时候吗？")
        "yeo" -> listOf("今天拍到什么了？", "下次带我去河边吧", "先说好，不许偷偷拍我")
        "yuna" -> listOf("你在做什么呢？", "今天有点累…", "布丁还有吗？", "雨下得真大呢", "明天去探店吧！")
        "noa" -> listOf("你在做什么呢？", "今天有点累…", "给我讲个故事吧", "雨下得真大呢", "在听什么唱片？")
        "mira" -> listOf("你在做什么呢？", "今天有点累…", "给我讲个故事吧", "雨下得真大呢", "红茶好喝吗？")
        else -> listOf("今天过得怎么样？", "你最近在忙什么？", "聊聊你喜欢的事吧")
    }

    LaunchedEffect(messages.size, streamingText) {
        if (scrollTargetIndex > 0) listState.animateScrollToItem(scrollTargetIndex)
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }
    LaunchedEffect(uiState.requestProviderConfig) {
        if (uiState.requestProviderConfig) {
            showAiConnection = true
            viewModel.dismissProviderRequest()
        }
    }

    Box(Modifier.fillMaxSize().imePadding().testTag("chat_screen")) {
        AiluaScreenScaffold(
            title = character.name,
            onBack = onBackToHome,
            onGoHome = onGoHome,
            topBar = {
                ChatTopBar(
                    characterId = character.id,
                    name = character.name,
                    currentActivity = currentActivity,
                    onBack = onBackToHome,
                    onOpenProfile = onOpenProfile,
                    onOpenMenu = { showMenu = true },
                ) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("查看资料", style = theme.text.body) },
                            onClick = { showMenu = false; onOpenProfile() },
                        )
                        DropdownMenuItem(
                            text = { Text("AI 连接", style = theme.text.body) },
                            onClick = { showMenu = false; showAiConnection = true },
                        )
                        DropdownMenuItem(
                            text = { Text("主动消息", style = theme.text.body) },
                            onClick = { showMenu = false; showProactive = true },
                        )
                        DropdownMenuItem(
                            text = { Text("存入羁绊记忆", style = theme.text.body) },
                            onClick = {
                                showMenu = false
                                saveMessageToMemory(latestSaveableMessage)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("清空对话记录", style = theme.text.body) },
                            onClick = { showMenu = false; viewModel.clearSession() },
                        )
                    }
                }
            },
            bottomBar = {
                Column {
                    if (firstSession.receivedFirstReply && !firstSession.viewedLiving) {
                        Text(
                            "${character.name}会记住重要的事情，也会继续自己的生活。",
                            modifier = Modifier.padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 4.dp),
                            style = theme.text.caption,
                            color = theme.palette.onSurfaceMuted,
                        )
                    }
                    ChatQuickReplies(quickPrompts, enabled = !uiState.isGenerating, onSelect = { viewModel.send(it) })
                    ChatComposer(
                        inputText = inputText,
                        isGenerating = uiState.isGenerating,
                        onInputTextChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank() && !uiState.isGenerating) {
                                val text = inputText.trim()
                                inputText = ""
                                viewModel.send(text)
                            }
                        },
                        onStop = { viewModel.cancelGeneration() },
                        onAttachClick = { showActionSheet = !showActionSheet },
                        onMicClick = {},
                        showMicrophone = false,
                    )
                    AnimatedVisibility(visible = showActionSheet) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(theme.surfaces.screen)
                                .padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            ChatActionItem("传递暖意", "❤️") {
                                showActionSheet = false
                                viewModel.send("伸出手轻轻碰了碰 ${character.name} 放在桌上的杯沿，对 TA 温和地笑了笑。")
                            }
                            ChatActionItem("凝华记忆", "💎") {
                                showActionSheet = false
                                saveMessageToMemory(latestSaveableMessage)
                            }
                        }
                    }
                }
            },
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            character = character,
                            isBookmarked = bookmarkedMsgIds.contains(memorySourceRefId(message)),
                            onToggleBookmark = {
                                try {
                                    val isBookmarked = AiluaLocalStore.toggleMessageBookmark(memorySourceRefId(message))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(if (isBookmarked) "已收藏" else "已取消收藏")
                                    }
                                } catch (_: Exception) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("收藏操作失败，请重试") }
                                }
                            },
                            onSaveMemory = {
                                saveMessageToMemory(message.takeIf { it.isSaveableChatMessage() })
                            },
                            onRegenerate = { viewModel.regenerate() },
                            canRegenerate = !uiState.isGenerating && message.id == lastAssistantId,
                            onSwitchVariant = { direction -> viewModel.switchVariant(message.id, direction) },
                        )
                    }
                    if (streamingText != null) {
                        item(key = "streaming-reply") { StreamingReplyBubble(character, streamingText) }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                ChatJumpToLatest(
                    visible = showJumpToBottom,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    onClick = {
                        coroutineScope.launch {
                            if (scrollTargetIndex > 0) listState.animateScrollToItem(scrollTargetIndex)
                        }
                    },
                )
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
        if (showAiConnection) AiConnectionSheet(onDismiss = { showAiConnection = false })
        if (showProactive) ProactiveSettingsSheet(onDismiss = { showProactive = false })
    }
}

internal fun latestSaveableChatMessage(messages: List<ChatMessage>): ChatMessage? =
    messages.lastOrNull { it.isSaveableChatMessage() }

private fun ChatMessage.isSaveableChatMessage(): Boolean =
    id.isNotBlank() && text.isNotBlank() && statusLabel == null &&
        (sender == MessageSender.USER || sender == MessageSender.CHARACTER)
