package com.example.data.desktop

/** One-time conversion of the old ordered grid. The old preference is retained as rollback data. */
object WorkspaceSeed {
    const val HOME_ID = "page_home"
    val desktopApps = listOf("mailbox", "gallery", "check_phone", "memories", "relations", "diary", "theater", "call_history")
    val dockApps = listOf("messages", "moments", "living", "contacts", "apps")

    fun fromLegacyOrder(savedIds: List<String>): WorkspaceSnapshot {
        val saved = savedIds.distinct().filter { it in desktopApps }
        val order = saved + desktopApps.filterNot { it in saved }
        val page = DesktopPage(HOME_ID, 0, true)
        val items = order.mapIndexed { index, id ->
            DesktopItem("app_$id", DesktopItemType.APP, id, DesktopContainer.WORKSPACE,
                HOME_ID, index % 4, index / 4, rank = index)
        } + dockApps.mapIndexed { index, id ->
            DesktopItem("dock_$id", DesktopItemType.APP, id, DesktopContainer.DOCK,
                null, index, 0, rank = index)
        }
        return WorkspaceSnapshot(listOf(page), items)
    }
}
