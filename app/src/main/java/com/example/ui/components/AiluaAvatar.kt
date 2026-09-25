package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender

@Composable
fun AiluaAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    showHalo: Boolean = true,
    showLivingStatus: Boolean = false,
    statusText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Box(
        modifier = modifier
            .size(if (showLivingStatus && statusText != null) size + 20.dp else size)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("ailua_avatar_box"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsating living halo
        if (showHalo) {
            Canvas(
                modifier = Modifier
                    .size(size * 1.15f)
            ) {
                val center = Offset(size.toPx() * 1.15f / 2, size.toPx() * 1.15f / 2)
                val radius = (size.toPx() / 2) * haloScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AiluaMutedLavender.copy(alpha = pulseAlpha * 0.4f),
                            AiluaMistBlue.copy(alpha = pulseAlpha * 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.2f
                    ),
                    center = center,
                    radius = radius * 1.15f
                )
                // Thin orbital ring
                drawCircle(
                    color = AiluaMutedLavender.copy(alpha = pulseAlpha * 0.6f),
                    center = center,
                    radius = radius * 1.05f,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // Inner Avatar Drawing (Anime Aesthetic Portrait)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFE9E4F0),
                            Color(0xFFD5CEE5),
                            Color(0xFFC7BEDB)
                        )
                    )
                )
                .border(1.5.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height

                // Background soft twilight aura
                drawCircle(
                    color = Color(0xFFF2EDF8),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.42f
                )

                // Hair - Back volume (Dark charcoal-violet)
                val hairColor = Color(0xFF2E2A3A)
                drawCircle(
                    color = hairColor,
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = w * 0.38f
                )

                // Neck & Soft Sweater Collar
                val neckColor = Color(0xFFFDEEE6)
                drawOval(
                    color = neckColor,
                    topLeft = Offset(w * 0.42f, h * 0.58f),
                    size = Size(w * 0.16f, h * 0.22f)
                )
                // Sweater (warm off-white knit)
                val sweaterColor = Color(0xFFFAF7F2)
                drawArc(
                    color = sweaterColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.15f, h * 0.68f),
                    size = Size(w * 0.7f, h * 0.6f)
                )

                // Face Shape
                val faceColor = Color(0xFFFFF2EB)
                drawOval(
                    color = faceColor,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.46f)
                )

                // Cheeks Blush
                val blushColor = Color(0xFFF7BDBC).copy(alpha = 0.7f)
                drawOval(
                    color = blushColor,
                    topLeft = Offset(w * 0.31f, h * 0.50f),
                    size = Size(w * 0.10f, h * 0.06f)
                )
                drawOval(
                    color = blushColor,
                    topLeft = Offset(w * 0.59f, h * 0.50f),
                    size = Size(w * 0.10f, h * 0.06f)
                )

                // Anime Eyes (Gentle, crescent calm look)
                val eyeColor = Color(0xFF3E3952)
                val eyeHighlight = Color(0xFFFFFFFF)

                // Left Eye
                drawArc(
                    color = eyeColor,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.42f),
                    size = Size(w * 0.11f, h * 0.08f),
                    style = Stroke(width = w * 0.035f)
                )
                drawCircle(
                    color = eyeHighlight,
                    center = Offset(w * 0.38f, h * 0.44f),
                    radius = w * 0.016f
                )

                // Right Eye
                drawArc(
                    color = eyeColor,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(w * 0.55f, h * 0.42f),
                    size = Size(w * 0.11f, h * 0.08f),
                    style = Stroke(width = w * 0.035f)
                )
                drawCircle(
                    color = eyeHighlight,
                    center = Offset(w * 0.59f, h * 0.44f),
                    radius = w * 0.016f
                )

                // Gentle Smile
                val mouthPath = Path().apply {
                    moveTo(w * 0.46f, h * 0.60f)
                    quadraticTo(w * 0.50f, h * 0.63f, w * 0.54f, h * 0.60f)
                }
                drawPath(
                    path = mouthPath,
                    color = Color(0xFFD08A8A),
                    style = Stroke(width = w * 0.025f)
                )

                // Bangs & Hair strands framing face
                val bangsPath = Path().apply {
                    moveTo(w * 0.26f, h * 0.36f)
                    quadraticTo(w * 0.36f, h * 0.42f, w * 0.42f, h * 0.36f)
                    quadraticTo(w * 0.48f, h * 0.44f, w * 0.56f, h * 0.35f)
                    quadraticTo(w * 0.64f, h * 0.42f, w * 0.74f, h * 0.36f)
                    lineTo(w * 0.74f, h * 0.24f)
                    quadraticTo(w * 0.50f, h * 0.18f, w * 0.26f, h * 0.24f)
                    close()
                }
                drawPath(path = bangsPath, color = hairColor)

                // Left & Right hair side locks
                drawOval(
                    color = hairColor,
                    topLeft = Offset(w * 0.22f, h * 0.32f),
                    size = Size(w * 0.12f, h * 0.32f)
                )
                drawOval(
                    color = hairColor,
                    topLeft = Offset(w * 0.66f, h * 0.32f),
                    size = Size(w * 0.12f, h * 0.32f)
                )

                // Subtle Star Hairpin (Moon Gold)
                drawCircle(
                    color = AiluaMoonGold,
                    center = Offset(w * 0.68f, h * 0.28f),
                    radius = w * 0.04f
                )
            }
        }

        // Optional tiny living indicator badge
        if (showLivingStatus) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6EC6A1))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .size(size * 0.26f)
            )
        }

        // Subtitle badge pill underneath if provided
        if (statusText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
