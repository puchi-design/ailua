package com.example.ui.systemui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.systemui.live.LiveActivityType
import com.example.data.systemui.live.VirtualLiveActivity
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun LiveActivityExpanded(
    activity: VirtualLiveActivity,
    onReturnToCall: () -> Unit,
    onEnd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = LocalAiluaTheme.current.liveActivity
    Surface(
        color = spec.backgroundColor, contentColor = spec.foregroundColor,
        shape = RoundedCornerShape(LocalAiluaTheme.current.shapes.large.dp),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth().testTag("live_activity_expanded"),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(activity.title, style = LocalAiluaTheme.current.text.title, fontWeight = FontWeight.SemiBold)
                    activity.subtitle?.let { Text(it, style = LocalAiluaTheme.current.text.secondary,
                        color = spec.foregroundColor.copy(alpha = 0.72f)) }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "收起通话状态", tint = spec.foregroundColor)
                }
            }
            Text(liveActivityDuration(activity), style = LocalAiluaTheme.current.text.display, fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onReturnToCall, enabled = activity.route != null,
                    modifier = Modifier.testTag("live_return_to_call")) {
                    Text("返回通话", color = spec.foregroundColor)
                }
                if (activity.type == LiveActivityType.CALL) {
                    TextButton(onClick = onEnd, modifier = Modifier.testTag("live_end_call")) {
                        Text("结束通话", color = spec.foregroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
