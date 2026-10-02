package com.example.ui.systemui.live

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
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
        shape = RoundedCornerShape(spec.expandedCornerRadiusDp.dp),
        border = BorderStroke(spec.border.widthDp.dp, spec.border.color),
        shadowElevation = spec.shadow.elevationDp.dp,
        modifier = modifier.fillMaxWidth().testTag("live_activity_expanded"),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(activity.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    activity.subtitle?.let { Text(it, fontSize = 12.sp,
                        color = spec.foregroundColor.copy(alpha = 0.72f)) }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "收起心动胶囊", tint = spec.foregroundColor)
                }
            }
            Text(liveActivityDuration(activity), fontSize = 38.sp, fontWeight = FontWeight.Light,
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
