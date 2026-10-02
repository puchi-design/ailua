package com.example.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.ai.repository.ProviderGraph
import com.example.ui.components.AiConnectionSheet
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.components.WorldTimeDevSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReality: () -> Unit,
    onPrivacy: () -> Unit,
    onCharacters: () -> Unit,
    onToggleTheme: () -> Unit,
    isDarkTheme: Boolean,
) {
    val context = LocalContext.current
    val activeId by ProviderGraph.repository.activeProfileId.collectAsStateWithLifecycle()
    var showAi by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showDev by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var devEnabled by remember {
        mutableStateOf(context.getSharedPreferences("ailua_settings", Context.MODE_PRIVATE).getBoolean("developer", false))
    }
    var resetError by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().testTag("settings_screen")) {
        VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("‹ 返回应用库") }
            Text("设置", style = MaterialTheme.typography.headlineMedium)
            SettingsRow("AI 服务", if (activeId == null) "还没有连接 AI" else "已配置 AI 连接") { showAi = true }
            SettingsRow("角色", "导入或创建角色") { onCharacters() }
            SettingsRow("现实感知", "电量、屏幕、使用统计与健康数据") { onReality() }
            SettingsRow("主题", if (isDarkTheme) "深色" else "浅色") { onToggleTheme() }
            Text("通知 · 主动消息与来信会保存在通知中心，可在控制中心开启专注模式", style = MaterialTheme.typography.bodySmall)
            SettingsRow("隐私", "了解本机存储与 AI 服务商") { onPrivacy() }
            SettingsRow("数据", "重置本机 AILUA 数据") { showReset = true }
            resetError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("AILUA ${BuildConfig.VERSION_NAME}", modifier = Modifier.clickable {
                versionTaps++
                if (versionTaps >= 7) {
                    devEnabled = true
                    context.getSharedPreferences("ailua_settings", Context.MODE_PRIVATE).edit().putBoolean("developer", true).apply()
                }
            }.padding(10.dp))
            if (devEnabled) SettingsRow("开发者选项", "世界时间调试") { showDev = true }
        }
        VirtualPhoneHomeBar(canGoBack = true, onBack = onBack, onGoHome = onBack)
    }
    if (showAi) AiConnectionSheet(onDismiss = { showAi = false }, onConnected = { showAi = false })
    if (showDev) WorldTimeDevSheet(onDismiss = { showDev = false })
    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("重置 AILUA？") },
        text = { Text("这会永久删除本机聊天、角色、世界、记忆和 AI 配置。确认后应用将关闭，需要重新打开。") },
        confirmButton = { TextButton(onClick = {
            showReset = false
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (!manager.clearApplicationUserData()) resetError = "重置未完成，请在系统应用信息中清除数据"
        }) { Text("删除本机数据") } },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("取消") } },
    )
}

@Composable
private fun SettingsRow(title: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›")
    }
}
