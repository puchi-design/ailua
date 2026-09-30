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
    suspend fun deleteWidget(itemId: String): WorkspaceSnapshot
    suspend fun setHomePage(pageId: String)
}
