package com.example.data.desktop.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.data.chat.local.ChatDatabase
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.GridSpec
import com.example.data.desktop.WorkspaceRepository
import com.example.data.desktop.WorkspaceCommit
import com.example.data.desktop.WorkspaceSeed
import com.example.data.desktop.WorkspaceSnapshot
import com.example.ui.design.launcher.layout.GridOccupancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.UUID

class SqlDelightWorkspaceRepository(private val database: ChatDatabase) : WorkspaceRepository {
    private val pages get() = database.desktopPageQueries
    private val items get() = database.desktopItemQueries

    /** Idempotent first-open seed, committed with pages and items together. */
    fun migrateIfEmpty(legacyOrder: List<String>) {
        database.transaction {
            if (pages.selectAllPages().executeAsList().isNotEmpty()) return@transaction
            val seed = WorkspaceSeed.fromLegacyOrder(legacyOrder)
            seed.pages.forEach { pages.insertPage(it.id, it.rank.toLong(), if (it.isHome) 1L else 0L) }
            seed.items.forEach { insert(it) }
        }
    }

    override fun observeWorkspace(): Flow<WorkspaceSnapshot> = combine(
        pages.selectAllPages().asFlow().mapToList(Dispatchers.IO),
        items.selectAllItems().asFlow().mapToList(Dispatchers.IO),
    ) { pageRows, itemRows ->
        WorkspaceSnapshot(
            pageRows.map { DesktopPage(it.id, it.rank.toInt(), it.is_home != 0L) },
            itemRows.map { it.toDomain() },
        )
    }

    override fun snapshot() = WorkspaceSnapshot(
        pages.selectAllPages().executeAsList().map { DesktopPage(it.id, it.rank.toInt(), it.is_home != 0L) },
        items.selectAllItems().executeAsList().map { it.toDomain() },
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
                items.updatePlacement(item.container.name, item.pageId, item.cellX.toLong(),
                    item.cellY.toLong(), item.spanX.toLong(), item.spanY.toLong(), item.rank.toLong(), item.id)
            }
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
                items.updatePlacement(item.container.name, item.pageId, item.cellX.toLong(),
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

    private fun validate(state: WorkspaceSnapshot) {
        require(state.pages.count { it.isHome } == 1)
        require(state.items.filter { it.type == DesktopItemType.APP }.map { it.sourceId }.distinct().size ==
            state.items.count { it.type == DesktopItemType.APP }) { "Duplicate app" }
        val pageIds = state.pages.map { it.id }.toSet()
        val dock = state.hotseatItems()
        require(dock.size <= 5 && dock.map { it.cellX }.distinct().size == dock.size)
        require(dock.all { it.pageId == null && it.rank in 0..4 && it.cellX == it.rank &&
            it.cellY == 0 && it.spanX == 1 && it.spanY == 1 })
        for (page in state.pages) {
            val occupancy = GridOccupancy(GridSpec().columns, GridSpec().rows)
            state.itemsFor(page.id).forEach { occupancy.mark(it) }
        }
        require(state.items.filter { it.container == DesktopContainer.WORKSPACE }.all { it.pageId in pageIds })
    }

    private fun insert(item: DesktopItem) {
        items.insertItem(item.id, item.type.name, item.sourceId, item.container.name, item.pageId,
            item.cellX.toLong(), item.cellY.toLong(), item.spanX.toLong(), item.spanY.toLong(),
            item.rank.toLong(), if (item.locked) 1L else 0L)
    }

    private fun com.example.data.chat.local.Desktop_item.toDomain() = DesktopItem(
        id = id, type = DesktopItemType.valueOf(item_type), sourceId = source_id,
        container = DesktopContainer.valueOf(container), pageId = page_id,
        cellX = cell_x.toInt(), cellY = cell_y.toInt(), spanX = span_x.toInt(), spanY = span_y.toInt(),
        rank = rank.toInt(), locked = locked != 0L,
    )
}
