package com.example.ui.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.data.desktop.DesktopItem
import com.example.ui.home.WidgetHostContext
import com.example.ui.home.WidgetRegistry
import com.example.ui.home.WidgetSize
import kotlin.math.abs

@Composable
fun WorkspaceWidgetItem(
    item: DesktopItem,
    context: WidgetHostContext,
    isEditing: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onResizePreview: (WidgetSize) -> Unit,
    onResizeCommit: (WidgetSize) -> Unit,
) {
    val spec = WidgetRegistry.resolve(item.sourceId)
    val size = WidgetSize(item.spanX, item.spanY)
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        Modifier.fillMaxSize().padding(3.dp).clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
            .border(if (selected) 2.dp else 1.dp,
                if (selected) context.accent else context.accent.copy(alpha = 0.25f), shape)
            .then(if (isEditing) Modifier.clickable { onSelect() } else Modifier)
            .testTag("workspace_widget_${item.id}"),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            spec.content(context, size)
            if (isEditing) {
                Box(Modifier.fillMaxSize().zIndex(1f).clickable { onSelect() })
            }
        }
        if (isEditing && selected) {
            Box(
                Modifier.align(Alignment.TopEnd).zIndex(2f).padding(5.dp).size(24.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer)
                    .clickable { onDelete() }.testTag("widget_delete"),
                contentAlignment = Alignment.Center,
            ) { Text("×", color = MaterialTheme.colorScheme.onErrorContainer) }

            val density = LocalDensity.current
            val cellWidth = with(density) { maxWidth.toPx() } / item.spanX
            val cellHeight = with(density) { maxHeight.toPx() } / item.spanY
            var delta by remember(item.id) { mutableStateOf(Offset.Zero) }
            var candidate by remember(item.id) { mutableStateOf(size) }
            Box(
                Modifier.align(Alignment.BottomEnd).zIndex(2f).padding(4.dp).size(30.dp)
                    .clip(CircleShape).background(context.accent)
                    .clickable {
                        val index = spec.supportedSizes.indexOf(size)
                        onResizeCommit(spec.supportedSizes[(index + 1) % spec.supportedSizes.size])
                    }
                    .pointerInput(item.id, size) {
                        detectDragGestures(
                            onDragStart = { delta = Offset.Zero; candidate = size },
                            onDrag = { change, amount ->
                                change.consume()
                                delta += amount
                                val targetX = item.spanX + delta.x / cellWidth
                                val targetY = item.spanY + delta.y / cellHeight
                                candidate = spec.supportedSizes.minBy { option ->
                                    abs(option.spanX - targetX) + abs(option.spanY - targetY)
                                }
                                onResizePreview(candidate)
                            },
                            onDragEnd = { onResizeCommit(candidate) },
                            onDragCancel = { onResizePreview(size) },
                        )
                    }
                    .testTag("widget_resize"),
                contentAlignment = Alignment.Center,
            ) { Text("◢", color = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}
