package com.example.ui.systemui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.systemui.control.ControlCenterState
import com.example.ui.components.LocalOsChromeState
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ControlCenter(
    state: ControlCenterState,
    isDarkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    onFocusModeChange: (Boolean) -> Unit,
    onQuietModeChange: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onLock: () -> Unit,
    onTheme: () -> Unit,
    onLaunchRoute: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAiluaTheme.current
    val chrome = LocalOsChromeState.current
    Column(
        modifier.fillMaxSize().background(theme.palette.backgroundPrimary.copy(alpha = theme.controlCenter.panelAlpha))
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp)
            .testTag("control_center"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(chrome.timeLabel, color = theme.palette.onSurface,
                    fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text("控制中心", color = theme.palette.onSurfaceMuted, fontSize = 12.sp)
            }
            Text("电量 ${chrome.batteryLabel}", color = theme.palette.onSurfaceMuted, fontSize = 12.sp,
                modifier = Modifier.testTag("control_battery"))
            IconButton(onClick = onClose, modifier = Modifier.testTag("control_close")) {
                Icon(Icons.Default.Close, "收起控制中心", tint = theme.palette.onSurface)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ControlTile(chrome.networkLabel, if (chrome.networkLabel.contains("离线")) "未连接" else "已连接",
                Icons.Default.Wifi, active = !chrome.networkLabel.contains("离线"),
                modifier = Modifier.weight(1f), tag = "control_network")
            ControlTile("深色外观", if (isDarkTheme) "已开启" else "已关闭", Icons.Default.DarkMode,
                active = isDarkTheme, onClick = onToggleDarkMode, modifier = Modifier.weight(1f), tag = "control_dark")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ControlTile("专注", if (state.focusMode) "仅在通知中心显示" else "允许横幅提醒", Icons.Default.DoNotDisturbOn,
                active = state.focusMode, onClick = { onFocusModeChange(!state.focusMode) },
                modifier = Modifier.weight(1f), tag = "control_focus")
            ControlTile("静音", if (state.quietMode) "安静提醒" else "允许提醒触感", Icons.Default.VolumeOff,
                active = state.quietMode, onClick = { onQuietModeChange(!state.quietMode) },
                modifier = Modifier.weight(1f), tag = "control_quiet")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ControlTile("锁屏", "轻轻收起此刻", Icons.Default.Lock, onClick = onLock,
                modifier = Modifier.weight(1f), tag = "control_lock")
            ControlTile("主题", "前往桌面换装", Icons.Default.Palette, onClick = onTheme,
                modifier = Modifier.weight(1f), tag = "control_theme")
        }
        BrightnessControl(state.virtualBrightness, onBrightnessChange)
        CompanionControlCard(onLaunchRoute)
        Spacer(Modifier.height(12.dp))
    }
}
