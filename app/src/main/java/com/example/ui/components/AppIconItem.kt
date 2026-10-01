package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMutedLavender
import com.example.ui.themeengine.EditMotion
import com.example.ui.themeengine.GlyphTintMode
import com.example.ui.themeengine.IconContainerStyle
import com.example.ui.themeengine.IconShapeSpec
import com.example.ui.themeengine.IdentityColorMode
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.TypographyFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

object AppIconDefaults {
    val ContainerSize = 56.dp
    const val GlyphScale = 0.46f
    val LabelSpacing = 5.dp
    val LabelSize = 11.sp
    val BadgeOffsetX = 3.dp
    val BadgeOffsetY = (-4).dp
    val BadgeTextSize = 9.sp
}

private const val SquircleExponent = 4f

val AppIconShape: Shape = GenericShape { size, _ ->
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val steps = 72
    val power = 2f / SquircleExponent
    for (i in 0 until steps) {
        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
        val cosAngle = cos(angle)
        val sinAngle = sin(angle)
        val x = halfWidth + halfWidth * sign(cosAngle) * abs(cosAngle).pow(power)
        val y = halfHeight + halfHeight * sign(sinAngle) * abs(sinAngle).pow(power)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun iconShapeFor(shape: IconShapeSpec): Shape = when (shape) {
    IconShapeSpec.SQUIRCLE -> AppIconShape
    IconShapeSpec.CIRCLE -> CircleShape
    IconShapeSpec.ROUNDED_RECT -> RoundedCornerShape(12.dp)
    IconShapeSpec.SOFT_SQUARE -> RoundedCornerShape(18.dp)
    IconShapeSpec.NONE -> RoundedCornerShape(0.dp)
}

data class AppVisualIdentity(val glyph: ImageVector, val identityColors: List<Color>)

fun getAppIdentity(iconKey: String): AppVisualIdentity {
    val (glyph, colors) = getAppVisuals(iconKey)
    return AppVisualIdentity(glyph, colors)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    name: String,
    iconKey: String,
    modifier: Modifier = Modifier,
    size: Dp = AppIconDefaults.ContainerSize,
    badge: String? = null,
    showLabel: Boolean = true,
    editMode: Boolean = false,
    isDragging: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val theme = LocalAiluaTheme.current
    val iconSpec = theme.icons
    val motion = theme.motion
    val identity = getAppIdentity(iconKey)
    val shape = iconShapeFor(iconSpec.shape)
    val containerSize = size * iconSpec.containerScale
    val glyphSize = size * iconSpec.glyphScale
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) motion.pressScale else 1f,
        animationSpec = spring(dampingRatio = motion.springDamping, stiffness = motion.springStiffness),
        label = "icon_press_scale"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) motion.dragScale else 1f,
        animationSpec = spring(dampingRatio = motion.springDamping, stiffness = motion.springStiffness),
        label = "icon_drag_scale"
    )
    val editFactor by animateFloatAsState(
        targetValue = if (editMode && motion.editMotion != EditMotion.NONE) 1f else 0f,
        animationSpec = spring(dampingRatio = motion.springDamping, stiffness = motion.springStiffness),
        label = "icon_edit_factor"
    )
    val phase = if (editMode && motion.editMotion != EditMotion.NONE) {
        val animated by rememberInfiniteTransition(label = "icon_edit")
            .animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
                label = "icon_edit_phase"
            )
        animated
    } else 0f

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    }

    Column(
        modifier = modifier.scale(pressScale * dragScale)
            .then(clickModifier).testTag("app_icon_$iconKey"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            val identityColor = identity.identityColors.first()
            val glyphTint = when (iconSpec.glyphTintMode) {
                GlyphTintMode.WHITE -> Color.White
                GlyphTintMode.IDENTITY -> if (iconSpec.identityColorMode == IdentityColorMode.NONE) theme.palette.onSurface else identityColor
                GlyphTintMode.ON_SURFACE -> theme.palette.onSurface
            }
            val editModifier = Modifier.graphicsLayer {
                val wave = sin(phase * PI / 180.0).toFloat() * editFactor
                when (motion.editMotion) {
                    EditMotion.WIGGLE -> rotationZ = wave * 2.4f
                    EditMotion.FLOAT -> translationY = -wave * this.size.height * 0.06f
                    EditMotion.SCALE -> {
                        scaleX = 1f + wave * 0.035f
                        scaleY = 1f + wave * 0.035f
                    }
                    EditMotion.NONE -> Unit
                }
            }
            if (iconSpec.containerStyle == IconContainerStyle.GLYPH_ONLY) {
                Icon(
                    imageVector = identity.glyph,
                    contentDescription = name,
                    modifier = Modifier.size(glyphSize).then(editModifier),
                    tint = glyphTint
                )
            } else {
                val fill = when (iconSpec.containerStyle) {
                    IconContainerStyle.GRADIENT -> Brush.linearGradient(identity.identityColors)
                    IconContainerStyle.SOLID -> Brush.linearGradient(listOf(theme.palette.surface, theme.palette.surface))
                    IconContainerStyle.OUTLINE -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    IconContainerStyle.GLASS -> Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), theme.palette.surface.copy(alpha = 0.14f))
                    )
                    IconContainerStyle.PAPER -> Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.24f), theme.palette.surface)
                    )
                    IconContainerStyle.GLYPH_ONLY -> Brush.linearGradient(identity.identityColors)
                }
                Box(
                    modifier = Modifier.size(containerSize).then(editModifier)
                        .shadow(
                            elevation = (iconSpec.shadow.elevationDp + if (isDragging) 6f else 0f).dp,
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = 0.16f),
                            spotColor = Color.Black.copy(alpha = 0.20f)
                        )
                        .clip(shape).background(fill)
                        .then(if (iconSpec.border.widthDp > 0f)
                            Modifier.border(iconSpec.border.widthDp.dp, iconSpec.border.color, shape)
                        else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = identity.glyph,
                        contentDescription = name,
                        modifier = Modifier.size(glyphSize),
                        tint = glyphTint
                    )
                    if (iconSpec.containerStyle == IconContainerStyle.PAPER) {
                        Box(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
                                .size(width = containerSize * 0.28f, height = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(identityColor.copy(alpha = 0.84f))
                        )
                    }
                }
            }

            if (!badge.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = AppIconDefaults.BadgeOffsetX, y = AppIconDefaults.BadgeOffsetY)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFE87A7A), Color(0xFFD45555))))
                        .border(1.5.dp, Color.White.copy(alpha = 0.95f), CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = AppIconDefaults.BadgeTextSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(AppIconDefaults.LabelSpacing))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = AppIconDefaults.LabelSize * theme.typography.labelSizeScale,
                    fontWeight = theme.typography.labelWeight,
                    fontFamily = when (theme.typography.family) {
                        TypographyFamily.SERIF -> FontFamily.Serif
                        TypographyFamily.MONO -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                    }
                ),
                color = iconSpec.labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
fun getAppVisuals(iconKey: String): Pair<ImageVector, List<Color>> {
    return when (iconKey) {
        "chat" -> Pair(
            Icons.Default.ChatBubbleOutline,
            listOf(Color(0xFF8CAAC6), Color(0xFF6B8CA8))
        )
        "moments" -> Pair(
            Icons.Default.LocalFlorist,
            listOf(Color(0xFFDCA8A8), Color(0xFFBA8383))
        )
        "living" -> Pair(
            Icons.Default.Spa,
            listOf(Color(0xFFAFA3C8), Color(0xFF8F80AD))
        )
        "memories" -> Pair(
            Icons.Default.Stars,
            listOf(Color(0xFFD8BD8C), Color(0xFFBA9C67))
        )
        "mailbox", "mail", "letter" -> Pair(
            Icons.Default.Drafts,
            listOf(Color(0xFFDCA8B5), Color(0xFFB88291))
        )
        "call", "phone" -> Pair(
            Icons.Default.Call,
            listOf(Color(0xFF86B9A5), Color(0xFF5E9A84))
        )
        "gallery" -> Pair(
            Icons.Default.PhotoAlbum,
            listOf(Color(0xFF86B9B0), Color(0xFF639E94))
        )
        "world" -> Pair(
            Icons.Default.Language,
            listOf(Color(0xFF8CA5C8), Color(0xFF6E8CB4))
        )
        "games" -> Pair(
            Icons.Default.Gamepad,
            listOf(Color(0xFFCCA8B9), Color(0xFFA78294))
        )
        "apps" -> Pair(
            Icons.Default.GridView,
            listOf(Color(0xFF9EA3AF), Color(0xFF7E8492))
        )
        "contacts" -> Pair(
            Icons.Default.Contacts,
            listOf(Color(0xFF8BA5BE), Color(0xFF6E8CA8))
        )
        "relations" -> Pair(
            Icons.Default.Share,
            listOf(Color(0xFF9E95B8), Color(0xFF7E749C))
        )
        "check_phone" -> Pair(
            Icons.Default.Visibility,
            listOf(Color(0xFFD6B57E), Color(0xFFBA975E))
        )
        "diary" -> Pair(
            Icons.Default.BookmarkBorder,
            listOf(Color(0xFFCCA2A2), Color(0xFFAA8080))
        )
        "bridge" -> Pair(
            Icons.Default.Nfc,
            listOf(Color(0xFF90B5C6), Color(0xFF7099AC))
        )
        "agent", "assistant" -> Pair(
            Icons.Default.SmartToy,
            listOf(Color(0xFFA2AEC6), Color(0xFF8190AC))
        )
        "dream" -> Pair(
            Icons.Default.Nightlight,
            listOf(Color(0xFFB1A2C8), Color(0xFF907EAC))
        )
        "creator" -> Pair(
            Icons.Default.Palette,
            listOf(Color(0xFFCE9EAC), Color(0xFFAF7B89))
        )
        "lore" -> Pair(
            Icons.Default.BookmarkBorder,
            listOf(Color(0xFFD1B78C), Color(0xFFB39665))
        )
        "shortcut" -> Pair(
            Icons.Default.Extension,
            listOf(Color(0xFF9DB5C8), Color(0xFF7897AE))
        )
        "im" -> Pair(
            Icons.Default.Send,
            listOf(Color(0xFF8EBDAA), Color(0xFF6DA28E))
        )
        "theater" -> Pair(
            Icons.Default.Movie,
            listOf(Color(0xFFC09EB8), Color(0xFFA27C99))
        )
        "market" -> Pair(
            Icons.Default.Storefront,
            listOf(Color(0xFF4C475A), Color(0xFF332F40))
        )
        "lock" -> Pair(
            Icons.Default.Lock,
            listOf(Color(0xFF5B3C46), Color(0xFF3D252D))
        )
        else -> Pair(
            Icons.Default.AutoAwesome,
            listOf(AiluaMistBlue, AiluaMutedLavender)
        )
    }
}
