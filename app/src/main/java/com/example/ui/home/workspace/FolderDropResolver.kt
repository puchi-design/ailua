package com.example.ui.home.workspace

import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.WorkspaceSnapshot

/** Folder creation is a separate drop action, not a reorder solution. */
sealed interface FolderDropIntent {
    data object None : FolderDropIntent
    data class Create(val draggedAppId: String, val targetAppId: String) : FolderDropIntent
    data class Add(val draggedAppId: String, val folderId: String) : FolderDropIntent
}

object FolderDropResolver {
    fun resolve(
        snapshot: WorkspaceSnapshot,
        dragged: DesktopItem,
        targetContainer: DesktopContainer,
        targetPageId: String?,
        targetCell: CellRect,
    ): FolderDropIntent {
        if (dragged.type != DesktopItemType.APP || dragged.locked ||
            dragged.container == DesktopContainer.FOLDER) return FolderDropIntent.None
        val target = when (targetContainer) {
            DesktopContainer.WORKSPACE -> targetPageId?.let(snapshot::itemsFor)
                ?.firstOrNull { it.cellX == targetCell.x && it.cellY == targetCell.y }
            DesktopContainer.HOTSEAT -> snapshot.hotseatItems()
                .firstOrNull { it.cellX == targetCell.x }
            DesktopContainer.FOLDER -> null
        } ?: return FolderDropIntent.None
        if (target.id == dragged.id || target.locked) return FolderDropIntent.None
        return when (target.type) {
            DesktopItemType.APP -> FolderDropIntent.Create(dragged.id, target.id)
            DesktopItemType.FOLDER -> if (snapshot.folder(target.id) != null)
                FolderDropIntent.Add(dragged.id, target.id) else FolderDropIntent.None
            DesktopItemType.AILUA_WIDGET -> FolderDropIntent.None
        }
    }

    fun targetId(intent: FolderDropIntent): String? = when (intent) {
        is FolderDropIntent.Create -> intent.targetAppId
        is FolderDropIntent.Add -> intent.folderId
        FolderDropIntent.None -> null
    }
}
