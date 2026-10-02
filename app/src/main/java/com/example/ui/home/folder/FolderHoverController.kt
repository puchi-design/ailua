package com.example.ui.home.folder

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.desktop.CellRect

/** A drop enters folder mode only after the pointer stays in the cell's middle 60%. */
object FolderHoverController {
    const val DWELL_MS = 300L

    fun isCentered(
        pointer: Offset,
        container: Rect,
        cell: CellRect,
        columns: Int,
        rows: Int,
    ): Boolean {
        if (columns <= 0 || rows <= 0 || !container.contains(pointer)) return false
        val width = container.width / columns
        val height = container.height / rows
        val centerX = container.left + (cell.x + 0.5f) * width
        val centerY = container.top + (cell.y + 0.5f) * height
        return kotlin.math.abs(pointer.x - centerX) <= width * 0.30f &&
            kotlin.math.abs(pointer.y - centerY) <= height * 0.30f
    }
}
