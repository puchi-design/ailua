package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AiluaLocalStore
import com.example.data.model.ProactiveSettings
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ProactiveSettingsSheet — P3D-2 proactive message settings, opened from the
 * chat overflow menu. Master toggle defaults OFF (spec: proactive only after
 * the user enables it); interval / daily limit are quick pills, quiet hours
 * are shown as the fixed 23:00-08:00 window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProactiveSettingsSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val settings by AiluaLocalStore.proactiveSettings.collectAsStateWithLifecycle()
    val state = AiluaLocalStore.getProactiveState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AiluaMoonGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = AiluaMoonGold,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "主动消息",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "她会按节奏主动找你 · 仅在本机规则允许时发送",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "启用主动消息",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "关闭时引擎完全不发送（默认）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = {
                        AiluaLocalStore.saveProactiveSettings(settings.copy(enabled = it))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AiluaMistBlue),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OptionRow(
                label = "发送间隔",
                hint = "两条主动消息之间的最少真实时间",
            ) {
                listOf(2, 6, 12).forEach { hours ->
                    Pill(
                        text = "${hours}h",
                        selected = settings.intervalHours == hours,
                        onClick = {
                            AiluaLocalStore.saveProactiveSettings(settings.copy(intervalHours = hours))
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OptionRow(
                label = "安静时段",
                hint = "真实时间的静默窗口，不发送任何消息",
            ) {
                Text(
                    text = "23:00 - 08:00",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OptionRow(
                label = "每日上限",
                hint = "每个自然日最多发送条数",
            ) {
                listOf(1, 3, 5).forEach { limit ->
                    Pill(
                        text = "$limit 条",
                        selected = settings.dailyLimit == limit,
                        onClick = {
                            AiluaLocalStore.saveProactiveSettings(settings.copy(dailyLimit = limit))
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = statusLine(settings.enabled, state.sentDate, state.sentCount, settings.dailyLimit, state.lastSuccessAtEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = AiluaMistBlue,
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
private fun OptionRow(
    label: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) AiluaMistBlue.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) AiluaMistBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
