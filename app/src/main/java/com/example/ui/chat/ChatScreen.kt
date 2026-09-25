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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.MockData
import com.example.data.model.ChatMessage
import com.example.data.model.CharacterProfile
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    character: CharacterProfile = MockData.sampleCharacter,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val messages = remember(character.id) {
        mutableStateListOf(*MockData.getChatMessagesForCharacter(character.id).toTypedArray())
    }
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    val bookmarkedMsgIds = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val showJumpToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex < (messages.size - 2).coerceAtLeast(0)
        }
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

    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size - 1)
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
                    text = { Text("存入羁绊记忆") },
                    onClick = {
                        showMenu = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已将最近对话凝华为心契记忆")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("分支对话探讨") },
                    onClick = {
                        showMenu = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已开启轻量平行对话分支")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("清空虚拟通话记录") },
                    onClick = {
                        showMenu = false
                        messages.clear()
                    }
                )
            }

            // Message Stream
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
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("已保存到「记忆晶核」")
                            }
                        },
                        onRegenerate = {
                            coroutineScope.launch {
                                val (regenText, reactions) = when (character.id.lowercase()) {
                                    "yuna" -> Pair(
                                        "（悠奈眨了眨眼睛，笑容灿烂）有你在身边，感觉每天都有数不完的新奇冒险！",
                                        listOf("🍮", "✨")
                                    )
                                    "noa" -> Pair(
                                        "（诺亚微微颔首，目光温和）无需过多言语，片刻的宁静，足抵万千喧嚣。",
                                        listOf("📖", "🌙")
                                    )
                                    else -> Pair(
                                        "（${character.name} 微微侧过头，眼眸里映着温柔的灯光）其实，能像现在这样安安静静地和你待着，我就已经很满足了。",
                                        listOf("✨", "🌸")
                                    )
                                }
                                messages.add(
                                    ChatMessage(
                                        id = "regen_${System.currentTimeMillis()}",
                                        sender = MessageSender.CHARACTER,
                                        senderCharacterId = character.avatarId,
                                        senderName = character.name,
                                        type = MessageType.TEXT,
                                        text = regenText,
                                        timestamp = "刚才",
                                        reactions = reactions
                                    )
                                )
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(6.dp)) }
            }

            // Quick Prompt Chips
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
                            .clickable {
                                messages.add(
                                    ChatMessage(
                                        id = "u_${System.currentTimeMillis()}",
                                        sender = MessageSender.USER,
                                        type = MessageType.TEXT,
                                        text = prompt,
                                        timestamp = "刚才"
                                    )
                                )
                                // Trigger character reply
                                coroutineScope.launch {
                                    handleCharacterAutoReply(prompt, messages, character)
                                }
                            }
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
                        messages.add(
                            ChatMessage(
                                id = "v_u_${System.currentTimeMillis()}",
                                sender = MessageSender.USER,
                                type = MessageType.TEXT,
                                text = "［语音轻语 6秒］今晚能一起听着雨声多聊一会儿吗？",
                                timestamp = "刚才"
                            )
                        )
                        coroutineScope.launch {
                            handleCharacterAutoReply("语音轻语", messages, character)
                        }
                    }
                )
            } else {
                ChatInputBar(
                    inputText = inputText,
                    characterName = character.name,
                    onInputTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            val text = inputText.trim()
                            messages.add(
                                ChatMessage(
                                    id = "u_${System.currentTimeMillis()}",
                                    sender = MessageSender.USER,
                                    type = MessageType.TEXT,
                                    text = text,
                                    timestamp = "刚才"
                                )
                            )
                            inputText = ""
                            coroutineScope.launch {
                                handleCharacterAutoReply(text, messages, character)
                            }
                        }
                    },
                    onAttachClick = { showActionSheet = !showActionSheet },
                    onMicClick = { isVoiceRecording = true }
                )
            }

            // Additional Action Sheet
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
                        messages.add(
                            ChatMessage(
                                id = "act_${System.currentTimeMillis()}",
                                sender = MessageSender.USER,
                                type = MessageType.ACTION_NARRATIVE,
                                text = "伸出手轻轻碰了碰 ${character.name} 放在桌上的杯沿，对 TA 温和地笑了笑。",
                                timestamp = "刚才"
                            )
                        )
                        coroutineScope.launch {
                            val (replyText, reactions) = when (character.id.lowercase()) {
                                "yuna" -> Pair(
                                    "指尖触碰的瞬间，悠奈像受惊的小鹿般笑出声来，顺手把刚烤好的松饼推到你面前～",
                                    listOf("🍮", "🌸")
                                )
                                "noa" -> Pair(
                                    "指尖微触，诺亚放下手中的诗卷，眼中掠过一丝温意，轻轻将温热的茶杯推近了些许。",
                                    listOf("📖", "🌙")
                                )
                                else -> Pair(
                                    "指尖感受到杯子的温热，${character.name} 微微睁大眼睛，随即莞尔一笑，将羊毛毯又往你身旁拉近了一些。",
                                    listOf("🌸", "🍵")
                                )
                            }
                            messages.add(
                                ChatMessage(
                                    id = "resp_${System.currentTimeMillis()}",
                                    sender = MessageSender.CHARACTER,
                                    senderCharacterId = character.avatarId,
                                    senderName = character.name,
                                    type = MessageType.ACTION_NARRATIVE,
                                    text = replyText,
                                    timestamp = "刚才",
                                    reactions = reactions
                                )
                            )
                        }
                    }
                    ActionSheetItem("分享照片", "📷") {
                        showActionSheet = false
                        messages.add(
                            ChatMessage(
                                id = "act_${System.currentTimeMillis()}",
                                sender = MessageSender.USER,
                                type = MessageType.TEXT,
                                text = "［发送了一张深夜街角的照片］你看，今晚的月光落在湿漉漉的石板路上很美。",
                                timestamp = "刚才"
                            )
                        )
                        coroutineScope.launch {
                            handleCharacterAutoReply("照片", messages, character)
                        }
                    }
                    ActionSheetItem("语音轻语", "🎙️") {
                        showActionSheet = false
                        messages.add(
                            ChatMessage(
                                id = "v_${System.currentTimeMillis()}",
                                sender = MessageSender.CHARACTER,
                                senderCharacterId = character.avatarId,
                                senderName = character.name,
                                type = MessageType.VOICE,
                                text = "［语音 8秒］",
                                timestamp = "刚才",
                                voiceDurationSeconds = 8
                            )
                        )
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
                            listState.animateScrollToItem(messages.size - 1)
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
    }
}

@Composable
private fun ChatHeader(
    character: CharacterProfile,
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
                    text = character.currentActivity,
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

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    character: CharacterProfile,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onSaveMemory: () -> Unit,
    onRegenerate: () -> Unit
) {
    var alternateIndex by remember(message.id) { mutableStateOf(0) }
    val alternateTexts = remember(message.id, message.text) {
        listOf(
            message.text,
            when (character.id.lowercase()) {
                "yuna" -> "（悠奈晃了晃手中的甜品包装袋，眼睛笑得眯起来）哼哼，无论什么时候，只要你需要，我都在你一抬眼就能看到的地方！"
                "noa" -> "（诺亚翻过手中泛黄的旧书页，轻声应答）夜雨是天地间最好的伴奏，很高兴能与你共享这份安谧。"
                else -> "（小弥把温热的茶杯轻轻推到你手边，睫毛轻颤）其实……今天在窗边看雨的时候，心里也一直在悄悄想着你。"
            }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
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
                                    text = if (message.sender == MessageSender.CHARACTER) alternateTexts[alternateIndex % alternateTexts.size] else message.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.5.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
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

                        // SillyTavern-style Swipe Alternate picker pill
                        if (message.sender == MessageSender.CHARACTER && message.type == MessageType.TEXT) {
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
                                        .clickable {
                                            alternateIndex = (alternateIndex - 1 + alternateTexts.size) % alternateTexts.size
                                        },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(alternateIndex % alternateTexts.size) + 1}/${alternateTexts.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "下一回复分支",
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable {
                                            alternateIndex = (alternateIndex + 1) % alternateTexts.size
                                        },
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

@Composable
private fun ChatInputBar(
    inputText: String,
    characterName: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
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
                    text = "对 $characterName 说点什么…",
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
            onClick = onSend,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (inputText.isNotBlank()) AiluaMistBlue else MaterialTheme.colorScheme.surfaceVariant)
                .testTag("chat_send_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                modifier = Modifier.size(16.dp),
                tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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

private fun handleCharacterAutoReply(
    userText: String,
    messages: MutableList<ChatMessage>,
    character: CharacterProfile
) {
    val replyText = when (character.id.lowercase()) {
        "yuna" -> when {
            userText.contains("累") -> "（拉住你的手晃了晃）累了就快停下来！我把焦糖布丁热一热分你一半，吃甜的心情立刻就会好起来哦！"
            userText.contains("做什么") -> "我在整理今天扫街拍的雨天猫猫抓拍！你看，这只猫居然在屋檐下甩水，超级可爱～"
            userText.contains("布丁") -> "是全家便利店最后一盒限定款！奶香超浓郁，明天我专门去给你多买两个！"
            userText.contains("照片") -> "哇！这张照片的光影抓得太有感觉了！下次一定要带我去这里打卡！"
            userText.contains("探店") -> "我知道三家超级棒的日落甜品店，这周末就出发！"
            else -> "嘿嘿，听你说话好开心！我们明天再一起去木兰茶馆好不好？"
        }
        "noa" -> when {
            userText.contains("累") -> "人的思绪如琴弦，紧绷太久便失了音准。坐下来，听一曲白噪音，把心事暂且搁置吧。"
            userText.contains("做什么") -> "正在用雪松油擦拭老旧黑胶唱机的木壳。雨水天气容易受潮，这些老物件需要格外呵护。"
            userText.contains("书") || userText.contains("故事") -> "月光书阁藏着一本1930年的星河十四行诗集，扉页写着：『微光纵然微弱，亦足照亮一页长夜。』"
            userText.contains("照片") -> "石板路上倒映的冷色月光，确实有一种古典画作的沉静质感。"
            userText.contains("唱片") -> "现在转盘上放的是1978年的爵士萨克斯独奏，音质温润醇厚。"
            else -> "静听长夜细雨，此中自有从容处。我在月光书阁，随时欢迎你的到来。"
        }
        else -> when {
            userText.contains("累") -> "（伸出手轻轻摸了摸你的发梢）今天真的辛苦啦。别想工作的事情了，闭上眼睛，我在这里陪着你，听一会儿雨声吧。"
            userText.contains("做什么") -> "我刚才在看窗户上滑下来的雨珠，猜哪一颗能最先滑到底部呢～要不要一起猜一局？"
            userText.contains("故事") -> "从前有一座静悄悄的钟表镇，夜晚下雨的时候，时间的齿轮会放慢两倍，只留给彼此心有灵犀的人慢慢相处…"
            userText.contains("红茶") -> "温温热热的，放了半勺薄荷蜂蜜。留的那杯温度刚好，喝一口整个人都会暖和起来的。"
            userText.contains("照片") -> "真美…仿佛我也和你并肩站在那条湿漉漉的街道上，吹着同一缕夜风。"
            else -> "嗯，我在听。无论你想说什么，${character.name}都一直在这里陪着你。"
        }
    }

    val senderName = character.name
    val charAvatarId = character.avatarId

    messages.add(
        ChatMessage(
            id = "c_${System.currentTimeMillis()}",
            sender = MessageSender.CHARACTER,
            senderCharacterId = charAvatarId,
            senderName = senderName,
            type = MessageType.TEXT,
            text = replyText,
            timestamp = "刚才",
            reactions = if (character.id == "yuna") listOf("🍮", "✨") else if (character.id == "noa") listOf("📖", "🌙") else listOf("🌙", "🍵")
        )
    )
}
