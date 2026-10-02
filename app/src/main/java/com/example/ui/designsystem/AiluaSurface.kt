package com.example.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

enum class SurfaceTone { RAISED, INSET, OVERLAY }

/** For distinct information or controls. Ordinary lists should remain plain. */
@Composable
fun AiluaSurface(
    modifier: Modifier = Modifier,
    tone: SurfaceTone = SurfaceTone.RAISED,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = LocalAiluaTheme.current
    val color = when (tone) {
        SurfaceTone.RAISED -> theme.surfaces.raised
        SurfaceTone.INSET -> theme.surfaces.inset
        SurfaceTone.OVERLAY -> theme.surfaces.overlay
    }
    Box(modifier.clip(RoundedCornerShape(theme.shapes.large.dp)).background(color), content = content)
}
