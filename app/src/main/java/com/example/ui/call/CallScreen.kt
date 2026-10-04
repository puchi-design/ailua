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
import com.example.data.registry.CharacterRegistry
import com.example.data.model.CallAction
import com.example.data.model.CallSession
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.designsystem.publicCharacterName
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.delay

@Composable
@Suppress("UNUSED_PARAMETER")
fun CallScreen(
    previewCallSession: CallSession? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onCallEnded: () -> Unit = {},
    characterId: String = CharacterContext.currentId(),
) {
    val theme = LocalAiluaTheme.current
    val isPreview = LocalInspectionMode.current
    val liveCall by CallStateEngine.currentCall.collectAsStateWithLifecycle()
    val isMuted by CallStateEngine.isMuted.collectAsStateWithLifecycle()
    val isSpeaker by CallStateEngine.isSpeaker.collectAsStateWithLifecycle()
    val currentCall = previewCallSession ?: liveCall
    // Preserve the existing engine-owned timer and preview guard.
    if (!isPreview) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                CallStateEngine.incrementDuration(1)
            }
        }
    }
    val duration = currentCall?.durationSeconds ?: 0
    val durationText = "%02d:%02d".format(duration / 60, duration % 60)
    Column(Modifier.fillMaxSize().background(theme.surfaces.screen).testTag("call_screen"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        VirtualPhoneStatusBar(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        Spacer(Modifier.weight(0.65f))
        CharacterPortrait(currentCall?.characterId ?: characterId, PortraitVariant.HERO)
        Spacer(Modifier.height(theme.layout.sectionGap.dp))
        Text(publicCharacterName(currentCall?.characterId ?: characterId, currentCall?.callerName ?: CharacterRegistry.getCharacter(characterId).name),
            style = theme.text.display, color = theme.palette.onSurface)
        Spacer(Modifier.height(12.dp))
        Text(durationText, style = theme.text.title, color = theme.palette.onSurface)
        Text("正在通话", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            CallControl(if (isMuted) "已静音" else "静音", if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                "call_mute_btn", active = isMuted) { CallStateEngine.handleAction(CallAction.TOGGLE_MUTE) }
            CallControl("挂断", Icons.Default.CallEnd, "call_end_btn", destructive = true) {
                CallStateEngine.handleAction(CallAction.END)
                onCallEnded()
            }
            CallControl("扬声器", if (isSpeaker) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                "call_speaker_btn", active = isSpeaker) { CallStateEngine.handleAction(CallAction.TOGGLE_SPEAKER) }
        }
        Spacer(Modifier.height(56.dp))
    }
}

/** Shared call controls: presentation only; all actions remain engine callbacks. */
@Composable
internal fun CallControl(
    label: String,
    icon: ImageVector,
    tag: String,
    active: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalAiluaTheme.current
    val background = when {
        destructive -> MaterialTheme.colorScheme.errorContainer
        active -> theme.palette.accent.copy(alpha = 0.24f)
        else -> theme.surfaces.inset
    }
    val foreground = if (destructive) MaterialTheme.colorScheme.onErrorContainer else theme.palette.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick, Modifier.size(64.dp).clip(CircleShape).background(background).testTag(tag)) {
            Icon(icon, label, Modifier.size(26.dp), tint = foreground)
        }
        Text(label, style = theme.text.secondary, color = theme.palette.onSurface)
    }
}
