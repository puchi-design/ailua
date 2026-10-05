package com.example.ui.systemui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.systemui.notification.VirtualNotification
import com.example.ui.themeengine.LocalAiluaTheme

/** A system overlay. Navigation and notification mutations remain Host callbacks. */
@Composable
fun NotificationShade(
    notifications: List<VirtualNotification>,
    onOpen: (VirtualNotification) -> Unit,
    onDismiss: (VirtualNotification) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    quickControls: @Composable () -> Unit = {},
    ongoingContent: @Composable () -> Unit = {},
) {
    val theme = LocalAiluaTheme.current
    Column(
        modifier.fillMaxSize().testTag("notification_shade")
            .background(theme.palette.backgroundPrimary.copy(alpha = theme.shade.backgroundAlpha))
            .safeDrawingPadding()
            .padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NotificationShadeHeader(onClose)
        quickControls()
        ongoingContent()
        if (notifications.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("没有新通知", color = theme.palette.onSurfaceMuted, style = LocalAiluaTheme.current.text.body)
            }
        } else {
            NotificationStack(notifications, onOpen, onDismiss, Modifier.weight(1f).fillMaxWidth())
            TextButton(onClick = onClearAll, modifier = Modifier.align(Alignment.End).testTag("clear_all_notifications")) {
                Text("清除全部", color = theme.palette.onSurface)
            }
        }
    }
}
