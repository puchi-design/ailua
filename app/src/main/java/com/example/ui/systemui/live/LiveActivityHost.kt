package com.example.ui.systemui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.systemui.live.LiveActivityProjection
import com.example.data.systemui.live.VirtualLiveActivity

/** Expanded overlay only. Surface ordering and lock rules remain with VirtualSystemUiHost. */
@Composable
fun LiveActivityHost(
    activities: List<VirtualLiveActivity>,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onLaunchRoute: (String) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LiveActivityProjection.ordered(activities).firstOrNull() ?: return
    if (!expanded) return
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss,
        ))
        LiveActivityExpanded(
            activity = activity,
            onReturnToCall = { activity.route?.let(onLaunchRoute) },
            onEnd = onEnd,
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 20.dp, vertical = 58.dp),
        )
    }
}
