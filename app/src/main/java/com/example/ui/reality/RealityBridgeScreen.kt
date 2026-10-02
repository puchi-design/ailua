package com.example.ui.reality

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.data.reality.RealityRepository
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

@Composable
fun RealityBridgeScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onGoHome: () -> Unit = onBack
) {
    val theme = LocalAiluaTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by RealityRepository.settings.collectAsState()
    val snapshot by RealityRepository.snapshot.collectAsState()
    val healthLauncher = if (Build.VERSION.SDK_INT >= 26) {
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            scope.launch { RealityRepository.refresh(force = true) }
        }
    } else null
    LaunchedEffect(settings) { RealityRepository.refresh(force = true) }

    AiluaScreenScaffold(title = "现实连接", onBack = onBack, onGoHome = onGoHome) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(theme.layout.screenHorizontalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp)
        ) {
            Text("只读取本机概括信号。使用统计和健康数据需主动开启；原始记录不会写入世界事件或聊天数据库。", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            SettingRow("启用现实连接", settings.enabled) { RealityRepository.setSettings(settings.copy(enabled = it)) }
            HorizontalDivider(color = theme.surfaces.divider)
            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaSectionHeader("设备状态")
                SettingRow("使用电量作为聊天背景", settings.batteryEnabled) { RealityRepository.setSettings(settings.copy(batteryEnabled = it)) }
                StateLine("电量", "${snapshot?.batteryPercent?.let { "$it%" } ?: "未知"}${if (snapshot?.charging == true) " · 充电中" else ""}")
                SettingRow("读取屏幕交互状态", settings.screenEnabled) { RealityRepository.setSettings(settings.copy(screenEnabled = it)) }
                StateLine("屏幕", when (snapshot?.screenInteractive) { true -> "正在使用"; false -> "未交互"; null -> "未知" })
            }
            HorizontalDivider(color = theme.surfaces.divider)
            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaSectionHeader("屏幕使用")
                SettingRow("允许读取使用时长", settings.usageEnabled) { RealityRepository.setSettings(settings.copy(usageEnabled = it)) }
                StateLine("使用统计权限", if (snapshot?.usagePermissionGranted == true) "已授权" else "未授权")
                StateLine("今日使用时间", snapshot?.todayScreenTimeMinutes?.let { "$it 分钟" } ?: "未知")
                StateLine("最近应用类别", snapshot?.recentAppCategory ?: "未知")
                if (snapshot?.usagePermissionGranted != true) {
                    AiluaChip(label = "打开使用统计设置", onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
                }
            }
            HorizontalDivider(color = theme.surfaces.divider)
            Column(verticalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
                AiluaSectionHeader("健康数据")
                SettingRow("允许读取健康概况", settings.healthEnabled) { RealityRepository.setSettings(settings.copy(healthEnabled = it)) }
                val healthStatus = RealityRepository.healthStatus()
                StateLine("Health Connect", if (healthStatus == HealthConnectClient.SDK_AVAILABLE) "可用" else "此设备暂不可用")
                StateLine("步数权限", if (snapshot?.healthStepsGranted == true) "已授权" else "未连接")
                StateLine("今日步数", snapshot?.todaySteps?.toString() ?: "未知")
                StateLine("睡眠权限", if (snapshot?.healthSleepGranted == true) "已授权" else "未连接")
                StateLine("最近睡眠", snapshot?.lastSleepMinutes?.let { "$it 分钟" } ?: "未知")
                if (healthStatus == HealthConnectClient.SDK_AVAILABLE && settings.healthEnabled &&
                    (snapshot?.healthStepsGranted != true || snapshot?.healthSleepGranted != true)) {
                    AiluaChip(label = "连接 Health Connect", onClick = {
                        healthLauncher?.launch(setOf(RealityRepository.stepsPermission, RealityRepository.sleepPermission))
                    })
                }
            }
            AiluaChip(label = "刷新状态", onClick = { scope.launch { RealityRepository.refresh(force = true) } })
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val theme = LocalAiluaTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = theme.text.body, color = theme.palette.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StateLine(label: String, value: String) {
    val theme = LocalAiluaTheme.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
        Text(value, modifier = Modifier.weight(1f), style = theme.text.secondary, color = theme.palette.onSurface)
    }
}
