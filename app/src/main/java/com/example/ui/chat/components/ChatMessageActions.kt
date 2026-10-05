package com.example.ui.chat.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.model.ChatMessage
import com.example.data.chat.rich.RichMessageType
import com.example.data.model.MessageSender
import com.example.data.model.MessageType
import com.example.ui.themeengine.LocalAiluaTheme

/** The message itself is the entry point; actions stay out of the reading flow. */
@Composable
fun ChatMessageActions(
    expanded: Boolean,
    message: ChatMessage,
    isBookmarked: Boolean,
    canRegenerate: Boolean,
    onDismiss: () -> Unit,
    onSaveMemory: () -> Unit,
    onToggleBookmark: () -> Unit,
    onRegenerate: () -> Unit,
    onSwitchVariant: (Int) -> Unit,
    allowMemoryAndBookmark: Boolean = true,
    onQuote: (() -> Unit)? = null,
) {
    val theme = LocalAiluaTheme.current
    val clipboard = LocalClipboardManager.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        @Composable
        fun Action(label: String, callback: () -> Unit) {
            DropdownMenuItem(
                text = { Text(label, style = theme.text.body, color = theme.palette.onSurface) },
                onClick = { onDismiss(); callback() },
            )
        }
        val copyText = if (message.richPayloads.isEmpty()) message.text else
            message.richPayloads.joinToString("") { payload ->
                when (payload.type) {
                    RichMessageType.TEXT -> payload.label.orEmpty()
                    RichMessageType.RED_PACKET -> "[红包:${payload.amount ?: ""}:${payload.label.orEmpty()}]"
                    RichMessageType.TRANSFER -> "[转账:${payload.amount ?: ""}:${payload.label.orEmpty()}]"
                    RichMessageType.GIFT -> "[礼物:${payload.label.orEmpty()}]"
                    RichMessageType.LOCATION -> "[位置:${payload.locationName ?: payload.label.orEmpty()}]"
                    RichMessageType.STICKER -> "[表情:${payload.iconKey ?: payload.label.orEmpty()}]"
                    RichMessageType.QUOTE -> payload.label.orEmpty()
                }
            }
        Action("复制") { clipboard.setText(AnnotatedString(copyText)) }
        if (onQuote != null && message.sender == MessageSender.CHARACTER &&
            message.text.isNotBlank() && message.statusLabel == null) {
            Action("引用", onQuote)
        }
        if (allowMemoryAndBookmark && message.text.isNotBlank()) Action("保存记忆", onSaveMemory)
        if (canRegenerate) Action("重新生成", onRegenerate)
        if (allowMemoryAndBookmark) Action(if (isBookmarked) "取消收藏" else "收藏", onToggleBookmark)
        if (message.sender == MessageSender.CHARACTER &&
            message.type == MessageType.TEXT && message.variantCount > 1
        ) {
            Action("上一回复分支 · ${message.variantIndex + 1}/${message.variantCount}") { onSwitchVariant(-1) }
            Action("下一回复分支 · ${message.variantIndex + 1}/${message.variantCount}") { onSwitchVariant(1) }
        }
    }
}
