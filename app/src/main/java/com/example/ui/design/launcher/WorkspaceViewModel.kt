package com.example.ui.design.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.desktop.DesktopPlacement
import com.example.data.desktop.DesktopPage
import com.example.data.desktop.WorkspaceCommit
import com.example.data.desktop.WorkspaceGraph
import com.example.data.desktop.WorkspaceRepository
import com.example.data.desktop.WorkspaceSeed
import com.example.data.local.AiluaLocalStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceViewModel(context: Context) : ViewModel() {
    private val ready = CompletableDeferred<WorkspaceRepository>()
    private val mutableWorkspace = MutableStateFlow(WorkspaceSeed.fromLegacyOrder(AiluaLocalStore.getHomeAppOrder()))
    val workspace = mutableWorkspace.asStateFlow()
    private val mutableIsLoaded = MutableStateFlow(false)
    val isLoaded = mutableIsLoaded.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()

    init {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val repository = withContext(Dispatchers.IO) {
                    WorkspaceGraph.init(appContext)
                    WorkspaceGraph.repository
                }
                ready.complete(repository)
                repository.observeWorkspace().collect {
                    mutableWorkspace.value = it
                    mutableIsLoaded.value = true
                }
            } catch (e: CancellationException) {
                ready.cancel()
                throw e
            } catch (_: Exception) {
                ready.completeExceptionally(IllegalStateException("Workspace unavailable"))
                mutableError.value = "桌面布局暂时无法读取"
            }
        }
    }

    fun commitLayout(placements: Map<String, DesktopPlacement>) {
        if (placements.isEmpty()) return
        viewModelScope.launch {
            try {
                ready.await().commitLayout(placements)
                mutableError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableError.value = "桌面布局未保存，请重试"
            }
        }
    }

    suspend fun applyDrop(commit: WorkspaceCommit): Boolean {
        return try {
            mutableWorkspace.value = ready.await().applyDrop(commit)
            mutableError.value = null
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableError.value = "桌面布局未保存，请重试"
            false
        }
    }

    suspend fun addWidget(sourceId: String, pageId: String, spanX: Int, spanY: Int): Boolean? {
        return try {
            val updated = ready.await().addWidget(sourceId, pageId, spanX, spanY)
            if (updated != null) mutableWorkspace.value = updated
            mutableError.value = null
            updated != null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableError.value = "组件未添加，请重试"
            null
        }
    }

    suspend fun deleteWidget(itemId: String): Boolean {
        return try {
            mutableWorkspace.value = ready.await().deleteWidget(itemId)
            mutableError.value = null
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableError.value = "组件未删除，请重试"
            false
        }
    }

    suspend fun createFolder(draggedId: String, targetId: String, title: String?): Boolean =
        updateWorkspace("文件夹未创建，请重试") {
            createFolder(draggedId, targetId, title)
        }

    suspend fun addItemToFolder(itemId: String, folderId: String): Boolean =
        updateWorkspace("未能加入文件夹，请重试") {
            addItemToFolder(itemId, folderId)
        }

    suspend fun moveFolderItem(itemId: String, folderId: String, rank: Int): Boolean =
        updateWorkspace("文件夹排序未保存，请重试") {
            moveFolderItem(itemId, folderId, rank)
        }

    suspend fun moveItemOutOfFolder(itemId: String, placement: DesktopPlacement): Boolean =
        updateWorkspace("未能移出文件夹，请重试") {
            moveItemOutOfFolder(itemId, placement)
        }

    suspend fun renameFolder(folderId: String, title: String): Boolean =
        updateWorkspace("文件夹名称未保存，请重试") {
            renameFolder(folderId, title)
        }

    suspend fun createPage(): DesktopPage? = try {
        val repository = ready.await()
        val page = repository.createPage()
        mutableWorkspace.value = withContext(Dispatchers.IO) { repository.snapshot() }
        mutableError.value = null
        page
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        mutableError.value = "页面未创建，请重试"
        null
    }

    suspend fun deletePage(pageId: String): Boolean = updatePages("仅能删除空白的非主屏页面") {
        this.deletePage(pageId)
    }

    suspend fun reorderPages(pageIds: List<String>): Boolean = updatePages("页面顺序未保存，请重试") {
        this.reorderPages(pageIds)
    }

    suspend fun setHomePage(pageId: String): Boolean = updatePages("主屏未设置，请重试") {
        this.setHomePage(pageId)
    }

    private suspend fun updatePages(
        message: String,
        operation: suspend WorkspaceRepository.() -> Unit,
    ): Boolean = try {
        val repository = ready.await()
        repository.operation()
        mutableWorkspace.value = withContext(Dispatchers.IO) { repository.snapshot() }
        mutableError.value = null
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        mutableError.value = message
        false
    }

    private suspend fun updateWorkspace(
        message: String,
        operation: suspend WorkspaceRepository.() -> com.example.data.desktop.WorkspaceSnapshot,
    ): Boolean = try {
        mutableWorkspace.value = ready.await().operation()
        mutableError.value = null
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        mutableError.value = message
        false
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkspaceViewModel(context) }
        }
    }
}
