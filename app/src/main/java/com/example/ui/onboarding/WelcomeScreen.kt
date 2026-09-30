package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.repository.ProviderGraph
import com.example.data.registry.CharacterRegistry
import com.example.ui.components.AiConnectionSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onFinish: (String, Boolean) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var showConnection by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf("mira") }
    val connected = ProviderGraph.repository.activeProfile() != null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("welcome_screen"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("AILUA", style = MaterialTheme.typography.headlineLarge)
        Text("你的另一部手机", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        when (step) {
            0 -> {
                Text("她会聊天，也会在自己的世界里继续生活。", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { step = 1 }) { Text("开始") }
            }
            1 -> {
                Text("连接 AI", style = MaterialTheme.typography.headlineSmall)
                Text("聊天需要你选择一个 AI 服务。世界在未连接时仍会继续生活。")
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("AILUA 官方服务 · 即将开放") }
                Button(onClick = { showConnection = true }, modifier = Modifier.fillMaxWidth()) { Text("自带 API Key") }
                if (connected) Text("已保存 AI 连接，可以继续。")
                TextButton(onClick = { step = 2 }) { Text(if (connected) "继续" else "暂时跳过") }
            }
            2 -> {
                Text("选一个角色开始", style = MaterialTheme.typography.headlineSmall)
                CharacterRegistry.getAllCharacters().take(3).forEach { character ->
                    OutlinedButton(onClick = { selectedId = character.id }, modifier = Modifier.fillMaxWidth()) {
                        Text("${if (selectedId == character.id) "✓ " else ""}${character.name} · ${character.title}")
                    }
                }
                TextButton(onClick = { onFinish(selectedId, true) }) { Text("导入角色卡") }
                Button(onClick = { step = 3 }) { Text("继续") }
            }
            else -> {
                Text("她已经搬进来了。", style = MaterialTheme.typography.headlineSmall)
                Text(if (connected) "去和她打个招呼吧。" else "你可以先看看她的世界，之后再连接 AI。")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onFinish(selectedId, false) }) { Text(if (connected) "去找她" else "进入 AILUA") }
            }
        }
    }
    if (showConnection) AiConnectionSheet(
        onDismiss = { showConnection = false },
        onConnected = { showConnection = false; step = 2 },
    )
}
