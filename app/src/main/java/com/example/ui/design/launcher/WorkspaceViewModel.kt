package com.example.ui.design.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.desktop.DesktopPlacement
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
                repository.observeWorkspace().collect { mutableWorkspace.value = it }
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

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkspaceViewModel(context) }
        }
    }
}
