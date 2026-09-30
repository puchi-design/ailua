package com.example.ui.reality

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.reality.RealitySettings
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import kotlinx.coroutines.launch

@Composable
fun RealityBridgeScreen(onBack: () -> Unit, isDarkTheme: Boolean = false, onToggleTheme: () -> Unit = {}) {
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

    Column(Modifier.fillMaxSize()) {
        VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TextButton(onClick = onBack) { Text("‹ 返回应用库") }
            Text("现实感知", style = MaterialTheme.typography.headlineSmall)
            Text("只读取本机概括信号。使用统计和健康数据需主动开启；原始记录不会写入世界事件或聊天数据库。", style = MaterialTheme.typography.bodyMedium)
            SettingRow("启用现实感知", settings.enabled) { RealityRepository.setSettings(settings.copy(enabled = it)) }
            SettingRow("使用电量作为聊天背景", settings.batteryEnabled) { RealityRepository.setSettings(settings.copy(batteryEnabled = it)) }
            SettingRow("读取屏幕交互状态", settings.screenEnabled) { RealityRepository.setSettings(settings.copy(screenEnabled = it)) }
            Text("电量：${snapshot?.batteryPercent?.let { "$it%" } ?: "未知"}${if (snapshot?.charging == true) " · 充电中" else ""}")
            Text("屏幕：${when (snapshot?.screenInteractive) { true -> "正在使用"; false -> "未交互"; null -> "未知" }}")
            SettingRow("允许读取使用时长", settings.usageEnabled) { RealityRepository.setSettings(settings.copy(usageEnabled = it)) }
            Text("使用统计权限：${if (snapshot?.usagePermissionGranted == true) "已授权" else "需要在系统设置中授权"}")
            Text("今日使用时间：${snapshot?.todayScreenTimeMinutes?.let { "$it 分钟" } ?: "未知"}")
            Text("最近应用类别：${snapshot?.recentAppCategory ?: "未知"}")
            if (snapshot?.usagePermissionGranted != true) {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text("打开使用统计设置") }
            }
            SettingRow("允许读取健康概况", settings.healthEnabled) { RealityRepository.setSettings(settings.copy(healthEnabled = it)) }
            val healthStatus = RealityRepository.healthStatus()
            Text("Health Connect：${if (healthStatus == HealthConnectClient.SDK_AVAILABLE) "可用" else "此设备暂不可用"}")
            Text("步数权限：${if (snapshot?.healthStepsGranted == true) "已授权" else "未连接"}")
            Text("今日步数：${snapshot?.todaySteps?.toString() ?: "未知"}")
            Text("睡眠权限：${if (snapshot?.healthSleepGranted == true) "已授权" else "未连接"}")
            Text("最近睡眠：${snapshot?.lastSleepMinutes?.let { "$it 分钟" } ?: "未知"}")
            if (healthStatus == HealthConnectClient.SDK_AVAILABLE && settings.healthEnabled &&
                (snapshot?.healthStepsGranted != true || snapshot?.healthSleepGranted != true)) {
                Button(onClick = { healthLauncher?.launch(setOf(RealityRepository.stepsPermission, RealityRepository.sleepPermission)) }) {
                    Text("连接 Health Connect")
                }
            }
            TextButton(onClick = { scope.launch { RealityRepository.refresh(force = true) } }) { Text("刷新状态") }
        }
        VirtualPhoneHomeBar(onGoHome = onBack)
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
