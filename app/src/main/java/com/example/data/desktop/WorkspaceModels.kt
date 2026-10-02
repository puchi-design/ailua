package com.example.data.desktop

enum class DesktopItemType { APP, AILUA_WIDGET, FOLDER }
enum class DesktopContainer { WORKSPACE, HOTSEAT, FOLDER }

data class DesktopPage(val id: String, val rank: Int, val isHome: Boolean)
data class DesktopFolder(val id: String, val title: String)

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
    val parentFolderId: String? = null,
) {
    fun placement() = DesktopPlacement(container, pageId, cellX, cellY, spanX, spanY, rank, parentFolderId)
    fun withPlacement(p: DesktopPlacement) = copy(
        container = p.container, pageId = p.pageId, cellX = p.cellX, cellY = p.cellY,
        spanX = p.spanX, spanY = p.spanY, rank = p.rank, parentFolderId = p.parentFolderId,
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
    val parentFolderId: String? = null,
)

data class WorkspaceSnapshot(
    val pages: List<DesktopPage>,
    val items: List<DesktopItem>,
    val folders: List<DesktopFolder> = emptyList(),
) {
    fun itemsFor(pageId: String) = items.filter { it.container == DesktopContainer.WORKSPACE && it.pageId == pageId }
    fun hotseatItems() = items.filter { it.container == DesktopContainer.HOTSEAT }.sortedBy { it.cellX }
    fun dockItems() = hotseatItems()
    fun folder(folderId: String): DesktopFolder? = folders.firstOrNull { it.id == folderId }
    fun folderItem(folderId: String): DesktopItem? = items.firstOrNull {
        it.id == folderId && it.type == DesktopItemType.FOLDER
    }
    fun folderItems(folderId: String): List<DesktopItem> = items
        .filter { it.container == DesktopContainer.FOLDER && it.parentFolderId == folderId }
        .sortedWith(compareBy(DesktopItem::rank, DesktopItem::id))
}

/** A resolved drop is written as one transaction, including an optional new page. */
data class WorkspaceCommit(
    val placements: Map<String, DesktopPlacement>,
    val newPage: DesktopPage? = null,
)

data class GridSpec(val columns: Int = 4, val rows: Int = 6) {
    init { require(columns > 0 && rows > 0) }
}

data class CellRect(val x: Int, val y: Int, val spanX: Int = 1, val spanY: Int = 1) {
    fun overlaps(other: CellRect): Boolean =
        x < other.x + other.spanX && x + spanX > other.x &&
            y < other.y + other.spanY && y + spanY > other.y
}
