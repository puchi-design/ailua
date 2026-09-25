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
    avatarId: String = "mira",
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

    val (haloColor1, haloColor2) = when (avatarId) {
        "yuna" -> Pair(Color(0xFFE5A992), Color(0xFFF2D1C9))
        "noa" -> Pair(Color(0xFF8CA5BE), Color(0xFFC0D3E5))
        "group_tea" -> Pair(AiluaMoonGold, AiluaMutedLavender)
        else -> Pair(AiluaMutedLavender, AiluaMistBlue)
    }

    Box(
        modifier = modifier
            .size(if (showLivingStatus && statusText != null) size + 20.dp else size)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("ailua_avatar_box_$avatarId"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsating living halo
        if (showHalo) {
            Canvas(modifier = Modifier.size(size * 1.15f)) {
                val center = Offset(size.toPx() * 1.15f / 2, size.toPx() * 1.15f / 2)
                val radius = (size.toPx() / 2) * haloScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            haloColor1.copy(alpha = pulseAlpha * 0.4f),
                            haloColor2.copy(alpha = pulseAlpha * 0.15f),
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
                    color = haloColor1.copy(alpha = pulseAlpha * 0.6f),
                    center = center,
                    radius = radius * 1.05f,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // Inner Avatar Drawing
        val bgGradients = when (avatarId) {
            "yuna" -> listOf(Color(0xFFFBECE5), Color(0xFFF3D5C7), Color(0xFFE6BEAD))
            "noa" -> listOf(Color(0xFFE3E9F3), Color(0xFFCBD6E6), Color(0xFFB5C3D6))
            "group_tea" -> listOf(Color(0xFFFBF4E8), Color(0xFFEFE4D2), Color(0xFFDFD1BC))
            else -> listOf(Color(0xFFE9E4F0), Color(0xFFD5CEE5), Color(0xFFC7BEDB))
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.verticalGradient(bgGradients))
                .border(1.5.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height

                if (avatarId == "group_tea") {
                    // Group Chat Avatar: Tea cup with steam and 3 soft dots
                    drawCircle(
                        color = Color(0xFFFFFDF8),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.42f
                    )
                    // Tea cup body
                    drawOval(
                        color = Color(0xFFD6B57E),
                        topLeft = Offset(w * 0.30f, h * 0.42f),
                        size = Size(w * 0.40f, h * 0.32f)
                    )
                    // Steam paths
                    val steamPath = Path().apply {
                        moveTo(w * 0.42f, h * 0.38f)
                        quadraticTo(w * 0.38f, h * 0.30f, w * 0.42f, h * 0.22f)
                        moveTo(w * 0.58f, h * 0.38f)
                        quadraticTo(w * 0.62f, h * 0.30f, w * 0.58f, h * 0.22f)
                    }
                    drawPath(
                        path = steamPath,
                        color = Color(0xFFBA9C67),
                        style = Stroke(width = w * 0.035f)
                    )
                    // 3 participant dots (Mira purple, Yuna coral, Noa blue)
                    drawCircle(color = Color(0xFF9E95B8), center = Offset(w * 0.32f, h * 0.76f), radius = w * 0.05f)
                    drawCircle(color = Color(0xFFCCA2A2), center = Offset(w * 0.50f, h * 0.78f), radius = w * 0.05f)
                    drawCircle(color = Color(0xFF8BA5BE), center = Offset(w * 0.68f, h * 0.76f), radius = w * 0.05f)
                    return@Canvas
                }

                // Background soft twilight aura
                drawCircle(
                    color = Color(0xFFF2EDF8),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.42f
                )

                // Hair Color based on character
                val hairColor = when (avatarId) {
                    "yuna" -> Color(0xFF6B3A26) // Chestnut amber
                    "noa" -> Color(0xFF27324A)  // Slate indigo
                    else -> Color(0xFF2E2A3A)   // Charcoal violet (Mira)
                }

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

                // Sweater
                val sweaterColor = when (avatarId) {
                    "yuna" -> Color(0xFFFFF2E6) // Soft apricot
                    "noa" -> Color(0xFFEDF2F8)  // Pale frost blue
                    else -> Color(0xFFFAF7F2)   // Cream knit
                }
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
                val blushColor = when (avatarId) {
                    "yuna" -> Color(0xFFF99F92).copy(alpha = 0.75f)
                    "noa" -> Color(0xFFE5B5B5).copy(alpha = 0.55f)
                    else -> Color(0xFFF7BDBC).copy(alpha = 0.7f)
                }
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

                // Anime Eyes
                val eyeColor = Color(0xFF3E3952)
                val eyeHighlight = Color(0xFFFFFFFF)

                if (avatarId == "yuna") {
                    // Yuna: energetic rounder sparkling eyes
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(w * 0.34f, h * 0.40f),
                        size = Size(w * 0.11f, h * 0.12f)
                    )
                    drawCircle(color = eyeHighlight, center = Offset(w * 0.38f, h * 0.43f), radius = w * 0.024f)
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(w * 0.55f, h * 0.40f),
                        size = Size(w * 0.11f, h * 0.12f)
                    )
                    drawCircle(color = eyeHighlight, center = Offset(w * 0.59f, h * 0.43f), radius = w * 0.024f)
                } else if (avatarId == "noa") {
                    // Noa: calm thoughtful eyes + thin wire glasses bridge
                    drawArc(
                        color = eyeColor,
                        startAngle = 195f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(w * 0.34f, h * 0.42f),
                        size = Size(w * 0.11f, h * 0.07f),
                        style = Stroke(width = w * 0.035f)
                    )
                    drawArc(
                        color = eyeColor,
                        startAngle = 195f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(w * 0.55f, h * 0.42f),
                        size = Size(w * 0.11f, h * 0.07f),
                        style = Stroke(width = w * 0.035f)
                    )
                    // Glasses bridge
                    drawLine(
                        color = Color(0xFF8BA5BE),
                        start = Offset(w * 0.45f, h * 0.44f),
                        end = Offset(w * 0.55f, h * 0.44f),
                        strokeWidth = w * 0.02f
                    )
                } else {
                    // Mira: calm crescent eyes
                    drawArc(
                        color = eyeColor,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(w * 0.34f, h * 0.42f),
                        size = Size(w * 0.11f, h * 0.08f),
                        style = Stroke(width = w * 0.035f)
                    )
                    drawCircle(color = eyeHighlight, center = Offset(w * 0.38f, h * 0.44f), radius = w * 0.016f)
                    drawArc(
                        color = eyeColor,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(w * 0.55f, h * 0.42f),
                        size = Size(w * 0.11f, h * 0.08f),
                        style = Stroke(width = w * 0.035f)
                    )
                    drawCircle(color = eyeHighlight, center = Offset(w * 0.59f, h * 0.44f), radius = w * 0.016f)
                }

                // Gentle Smile
                val mouthPath = Path().apply {
                    moveTo(w * 0.46f, h * 0.60f)
                    quadraticTo(w * 0.50f, h * 0.63f, w * 0.54f, h * 0.60f)
                }
                drawPath(path = mouthPath, color = Color(0xFFD08A8A), style = Stroke(width = w * 0.025f))

                // Bangs & Hair strands
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

                // Hair side locks
                drawOval(color = hairColor, topLeft = Offset(w * 0.22f, h * 0.32f), size = Size(w * 0.12f, h * 0.32f))
                drawOval(color = hairColor, topLeft = Offset(w * 0.66f, h * 0.32f), size = Size(w * 0.12f, h * 0.32f))

                // Hair accessory
                when (avatarId) {
                    "yuna" -> {
                        // Coral flower pin
                        drawCircle(color = Color(0xFFF28482), center = Offset(w * 0.68f, h * 0.28f), radius = w * 0.045f)
                        drawCircle(color = Color(0xFFFFF2EB), center = Offset(w * 0.68f, h * 0.28f), radius = w * 0.015f)
                    }
                    "noa" -> {
                        // Tiny silver star pin
                        drawCircle(color = Color(0xFFC0D3E5), center = Offset(w * 0.32f, h * 0.26f), radius = w * 0.035f)
                    }
                    else -> {
                        // Mira: Moon Gold star hairpin
                        drawCircle(color = AiluaMoonGold, center = Offset(w * 0.68f, h * 0.28f), radius = w * 0.04f)
                    }
                }
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
