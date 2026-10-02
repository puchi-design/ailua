package com.example.ui.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val shape = RoundedCornerShape(runtime.shapes.medium.dp)
    val outline = if (selected) runtime.palette.accent else runtime.surfaces.divider
    val outlineWidth = if (selected) 2.dp else 0.dp
    val background = when (spec.backgroundStyle) {
        WidgetBackgroundStyle.TRANSPARENT -> Color.Transparent
        else -> runtime.surfaces.raised.copy(alpha = spec.surfaceAlpha.coerceIn(0.78f, 1f))
    }
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (outlineWidth > 0.dp) Modifier.border(outlineWidth, outline, shape) else Modifier)
    ) {
        content()
    }
}
