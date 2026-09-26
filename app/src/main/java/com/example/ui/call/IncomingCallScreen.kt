package com.example.ui.call

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.CallStateEngine
import com.example.data.model.CallAction
import com.example.data.model.CallSession
import com.example.ui.components.AiluaAvatar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold

@Composable
fun IncomingCallScreen(
    previewSession: CallSession? = null,
    onAnswer: () -> Unit = {},
    onDecline: () -> Unit = {},
    onDismissLater: () -> Unit = {}
) {
    val callSession by CallStateEngine.currentCall.collectAsStateWithLifecycle()
    val session = previewSession ?: callSession

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF14121F),
                        Color(0xFF1F1A2F),
                        Color(0xFF0F0E17)
                    )
                )
            )
            .testTag("incoming_call_screen"),
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

            // Caller Identity Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AILUA 心契来电",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        color = AiluaMoonGold.copy(alpha = 0.85f),
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = session?.callerName ?: "小弥",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = session?.reason ?: "窗外雨下得很大，要不要陪我听一会儿？",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Pulsing Avatar with Halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AiluaMistBlue.copy(alpha = 0.15f))
                        .border(1.5.dp, AiluaMoonGold.copy(alpha = 0.35f), CircleShape)
                )

                AiluaAvatar(
                    avatarId = session?.characterId ?: "mira",
                    size = 120.dp,
                    showHalo = true
                )
            }

            // Action Buttons Row: Later, Decline, Answer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Later (Snooze)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            CallStateEngine.clearCurrentCall()
                            onDismissLater()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("call_later_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "稍后",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "稍后", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.7f))
                }

                // Decline (Red)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            CallStateEngine.handleAction(CallAction.DECLINE)
                            onDecline()
                        },
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .testTag("call_decline_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "拒绝",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "拒绝", fontSize = 12.sp, color = Color.White)
                }

                // Answer (Green)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            CallStateEngine.handleAction(CallAction.ANSWER)
                            onAnswer()
                        },
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF43A047))
                            .testTag("call_answer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "接听",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "接听", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
