package com.example.ui.design.launcher.layout

import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.GridSpec

class GridOccupancy(val columns: Int, val rows: Int) {
    private val cells = BooleanArray(columns * rows)
    init { require(columns > 0 && rows > 0) }
    fun contains(r: CellRect) = r.x >= 0 && r.y >= 0 && r.spanX > 0 && r.spanY > 0 &&
        r.x + r.spanX <= columns && r.y + r.spanY <= rows
    fun isVacant(x: Int, y: Int, spanX: Int = 1, spanY: Int = 1): Boolean {
        val r = CellRect(x, y, spanX, spanY)
        if (!contains(r)) return false
        for (cy in y until y + spanY) for (cx in x until x + spanX)
            if (cells[cy * columns + cx]) return false
        return true
    }
    fun mark(item: DesktopItem) = mark(CellRect(item.cellX, item.cellY, item.spanX, item.spanY))
    fun unmark(item: DesktopItem) = unmark(CellRect(item.cellX, item.cellY, item.spanX, item.spanY))
    fun mark(r: CellRect) {
        require(isVacant(r.x, r.y, r.spanX, r.spanY)) { "Occupied or out of bounds: $r" }
        fill(r, true)
    }
    fun unmark(r: CellRect) { require(contains(r)); fill(r, false) }
    private fun fill(r: CellRect, value: Boolean) {
        for (cy in r.y until r.y + r.spanY) for (cx in r.x until r.x + r.spanX)
            cells[cy * columns + cx] = value
    }
    companion object {
        fun from(grid: GridSpec, items: List<DesktopItem>) =
            GridOccupancy(grid.columns, grid.rows).also { occupancy -> items.forEach(occupancy::mark) }
    }
}
