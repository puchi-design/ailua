package com.example.data.desktop

import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeWorkspace(): Flow<WorkspaceSnapshot>
    fun snapshot(): WorkspaceSnapshot
    suspend fun createPage(): DesktopPage
    suspend fun deletePage(pageId: String)
    suspend fun reorderPages(pageIds: List<String>)
    suspend fun moveItem(itemId: String, placement: DesktopPlacement)
    suspend fun commitLayout(placements: Map<String, DesktopPlacement>)
    suspend fun applyDrop(commit: WorkspaceCommit): WorkspaceSnapshot
    suspend fun addWidget(sourceId: String, pageId: String, spanX: Int, spanY: Int): WorkspaceSnapshot?
    suspend fun addAppToWorkspace(sourceId: String, preferredPageId: String? = null): WorkspaceSnapshot?
    suspend fun deleteWidget(itemId: String): WorkspaceSnapshot
    suspend fun setHomePage(pageId: String)
    suspend fun createFolder(
        draggedItemId: String,
        targetItemId: String,
        suggestedTitle: String? = null,
    ): WorkspaceSnapshot
    suspend fun addItemToFolder(itemId: String, folderId: String): WorkspaceSnapshot
    suspend fun moveFolderItem(itemId: String, folderId: String, rank: Int): WorkspaceSnapshot
    suspend fun moveItemOutOfFolder(itemId: String, placement: DesktopPlacement): WorkspaceSnapshot
    suspend fun renameFolder(folderId: String, title: String): WorkspaceSnapshot
    suspend fun dissolveFolderIfNeeded(folderId: String): WorkspaceSnapshot
}
