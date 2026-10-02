package com.example.ui.systemui.lockscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.themeengine.LocalAiluaTheme

/** A display projection only: notification ownership and read state stay in the bus. */
data class LockScreenNotificationItem(val id: String, val title: String, val body: String)

@Composable
fun LockScreenNotificationPreview(
    notifications: List<LockScreenNotificationItem>,
    onOpen: (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = LocalAiluaTheme.current.lockscreen.notificationStyle
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notifications.take(3).forEach { notification ->
            Surface(
                onClick = { onOpen(notification.id) },
                modifier = Modifier.fillMaxWidth().testTag("lock_notification_${notification.id}"),
                shape = RoundedCornerShape(style.cornerRadiusDp.dp),
                color = style.backgroundColor.copy(alpha = style.surfaceAlpha),
                contentColor = style.foregroundColor,
                border = BorderStroke(style.border.widthDp.dp, style.border.color),
                shadowElevation = style.shadow.elevationDp.dp
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                    Text(notification.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(notification.body, fontSize = 12.sp, color = style.foregroundColor.copy(alpha = 0.78f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (notifications.size > 3) {
            Surface(onClick = onOpenAll, color = style.backgroundColor.copy(alpha = style.surfaceAlpha),
                contentColor = style.foregroundColor, shape = RoundedCornerShape(style.cornerRadiusDp.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                    Text("+ ${notifications.size - 3} 条通知", fontSize = 12.sp)
                }
            }
        }
    }
}
