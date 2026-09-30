package com.example.data.desktop

import android.content.Context
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph

/** Reuses the process-wide MemoryGraph SQLDelight connection. */
object WorkspaceGraph {
    private var instance: SqlDelightWorkspaceRepository? = null
    val repository: SqlDelightWorkspaceRepository get() = checkNotNull(instance) { "WorkspaceGraph not initialized" }

    @Synchronized
    fun init(context: Context) {
        if (instance != null) return
        MemoryGraph.init(context.applicationContext)
        val repo = SqlDelightWorkspaceRepository(MemoryGraph.database)
        repo.migrateIfEmpty(AiluaLocalStore.getHomeAppOrder())
        repo.seedDefaultWidgetsOnce()
        AiluaLocalStore.markWorkspaceMigrated()
        instance = repo
    }
}
