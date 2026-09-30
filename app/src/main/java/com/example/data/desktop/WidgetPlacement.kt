package com.example.data.desktop

import com.example.ui.design.launcher.layout.GridOccupancy

/** Finds real vacant cells; widget ids identify instances, source ids identify kinds. */
object WidgetPlacement {
    val supportedSizes: Map<String, List<Pair<Int, Int>>> = mapOf(
        "world_clock" to listOf(2 to 1, 4 to 1, 2 to 2),
        "character_living" to listOf(4 to 1, 2 to 2, 4 to 2),
        "memory_echo" to listOf(2 to 2, 4 to 1),
        "bond" to listOf(2 to 1, 2 to 2),
    )

    fun supports(sourceId: String, spanX: Int, spanY: Int) =
        supportedSizes[sourceId]?.contains(spanX to spanY) == true

    fun firstVacant(items: List<DesktopItem>, spanX: Int, spanY: Int,
                    grid: GridSpec = GridSpec()): CellRect? {
        val occupied = GridOccupancy.from(grid, items)
        for (y in 0..grid.rows - spanY) for (x in 0..grid.columns - spanX) {
            if (occupied.isVacant(x, y, spanX, spanY)) return CellRect(x, y, spanX, spanY)
        }
        return null
    }

    /** One-time upgrade keeps every existing item and fills the first page with two visible widgets. */
    fun defaultHome(snapshot: WorkspaceSnapshot): List<DesktopItem>? {
        if (snapshot.items.any { it.type == DesktopItemType.AILUA_WIDGET }) return null
        val home = snapshot.pages.firstOrNull { it.isHome } ?: return null
        val existing = snapshot.itemsFor(home.id).sortedWith(compareBy<DesktopItem> { it.rank }.thenBy { it.id })
        val grid = GridSpec()
        val candidates = listOf(
            DesktopItem("widget_world_clock_001", DesktopItemType.AILUA_WIDGET, "world_clock",
                DesktopContainer.WORKSPACE, home.id, 0, 0, 4, 1),
            DesktopItem("widget_character_living_001", DesktopItemType.AILUA_WIDGET, "character_living",
                DesktopContainer.WORKSPACE, home.id, 0, 1, 4, 2),
        )
        // A crowded page may only fit the clock, or neither; existing items are never lost.
        for (count in candidates.size downTo 1) {
            val placed = candidates.take(count).toMutableList()
            var fits = true
            for (item in existing) {
                val cell = firstVacant(placed, item.spanX, item.spanY, grid)
                if (cell == null) { fits = false; break }
                placed += item.copy(cellX = cell.x, cellY = cell.y,
                    rank = cell.y * grid.columns + cell.x)
            }
            if (fits) return placed
        }
        return emptyList()
    }
}
