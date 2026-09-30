package com.example.ui.design.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.components.AppIconItem

/** Separate overlay; the real cell remains a ghost while a preview follows the pointer. */
@Composable
fun DragLayer(
    label: WorkspaceAppLabel,
    position: IntOffset,
    width: Int,
    height: Int,
) {
    val density = LocalDensity.current
    val widthDp: Dp = with(density) { width.toDp() }
    val heightDp: Dp = with(density) { height.toDp() }
    Box(
        Modifier.offset { position }.size(widthDp, heightDp).shadow(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppIconItem(
            name = label.name, iconKey = label.iconKey, badge = label.badge,
            editMode = true, isDragging = true, onClick = {},
        )
    }
}

@Composable
fun DropIndicator(position: IntOffset, width: Int, height: Int, valid: Boolean = true) {
    val density = LocalDensity.current
    val widthDp: Dp = with(density) { width.toDp() }
    val heightDp: Dp = with(density) { height.toDp() }
    Box(
        Modifier.offset { position }.size(widthDp, heightDp)
            .border(2.dp,
                (if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.7f),
                RoundedCornerShape(14.dp))
    )
}
