package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender
import com.example.ui.themeengine.LocalAiluaTheme

/** Virtual phone chrome follows the global theme and owns the two pull-down regions. */
@Composable
fun VirtualPhoneStatusBar(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    state: OsChromeState = LocalOsChromeState.current
) {
    val runtime = LocalAiluaTheme.current
    val spec = runtime.statusBar
    val textColor = spec.foregroundColor
    val subtleColor = textColor.copy(alpha = 0.72f)
    val controller = LocalVirtualSystemUiController.current
    val unseenCount = LocalUnseenNotificationCount.current
    val activityContent = LocalStatusBarActivityContent.current
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp)
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
        Text(state.timeLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = textColor, modifier = Modifier.testTag("virtual_status_time"))
        Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            when {
                activityContent != null -> activityContent()
                unseenCount > 0 -> Text("● $unseenCount", fontSize = 11.sp, color = textColor,
                    modifier = Modifier.testTag("notification_unseen_count"))
                !spec.minimal -> Text("AILUA OS", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = subtleColor)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Wifi, state.networkLabel, Modifier.size(13.dp), tint = textColor)
            if (activityContent == null && !spec.minimal) {
                Text(state.networkLabel, fontSize = 10.sp, color = subtleColor)
            }
            Text(state.batteryLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor)
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggleTheme
                ), contentAlignment = Alignment.Center
            ) {
                Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    "切换虚拟世界心境主题", Modifier.size(14.dp), tint = textColor)
            }
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
                modifier = Modifier.width(72.dp).height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoHome
                    )
                    .testTag("virtual_phone_home_indicator")
            )
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}