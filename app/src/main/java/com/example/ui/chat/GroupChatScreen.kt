package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.MockData
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupChatScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenRelations: () -> Unit = {}
) {
    val messages = remember { mutableStateListOf(*MockData.sampleGroupChatMessages.toTypedArray()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickPrompts = listOf(
        "悠奈的布丁分我一口！",
        "大家今晚雨夜都在喝什么呀？",
        "诺亚推荐点雨夜读物呗",
        "小弥，我来找你了"
    )

    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("group_chat_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Virtual OS Status Bar
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Header
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
                    modifier = Modifier.testTag("group_chat_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回消息列表",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiluaAvatar(size = 36.dp, avatarId = "group_tea", showHalo = false)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "雨夜茶会 ☕",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "4人",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "小弥、悠奈、诺亚、你",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // View Relations / Social Map button
                IconButton(
                    onClick = onOpenRelations,
                    modifier = Modifier.testTag("group_relations_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "查看角色关系网",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    GroupChatMessageItem(message = message)
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
                                coroutineScope.launch {
                                    handleGroupMultiCharacterReplies(prompt, messages)
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

            // Input Bar
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
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("group_chat_text_input"),
                    placeholder = {
                        Text(
                            text = "在雨夜茶会里和大家说点什么…",
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
                    onClick = {
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
                                handleGroupMultiCharacterReplies(text, messages)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) AiluaMistBlue else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("group_chat_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(16.dp),
                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Virtual Home Indicator Bar
            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBack,
                onGoHome = onBack
            )
        }
    }
}

@Composable
private fun GroupChatMessageItem(message: ChatMessage) {
    when (message.sender) {
        MessageSender.SYSTEM -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
            val charTagColor = when (message.senderCharacterId) {
                "yuna" -> Color(0xFFE5A992)
                "noa" -> Color(0xFF8CA5BE)
                else -> AiluaMutedLavender
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                AiluaAvatar(size = 36.dp, avatarId = message.senderCharacterId, showHalo = false)

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    // Character Name above message
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = charTagColor
                        )
                        Text(
                            text = message.timestamp,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (message.type == MessageType.ACTION_NARRATIVE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .border(0.5.dp, charTagColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "* ${message.text} *",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
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
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Reactions
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            message.reactions.forEach { reaction ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(text = reaction, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simulates group multi-character interactive dynamics
 * Demonstrates characters knowing and reacting to each other
 */
private suspend fun handleGroupMultiCharacterReplies(
    userText: String,
    messages: MutableList<ChatMessage>
) {
    if (userText.contains("布丁")) {
        delay(600)
        messages.add(
            ChatMessage(
                id = "grp_r1_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "yuna",
                senderName = "悠奈",
                text = "（把布丁护在怀里）哇！那…那只能分你一小勺哦！焦糖的部分可珍贵了～",
                timestamp = "刚才",
                reactions = listOf("🍮", "✨")
            )
        )
        delay(800)
        messages.add(
            ChatMessage(
                id = "grp_r2_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "mira",
                senderName = "Mira",
                text = "悠奈你别逗他啦。我这里做了手工曲奇，一会儿分给大家配茶吃。",
                timestamp = "刚才",
                reactions = listOf("🍪", "❤️")
            )
        )
    } else if (userText.contains("读物") || userText.contains("书")) {
        delay(600)
        messages.add(
            ChatMessage(
                id = "grp_r3_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "noa",
                senderName = "诺亚",
                text = "推荐《古书堂事件手帖》或爱伦·坡的早期短篇。雨夜最适合伴随慢节奏的纸页翻动声入眠。",
                timestamp = "刚才",
                reactions = listOf("📖")
            )
        )
        delay(800)
        messages.add(
            ChatMessage(
                id = "grp_r4_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "yuna",
                senderName = "悠奈",
                text = "哇，诺亚一聊到书就开始滔滔不绝了！不过确实很有氛围感～",
                timestamp = "刚才"
            )
        )
    } else if (userText.contains("小弥") || userText.contains("找你")) {
        delay(600)
        messages.add(
            ChatMessage(
                id = "grp_r5_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "mira",
                senderName = "Mira",
                text = "（眉眼弯弯地抬眸望向你）嗯！我就在窗边这儿呢，茶也一直温着。",
                timestamp = "刚才",
                reactions = listOf("🌸", "🍵")
            )
        )
        delay(800)
        messages.add(
            ChatMessage(
                id = "grp_r6_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "yuna",
                senderName = "悠奈",
                text = "哎呀呀，你们俩又开始心契默契连线了，我和诺亚就当温馨背景板好了哈哈！",
                timestamp = "刚才",
                reactions = listOf("👀")
            )
        )
    } else {
        delay(600)
        messages.add(
            ChatMessage(
                id = "grp_r7_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "mira",
                senderName = "Mira",
                text = "今天辛苦了。雨夜的茶会就是为了让大家卸下包袱安安静静聚一聚的。",
                timestamp = "刚才"
            )
        )
        delay(700)
        messages.add(
            ChatMessage(
                id = "grp_r8_${System.currentTimeMillis()}",
                sender = MessageSender.CHARACTER,
                senderCharacterId = "noa",
                senderName = "诺亚",
                text = "正是。雨声是天然的防噪屏障，祝各位今晚都有安宁的好梦。",
                timestamp = "刚才"
            )
        )
    }
}
