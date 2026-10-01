package com.example.ui.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.WidgetBackgroundStyle

/** The frame owns only appearance; the workspace continues to own selection and resizing. */
@Composable
fun ThemeWidgetFrame(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val runtime = LocalAiluaTheme.current
    val spec = runtime.widgets
    val shape = RoundedCornerShape(spec.cornerRadiusDp.dp)
    val outline = if (selected) runtime.palette.accent else spec.border.color
    val outlineWidth = if (selected) 2.dp else spec.border.widthDp.dp
    val background = when (spec.backgroundStyle) {
        WidgetBackgroundStyle.CARD, WidgetBackgroundStyle.PAPER ->
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), spec.backgroundColor.copy(alpha = spec.surfaceAlpha)))
        WidgetBackgroundStyle.GLASS ->
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), spec.backgroundColor.copy(alpha = spec.surfaceAlpha)))
        WidgetBackgroundStyle.FLAT ->
            Brush.verticalGradient(listOf(spec.backgroundColor.copy(alpha = spec.surfaceAlpha), spec.backgroundColor.copy(alpha = spec.surfaceAlpha)))
        WidgetBackgroundStyle.TRANSPARENT ->
            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    BoxWithConstraints(
        modifier = modifier
            .then(if (spec.shadow.elevationDp > 0f)
                Modifier.shadow(spec.shadow.elevationDp.dp, shape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.14f))
            else Modifier)
            .clip(shape)
            .background(background)
            .then(if (outlineWidth > 0.dp) Modifier.border(outlineWidth, outline, shape) else Modifier)
    ) {
        content()
        if (spec.backgroundStyle == WidgetBackgroundStyle.PAPER) {
            Box(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(runtime.palette.accent.copy(alpha = 0.62f))
            )
        }
        if (spec.backgroundStyle == WidgetBackgroundStyle.GLASS) {
            Box(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.42f))
            )
        }
    }
}