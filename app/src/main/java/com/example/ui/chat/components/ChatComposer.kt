package com.example.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ChatComposer(
    inputText: String,
    isGenerating: Boolean,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit,
    inputTestTag: String = "chat_text_input",
    sendTestTag: String = "chat_send_btn",
    maxLines: Int = 1,
    showAttachments: Boolean = true,
    showMicrophone: Boolean = true,
    placeholder: String = "输入消息……",
) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(theme.shapes.large.dp))
            .background(theme.surfaces.inset)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAttachments) {
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Default.Add, "添加交互", tint = theme.palette.onSurfaceMuted)
            }
        }
        BasicTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                .padding(start = if (showAttachments) 0.dp else 12.dp).testTag(inputTestTag),
            textStyle = theme.text.body.copy(color = theme.palette.onSurface),
            cursorBrush = SolidColor(theme.palette.accent),
            singleLine = maxLines == 1,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(imeAction = if (maxLines == 1) ImeAction.Send else ImeAction.Default),
            keyboardActions = KeyboardActions(onSend = { if (!isGenerating) onSend() }),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (inputText.isEmpty()) {
                        Text(
                            if (isGenerating) "正在回复…" else placeholder,
                            style = theme.text.body,
                            color = theme.palette.onSurfaceMuted,
                            maxLines = 1,
                        )
                    }
                    field()
                }
            },
        )
        if (showMicrophone) {
            IconButton(onClick = onMicClick) {
                Icon(Icons.Default.Mic, "语音轻语", tint = theme.palette.onSurfaceMuted)
            }
        }
        IconButton(
            onClick = { if (isGenerating) onStop() else onSend() },
            enabled = isGenerating || inputText.isNotBlank(),
            modifier = Modifier
                .clip(CircleShape)
                .background(theme.palette.accent.copy(alpha = if (isGenerating || inputText.isNotBlank()) 0.18f else 0.06f))
                .testTag(sendTestTag),
        ) {
            Icon(
                if (isGenerating) Icons.Default.Close else Icons.Default.ArrowUpward,
                if (isGenerating) "停止生成" else "发送",
                tint = if (isGenerating || inputText.isNotBlank()) theme.palette.onSurface else theme.palette.onSurfaceMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun ChatActionItem(title: String, emoji: String, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier = Modifier.clip(RoundedCornerShape(theme.shapes.small.dp))
            .clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, style = theme.text.title)
        Text(title, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
    }
}
