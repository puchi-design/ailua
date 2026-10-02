package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.ProactiveGraph
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldPlanRuntime
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.AiluaSurface
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldTimeDevSheet(onDismiss: () -> Unit, sheetState: SheetState = rememberModalBottomSheetState()) {
    val clock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val scheduledActions by WorldHeartbeatEngine.scheduledActions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val nextAction = scheduledActions.filter { !it.fired }
        .mapNotNull { action ->
            val offset = if (action.worldDate.isBlank()) 0 else com.example.data.engine.WorldPlanValidator.dayOffset(clock.dateLabel, action.worldDate)
                ?: return@mapNotNull null
            val distance = offset * 1440 + action.triggerTimeMinutes - clock.minutesOfDay
            if (distance > 0) action to distance else null
        }.minByOrNull { it.second }?.first

    val theme = LocalAiluaTheme.current
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, containerColor = theme.surfaces.raised,
        modifier = Modifier.testTag("world_time_dev_sheet"),
    ) {
        HideDialogStatusBar()
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("虚拟时间", style = theme.text.title, color = theme.palette.onSurface)
            Text("开发者控制", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            AiluaSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(clock.dateLabel, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                    Text(clock.timeFormatted, style = theme.text.display, color = theme.palette.onSurface)
                    Text("${clock.dayPhase.icon} ${clock.dayPhase.label} · ${clock.weather.icon} ${clock.weather.label}",
                        style = theme.text.secondary, color = theme.palette.onSurface)
                    Text(clock.weather.atmosphere, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiluaSectionHeader("下一个事件")
                if (nextAction != null) {
                    Text("${nextAction.triggerTimeString} · ${nextAction.title}",
                        style = theme.text.body, color = theme.palette.onSurface)
                    Text(nextAction.description, style = theme.text.secondary, color = theme.palette.onSurfaceMuted, maxLines = 2)
                } else {
                    Text("今天的预定事件已全部完成", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
                }
            }
            OutlinedButton(
                onClick = { scope.launch { WorldPlanRuntime.maybePlan(force = true) } },
                modifier = Modifier.fillMaxWidth().testTag("generate_world_plan"),
                shape = RoundedCornerShape(theme.shapes.medium.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.palette.onSurface),
            ) { Text("生成下一段世界计划", style = theme.text.secondary) }
            AiluaSectionHeader("推进时间")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { WorldHeartbeatEngine.advanceTime(10) },
                    modifier = Modifier.weight(1f).testTag("time_plus_10"),
                    shape = RoundedCornerShape(theme.shapes.medium.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.palette.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text("+10 分钟", style = theme.text.caption) }
                OutlinedButton(
                    onClick = { WorldHeartbeatEngine.advanceTime(30) },
                    modifier = Modifier.weight(1f).testTag("time_plus_30"),
                    shape = RoundedCornerShape(theme.shapes.medium.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.palette.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text("+30 分钟", style = theme.text.caption) }
                OutlinedButton(
                    onClick = { WorldHeartbeatEngine.advanceTime(60) },
                    modifier = Modifier.weight(1f).testTag("time_plus_60"),
                    shape = RoundedCornerShape(theme.shapes.medium.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.palette.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text("+1 小时", style = theme.text.caption) }
            }
            Button(
                onClick = { WorldHeartbeatEngine.jumpToNextScheduledEvent() },
                modifier = Modifier.fillMaxWidth().testTag("time_jump_next"),
                shape = RoundedCornerShape(theme.shapes.medium.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.palette.accent.copy(alpha = 0.18f),
                    contentColor = theme.palette.onSurface),
            ) { Text("跳到下一个事件", style = theme.text.body) }
            OutlinedButton(
                onClick = { ProactiveGraph.forceFire() },
                modifier = Modifier.fillMaxWidth().testTag("proactive_force_fire"),
                shape = RoundedCornerShape(theme.shapes.medium.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.palette.onSurface),
            ) { Text("立即触发主动消息", style = theme.text.secondary) }
        }
    }
}
