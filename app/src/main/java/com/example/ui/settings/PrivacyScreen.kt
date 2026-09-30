package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar

@Composable
fun PrivacyScreen(onBack: () -> Unit, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack) { Text("‹ 返回设置") }
            Text("隐私与数据", style = MaterialTheme.typography.headlineMedium)
            Text("聊天、角色、记忆和虚拟世界记录保存在本机。API Key 使用 Android Keystore 加密保存。")
            Text("使用统计和 Health Connect 仅在你主动开启并授权后读取；未授权时保持未知。可随时在现实感知中关闭。")
            Text("与远程 AI 聊天时，必要的对话、角色及有限上下文会发送给你选择的 AI 服务商，受其服务条款约束。现实环境只会以低敏摘要按需加入上下文。")
            Text("AILUA 当前没有账号或云同步。")
        }
        VirtualPhoneHomeBar(canGoBack = true, onBack = onBack, onGoHome = onBack)
    }
}
