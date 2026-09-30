package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.registry.CharacterRegistry
import com.example.ui.components.AiConnectionSheet
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenRelations: () -> Unit = {},
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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().testTag("group_chat_screen")) {
        VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Column(Modifier.weight(1f)) {
                Text("雨夜茶会", style = MaterialTheme.typography.titleLarge)
                Text("小弥 · 悠奈 · 诺亚 · 你", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onOpenRelations) { Text("关系") }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.messages.isEmpty()) item {
                Text("茶会还很安静。你可以先和大家打个招呼。", modifier = Modifier.padding(18.dp))
            }
            items(state.messages, key = { it.id }) { message ->
                GroupBubble(message)
            }
            if (state.streamingSpeakerId != null) item(key = "streaming") {
                GroupBubble(GroupUiMessage("streaming", state.streamingSpeakerId, state.streamingText?.ifBlank { "正在输入…" } ?: "正在输入…"))
            }
        }

        state.error?.let { error ->
            Text(error, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            Row {
                if (error.contains("连接 AI")) TextButton(onClick = { showConnection = true }) { Text("连接 AI") }
                if (state.messages.lastOrNull()?.failed == true) TextButton(onClick = { vm.regenerate() }) { Text("重试回复") }
                TextButton(onClick = { vm.dismissError() }) { Text("关闭") }
            }
        }
        if (state.busy) TextButton(onClick = { vm.cancel() }) { Text("停止生成") }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f).testTag("group_chat_text_input"),
                placeholder = { Text("和大家说点什么…") }, maxLines = 3,
            )
            Button(
                onClick = { val message = input.trim(); input = ""; vm.send(message) },
                enabled = input.isNotBlank() && !state.busy,
                modifier = Modifier.padding(start = 8.dp).testTag("group_chat_send_btn"),
            ) { Text("发送") }
        }
        VirtualPhoneHomeBar(canGoBack = true, onBack = onBack, onGoHome = onBack)
    }
    if (showConnection) AiConnectionSheet(onDismiss = { showConnection = false }, onConnected = { showConnection = false })
}

@Composable
private fun GroupBubble(message: GroupUiMessage) {
    val isUser = message.speakerId == null && !message.failed
    val speaker = message.speakerId?.let { CharacterRegistry.getCharacter(it) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (speaker != null) AiluaAvatar(size = 32.dp, avatarId = speaker.avatarId, showHalo = false)
        Column(
            Modifier.padding(start = 6.dp).background(
                if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp),
            ).padding(10.dp),
        ) {
            if (speaker != null) Text(speaker.name, style = MaterialTheme.typography.labelSmall)
            Text(message.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
