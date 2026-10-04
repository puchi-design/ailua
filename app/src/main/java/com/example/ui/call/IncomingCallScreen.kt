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
import com.example.data.context.CharacterContext
import com.example.data.model.CallAction
import com.example.data.model.CallSession
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.delay

@Composable
fun IncomingCallScreen(
    previewSession: CallSession? = null,
    onAnswer: () -> Unit = {},
    onDecline: () -> Unit = {},
    onDismissLater: () -> Unit = {}
) {
    val theme = LocalAiluaTheme.current
    val callSession by CallStateEngine.currentCall.collectAsStateWithLifecycle()
    val session = previewSession ?: callSession
    Column(Modifier.fillMaxSize().background(theme.surfaces.screen).testTag("incoming_call_screen"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        VirtualPhoneStatusBar()
        Spacer(Modifier.weight(0.65f))
        CharacterPortrait(session?.characterId ?: CharacterContext.currentId(), PortraitVariant.HERO)
        Spacer(Modifier.height(theme.layout.sectionGap.dp))
        Text(publicCharacterName(session?.characterId ?: CharacterContext.currentId(), session?.callerName ?: CharacterContext.currentCharacter().name),
            style = theme.text.display, color = theme.palette.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("来电…", style = theme.text.body, color = theme.palette.onSurfaceMuted)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            CallControl("拒绝", Icons.Default.CallEnd, "call_decline_btn", destructive = true) {
                CallStateEngine.handleAction(CallAction.DECLINE)
                onDecline()
            }
            CallControl("接听", Icons.Default.Call, "call_answer_btn", active = true) {
                CallStateEngine.handleAction(CallAction.ANSWER)
                onAnswer()
            }
        }
        TextButton(onClick = { CallStateEngine.clearCurrentCall(); onDismissLater() },
            modifier = Modifier.padding(top = 20.dp).testTag("call_later_btn")) {
            Text("稍后", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
        }
        Spacer(Modifier.height(28.dp))
    }
}
