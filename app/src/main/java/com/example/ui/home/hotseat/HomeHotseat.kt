package com.example.ui.home.hotseat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopFolder
import com.example.data.desktop.WorkspaceSnapshot
import com.example.ui.components.AppIconItem
import com.example.ui.design.launcher.WorkspaceAppLabel
import com.example.ui.home.folder.WorkspaceFolderItem
import com.example.ui.themeengine.DockContainerMode
import com.example.ui.themeengine.DockVisualSpec
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.material.ailuaMaterialSurface
import com.example.ui.themeengine.material.surfaceMaterial

/** Shared presentation only; slot bounds, items and drag behavior remain in HomeHotseat. */
@Composable
fun HomeDockFrame(
    spec: DockVisualSpec,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        if (spec.containerMode != DockContainerMode.NONE) {
            // The backing remains a sibling so translucent material never owns padded icons.
            Box(Modifier.matchParentSize().ailuaMaterialSurface(spec.surfaceMaterial()))
        }
        content()
    }
}

/** Five real slots projected from DesktopItem(HOTSEAT); gaps remain usable cells. */
@Composable
fun HomeHotseat(
    items: List<DesktopItem>,
    snapshot: WorkspaceSnapshot,
    labels: Map<String, WorkspaceAppLabel>,
    accent: Color,
    isEditing: Boolean,
    draggedItemId: String?,
    hoverSlot: Int?,
    canDrop: Boolean,
    onBounds: (Rect) -> Unit,
    onAppClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    folderHoverTargetId: String?,
    iconScale: Float,
) {
    val runtime = LocalAiluaTheme.current
    val dock = runtime.dock
    val hasContainer = dock.containerMode != DockContainerMode.NONE

    HomeDockFrame(dock, Modifier.fillMaxWidth().padding(horizontal = runtime.layout.screenHorizontalPadding.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = if (hasContainer) 82.dp else 0.dp)
                .padding(horizontal = dock.horizontalPaddingDp.coerceAtLeast(0f).dp,
                    vertical = dock.verticalPaddingDp.coerceAtLeast(0f).dp)
                .onGloballyPositioned { onBounds(it.boundsInRoot()) }
                .testTag("virtual_phone_dock"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { slot ->
                val item = items.firstOrNull { it.cellX == slot }
                val label = item?.let { labels[it.sourceId] }
                val outline = when {
                    item != null && folderHoverTargetId == item.id -> accent
                    hoverSlot == slot -> if (canDrop) accent else MaterialTheme.colorScheme.error
                    item == null && isEditing -> accent.copy(alpha = 0.24f)
                    else -> Color.Transparent
                }
                Box(
                    Modifier.weight(1f).border(1.dp, outline, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item != null) {
                        Box(
                            modifier = Modifier.then(if (item.id == draggedItemId) Modifier.drawGhost() else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (item.type) {
                                DesktopItemType.APP -> if (label != null) AppIconItem(
                                    name = label.name, iconKey = label.iconKey, badge = label.badge,
                                    showLabel = false, editMode = isEditing,
                                    size = com.example.ui.components.AppIconDefaults.ContainerSize * dock.iconScale * iconScale * 0.94f,
                                    onClick = if (isEditing) ({}) else ({ onAppClick(item.sourceId) }),
                                )
                                DesktopItemType.FOLDER -> WorkspaceFolderItem(
                                    folder = snapshot.folder(item.id) ?: DesktopFolder(item.id, "文件夹"),
                                    children = snapshot.folderItems(item.id), labels = labels,
                                    isEditing = isEditing, showLabel = false,
                                    size = com.example.ui.components.AppIconDefaults.ContainerSize * dock.iconScale * iconScale * 0.94f,
                                    onClick = { onFolderClick(item.id) },
                                    modifier = Modifier.then(if (folderHoverTargetId == item.id)
                                        Modifier.border(2.dp, accent, RoundedCornerShape(18.dp)) else Modifier),
                                )
                                DesktopItemType.AILUA_WIDGET -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.drawGhost(): Modifier = this.then(Modifier.alpha(0.25f))
