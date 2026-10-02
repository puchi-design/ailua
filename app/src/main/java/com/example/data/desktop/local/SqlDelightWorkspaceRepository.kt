package com.example.data.desktop.local

import app.cash.sqldelight.coroutines.asFlow
import com.example.data.chat.local.ChatDatabase
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopFolder
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.GridSpec
import com.example.data.desktop.WorkspaceRepository
import com.example.data.desktop.WorkspaceCommit
import com.example.data.desktop.WorkspaceSeed
import com.example.data.desktop.WorkspaceSnapshot
import com.example.data.desktop.WidgetPlacement
import com.example.ui.design.launcher.layout.GridOccupancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.util.UUID

class SqlDelightWorkspaceRepository(private val database: ChatDatabase) : WorkspaceRepository {
    private val pages get() = database.desktopPageQueries
    private val items get() = database.desktopItemQueries
    private val folders get() = database.desktopFolderQueries

    /** Idempotent first-open seed, committed with pages and items together. */
    fun migrateIfEmpty(legacyOrder: List<String>) {
        database.transaction {
            if (pages.selectAllPages().executeAsList().isNotEmpty()) return@transaction
            val seed = WorkspaceSeed.fromLegacyOrder(legacyOrder)
            seed.pages.forEach { pages.insertPage(it.id, it.rank.toLong(), if (it.isHome) 1L else 0L) }
            seed.items.forEach { insert(it) }
        }
    }

    override fun observeWorkspace(): Flow<WorkspaceSnapshot> = merge(
        pages.selectAllPages().asFlow().map { Unit },
        items.selectAllItems().asFlow().map { Unit },
        folders.selectAllFolders().asFlow().map { Unit },
    ).map {
        // Read all three tables together after an invalidation. Combining independent row flows
        // can briefly pair a new folder item with stale folder metadata after one transaction.
        database.transactionWithResult { snapshot() }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override fun snapshot() = WorkspaceSnapshot(
        pages.selectAllPages().executeAsList().map { DesktopPage(it.id, it.rank.toInt(), it.is_home != 0L) },
        items.selectAllItems().executeAsList().map { it.toDomain() },
        folders.selectAllFolders().executeAsList().map { DesktopFolder(it.id, it.title) },
    )

    override suspend fun createPage(): DesktopPage = withContext(Dispatchers.IO) {
        database.transactionWithResult {
            val current = snapshot()
            val page = DesktopPage(UUID.randomUUID().toString(), current.pages.size, false)
            pages.insertPage(page.id, page.rank.toLong(), 0L)
            page
        }
    }

    override suspend fun deletePage(pageId: String) = withContext(Dispatchers.IO) {
        database.transaction {
            val current = snapshot()
            require(current.pages.size > 1 && current.pages.any { it.id == pageId && !it.isHome })
            require(current.itemsFor(pageId).isEmpty()) { "Only empty pages can be removed" }
            pages.deletePage(pageId)
            current.pages.filterNot { it.id == pageId }.forEachIndexed { index, page ->
                pages.updatePageRank(index.toLong(), page.id)
            }
        }
    }

    override suspend fun reorderPages(pageIds: List<String>) = withContext(Dispatchers.IO) {
        database.transaction {
            val current = snapshot()
            require(pageIds.size == current.pages.size && pageIds.toSet() == current.pages.map { it.id }.toSet())
            pageIds.forEachIndexed { index, id -> pages.updatePageRank(index.toLong(), id) }
        }
    }

    override suspend fun setHomePage(pageId: String) = withContext(Dispatchers.IO) {
        database.transaction {
            require(snapshot().pages.any { it.id == pageId })
            pages.clearHomePage()
            pages.markHomePage(pageId)
        }
    }

    override suspend fun moveItem(itemId: String, placement: DesktopPlacement) =
        commitLayout(mapOf(itemId to placement))

    override suspend fun commitLayout(placements: Map<String, DesktopPlacement>) = withContext(Dispatchers.IO) {
        if (placements.isEmpty()) return@withContext
        database.transaction {
            val before = snapshot()
            require(placements.keys.all { id -> before.items.any { it.id == id && !it.locked } })
            val after = before.copy(items = before.items.map { item ->
                placements[item.id]?.let(item::withPlacement) ?: item
            })
            validate(after)
            after.items.filter { it.id in placements }.forEach { item ->
                items.updatePlacement(item.container.name, item.pageId, item.parentFolderId, item.cellX.toLong(),
                    item.cellY.toLong(), item.spanX.toLong(), item.spanY.toLong(), item.rank.toLong(), item.id)
            }
        }
    }

    /** The marker is stored in the same database transaction, so deleting all widgets stays deleted. */
    fun seedDefaultWidgetsOnce() {
        database.transaction {
            if (database.workspaceWidgetStateQueries.selectWidgetSeedState().executeAsOneOrNull() != null)
                return@transaction
            val before = snapshot()
            val planned = WidgetPlacement.defaultHome(before).orEmpty()
            planned.filter { it.type == DesktopItemType.AILUA_WIDGET }.forEach(::insert)
            planned.filter { it.type != DesktopItemType.AILUA_WIDGET }.forEach { item ->
                val previous = before.items.first { it.id == item.id }
                if (item.placement() != previous.placement()) {
                    items.updatePlacement(item.container.name, item.pageId, item.parentFolderId, item.cellX.toLong(),
                        item.cellY.toLong(), item.spanX.toLong(), item.spanY.toLong(), item.rank.toLong(), item.id)
                }
            }
            database.workspaceWidgetStateQueries.markWidgetSeeded()
            validate(snapshot())
        }
    }

    override suspend fun applyDrop(commit: WorkspaceCommit): WorkspaceSnapshot = withContext(Dispatchers.IO) {
        database.transactionWithResult {
            val before = snapshot()
            require(commit.placements.keys.all { id -> before.items.any { it.id == id && !it.locked } })
            commit.newPage?.let { page ->
                require(!page.isHome && page.rank == before.pages.size && before.pages.none { it.id == page.id })
                require(commit.placements.values.any { it.pageId == page.id && it.container == DesktopContainer.WORKSPACE })
                pages.insertPage(page.id, page.rank.toLong(), 0L)
            }
            val withPage = if (commit.newPage == null) before else before.copy(pages = before.pages + commit.newPage)
            val after = withPage.copy(items = withPage.items.map { item ->
                commit.placements[item.id]?.let(item::withPlacement) ?: item
            })
            validate(after)
            after.items.filter { it.id in commit.placements }.forEach { item ->
                items.updatePlacement(item.container.name, item.pageId, item.parentFolderId, item.cellX.toLong(),
                    item.cellY.toLong(), item.spanX.toLong(), item.spanY.toLong(), item.rank.toLong(), item.id)
            }
            // Only trailing empty ordinary pages are reclaimed; the home page and one-page minimum survive.
            var current = snapshot()
            while (current.pages.size > 1) {
                val last = current.pages.last()
                if (last.isHome || current.itemsFor(last.id).isNotEmpty()) break
                pages.deletePage(last.id)
                current = snapshot()
            }
            current
        }
    }

    override suspend fun addWidget(sourceId: String, pageId: String, spanX: Int, spanY: Int): WorkspaceSnapshot? =
        withContext(Dispatchers.IO) {
            require(WidgetPlacement.supports(sourceId, spanX, spanY))
            database.transactionWithResult {
                val before = snapshot()
                require(before.pages.any { it.id == pageId })
                val cell = WidgetPlacement.firstVacant(before.itemsFor(pageId), spanX, spanY)
                    ?: return@transactionWithResult null
                val widget = DesktopItem(
                    "widget_${sourceId}_${UUID.randomUUID()}", DesktopItemType.AILUA_WIDGET, sourceId,
                    DesktopContainer.WORKSPACE, pageId, cell.x, cell.y, spanX, spanY,
                    cell.y * GridSpec().columns + cell.x,
                )
                insert(widget)
                snapshot()
            }
        }

    override suspend fun deleteWidget(itemId: String): WorkspaceSnapshot = withContext(Dispatchers.IO) {
        database.transactionWithResult {
            val before = snapshot()
            require(before.items.any { it.id == itemId && it.type == DesktopItemType.AILUA_WIDGET })
            items.deleteItem(itemId)
            var current = snapshot()
            while (current.pages.size > 1) {
                val last = current.pages.last()
                if (last.isHome || current.itemsFor(last.id).isNotEmpty()) break
                pages.deletePage(last.id)
                current = snapshot()
            }
            current
        }
    }

    override suspend fun createFolder(
        draggedItemId: String,
        targetItemId: String,
        suggestedTitle: String?,
    ): WorkspaceSnapshot =
        mutateFolders { before ->
            require(draggedItemId != targetItemId)
            val dragged = before.items.first { it.id == draggedItemId }
            val target = before.items.first { it.id == targetItemId }
            require(dragged.type == DesktopItemType.APP && target.type == DesktopItemType.APP)
            require(!dragged.locked && !target.locked)
            require(dragged.container != DesktopContainer.FOLDER && target.container != DesktopContainer.FOLDER)
            val folderId = UUID.randomUUID().toString()
            val folderItem = target.copy(
                id = folderId, type = DesktopItemType.FOLDER, sourceId = folderId,
                parentFolderId = null,
            )
            val children = listOf(target, dragged).mapIndexed { rank, item ->
                item.withPlacement(DesktopPlacement(DesktopContainer.FOLDER, null, 0, 0,
                    rank = rank, parentFolderId = folderId))
            }
            before.copy(
                items = before.items.filterNot { it.id == target.id || it.id == dragged.id } +
                    folderItem + children,
                folders = before.folders + DesktopFolder(folderId, suggestedTitle?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: "文件夹"),
            )
        }

    override suspend fun addItemToFolder(itemId: String, folderId: String): WorkspaceSnapshot =
        mutateFolders { before ->
            val item = before.items.first { it.id == itemId }
            require(item.type == DesktopItemType.APP && !item.locked)
            require(item.container != DesktopContainer.FOLDER)
            require(before.folder(folderId) != null && before.folderItem(folderId)?.locked == false)
            val placement = DesktopPlacement(DesktopContainer.FOLDER, null, 0, 0,
                rank = before.folderItems(folderId).size, parentFolderId = folderId)
            before.copy(items = before.items.map { if (it.id == itemId) it.withPlacement(placement) else it })
        }

    override suspend fun moveFolderItem(itemId: String, folderId: String, rank: Int): WorkspaceSnapshot =
        mutateFolders { before ->
            val children = before.folderItems(folderId)
            require(children.any { it.id == itemId && !it.locked } && rank in children.indices)
            val reordered = children.filterNot { it.id == itemId }.toMutableList()
            reordered.add(rank, children.first { it.id == itemId })
            val placements = reordered.mapIndexed { index, child ->
                child.id to child.placement().copy(rank = index)
            }.toMap()
            before.copy(items = before.items.map { item ->
                placements[item.id]?.let(item::withPlacement) ?: item
            })
        }

    override suspend fun moveItemOutOfFolder(
        itemId: String,
        placement: DesktopPlacement,
    ): WorkspaceSnapshot = mutateFolders { before ->
        val item = before.items.first { it.id == itemId }
        require(item.type == DesktopItemType.APP && item.container == DesktopContainer.FOLDER && !item.locked)
        require(placement.container != DesktopContainer.FOLDER && placement.parentFolderId == null)
        val folderId = requireNotNull(item.parentFolderId)
        val remaining = before.folderItems(folderId).filterNot { it.id == itemId }
        val ranks = remaining.mapIndexed { index, child -> child.id to index }.toMap()
        val moved = before.copy(items = before.items.map { current ->
            when (current.id) {
                itemId -> current.withPlacement(placement)
                in ranks -> current.copy(rank = ranks.getValue(current.id))
                else -> current
            }
        })
        dissolveInMemory(moved, folderId)
    }

    override suspend fun renameFolder(folderId: String, title: String): WorkspaceSnapshot =
        mutateFolders { before ->
            require(before.folderItem(folderId) != null)
            require(before.folder(folderId) != null)
            before.copy(folders = before.folders.map { folder ->
                if (folder.id == folderId) folder.copy(title = title.trim().ifEmpty { "文件夹" }) else folder
            })
        }

    override suspend fun dissolveFolderIfNeeded(folderId: String): WorkspaceSnapshot =
        mutateFolders { before -> dissolveInMemory(before, folderId) }

    private suspend fun mutateFolders(
        change: (WorkspaceSnapshot) -> WorkspaceSnapshot,
    ): WorkspaceSnapshot = withContext(Dispatchers.IO) {
        database.transactionWithResult {
            val before = snapshot()
            val after = change(before)
            validate(after)
            persistFolderTransition(before, after)
            snapshot()
        }
    }

    private fun dissolveInMemory(state: WorkspaceSnapshot, folderId: String): WorkspaceSnapshot {
        val folderItem = requireNotNull(state.folderItem(folderId))
        require(state.folder(folderId) != null)
        val children = state.folderItems(folderId)
        if (children.size > 1) return state
        val survivorId = children.singleOrNull()?.id
        return state.copy(
            items = state.items.filterNot { it.id == folderId }.map { item ->
                if (item.id == survivorId) item.withPlacement(folderItem.placement()) else item
            },
            folders = state.folders.filterNot { it.id == folderId },
        )
    }

    private fun persistFolderTransition(before: WorkspaceSnapshot, after: WorkspaceSnapshot) {
        val oldItems = before.items.associateBy { it.id }
        val newItems = after.items.associateBy { it.id }
        oldItems.keys.filterNot { it in newItems }.forEach(items::deleteItem)
        val oldFolders = before.folders.associateBy { it.id }
        val newFolders = after.folders.associateBy { it.id }
        oldFolders.keys.filterNot { it in newFolders }.forEach(folders::deleteFolder)
        after.folders.forEach { folder ->
            val old = oldFolders[folder.id]
            if (old == null) folders.insertFolder(folder.id, folder.title)
            else if (old.title != folder.title) folders.renameFolder(folder.title, folder.id)
        }
        after.items.forEach { item ->
            val old = oldItems[item.id]
            if (old == null) insert(item)
            else {
                require(old.type == item.type && old.sourceId == item.sourceId && old.locked == item.locked)
                if (old.placement() != item.placement()) {
                    items.updatePlacement(item.container.name, item.pageId, item.parentFolderId,
                        item.cellX.toLong(), item.cellY.toLong(), item.spanX.toLong(),
                        item.spanY.toLong(), item.rank.toLong(), item.id)
                }
            }
        }
    }

    private fun validate(state: WorkspaceSnapshot) {
        require(state.pages.count { it.isHome } == 1)
        require(state.pages.map { it.id }.distinct().size == state.pages.size)
        require(state.items.map { it.id }.distinct().size == state.items.size)
        require(state.items.filter { it.type == DesktopItemType.APP }.map { it.sourceId }.distinct().size ==
            state.items.count { it.type == DesktopItemType.APP }) { "Duplicate app" }
        val pageIds = state.pages.map { it.id }.toSet()
        require(state.items.all { item ->
            when (item.container) {
                DesktopContainer.WORKSPACE -> item.pageId in pageIds && item.parentFolderId == null
                DesktopContainer.HOTSEAT -> item.pageId == null && item.parentFolderId == null
                DesktopContainer.FOLDER -> item.pageId == null && item.parentFolderId != null &&
                    item.type == DesktopItemType.APP && item.cellX == 0 && item.cellY == 0 &&
                    item.spanX == 1 && item.spanY == 1
            }
        })
        require(state.items.filter { it.type == DesktopItemType.APP }.all {
            it.spanX == 1 && it.spanY == 1
        })
        val folderItems = state.items.filter { it.type == DesktopItemType.FOLDER }
        require(folderItems.all {
            it.container != DesktopContainer.FOLDER && it.sourceId == it.id &&
                it.spanX == 1 && it.spanY == 1
        })
        require(state.folders.map { it.id }.distinct().size == state.folders.size)
        require(state.folders.all { it.title.isNotBlank() })
        require(state.folders.map { it.id }.toSet() == folderItems.map { it.id }.toSet())
        require(state.items.filter { it.container == DesktopContainer.FOLDER }.all {
            it.parentFolderId in state.folders.map(DesktopFolder::id)
        })
        require(folderItems.all { folder ->
            val children = state.folderItems(folder.id)
            children.size >= 2 && children.map { it.rank } == children.indices.toList()
        })
        val dock = state.hotseatItems()
        require(dock.size <= 5 && dock.map { it.cellX }.distinct().size == dock.size)
        require(dock.all { it.pageId == null && it.rank in 0..4 && it.cellX == it.rank &&
            it.cellY == 0 && it.spanX == 1 && it.spanY == 1 })
        for (page in state.pages) {
            val occupancy = GridOccupancy(GridSpec().columns, GridSpec().rows)
            state.itemsFor(page.id).forEach { occupancy.mark(it) }
        }
        require(state.items.filter { it.type == DesktopItemType.AILUA_WIDGET }.all {
            it.container == DesktopContainer.WORKSPACE && WidgetPlacement.supports(it.sourceId, it.spanX, it.spanY)
        })
    }

    private fun insert(item: DesktopItem) {
        items.insertItem(item.id, item.type.name, item.sourceId, item.container.name, item.pageId,
            item.parentFolderId,
            item.cellX.toLong(), item.cellY.toLong(), item.spanX.toLong(), item.spanY.toLong(),
            item.rank.toLong(), if (item.locked) 1L else 0L)
    }

    private fun com.example.data.chat.local.Desktop_item.toDomain() = DesktopItem(
        id = id, type = DesktopItemType.valueOf(item_type), sourceId = source_id,
        container = DesktopContainer.valueOf(container), pageId = page_id,
        cellX = cell_x.toInt(), cellY = cell_y.toInt(), spanX = span_x.toInt(), spanY = span_y.toInt(),
        rank = rank.toInt(), locked = locked != 0L, parentFolderId = parent_folder_id,
    )
}
