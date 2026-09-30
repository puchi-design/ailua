package com.example.ui.design.launcher

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.GridSpec
import com.example.ui.components.AppIconItem
import com.example.ui.design.launcher.layout.DragDirection
import com.example.ui.design.launcher.layout.LayoutSolution
import com.example.ui.design.launcher.layout.ReorderSolver
import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface DragState {
    data object Idle : DragState
    data class Pressed(val itemId: String) : DragState
    data class Dragging(val itemId: String, val displacement: Offset) : DragState
    data class EdgeDwell(val itemId: String, val displacement: Offset) : DragState
    data class Dropping(val itemId: String) : DragState
    data class Cancelling(val itemId: String) : DragState
}

data class WorkspaceAppLabel(val name: String, val iconKey: String, val badge: String?)

/** Single-page drag session. Pointer frames only update transient Compose state. */
@Composable
fun WorkspaceAppGrid(
    items: List<DesktopItem>,
    displayRows: Int,
    labels: Map<String, WorkspaceAppLabel>,
    isEditing: Boolean,
    onEnterEdit: () -> Unit,
    onDragging: (Boolean) -> Unit,
    onCommit: (Map<String, DesktopPlacement>) -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val grid = remember { GridSpec() }
    val layoutGrid = GridSpec(rows = displayRows)
    var dragState by remember { mutableStateOf<DragState>(DragState.Idle) }
    var solution by remember { mutableStateOf<LayoutSolution?>(null) }
    val draggedId = when (val current = dragState) {
        is DragState.Pressed -> current.itemId
        is DragState.Dragging -> current.itemId
        is DragState.EdgeDwell -> current.itemId
        is DragState.Dropping -> current.itemId
        is DragState.Cancelling -> current.itemId
        DragState.Idle -> null
    }
    fun clear() { dragState = DragState.Idle; solution = null; onDragging(false) }

    BoxWithConstraints(modifier.testTag("home_app_grid")) {
        val cellWidth = constraints.maxWidth.toFloat() / grid.columns
        val cellHeight = constraints.maxHeight.toFloat() / layoutGrid.rows
        CellLayout(layoutGrid, items, solution, Modifier.fillMaxSize()) { item ->
            val label = labels[item.sourceId]
            if (label != null) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .alpha(if (draggedId == item.id) 0.28f else 1f)
                        .pointerInput(item.id, items, cellWidth, cellHeight) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragState = DragState.Pressed(item.id)
                                    solution = null
                                    onEnterEdit()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDragging(true)
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    val previous = (dragState as? DragState.Dragging)?.displacement ?: Offset.Zero
                                    val next = previous + delta
                                    dragState = DragState.Dragging(item.id, next)
                                    val x = (item.cellX + next.x / cellWidth).roundToInt()
                                    val y = (item.cellY + next.y / cellHeight).roundToInt()
                                    val direction = if (abs(delta.x) >= abs(delta.y)) {
                                        if (delta.x >= 0) DragDirection.RIGHT else DragDirection.LEFT
                                    } else if (delta.y >= 0) DragDirection.DOWN else DragDirection.UP
                                    solution = ReorderSolver.solve(items, item, x, y, grid, direction)
                                },
                                onDragEnd = {
                                    val chosen = solution
                                    if (chosen != null) {
                                        val placements = items.mapNotNull { placed ->
                                            val rect = chosen.placements[placed.id] ?: return@mapNotNull null
                                            val target = placed.placement().copy(cellX = rect.x, cellY = rect.y)
                                            if (target == placed.placement()) null else placed.id to target
                                        }.toMap()
                                        if (placements.isNotEmpty()) onCommit(placements)
                                    }
                                    clear()
                                },
                                onDragCancel = { clear() },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIconItem(
                        name = label.name, iconKey = label.iconKey, badge = label.badge,
                        editMode = isEditing,
                        onClick = if (isEditing) ({}) else ({ onAppClick(item.sourceId) }),
                    )
                }
            }
        }
        val dragged = items.firstOrNull { it.id == draggedId }
        val position = (dragState as? DragState.Dragging)?.displacement
        val label = dragged?.let { labels[it.sourceId] }
        if (dragged != null && position != null && label != null) {
            solution?.placements?.get(dragged.id)?.let { cell ->
                DropIndicator(
                    position = IntOffset((cell.x * cellWidth).roundToInt(), (cell.y * cellHeight).roundToInt()),
                    width = cellWidth.roundToInt(), height = cellHeight.roundToInt(),
                )
            }
            DragLayer(
                label = label,
                position = IntOffset((dragged.cellX * cellWidth + position.x).roundToInt(),
                    (dragged.cellY * cellHeight + position.y).roundToInt()),
                width = cellWidth.roundToInt(),
                height = cellHeight.roundToInt(),
            )
        }
    }
}
