package com.example.ui.home.workspace

import androidx.compose.ui.geometry.Offset
import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopContainer
import com.example.ui.design.launcher.layout.DragDirection

enum class WorkspaceDragPhase { IDLE, DRAGGING, EDGE_DWELL, DROPPING }

/** One session survives page changes; pages only render its current projection. */
data class WorkspaceDragState(
    val draggedItemId: String? = null,
    val sourceContainer: DesktopContainer? = null,
    val sourcePageId: String? = null,
    val sourceCell: CellRect? = null,
    val currentPageId: String? = null,
    val targetContainer: DesktopContainer? = null,
    val hoverCell: CellRect? = null,
    val pointerPosition: Offset = Offset.Zero,
    val grabOffset: Offset = Offset.Zero,
    val previewWidth: Int = 0,
    val previewHeight: Int = 0,
    val direction: DragDirection = DragDirection.RIGHT,
    val phase: WorkspaceDragPhase = WorkspaceDragPhase.IDLE,
) {
    val isDragging: Boolean get() = phase != WorkspaceDragPhase.IDLE
}
