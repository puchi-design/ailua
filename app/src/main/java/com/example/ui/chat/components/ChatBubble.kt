package com.example.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterProfile
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun StreamingReplyBubble(character: CharacterProfile, text: String) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CharacterPortrait(character.id, PortraitVariant.AVATAR, Modifier.size(32.dp))
        Box(
            modifier = Modifier.weight(1f, fill = false)
                .clip(RoundedCornerShape(theme.shapes.medium.dp))
                .background(theme.surfaces.inset)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text.ifEmpty { "正在输入…" },
                style = theme.text.body,
                color = if (text.isEmpty()) theme.palette.onSurfaceMuted else theme.palette.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    character: CharacterProfile,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onSaveMemory: () -> Unit,
    onRegenerate: () -> Unit,
    canRegenerate: Boolean,
    onSwitchVariant: (Int) -> Unit,
    showSenderName: Boolean = false,
    allowMemoryAndBookmark: Boolean = true,
    actionsEnabled: Boolean = true,
) {
    val theme = LocalAiluaTheme.current
    if (message.sender == MessageSender.SYSTEM) {
        Text(
            message.text,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            style = theme.text.caption.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            color = theme.palette.onSurfaceMuted,
        )
        return
    }
    var showActions by remember(message.id) { mutableStateOf(false) }
    var isPlaying by remember(message.id) { mutableStateOf(false) }
    val isUser = message.sender == MessageSender.USER
    val isVoice = !isUser && message.type == MessageType.VOICE
    val senderName = publicCharacterName(message.senderCharacterId.ifBlank { character.id }, message.senderName.ifBlank { character.name })

    Row(
        modifier = Modifier.fillMaxWidth().testTag("chat_message_${message.id}"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.spacedBy(theme.layout.itemGap.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            CharacterPortrait(
                characterId = message.senderCharacterId.ifBlank { character.id },
                variant = PortraitVariant.AVATAR,
                modifier = Modifier.size(32.dp),
            )
        }
        Column(
            modifier = if (isUser) Modifier.fillMaxWidth(0.86f) else Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (showSenderName && !isUser) {
                Text(senderName, style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            Box {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(theme.shapes.medium.dp))
                        .background(if (isUser) theme.palette.accent.copy(alpha = 0.16f) else theme.surfaces.inset)
                        .combinedClickable(
                            onClick = { if (isVoice) isPlaying = !isPlaying },
                            onLongClickLabel = "消息操作",
                            onLongClick = if (actionsEnabled) ({ showActions = true }) else null,
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when {
                        isVoice -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = "播放语音",
                                tint = theme.palette.accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                if (isPlaying) "正在倾听 $senderName…"
                                else "语音轻语 ${message.voiceDurationSeconds}″",
                                style = theme.text.body,
                                color = theme.palette.onSurface,
                            )
                        }
                        !isUser && message.type == MessageType.MEMORY_CARD -> {
                            Text(
                                "记忆 · ${message.memoryTag}",
                                style = theme.text.caption,
                                color = theme.palette.onSurfaceMuted,
                            )
                            Text(message.text, style = theme.text.body, color = theme.palette.onSurface)
                        }
                        else -> Text(
                            text = message.text.ifEmpty {
                                when (message.statusLabel) {
                                    "FAILED" -> "生成失败"
                                    "CANCELLED" -> "已取消"
                                    else -> ""
                                }
                            },
                            style = if (message.type == MessageType.ACTION_NARRATIVE) {
                                theme.text.body.copy(fontStyle = FontStyle.Italic)
                            } else theme.text.body,
                            color = if (message.text.isEmpty()) theme.palette.onSurfaceMuted else theme.palette.onSurface,
                        )
                    }
                }
                ChatMessageActions(
                    expanded = showActions,
                    message = message,
                    isBookmarked = isBookmarked,
                    canRegenerate = canRegenerate,
                    onDismiss = { showActions = false },
                    onSaveMemory = onSaveMemory,
                    onToggleBookmark = onToggleBookmark,
                    onRegenerate = onRegenerate,
                    onSwitchVariant = onSwitchVariant,
                    allowMemoryAndBookmark = allowMemoryAndBookmark,
                )
            }
            if (!isUser && message.statusLabel != null) {
                Text(
                    text = if (message.statusLabel == "FAILED") {
                        if (canRegenerate) "生成失败 · 长按可重新生成" else "生成失败"
                    } else "已取消",
                    modifier = Modifier.padding(top = 4.dp),
                    style = theme.text.caption,
                    color = theme.palette.onSurfaceMuted,
                )
            }
            if (message.reactions.isNotEmpty()) {
                Text(
                    message.reactions.joinToString("  "),
                    modifier = Modifier.padding(top = 4.dp),
                    style = theme.text.caption,
                    color = theme.palette.onSurfaceMuted,
                )
            }
            if (showSenderName && message.timestamp.isNotBlank()) {
                Text(message.timestamp, style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
