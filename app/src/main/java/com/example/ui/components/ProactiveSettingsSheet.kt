package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AiluaLocalStore
import com.example.ui.designsystem.AiluaChip
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.themeengine.LocalAiluaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProactiveSettingsSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val settings by AiluaLocalStore.proactiveSettings.collectAsStateWithLifecycle()
    val state = AiluaLocalStore.getProactiveState()
    val theme = LocalAiluaTheme.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = theme.surfaces.raised) {
        HideDialogStatusBar()
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(theme.layout.sectionGap.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("主动消息", style = theme.text.title, color = theme.palette.onSurface)
                Text("角色会按设定的节奏主动联系你", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("启用主动消息", style = theme.text.body, color = theme.palette.onSurface)
                    Text("关闭时不发送", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { AiluaLocalStore.saveProactiveSettings(settings.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.palette.onSurface,
                        checkedTrackColor = theme.palette.accent.copy(alpha = 0.35f)),
                )
            }
            OptionRow("发送间隔", "两条主动消息之间的最少真实时间") {
                listOf(2, 6, 12).forEach { hours ->
                    AiluaChip("${hours}h", selected = settings.intervalHours == hours,
                        onClick = { AiluaLocalStore.saveProactiveSettings(settings.copy(intervalHours = hours)) })
                }
            }
            OptionRow("安静时段", "真实时间的静默窗口，不发送任何消息") {
                Text("23:00 - 08:00", style = theme.text.body, color = theme.palette.onSurface)
            }
            OptionRow("每日上限", "每个自然日最多发送条数") {
                listOf(1, 3, 5).forEach { limit ->
                    AiluaChip("$limit 条", selected = settings.dailyLimit == limit,
                        onClick = { AiluaLocalStore.saveProactiveSettings(settings.copy(dailyLimit = limit)) })
                }
            }
            Text(
                statusLine(settings.enabled, state.sentDate, state.sentCount, settings.dailyLimit, state.lastSuccessAtEpochMs),
                style = theme.text.caption, color = theme.palette.onSurfaceMuted,
            )
        }
    }
}

private fun statusLine(
    enabled: Boolean,
    sentDate: String,
    sentCount: Int,
    dailyLimit: Int,
    lastSuccessAt: Long,
): String {
    if (!enabled) return "当前状态：已关闭"
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val count = if (sentDate == today) sentCount else 0
    val last = if (lastSuccessAt > 0) {
        SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(lastSuccessAt))
    } else {
        "从未"
    }
    return "今日已发 $count/$dailyLimit · 上次成功：$last"
}


@Composable
private fun OptionRow(label: String, hint: String, content: @Composable RowScope.() -> Unit) {
    val theme = LocalAiluaTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiluaSectionHeader(label)
        Text(hint, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
