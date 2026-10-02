package com.example.ui.home.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopFolder
import com.example.data.desktop.GridSpec
import com.example.data.desktop.WorkspaceSnapshot
import com.example.ui.components.AppIconItem
import com.example.ui.components.AppIconDefaults
import com.example.ui.design.launcher.CellLayout
import com.example.ui.design.launcher.DropIndicator
import com.example.ui.design.launcher.WorkspaceAppLabel
import com.example.ui.design.launcher.layout.LayoutSolution
import com.example.ui.home.WidgetHostContext
import com.example.ui.home.WidgetSize
import com.example.ui.home.widget.WorkspaceWidgetItem
import com.example.ui.home.folder.WorkspaceFolderItem
import com.example.ui.themeengine.LocalAiluaTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset

@Composable
fun WorkspacePageGrid(
    items: List<DesktopItem>,
    snapshot: WorkspaceSnapshot,
    labels: Map<String, WorkspaceAppLabel>,
    displayRows: Int,
    isEditing: Boolean,
    draggedItemId: String?,
    preview: LayoutSolution?,
    hoverCell: CellRect?,
    canDrop: Boolean,
    widgetContext: WidgetHostContext,
    selectedWidgetId: String?,
    resizeOutline: CellRect?,
    resizeValid: Boolean,
    onWidgetSelect: (String) -> Unit,
    onWidgetDelete: (String) -> Unit,
    onWidgetResizePreview: (String, WidgetSize) -> Unit,
    onWidgetResizeCommit: (String, WidgetSize) -> Unit,
    onBounds: (Rect) -> Unit,
    onAppClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    folderHoverTargetId: String?,
    showLabels: Boolean,
    iconScale: Float,
    modifier: Modifier = Modifier,
) {
    val grid = GridSpec(rows = displayRows)
    val theme = LocalAiluaTheme.current
    BoxWithConstraints(modifier.onGloballyPositioned { onBounds(it.boundsInRoot()) }.testTag("home_app_grid")) {
        val cellWidth = constraints.maxWidth.toFloat() / grid.columns
        val cellHeight = constraints.maxHeight.toFloat() / grid.rows
        CellLayout(grid, items, preview, Modifier.fillMaxSize()) { item ->
            if (item.type == DesktopItemType.AILUA_WIDGET) {
                Box(Modifier.fillMaxSize().alpha(if (item.id == draggedItemId) 0.25f else 1f)) {
                    WorkspaceWidgetItem(item, widgetContext, isEditing,
                        selected = selectedWidgetId == item.id,
                        onSelect = { onWidgetSelect(item.id) },
                        onDelete = { onWidgetDelete(item.id) },
                        onResizePreview = { onWidgetResizePreview(item.id, it) },
                        onResizeCommit = { onWidgetResizeCommit(item.id, it) })
                }
            } else if (item.type == DesktopItemType.APP) labels[item.sourceId]?.let { label ->
                Box(
                    Modifier.fillMaxSize().alpha(if (item.id == draggedItemId) 0.25f else 1f)
                        .then(if (folderHoverTargetId == item.id) Modifier.border(
                            2.dp, theme.palette.accent, RoundedCornerShape(20.dp)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIconItem(
                        name = label.name, iconKey = label.iconKey, badge = label.badge,
                        size = AppIconDefaults.ContainerSize * iconScale,
                        showLabel = showLabels,
                        editMode = isEditing,
                        onClick = if (isEditing) ({}) else ({ onAppClick(item.sourceId) }),
                    )
                }
            } else if (item.type == DesktopItemType.FOLDER) {
                val folder = snapshot.folder(item.id) ?: DesktopFolder(item.id, "文件夹")
                Box(
                    Modifier.fillMaxSize().alpha(if (item.id == draggedItemId) 0.25f else 1f)
                        .then(if (folderHoverTargetId == item.id) Modifier.border(
                            2.dp, theme.palette.accent, RoundedCornerShape(20.dp)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    WorkspaceFolderItem(folder, snapshot.folderItems(item.id), labels,
                        isEditing = isEditing, showLabel = showLabels,
                        size = AppIconDefaults.ContainerSize * iconScale,
                        onClick = { onFolderClick(item.id) })
                }
            }
        }
        if (hoverCell != null) {
            DropIndicator(
                position = IntOffset((hoverCell.x * cellWidth).roundToInt(), (hoverCell.y * cellHeight).roundToInt()),
                width = (cellWidth * hoverCell.spanX).roundToInt(),
                height = (cellHeight * hoverCell.spanY).roundToInt(), valid = canDrop,
            )
        }
        if (resizeOutline != null) {
            DropIndicator(
                position = IntOffset((resizeOutline.x * cellWidth).roundToInt(),
                    (resizeOutline.y * cellHeight).roundToInt()),
                width = (cellWidth * resizeOutline.spanX).roundToInt(),
                height = (cellHeight * resizeOutline.spanY).roundToInt(), valid = resizeValid,
            )
        }
    }
}

/** Every ordinary page uses the same spatial renderer for apps and widgets. */
@Composable
fun WorkspacePage(
    items: List<DesktopItem>,
    snapshot: WorkspaceSnapshot,
    labels: Map<String, WorkspaceAppLabel>,
    isEditing: Boolean,
    isDragging: Boolean,
    draggedItemId: String?,
    preview: LayoutSolution?,
    hoverCell: CellRect?,
    canDrop: Boolean,
    widgetContext: WidgetHostContext,
    selectedWidgetId: String?,
    resizeOutline: CellRect?,
    resizeValid: Boolean,
    onWidgetSelect: (String) -> Unit,
    onWidgetDelete: (String) -> Unit,
    onWidgetResizePreview: (String, WidgetSize) -> Unit,
    onWidgetResizeCommit: (String, WidgetSize) -> Unit,
    onBounds: (Rect) -> Unit,
    onAppClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    folderHoverTargetId: String?,
    showLabels: Boolean,
    iconScale: Float,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = !isDragging)
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        WorkspacePageGrid(
            items = items, snapshot = snapshot, labels = labels, displayRows = 6, isEditing = isEditing,
            draggedItemId = draggedItemId, preview = preview, hoverCell = hoverCell,
            widgetContext = widgetContext, selectedWidgetId = selectedWidgetId,
            resizeOutline = resizeOutline, resizeValid = resizeValid,
            onWidgetSelect = onWidgetSelect, onWidgetDelete = onWidgetDelete,
            onWidgetResizePreview = onWidgetResizePreview,
            onWidgetResizeCommit = onWidgetResizeCommit,
            canDrop = canDrop, onBounds = onBounds, onAppClick = onAppClick,
            onFolderClick = onFolderClick, folderHoverTargetId = folderHoverTargetId,
            showLabels = showLabels, iconScale = iconScale,
            modifier = Modifier.fillMaxWidth().height((6 * 82).dp),
        )
    }
}
