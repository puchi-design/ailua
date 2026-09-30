package com.example.data.desktop

enum class DesktopItemType { APP, AILUA_WIDGET, FOLDER }
enum class DesktopContainer { WORKSPACE, DOCK }

data class DesktopPage(val id: String, val rank: Int, val isHome: Boolean)

data class DesktopItem(
    val id: String,
    val type: DesktopItemType,
    val sourceId: String,
    val container: DesktopContainer,
    val pageId: String?,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val rank: Int = 0,
    val locked: Boolean = false,
) {
    fun placement() = DesktopPlacement(container, pageId, cellX, cellY, spanX, spanY, rank)
    fun withPlacement(p: DesktopPlacement) = copy(
        container = p.container, pageId = p.pageId, cellX = p.cellX, cellY = p.cellY,
        spanX = p.spanX, spanY = p.spanY, rank = p.rank,
    )
}

data class DesktopPlacement(
    val container: DesktopContainer,
    val pageId: String?,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val rank: Int = 0,
)

data class WorkspaceSnapshot(val pages: List<DesktopPage>, val items: List<DesktopItem>) {
    fun itemsFor(pageId: String) = items.filter { it.container == DesktopContainer.WORKSPACE && it.pageId == pageId }
    fun dockItems() = items.filter { it.container == DesktopContainer.DOCK }.sortedBy { it.rank }
}

data class GridSpec(val columns: Int = 4, val rows: Int = 6) {
    init { require(columns > 0 && rows > 0) }
}

data class CellRect(val x: Int, val y: Int, val spanX: Int = 1, val spanY: Int = 1) {
    fun overlaps(other: CellRect): Boolean =
        x < other.x + other.spanX && x + spanX > other.x &&
            y < other.y + other.spanY && y + spanY > other.y
}
