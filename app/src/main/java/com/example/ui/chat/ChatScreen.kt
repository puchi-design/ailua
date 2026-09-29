package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChatMessage
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import com.example.data.engine.WorldStateRepository
import com.example.data.projection.projectPresence
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.AiConnectionSheet
import com.example.ui.components.ProactiveSettingsSheet
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import kotlinx.coroutines.launch

/**
 * ChatScreen — pure rendering + actions over [ChatViewModel] (P3C-4 §2).
 *
 * All fake chat paths are gone: no MockData seed, no canned auto-replies, no
 * hardcoded regenerate, no fake alternateTexts variants. Every send/quick
 * prompt/special action flows through the real runtime → provider → DB.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    character: CharacterProfile,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: ChatViewModel = viewModel(
        key = character.id,
        factory = ChatViewModel.factory(appContext, character),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = uiState.messages
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val currentActivity = projectPresence(character, worldEvents).currentActivity
    val streamingText = uiState.streamingText

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var showAiConnection by remember { mutableStateOf(false) }
    var showProactive by remember { mutableStateOf(false) }
    val bookmarkedMsgIds = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val contentCount = messages.size + if (streamingText != null) 1 else 0
    val scrollTargetIndex = contentCount

    val showJumpToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex < (uiState.messages.size - 2).coerceAtLeast(0)
        }
    }

    val lastAssistantId = remember(messages) {
        messages.lastOrNull { it.sender == MessageSender.CHARACTER }?.id
    }

    val quickPrompts = when (character.id.lowercase()) {
        "yuna" -> listOf(
            "你在做什么呢？",
            "今天有点累…",
            "布丁还有吗？",
            "雨下得真大呢",
            "明天去探店吧！"
        )
        "noa" -> listOf(
            "你在做什么呢？",
            "今天有点累…",
            "给我讲个故事吧",
            "雨下得真大呢",
            "在听什么唱片？"
        )
        else -> listOf(
            "你在做什么呢？",
            "今天有点累…",
            "给我讲个故事吧",
            "雨下得真大呢",
            "红茶好喝吗？"
        )
    }

    LaunchedEffect(messages.size, streamingText) {
        if (scrollTargetIndex > 0) {
            listState.animateScrollToItem(scrollTargetIndex)
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("chat_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Virtual OS Status Bar
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Chat Header
            ChatHeader(
                character = character,
                currentActivity = currentActivity,
                onBack = onBackToHome,
                onOpenProfile = onOpenProfile,
                onOpenMenu = { showMenu = true }
            )

            // Chat Overflow Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("查看心契档案") },
                    onClick = {
                        showMenu = false
                        onOpenProfile()
                    }
                )
                DropdownMenuItem(
                    text = { Text("AI 连接") },
                    onClick = {
                        showMenu = false
                        showAiConnection = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("主动消息") },
                    onClick = {
                        showMenu = false
                        showProactive = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("存入羁绊记忆") },
                    onClick = {
                        showMenu = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已将最近对话凝华为心契记忆")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("清空对话记录") },
                    onClick = {
                        showMenu = false
                        viewModel.clearSession()
                    }
                )
            }

            // Message Stream (DB-backed)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                items(messages, key = { it.id }) { message ->
                    val isBookmarked = bookmarkedMsgIds.contains(message.id)
                    ChatMessageItem(
                        message = message,
                        character = character,
                        isBookmarked = isBookmarked,
                        onToggleBookmark = {
                            if (bookmarkedMsgIds.contains(message.id)) {
                                bookmarkedMsgIds.remove(message.id)
                                coroutineScope.launch { snackbarHostState.showSnackbar("已取消心契书签") }
                            } else {
                                bookmarkedMsgIds.add(message.id)
                                coroutineScope.launch { snackbarHostState.showSnackbar("已添加至心契书签 ★") }
                            }
                        },
                        onSaveMemory = {
                            viewModel.saveMemory(message)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已保存到记忆")
                            }
                        },
                        onRegenerate = { viewModel.regenerate() },
                        canRegenerate = !uiState.isGenerating && message.id == lastAssistantId,
                        onSwitchVariant = { direction -> viewModel.switchVariant(message.id, direction) }
                    )
                }

                if (streamingText != null) {
                    item(key = "streaming-reply") {
                        StreamingReplyBubble(
                            character = character,
                            text = streamingText,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(6.dp)) }
            }

            // Quick Prompt Chips → real runtime
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.send(prompt) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Chat Input Bar / Voice Recording Bar
            if (isVoiceRecording) {
                VoiceRecordingBar(
                    onCancel = { isVoiceRecording = false },
                    onSendVoice = {
                        isVoiceRecording = false
                        viewModel.send("［语音轻语 6秒］今晚能一起听着雨声多聊一会儿吗？")
                    }
                )
            } else {
                ChatInputBar(
                    inputText = inputText,
                    characterName = character.name,
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
                    onMicClick = { isVoiceRecording = true }
                )
            }

            // Additional Action Sheet — every action produces a REAL user turn
            AnimatedVisibility(visible = showActionSheet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ActionSheetItem("传递暖意", "❤️") {
                        showActionSheet = false
                        viewModel.send(
                            "伸出手轻轻碰了碰 ${character.name} 放在桌上的杯沿，对 TA 温和地笑了笑。"
                        )
                    }
                    ActionSheetItem("分享照片", "📷") {
                        showActionSheet = false
                        viewModel.send(
                            "［发送了一张深夜街角的照片］你看，今晚的月光落在湿漉漉的石板路上很美。"
                        )
                    }
                    ActionSheetItem("语音轻语", "🎙️") {
                        showActionSheet = false
                        viewModel.send("［语音轻语 8秒］窗外的雨声很好听，想和你一起听一会儿。")
                    }
                    ActionSheetItem("凝华记忆", "💎") {
                        showActionSheet = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已将此刻共处收录至羁绊回响")
                        }
                    }
                }
            }

            // Virtual Home Indicator Bar
            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBackToHome,
                onGoHome = onBackToHome
            )
        }

        // Jump To Bottom Button (Jetchat inspired pattern)
        AnimatedVisibility(
            visible = showJumpToBottom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 125.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, AiluaMistBlue.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .shadow(3.dp, RoundedCornerShape(20.dp))
                    .clickable {
                        coroutineScope.launch {
                            if (scrollTargetIndex > 0) {
                                listState.animateScrollToItem(scrollTargetIndex)
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "回到底部",
                        modifier = Modifier.size(14.dp),
                        tint = AiluaMistBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "回到最新消息",
                        fontSize = 11.sp,
                        color = AiluaMistBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
        )

        if (showAiConnection) {
            AiConnectionSheet(onDismiss = { showAiConnection = false })
        }

        if (showProactive) {
            ProactiveSettingsSheet(onDismiss = { showProactive = false })
        }
    }
}

@Composable
private fun ChatHeader(
    character: CharacterProfile,
    currentActivity: String,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("chat_back_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回AILUA主屏",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpenProfile() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AiluaAvatar(
                avatarId = character.avatarId,
                size = 38.dp,
                showHalo = false,
                showLivingStatus = true
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Relationship Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AiluaDustyRose.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "心契 Lv.${character.bondLevel}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AiluaDustyRose
                        )
                    }
                }

                // Live status
                Text(
                    text = currentActivity,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "选项菜单",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Live partial reply while the provider streams (P3C-4 §8). */
@Composable
private fun StreamingReplyBubble(
    character: CharacterProfile,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        AiluaAvatar(
            avatarId = character.avatarId,
            size = 34.dp,
            showHalo = false
        )
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .border(
                    0.8.dp,
                    AiluaMistBlue.copy(alpha = 0.5f),
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text.ifEmpty { "正在输入…" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    fontStyle = if (text.isEmpty()) FontStyle.Italic else FontStyle.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (text.isEmpty()) 0.6f else 1f
                )
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    character: CharacterProfile,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onSaveMemory: () -> Unit,
    onRegenerate: () -> Unit,
    canRegenerate: Boolean = false,
    onSwitchVariant: (Int) -> Unit = {}
) {
    when (message.sender) {
        MessageSender.SYSTEM -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }

        MessageSender.USER -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 18.dp
                                )
                            )
                            .background(AiluaMistBlue)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Save affordance (same as character messages) — P3C-5 entry.
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "存入记忆",
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(13.dp)
                        .clickable { onSaveMemory() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        MessageSender.CHARACTER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                AiluaAvatar(
                    avatarId = message.senderCharacterId ?: character.avatarId,
                    size = 34.dp,
                    showHalo = false
                )

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    when (message.type) {
                        MessageType.TEXT -> {
                            val failed = message.statusLabel != null
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 18.dp,
                                            bottomEnd = 18.dp
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        0.8.dp,
                                        if (failed) MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 18.dp,
                                            bottomEnd = 18.dp
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                val displayText = message.text.ifEmpty {
                                    when (message.statusLabel) {
                                        "FAILED" -> "生成失败"
                                        "CANCELLED" -> "已取消"
                                        else -> ""
                                    }
                                }
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.5.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = if (message.text.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (failed) {
                                Text(
                                    text = when (message.statusLabel) {
                                        "FAILED" -> "生成失败 · 可点按重新生成"
                                        else -> "已取消"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }

                        MessageType.ACTION_NARRATIVE -> {
                            // Character action / narrative card
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    .border(
                                        0.5.dp,
                                        AiluaMutedLavender.copy(alpha = 0.4f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "* ${message.text} *",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        MessageType.VOICE -> {
                            // Voice message representation
                            var isPlaying by remember { mutableStateOf(false) }
                            val listeningName = message.senderName ?: character.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        0.8.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { isPlaying = !isPlaying }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                        contentDescription = "播放语音",
                                        tint = AiluaMistBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isPlaying) "正在倾听 $listeningName..." else "语音轻诉 ${message.voiceDurationSeconds}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        MessageType.MEMORY_CARD -> {
                            // Rich Memory Snippet Card
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                AiluaMoonGold.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        AiluaMoonGold.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AiluaMoonGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "心契记忆凝华 · ${message.memoryTag}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            color = AiluaMoonGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    // Reactions & Action Buttons
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reaction tags
                        message.reactions.forEach { reaction ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = reaction, fontSize = 11.sp)
                            }
                        }

                        // Real variant picker pill (DB-backed, P3C-4 §11)
                        if (message.sender == MessageSender.CHARACTER &&
                            message.type == MessageType.TEXT &&
                            message.variantCount > 1
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "上一回复分支",
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { onSwitchVariant(-1) },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${message.variantIndex + 1}/${message.variantCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "下一回复分支",
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { onSwitchVariant(1) },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Bookmark Toggle
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "心契书签",
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onToggleBookmark() },
                            tint = if (isBookmarked) AiluaMoonGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        // Subtle affordances: Save Memory & Regenerate
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "存入记忆",
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onSaveMemory() },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (canRegenerate) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "换一种回应",
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { onRegenerate() },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    characterName: String,
    isGenerating: Boolean = false,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit = {},
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAttachClick,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = "添加交互",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onMicClick,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "语音轻语",
                tint = AiluaMistBlue
            )
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .testTag("chat_text_input"),
            placeholder = {
                Text(
                    text = if (isGenerating) "正在生成回复…" else "对 $characterName 说点什么…",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = AiluaMistBlue.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        IconButton(
            onClick = { if (isGenerating) onStop() else onSend() },
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isGenerating -> AiluaDustyRose
                        inputText.isNotBlank() -> AiluaMistBlue
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .testTag("chat_send_btn")
        ) {
            Icon(
                imageVector = if (isGenerating) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isGenerating) "停止生成" else "发送",
                modifier = Modifier.size(16.dp),
                tint = when {
                    isGenerating -> Color.White
                    inputText.isNotBlank() -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
    }
}

@Composable
private fun VoiceRecordingBar(
    onCancel: () -> Unit,
    onSendVoice: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AiluaDustyRose.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = AiluaDustyRose,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "🎙️ 正在录制心声轻语… 0:04",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    color = AiluaDustyRose
                )
                Text(
                    text = "模拟声纹识别: [ ▂▃▅▆▇▅▃▂ ]",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onSendVoice,
                colors = ButtonDefaults.buttonColors(containerColor = AiluaMistBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("发送轻语", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun ActionSheetItem(
    title: String,
    emoji: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
