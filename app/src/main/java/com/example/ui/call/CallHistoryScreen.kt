package com.example.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.CallStateEngine
import com.example.data.model.CallAction
import com.example.data.model.CallSession
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.CallState
import com.example.ui.designsystem.AiluaScreenScaffold
import kotlinx.coroutines.delay

@Composable
@Suppress("UNUSED_PARAMETER")
fun CallHistoryScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onStartCall: (String) -> Unit = {},
    onGoHome: () -> Unit = onBack,
) {
    val theme = LocalAiluaTheme.current
    val history by CallStateEngine.callHistory.collectAsStateWithLifecycle()
    AiluaScreenScaffold(title = "通话", onBack = onBack, onGoHome = onGoHome,
        backTestTag = "call_history_back_btn", modifier = Modifier.testTag("call_history_screen"),
        trailing = {
            TextButton(onClick = {
                val session = CallStateEngine.triggerIncomingCall()
                onStartCall(session.characterId)
            }, modifier = Modifier.testTag("call_simulate_incoming_btn")) {
                Text("模拟来电", style = theme.text.secondary, color = theme.palette.onSurface)
            }
        }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = theme.layout.screenHorizontalPadding.dp)) {
            if (history.isEmpty()) item {
                Text("暂无通话", style = theme.text.body, color = theme.palette.onSurfaceMuted,
                    modifier = Modifier.padding(vertical = theme.layout.sectionGap.dp))
            }
            items(history, key = { it.id }) { session ->
                CallHistoryItem(session)
                HorizontalDivider(color = theme.surfaces.divider)
            }
        }
    }
}

@Composable
private fun CallHistoryItem(session: CallSession) {
    val theme = LocalAiluaTheme.current
    val status = when {
        session.durationSeconds > 0 -> "呼入 · %02d:%02d".format(session.durationSeconds / 60, session.durationSeconds % 60)
        session.state == CallState.DECLINED -> "已拒接"
        else -> "未接通"
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 78.dp).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp), verticalAlignment = Alignment.CenterVertically) {
        CharacterPortrait(session.characterId, PortraitVariant.AVATAR)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(publicCharacterName(session.characterId, session.callerName), style = theme.text.body, color = theme.palette.onSurface,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(session.scheduledAtTime, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            }
            Text(status, style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            Text(session.reason, style = theme.text.caption, color = theme.palette.onSurfaceMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
