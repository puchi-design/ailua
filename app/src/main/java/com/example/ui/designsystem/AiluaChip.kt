package com.example.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun AiluaChip(
    label: String,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val theme = LocalAiluaTheme.current
    Row(
        modifier.clip(RoundedCornerShape(theme.shapes.pill.dp))
            .background(if (selected) theme.palette.accent.copy(alpha = 0.18f) else theme.surfaces.inset.copy(alpha = 0.55f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = if (onClick != null) 44.dp else 32.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke()
        Text(label, style = theme.text.secondary, color = if (selected) theme.palette.onSurface else theme.palette.onSurfaceMuted)
    }
}
