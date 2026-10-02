package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import com.example.ui.systemui.LocalVirtualSystemUiController
import com.example.ui.systemui.LocalUnseenNotificationCount
import com.example.ui.systemui.LocalStatusBarActivityContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


import com.example.ui.themeengine.LocalAiluaTheme

/** Virtual phone chrome follows the global theme and owns the two pull-down regions. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun VirtualPhoneStatusBar(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    state: OsChromeState = LocalOsChromeState.current
) {
    val runtime = LocalAiluaTheme.current
    val spec = runtime.statusBar
    val textColor = spec.foregroundColor
    val controller = LocalVirtualSystemUiController.current
    val unseenCount = LocalUnseenNotificationCount.current
    val activityContent = LocalStatusBarActivityContent.current
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 36.dp)
            .background(runtime.palette.surface.copy(alpha = spec.backgroundAlpha))
            .pointerInput(controller) {
                var startX = 0f
                var distance = 0f
                detectVerticalDragGestures(
                    onDragStart = { startX = it.x; distance = 0f },
                    onVerticalDrag = { change, delta -> distance += delta; change.consume() },
                    onDragCancel = { distance = 0f },
                    onDragEnd = {
                        if (distance > 24.dp.toPx()) {
                            if (startX < size.width * 0.6f) controller?.openNotifications()
                            else controller?.openControlCenter()
                        }
                    }
                )
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("打开通知中心") { controller?.openNotifications(); controller != null },
                    CustomAccessibilityAction("打开控制中心") { controller?.openControlCenter(); controller != null },
                )
            }
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(state.timeLabel, style = runtime.text.secondary, fontWeight = FontWeight.SemiBold,
            color = textColor, modifier = Modifier.testTag("virtual_status_time"))
        Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            when {
                activityContent != null -> activityContent()
                unseenCount > 0 -> Text("● $unseenCount", style = runtime.text.caption, color = textColor,
                    modifier = Modifier.testTag("notification_unseen_count"))

            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val networkIcon = when {
                state.networkLabel.contains("WiFi") -> Icons.Default.Wifi
                state.networkLabel.contains("5G") -> Icons.Default.SignalCellular4Bar
                else -> Icons.Default.WifiOff
            }
            Icon(networkIcon, state.networkLabel, Modifier.size(15.dp), tint = textColor)
            Text(state.batteryLabel, style = runtime.text.caption, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

@Composable
fun VirtualPhoneHomeBar(
    modifier: Modifier = Modifier,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = {}
) {
    val color = LocalAiluaTheme.current.statusBar.foregroundColor
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp, top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (canGoBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp).testTag("virtual_home_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回上一页",
                        modifier = Modifier.size(18.dp),
                        tint = color.copy(alpha = 0.7f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier.width(120.dp).height(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onGoHome
                    )
                    .semantics { contentDescription = "返回主屏" }
                    .testTag("virtual_phone_home_indicator")
                , contentAlignment = Alignment.Center
            ) {
                Box(Modifier.width(64.dp).height(4.dp).clip(CircleShape).background(color.copy(alpha = 0.24f)))
            }
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}
