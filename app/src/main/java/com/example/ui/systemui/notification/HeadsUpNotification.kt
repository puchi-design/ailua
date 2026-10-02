package com.example.ui.systemui.notification

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.systemui.notification.VirtualNotification
import kotlinx.coroutines.delay

/** The Host decides eligibility from live events; this view never reads persisted history. */
@Composable
fun HeadsUpNotification(
    notification: VirtualNotification,
    onOpen: (VirtualNotification) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismiss by rememberUpdatedState(onDismiss)
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(notification.id) {
        delay(4_000)
        dismiss()
    }
    Box(
        modifier.fillMaxWidth().testTag("heads_up_notification")
            .pointerInput(notification.id, threshold) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -threshold) dismiss() },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        dragged += delta
                    },
                )
            },
    ) {
        NotificationCard(notification, onOpen, compact = true)
    }
}
