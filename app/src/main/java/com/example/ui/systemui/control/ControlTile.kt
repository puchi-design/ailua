package com.example.ui.systemui.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ControlTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tag: String = "control_tile",
) {
    val theme = LocalAiluaTheme.current
    val spec = theme.controlCenter
    val card = spec.tileStyle
    val shape = RoundedCornerShape(theme.shapes.large.dp)
    val base = if (active) theme.palette.accent.copy(alpha = 0.20f).compositeOver(card.backgroundColor)
        else card.backgroundColor
    val color = base.copy(alpha = if (active) spec.activeAlpha else spec.inactiveAlpha)
    // Accent is a tint over the same card surface, preserving readable theme foregrounds.
    val foreground = card.foregroundColor
    Surface(
        color = color,
        contentColor = foreground,
        shape = shape,
        shadowElevation = 0.dp,
        modifier = modifier.testTag(tag)
            .semantics { stateDescription = subtitle }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier.heightIn(min = if (compact) 76.dp else 100.dp)
                .padding(if (compact) 10.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 19.dp else 25.dp))
            Text(title, style = if (compact) theme.text.caption else theme.text.body,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!compact) Text(subtitle, style = LocalAiluaTheme.current.text.caption, color = foreground.copy(alpha = 0.76f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
