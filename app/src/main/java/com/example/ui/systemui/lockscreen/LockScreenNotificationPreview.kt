package com.example.ui.systemui.lockscreen

import androidx.compose.foundation.clickable
import com.example.ui.designsystem.AiluaSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val theme = LocalAiluaTheme.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notifications.take(3).forEach { notification ->
            AiluaSurface(
                modifier = Modifier.fillMaxWidth().testTag("lock_notification_${notification.id}")
                    .clickable { onOpen(notification.id) },
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                    Text(notification.title, color = theme.palette.onSurface, style = theme.text.body, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(notification.body, style = LocalAiluaTheme.current.text.secondary, color = theme.palette.onSurfaceMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (notifications.size > 3) {
            AiluaSurface(Modifier.fillMaxWidth().clickable(onClick = onOpenAll)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                    Text("+ ${notifications.size - 3} 条通知", style = theme.text.secondary, color = theme.palette.onSurface)
                }
            }
        }
    }
}
