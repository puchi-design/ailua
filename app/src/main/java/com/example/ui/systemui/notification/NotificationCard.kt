package com.example.ui.systemui.notification

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.systemui.notification.VirtualNotification
import com.example.ui.components.AppIconItem
import com.example.ui.themeengine.LocalAiluaTheme
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
    val style = theme.shade.cardStyle
    val timeLabel = remember(notification.timestampEpochMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestampEpochMs))
    }
    Surface(
        onClick = { onOpen(notification) },
        modifier = modifier.fillMaxWidth().testTag("notification_${notification.id}"),
        shape = RoundedCornerShape(theme.shade.cardCornerRadiusDp.dp),
        color = style.backgroundColor.copy(alpha = theme.shade.cardAlpha),
        contentColor = style.foregroundColor,
        shadowElevation = style.shadow.elevationDp.dp,
        border = BorderStroke(style.border.widthDp.dp, style.border.color),
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 13.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AppIconItem(
                name = notification.title,
                iconKey = notification.sourceAppId,
                size = 36.dp,
                showLabel = false,
                onClick = { onOpen(notification) },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        notification.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = if (notification.seen) FontWeight.Medium else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(timeLabel, fontSize = 10.sp, color = style.foregroundColor.copy(alpha = 0.65f))
                }
                Text(
                    notification.body,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = if (compact) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                    color = style.foregroundColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}
