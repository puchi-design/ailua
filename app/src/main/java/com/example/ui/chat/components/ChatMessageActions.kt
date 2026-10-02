package com.example.ui.chat.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.model.ChatMessage
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
        Action("复制") { clipboard.setText(AnnotatedString(message.text)) }
        if (allowMemoryAndBookmark) Action("保存记忆", onSaveMemory)
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
