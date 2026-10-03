package com.example.ui.home.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.BorderSpec
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.material.ailuaMaterialSurface
import com.example.ui.themeengine.material.surfaceMaterial

/** The frame owns only appearance; the workspace continues to own selection and resizing. */
@Composable
fun ThemeWidgetFrame(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val runtime = LocalAiluaTheme.current
    val spec = runtime.widgets
    val material = spec.surfaceMaterial().let {
        if (selected) it.copy(border = BorderSpec(runtime.palette.accent, 2f)) else it
    }
    val shape = RoundedCornerShape(material.cornerRadiusDp.coerceAtLeast(0f).dp)
    BoxWithConstraints(modifier = modifier) {
        // Material is a sibling behind the content. Its RenderNode never contains padded
        // children, which prevents API 29 from punching rectangles into a translucent frame.
        Box(Modifier.matchParentSize().ailuaMaterialSurface(material))
        BoxWithConstraints(Modifier.clip(shape), content = content)
    }
}
