package com.example.ui.systemui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.systemui.notification.VirtualNotification
import com.example.ui.components.AppIconItem
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.designsystem.publicNotificationTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCard(
    notification: VirtualNotification,
    onOpen: (VirtualNotification) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val theme = LocalAiluaTheme.current
    val displayTitle = publicNotificationTitle(notification)
    val timeLabel = remember(notification.timestampEpochMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestampEpochMs))
    }
    AiluaSurface(
        modifier = modifier.fillMaxWidth().testTag("notification_${notification.id}")
            .clickable { onOpen(notification) },
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 13.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AppIconItem(
                name = displayTitle,
                iconKey = notification.sourceAppId,
                size = 36.dp,
                showLabel = false,
                onClick = { onOpen(notification) },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        displayTitle,
                        modifier = Modifier.weight(1f),
                        style = LocalAiluaTheme.current.text.body,
                        color = theme.palette.onSurface,
                        fontWeight = if (notification.seen) FontWeight.Medium else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(timeLabel, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                }
                Text(
                    notification.body,
                    style = LocalAiluaTheme.current.text.secondary,
                    maxLines = if (compact) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                    color = theme.palette.onSurfaceMuted,
                )
            }
        }
    }
}
