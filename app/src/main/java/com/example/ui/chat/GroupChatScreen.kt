package com.example.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.registry.CharacterRegistry
import com.example.ui.chat.components.ChatComposer
import com.example.ui.chat.components.ChatMessageItem
import com.example.ui.chat.components.ChatTopBar
import com.example.ui.components.AiConnectionSheet
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenRelations: () -> Unit = {},
    onGoHome: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val vm: GroupChatViewModel = viewModel(factory = GroupChatViewModel.factory(context))
    val state by vm.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var showConnection by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.streamingText) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    val theme = LocalAiluaTheme.current
    var showMenu by remember { mutableStateOf(false) }
    AiluaScreenScaffold(
        title = "雨夜茶会", onBack = onBack, onGoHome = onGoHome,
        modifier = Modifier.imePadding().testTag("group_chat_screen"),
        topBar = {
            ChatTopBar(
                characterId = "group_tea",
                name = "雨夜茶会",
                currentActivity = GroupChatViewModel.PARTICIPANTS.joinToString(" · ") { CharacterRegistry.getCharacter(it).name } + " · 你",
                onBack = onBack, onOpenProfile = onOpenRelations, onOpenMenu = { showMenu = true },
            ) {
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("关系", style = theme.text.body) },
                        onClick = { showMenu = false; onOpenRelations() })
                    DropdownMenuItem(text = { Text("AI 连接", style = theme.text.body) },
                        onClick = { showMenu = false; showConnection = true })
                }
            }
        },
        bottomBar = {
            Column {
                state.error?.let { error ->
                    Text(error, style = theme.text.secondary,
                        modifier = Modifier.padding(horizontal = theme.layout.screenHorizontalPadding.dp),
                        color = MaterialTheme.colorScheme.error)
                    Row(Modifier.padding(horizontal = 12.dp)) {
                        if (error.contains("连接 AI")) TextButton(onClick = { showConnection = true }) {
                            Text("连接 AI", style = theme.text.secondary)
                        }
                        if (state.messages.lastOrNull()?.failed == true) TextButton(onClick = { vm.regenerate() }) {
                            Text("重试回复", style = theme.text.secondary)
                        }
                        TextButton(onClick = { vm.dismissError() }) { Text("关闭", style = theme.text.secondary) }
                    }
                }
                ChatComposer(
                    inputText = input, isGenerating = state.busy, onInputTextChange = { input = it },
                    onSend = { val message = input.trim(); input = ""; vm.send(message) },
                    onStop = { vm.cancel() }, onAttachClick = {}, onMicClick = {},
                    inputTestTag = "group_chat_text_input", sendTestTag = "group_chat_send_btn",
                    maxLines = 3, showAttachments = false, showMicrophone = false, placeholder = "和大家说点什么…",
                )
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 8.dp)
                .testTag("group_members"),
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            GroupChatViewModel.PARTICIPANTS.forEach { characterId ->
                Column(Modifier.weight(1f).testTag("group_member_$characterId"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CharacterPortrait(characterId, PortraitVariant.AVATAR, Modifier.size(36.dp))
                    Text(CharacterRegistry.getCharacter(characterId).name, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.messages.isEmpty()) item {
                Text("茶会还很安静。你可以先和大家打个招呼。",
                    style = theme.text.body, color = theme.palette.onSurfaceMuted, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(state.messages, key = { it.id }) { message ->
                GroupBubble(message,
                    canRegenerate = !state.busy && message.id == state.messages.lastOrNull()?.id &&
                        (message.speakerId != null || message.failed),
                    onRegenerate = { vm.regenerate() })
            }
            if (state.streamingSpeakerId != null) item(key = "streaming") {
                GroupBubble(
                    GroupUiMessage("streaming", state.streamingSpeakerId,
                        state.streamingText?.ifBlank { "正在输入…" } ?: "正在输入…"),
                    canRegenerate = false, onRegenerate = {}, actionsEnabled = false,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    if (showConnection) AiConnectionSheet(onDismiss = { showConnection = false }, onConnected = { showConnection = false })
}

/** Presentation adapter only: group persistence, speaker routing and generation remain in the VM. */
@Composable
private fun GroupBubble(message: GroupUiMessage, canRegenerate: Boolean, onRegenerate: () -> Unit, actionsEnabled: Boolean = true) {
    val isUser = message.speakerId == null && !message.failed
    val speaker = message.speakerId?.let { CharacterRegistry.getCharacter(it) }
    ChatMessageItem(
        message = ChatMessage(
            id = message.id, sender = if (isUser) MessageSender.USER else MessageSender.CHARACTER,
            senderCharacterId = speaker?.id ?: "group_tea", senderName = speaker?.name ?: "群聊",
            text = message.text, timestamp = message.time, statusLabel = if (message.failed) "FAILED" else null,
        ),
        character = speaker ?: CharacterRegistry.getCharacter("mira"),
        isBookmarked = false, onToggleBookmark = {}, onSaveMemory = {},
        onRegenerate = onRegenerate, canRegenerate = canRegenerate, onSwitchVariant = {},
        showSenderName = true, allowMemoryAndBookmark = false, actionsEnabled = actionsEnabled,
    )
}
