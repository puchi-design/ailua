package com.example

import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.GridSpec
import com.example.ui.design.launcher.layout.DragDirection
import com.example.ui.design.launcher.layout.ReorderSolver
import org.junit.Assert.*
import org.junit.Test

class ReorderSolverTest {
    private fun item(id: String, x: Int, y: Int = 0, spanX: Int = 1, locked: Boolean = false) =
        DesktopItem(id, DesktopItemType.APP, id, DesktopContainer.WORKSPACE, "page_home",
            x, y, spanX = spanX, locked = locked)

    @Test fun emptyCellAndChainPushDoNotMutateInputs() {
        val a = item("a", 0)
        val b = item("b", 1)
        val c = item("c", 2)
        val input = listOf(a, b, c)
        val direct = ReorderSolver.solve(input, a, 0, 1)
        assertEquals(0, direct!!.cost)
        assertEquals(1, direct.placements.getValue("a").y)
        val push = ReorderSolver.solve(input, a, 1, 0, direction = DragDirection.RIGHT)!!
        assertEquals(1, push.placements.getValue("a").x)
        assertEquals(2, push.placements.getValue("b").x)
        assertEquals(3, push.placements.getValue("c").x)
        assertEquals(1, b.cellX) // preview/cancel leave repository models untouched
    }

    @Test fun blockedPushFallsBackToNearestVacancy() {
        val a = item("a", 0)
        val blocker = item("widget", 1, spanX = 2, locked = true)
        val result = ReorderSolver.solve(listOf(a, blocker), a, 1, 0, direction = DragDirection.RIGHT)!!
        assertEquals(1, blocker.cellX)
        assertFalse(result.placements.getValue("a").x == 1 && result.placements.getValue("a").y == 0)
        assertEquals(1, result.placements.getValue("widget").x)
    }

    @Test fun verticalPushUsesFreeCellAbove() {
        val gallery = item("gallery", 1, 2)
        val diary = item("diary", 1, 1)
        val result = ReorderSolver.solve(listOf(gallery, diary), gallery, 1, 1,
            direction = DragDirection.UP)!!
        assertEquals(1, result.placements.getValue("gallery").y)
        assertEquals(0, result.placements.getValue("diary").y)
    }

    @Test fun fullExistingPageCannotRearrangeIntoOccupiedCell() {
        val full = (0 until 6).flatMap { y -> (0 until 4).map { x -> item("i-$x-$y", x, y) } }
        val dragged = full.first()
        assertNull(ReorderSolver.solve(full, dragged, 1, 0, GridSpec(), DragDirection.RIGHT))
    }

    @Test fun fullPageAndOutOfBoundsHaveNoSolution() {
        val full = (0 until 6).flatMap { y -> (0 until 4).map { x -> item("i-$x-$y", x, y) } }
        val extra = item("dragged", 0)
        assertNull(ReorderSolver.solve(full + extra, extra, 0, 0, GridSpec()))
        assertNull(ReorderSolver.solve(listOf(extra), extra, 4, 0, GridSpec()))
    }
}
