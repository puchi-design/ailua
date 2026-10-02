package com.example.ui.systemui.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.systemui.live.LiveActivityProjection
import com.example.data.systemui.live.VirtualLiveActivity
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.delay

@Composable
internal fun liveActivityDuration(activity: VirtualLiveActivity): String {
    val now by produceState(System.currentTimeMillis(), activity.id, activity.startedAtEpochMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    return LiveActivityProjection.durationLabel(activity, now)
}

/** Only the highest priority ongoing activity is shown; the remainder use a count. */
@Composable
fun LiveActivityChip(
    activities: List<VirtualLiveActivity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = LiveActivityProjection.ordered(activities)
    val primary = ordered.firstOrNull() ?: return
    val spec = LocalAiluaTheme.current.liveActivity
    val duration = liveActivityDuration(primary)
    Surface(
        color = spec.backgroundColor, contentColor = spec.foregroundColor,
        shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.pill.dp),
        shadowElevation = 0.dp,
        modifier = modifier.widthIn(max = 185.dp).testTag("live_activity_chip")
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Call, null, modifier = Modifier.size(12.dp))
            Text(primary.title, style = LocalAiluaTheme.current.text.caption, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(duration, style = LocalAiluaTheme.current.text.caption, maxLines = 1)
            if (ordered.size > 1) Text("+${ordered.size - 1}", style = LocalAiluaTheme.current.text.caption)
        }
    }
}

/** The same ongoing call projected into the lock screen and notification shade. */
@Composable
fun LiveActivitySummary(
    activity: VirtualLiveActivity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = LocalAiluaTheme.current.liveActivity
    Surface(
        color = spec.backgroundColor, contentColor = spec.foregroundColor,
        shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.pill.dp),
        shadowElevation = 0.dp,
        modifier = modifier.testTag("live_activity_summary").clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
            Text("与${activity.title}通话中", style = LocalAiluaTheme.current.text.secondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(liveActivityDuration(activity), style = LocalAiluaTheme.current.text.secondary, fontWeight = FontWeight.Medium)
        }
    }
}
