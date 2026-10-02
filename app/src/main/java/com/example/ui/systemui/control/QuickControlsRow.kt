package com.example.ui.systemui.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.systemui.control.ControlCenterState
import com.example.ui.components.LocalOsChromeState
import com.example.ui.themeengine.LocalAiluaTheme

/** A compact view of the same tile state used by the full control surface. */
@Composable
fun QuickControlsRow(
    state: ControlCenterState,
    isDarkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    onFocusModeChange: (Boolean) -> Unit,
    onLock: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val network = LocalOsChromeState.current.networkLabel
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlTile("网络", network, Icons.Default.Wifi,
                active = !network.contains("离线"), modifier = Modifier.weight(1f),
                compact = true, tag = "quick_network")
            ControlTile("深色", if (isDarkTheme) "已开启" else "已关闭", Icons.Default.DarkMode,
                active = isDarkTheme, onClick = onToggleDarkMode, modifier = Modifier.weight(1f),
                compact = true, tag = "quick_dark")
            ControlTile("专注", if (state.focusMode) "已开启" else "已关闭", Icons.Default.DoNotDisturbOn,
                active = state.focusMode, onClick = { onFocusModeChange(!state.focusMode) },
                modifier = Modifier.weight(1f), compact = true, tag = "quick_focus")
            ControlTile("锁屏", "锁定虚拟手机", Icons.Default.Lock, onClick = onLock,
                modifier = Modifier.weight(1f), compact = true, tag = "quick_lock")
        }
        TextButton(onClick = onExpand,
            modifier = Modifier.align(Alignment.End).testTag("quick_expand")) {
            Text("展开控制中心", color = LocalAiluaTheme.current.palette.onSurface)
        }
    }
}
