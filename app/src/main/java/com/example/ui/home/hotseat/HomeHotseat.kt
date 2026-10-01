package com.example.ui.home.hotseat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.desktop.DesktopItem
import com.example.ui.components.AppIconItem
import com.example.ui.design.launcher.WorkspaceAppLabel
import com.example.ui.themeengine.DockContainerMode
import com.example.ui.themeengine.LocalAiluaTheme

/** Five real slots projected from DesktopItem(HOTSEAT); gaps remain usable cells. */
@Composable
fun HomeHotseat(
    items: List<DesktopItem>,
    labels: Map<String, WorkspaceAppLabel>,
    accent: Color,
    isEditing: Boolean,
    draggedItemId: String?,
    hoverSlot: Int?,
    canDrop: Boolean,
    onBounds: (Rect) -> Unit,
    onAppClick: (String) -> Unit,
) {
    val runtime = LocalAiluaTheme.current
    val dock = runtime.dock
    val shape = RoundedCornerShape(dock.cornerRadiusDp.dp)
    val hasContainer = dock.containerMode != DockContainerMode.NONE
    val tint = if (dock.containerMode == DockContainerMode.ISLAND) runtime.palette.accent.copy(alpha = dock.tintAlpha)
        else Color.Transparent
    val background = when (dock.containerMode) {
        DockContainerMode.ISLAND -> Brush.verticalGradient(listOf(
            Color.White.copy(alpha = 0.25f),
            dock.backgroundColor.copy(alpha = dock.surfaceAlpha)
        ))
        DockContainerMode.PAPER_STRIP -> Brush.verticalGradient(listOf(
            Color.White.copy(alpha = 0.18f),
            dock.backgroundColor.copy(alpha = dock.surfaceAlpha)
        ))
        else -> Brush.verticalGradient(listOf(
            dock.backgroundColor.copy(alpha = dock.surfaceAlpha),
            dock.backgroundColor.copy(alpha = dock.surfaceAlpha)
        ))
    }
    val containerModifier = if (hasContainer) {
        Modifier
            .then(if (dock.shadow.elevationDp > 0f)
                Modifier.shadow(dock.shadow.elevationDp.dp, shape,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f))
            else Modifier)
            .clip(shape)
            .background(background)
            .background(tint)
            .then(if (dock.border.widthDp > 0f)
                Modifier.border(dock.border.widthDp.dp, dock.border.color, shape)
            else Modifier)
    } else Modifier

    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .then(containerModifier)
                .padding(horizontal = dock.horizontalPaddingDp.dp, vertical = dock.verticalPaddingDp.dp)
                .onGloballyPositioned { onBounds(it.boundsInRoot()) }
                .testTag("virtual_phone_dock"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { slot ->
                val item = items.firstOrNull { it.cellX == slot }
                val label = item?.let { labels[it.sourceId] }
                val outline = when {
                    hoverSlot == slot -> if (canDrop) accent else MaterialTheme.colorScheme.error
                    item == null && isEditing -> accent.copy(alpha = 0.24f)
                    else -> Color.Transparent
                }
                Box(
                    Modifier.weight(1f).border(1.dp, outline, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item != null && label != null) {
                        Box(
                            modifier = Modifier.then(if (item.id == draggedItemId) Modifier.drawGhost() else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIconItem(
                                name = label.name, iconKey = label.iconKey, badge = label.badge,
                                showLabel = false, editMode = isEditing,
                                size = com.example.ui.components.AppIconDefaults.ContainerSize * dock.iconScale,
                                onClick = if (isEditing) ({}) else ({ onAppClick(item.sourceId) }),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.drawGhost(): Modifier = this.then(Modifier.alpha(0.25f))