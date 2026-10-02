package com.example.ui.systemui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.systemui.notification.VirtualNotification
import com.example.ui.themeengine.LocalAiluaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotificationStack(
    notifications: List<VirtualNotification>,
    onOpen: (VirtualNotification) -> Unit,
    onDismiss: (VirtualNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAiluaTheme.current
    val sections = remember(notifications) {
        notifications.groupBy { notificationDateLabel(it.timestampEpochMs) }
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(theme.shade.spacingDp.dp)) {
        sections.forEach { (date, entries) ->
            item(key = "date_$date") {
                Text(date, Modifier.padding(horizontal = 4.dp, vertical = 4.dp), color = theme.palette.onSurfaceMuted, fontSize = 12.sp)
            }
            items(entries, key = { it.id }) { notification ->
                DismissibleNotification(notification, onOpen, onDismiss)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleNotification(
    notification: VirtualNotification,
    onOpen: (VirtualNotification) -> Unit,
    onDismiss: (VirtualNotification) -> Unit,
) {
    val theme = LocalAiluaTheme.current
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) onDismiss(notification)
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(
                    theme.palette.accent.copy(alpha = 0.18f),
                    RoundedCornerShape(theme.shade.cardCornerRadiusDp.dp),
                ).padding(horizontal = 24.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "移除通知", tint = theme.palette.onSurface)
            }
        },
    ) { NotificationCard(notification, onOpen) }
}

private fun notificationDateLabel(epochMs: Long): String {
    val event = Calendar.getInstance().apply { timeInMillis = epochMs }
    val today = Calendar.getInstance()
    if (event.get(Calendar.YEAR) == today.get(Calendar.YEAR) && event.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) return "今天"
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (event.get(Calendar.YEAR) == today.get(Calendar.YEAR) && event.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) return "昨天"
    return SimpleDateFormat("M月d日", Locale.CHINA).format(Date(epochMs))
}
