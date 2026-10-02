package com.example.ui.themecenter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.*

/** Mini desktop drawn from the resolved runtime, so palette overrides update its visuals live. */
@Composable
fun ThemePreview(theme: AiluaThemeRuntime, modifier: Modifier = Modifier) {
    val label = theme.icons.labelColor
    val wallpaper = theme.wallpaper.colors.ifEmpty {
        listOf(theme.palette.backgroundPrimary, theme.palette.backgroundSecondary)
    }
    Box(modifier.clip(RoundedCornerShape(theme.shapes.medium.dp)).background(Brush.verticalGradient(wallpaper))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("9:41", color = label, style = theme.text.caption, fontWeight = FontWeight.Bold)
                Text("●  ▰", color = label, style = theme.text.caption)
            }
            Spacer(Modifier.height(6.dp))
            Text("今天也和你一起", color = label, style = theme.text.caption, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PreviewApp(theme, "chat", "聊天")
                PreviewApp(theme, "gallery", "相册")
                PreviewApp(theme, "moments", "动态")
                PreviewApp(theme, "living", "生活")
            }
            Spacer(Modifier.weight(1f))
            ThemePreviewWidget(theme)
            Spacer(Modifier.height(5.dp))
            ThemePreviewDock(theme)
            Spacer(Modifier.height(2.dp))
            Box(Modifier.size(width = 35.dp, height = 2.dp).clip(RoundedCornerShape(2.dp))
                .background(label.copy(alpha = 0.65f)).align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun PreviewApp(theme: AiluaThemeRuntime, key: String, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ThemePreviewIcon(theme, key, 27.dp)
        Spacer(Modifier.height(2.dp))
        Text(name, color = theme.icons.labelColor, style = theme.text.caption, maxLines = 1)
    }
}

@Composable
fun ThemePreviewIcon(theme: AiluaThemeRuntime, key: String, size: Dp) {
    val identity = when (key) {
        "chat" -> Color(0xFF7D9FBD)
        "gallery" -> Color(0xFF74AAA0)
        "moments" -> Color(0xFFCB8F9E)
        else -> Color(0xFFA697BF)
    }
    val glyph: ImageVector = when (key) {
        "chat" -> Icons.Default.ChatBubbleOutline
        "gallery" -> Icons.Default.PhotoAlbum
        "moments" -> Icons.Default.LocalFlorist
        else -> Icons.Default.Spa
    }
    val spec = theme.icons
    val shape: Shape = when (spec.shape) {
        IconShapeSpec.CIRCLE -> CircleShape
        IconShapeSpec.ROUNDED_RECT -> RoundedCornerShape(5.dp)
        IconShapeSpec.SOFT_SQUARE -> RoundedCornerShape(8.dp)
        IconShapeSpec.NONE -> RoundedCornerShape(0.dp)
        IconShapeSpec.SQUIRCLE -> RoundedCornerShape(10.dp)
    }
    val tint = when (spec.glyphTintMode) {
        GlyphTintMode.WHITE -> Color.White
        GlyphTintMode.IDENTITY -> identity
        GlyphTintMode.ON_SURFACE -> theme.palette.onSurface
    }
    val container = spec.containerStyle != IconContainerStyle.GLYPH_ONLY
    val background = when (spec.containerStyle) {
        IconContainerStyle.GRADIENT -> Brush.linearGradient(listOf(identity.copy(alpha = 0.9f), identity))
        IconContainerStyle.SOLID -> Brush.linearGradient(listOf(identity, identity))
        IconContainerStyle.GLASS -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.23f), theme.palette.surface.copy(alpha = 0.25f)))
        IconContainerStyle.PAPER -> Brush.linearGradient(listOf(theme.palette.surface, theme.palette.surfaceVariant))
        IconContainerStyle.OUTLINE -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        IconContainerStyle.GLYPH_ONLY -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val containerSize = size * spec.containerScale
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.size(containerSize)
            .then(if (container && spec.shadow.elevationDp > 0f) Modifier.shadow(spec.shadow.elevationDp.dp, shape) else Modifier)
            .clip(shape)
            .then(if (container) Modifier.background(background) else Modifier)
            .then(if (container && spec.border.widthDp > 0f) Modifier.border(spec.border.widthDp.dp, spec.border.color, shape) else Modifier),
            contentAlignment = Alignment.Center) {
            Icon(glyph, key, tint = tint, modifier = Modifier.size(size * spec.glyphScale))
        }
    }
}

@Composable
private fun ThemePreviewWidget(theme: AiluaThemeRuntime) {
    val spec = theme.widgets
    val shape = RoundedCornerShape(spec.cornerRadiusDp.dp.coerceAtLeast(0.dp))
    Column(Modifier.fillMaxWidth().height(44.dp)
        .then(if (spec.shadow.elevationDp > 0f) Modifier.shadow(spec.shadow.elevationDp.dp, shape) else Modifier)
        .clip(shape).background(spec.backgroundColor.copy(alpha = spec.surfaceAlpha))
        .border(spec.border.widthDp.dp, spec.border.color, shape)
        .padding(horizontal = 9.dp, vertical = 4.dp)) {
        Text("10月 · AILUA", color = spec.foregroundColor, style = theme.text.caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text("把喜欢的日子收进这里 ✦", color = spec.foregroundColor, style = theme.text.caption, maxLines = 1)
    }
}

@Composable
private fun ThemePreviewDock(theme: AiluaThemeRuntime) {
    val spec = theme.dock
    val hasContainer = spec.containerMode != DockContainerMode.NONE
    val shape = RoundedCornerShape(spec.cornerRadiusDp.dp.coerceAtLeast(0.dp))
    Row(Modifier.fillMaxWidth().height(32.dp)
        .then(if (hasContainer && spec.shadow.elevationDp > 0f) Modifier.shadow(spec.shadow.elevationDp.dp, shape) else Modifier)
        .then(if (hasContainer) Modifier.clip(shape).background(spec.backgroundColor.copy(alpha = spec.surfaceAlpha)) else Modifier)
        .then(if (hasContainer) Modifier.border(spec.border.widthDp.dp, spec.border.color, shape) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly) {
        ThemePreviewIcon(theme, "chat", 24.dp)
        ThemePreviewIcon(theme, "gallery", 24.dp)
        ThemePreviewIcon(theme, "moments", 24.dp)
        ThemePreviewIcon(theme, "living", 24.dp)
    }
}
