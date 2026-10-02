package com.example.ui.home.workspace

import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.GridSpec
import com.example.data.desktop.WorkspaceCommit
import com.example.data.desktop.WorkspaceSnapshot
import com.example.data.desktop.WidgetPlacement
import com.example.ui.design.launcher.layout.DragDirection
import com.example.ui.design.launcher.layout.LayoutSolution
import com.example.ui.design.launcher.layout.ReorderSolver

sealed interface DropPlan {
    data class Accept(val commit: WorkspaceCommit, val preview: LayoutSolution?) : DropPlan
    data class Reject(val reason: String) : DropPlan
}

/** Pure placement policy shared by page, cross-page, and hotseat drops. */
object DropResolver {
    fun resolveResize(snapshot: WorkspaceSnapshot, item: DesktopItem, spanX: Int, spanY: Int): DropPlan {
        if (item.type != DesktopItemType.AILUA_WIDGET ||
            !WidgetPlacement.supports(item.sourceId, spanX, spanY))
            return DropPlan.Reject("Unsupported size")
        val resized = item.copy(spanX = spanX, spanY = spanY)
        val plan = resolveDrop(snapshot, resized, DesktopContainer.WORKSPACE, item.pageId,
            CellRect(item.cellX, item.cellY, spanX, spanY), DragDirection.DOWN)
        if (plan is DropPlan.Accept) {
            val destination = plan.preview?.placements?.get(item.id)
            if (destination?.x != item.cellX || destination.y != item.cellY)
                return DropPlan.Reject("No room to resize")
        }
        return plan
    }

    fun resolveDrop(
        snapshot: WorkspaceSnapshot,
        item: DesktopItem,
        targetContainer: DesktopContainer,
        targetPageId: String?,
        targetCell: CellRect,
        direction: DragDirection = DragDirection.RIGHT,
        temporaryPage: DesktopPage? = null,
    ): DropPlan {
        if (item.locked || item.container == DesktopContainer.FOLDER) return DropPlan.Reject("Item cannot move")
        if (item.type == DesktopItemType.AILUA_WIDGET && targetContainer == DesktopContainer.HOTSEAT)
            return DropPlan.Reject("Widgets cannot enter hotseat")
        return when (targetContainer) {
            DesktopContainer.WORKSPACE -> resolveWorkspace(
                snapshot, item, targetPageId, targetCell, direction, temporaryPage,
            )
            DesktopContainer.HOTSEAT -> resolveHotseat(snapshot, item, targetCell)
            DesktopContainer.FOLDER -> DropPlan.Reject("Folders accept apps through FolderDropResolver")
        }
    }

    private fun resolveWorkspace(
        snapshot: WorkspaceSnapshot,
        item: DesktopItem,
        pageId: String?,
        cell: CellRect,
        direction: DragDirection,
        temporaryPage: DesktopPage?,
    ): DropPlan {
        if (pageId == null || snapshot.pages.none { it.id == pageId }) return DropPlan.Reject("Unknown page")
        val bounds = GridSpec()
        if (cell.x < 0 || cell.y < 0 || cell.x + item.spanX > bounds.columns ||
            cell.y + item.spanY > bounds.rows) return DropPlan.Reject("Outside workspace")
        val onPage = snapshot.itemsFor(pageId)
        val solverItem = if (item.container == DesktopContainer.WORKSPACE && item.pageId == pageId) item
            else item.copy(cellX = -1, cellY = -1)
        val solved = ReorderSolver.solve(onPage + solverItem, solverItem, cell.x, cell.y, bounds, direction)
            ?: return DropPlan.Reject("No available cell")
        val placements = solved.placements.mapNotNull { (id, rect) ->
            val original = snapshot.items.firstOrNull { it.id == id } ?: return@mapNotNull null
            val next = original.placement().copy(
                container = DesktopContainer.WORKSPACE,
                pageId = pageId,
                cellX = rect.x,
                cellY = rect.y,
                spanX = rect.spanX,
                spanY = rect.spanY,
                rank = rect.y * bounds.columns + rect.x,
            )
            if (next == original.placement()) null else id to next
        }.toMap()
        return DropPlan.Accept(WorkspaceCommit(placements, temporaryPage?.takeIf { it.id == pageId }), solved)
    }

    private fun resolveHotseat(snapshot: WorkspaceSnapshot, item: DesktopItem, cell: CellRect): DropPlan {
        if (item.spanX != 1 || item.spanY != 1 || cell.x !in 0..4 || cell.y != 0)
            return DropPlan.Reject("Outside hotseat")
        val occupant = snapshot.hotseatItems().firstOrNull { it.cellX == cell.x && it.id != item.id }
        if (item.container == DesktopContainer.WORKSPACE && occupant != null)
            return DropPlan.Reject("Hotseat cell is full")
        val placements = buildMap<String, DesktopPlacement> {
            val destination = item.placement().copy(
                container = DesktopContainer.HOTSEAT, pageId = null,
                cellX = cell.x, cellY = 0, rank = cell.x,
            )
            if (destination != item.placement()) put(item.id, destination)
            if (occupant != null) {
                val displaced = occupant.placement().copy(
                    cellX = item.cellX, cellY = 0, rank = item.cellX,
                )
                put(occupant.id, displaced)
            }
        }
        return DropPlan.Accept(WorkspaceCommit(placements), null)
    }
}
