package com.example.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.ai.repository.ProviderGraph
import com.example.ui.components.AiConnectionSheet
import com.example.ui.components.WorldTimeDevSheet
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.themeengine.LocalAiluaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReality: () -> Unit,
    onPrivacy: () -> Unit,
    onCharacters: () -> Unit,
    onToggleTheme: () -> Unit,
    isDarkTheme: Boolean,
    onGoHome: () -> Unit = onBack
) {
    val theme = LocalAiluaTheme.current
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

    AiluaScreenScaffold(title = "设置", onBack = onBack, onGoHome = onGoHome, modifier = Modifier.testTag("settings_screen")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = theme.layout.screenHorizontalPadding.dp)) {
            SettingsRow("AI 服务", if (activeId == null) "还没有连接 AI" else "已配置 AI 连接") { showAi = true }
            SettingsRow("外观", if (isDarkTheme) "深色模式" else "浅色模式", onToggleTheme)
            SettingsRow("主动消息", "主动消息与来信保存在通知中心；可在控制中心开启专注模式。")
            SettingsRow("角色工坊", "导入或创建角色", onCharacters)
            SettingsRow("现实连接", "电量、屏幕、使用统计与健康数据", onReality)
            SettingsRow("隐私", "本机存储与 AI 服务商", onPrivacy)
            SettingsRow("数据", "重置本机 AILUA 数据") { showReset = true }
            resetError?.let { Text(it, style = theme.text.secondary, color = theme.palette.onSurface) }
            if (devEnabled) SettingsRow("开发者", "世界时间调试") { showDev = true }
            SettingsRow("关于", "AILUA ${BuildConfig.VERSION_NAME}") {
                versionTaps++
                if (versionTaps >= 7) {
                    devEnabled = true
                    context.getSharedPreferences("ailua_settings", Context.MODE_PRIVATE).edit().putBoolean("developer", true).apply()
                }
            }
        }
    }
    if (showAi) AiConnectionSheet(onDismiss = { showAi = false }, onConnected = { showAi = false })
    if (showDev) WorldTimeDevSheet(onDismiss = { showDev = false })
    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("重置 AILUA？", style = theme.text.title) },
        text = { Text("这会永久删除本机聊天、角色、世界、记忆和 AI 配置。确认后应用将关闭，需要重新打开。", style = theme.text.body) },
        confirmButton = { TextButton(onClick = {
            showReset = false
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (!manager.clearApplicationUserData()) resetError = "重置未完成，请在系统应用信息中清除数据"
        }) { Text("删除本机数据", style = theme.text.secondary) } },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("取消", style = theme.text.secondary) } }
    )
}

@Composable
private fun SettingsRow(title: String, detail: String, onClick: (() -> Unit)? = null) {
    val theme = LocalAiluaTheme.current
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = theme.layout.screenHorizontalPadding.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = theme.text.body, color = theme.palette.onSurface)
                Text(detail, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
            if (onClick != null) Text("›", style = theme.text.section, color = theme.palette.onSurfaceMuted)
        }
        HorizontalDivider(color = theme.surfaces.divider)
    }
}
