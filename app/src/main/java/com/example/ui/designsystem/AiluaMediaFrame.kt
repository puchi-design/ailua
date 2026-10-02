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

@Composable
fun AiluaMediaFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val theme = LocalAiluaTheme.current
    Box(modifier.clip(RoundedCornerShape(theme.shapes.medium.dp)).background(theme.surfaces.inset), content = content)
}
