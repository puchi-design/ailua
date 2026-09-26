package com.example.ui.call

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.CallStateEngine
import com.example.data.model.CallAction
import com.example.data.model.CallSession
import com.example.ui.components.AiluaAvatar
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    previewCallSession: CallSession? = null,
    onCallEnded: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val liveCall by CallStateEngine.currentCall.collectAsStateWithLifecycle()
    val isMuted by CallStateEngine.isMuted.collectAsStateWithLifecycle()
    val isSpeaker by CallStateEngine.isSpeaker.collectAsStateWithLifecycle()

    val currentCall = previewCallSession ?: liveCall

    // Timer effect for live call duration (disabled in Compose @Preview to protect state)
    if (!isPreview) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                CallStateEngine.incrementDuration(1)
            }
        }
    }

    val duration = currentCall?.durationSeconds ?: 0
    val minutes = duration / 60
    val seconds = duration % 60
    val durationText = "%02d:%02d".format(minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF14121F),
                        Color(0xFF1F1B30),
                        Color(0xFF100E19)
                    )
                )
            )
            .testTag("call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Companion Name and Duration
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentCall?.callerName ?: "小弥",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "通话中 $durationText",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        color = AiluaMoonGold,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "心契白噪音连通中 · 雨打青石板与室内私语",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }

            // Avatar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AiluaAvatar(
                    avatarId = currentCall?.characterId ?: "mira",
                    size = 130.dp,
                    showHalo = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Animated Sound Waveform (Simulated Companion Audio)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(36.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val barCount = 18
                    val barWidth = w / (barCount * 1.5f)

                    for (i in 0 until barCount) {
                        val x = i * (barWidth * 1.5f)
                        val factor = Math.sin(wavePhase.toDouble() + i * 0.45).toFloat()
                        val barHeight = ((Math.abs(factor) * 0.75f + 0.25f) * h).coerceAtLeast(6f)
                        val top = (h - barHeight) / 2

                        drawRoundRect(
                            color = AiluaMistBlue.copy(alpha = 0.5f + Math.abs(factor) * 0.45f),
                            topLeft = Offset(x, top),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                    }
                }
            }

            // Call In-progress Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallStateEngine.handleAction(CallAction.TOGGLE_MUTE) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f))
                                .testTag("call_mute_btn")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "静音",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMuted) "已静音" else "麦克风",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // End Call Button (Center Big Red)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                CallStateEngine.handleAction(CallAction.END)
                                onCallEnded()
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                                .testTag("call_end_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "挂断",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "挂断", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Speaker Toggle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallStateEngine.handleAction(CallAction.TOGGLE_SPEAKER) },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaker) AiluaMoonGold.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f))
                                .testTag("call_speaker_btn")
                        ) {
                            Icon(
                                imageVector = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "扬声器",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSpeaker) "扬声器" else "听筒",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
