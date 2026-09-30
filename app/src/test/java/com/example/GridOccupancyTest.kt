package com.example

import com.example.data.desktop.CellRect
import com.example.ui.design.launcher.layout.GridOccupancy
import org.junit.Assert.*
import org.junit.Test

class GridOccupancyTest {
    @Test fun spansBoundsAndOverlap() {
        val grid = GridOccupancy(4, 6)
        assertTrue(grid.isVacant(0, 0, 2, 2))
        grid.mark(CellRect(0, 0, 2, 2))
        assertFalse(grid.isVacant(1, 1))
        assertTrue(grid.isVacant(2, 0, 2, 1))
        assertFalse(grid.isVacant(3, 0, 2, 1))
        assertFalse(grid.isVacant(-1, 0))
        assertThrows(IllegalArgumentException::class.java) { grid.mark(CellRect(1, 1)) }
        grid.unmark(CellRect(0, 0, 2, 2))
        assertTrue(grid.isVacant(1, 1))
    }

    @Test fun fullGridHasNoVacancy() {
        val grid = GridOccupancy(4, 6)
        for (y in 0 until 6) for (x in 0 until 4) grid.mark(CellRect(x, y))
        assertFalse(grid.isVacant(0, 0))
        assertFalse(grid.isVacant(2, 4, 2, 2))
    }
}
