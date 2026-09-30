package com.example.data.desktop

import android.content.Context
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.desktop.local.SqlDelightWorkspaceRepository
import com.example.data.local.AiluaLocalStore

/** Uses the existing SQLDelight database file and driver factory. */
object WorkspaceGraph {
    private var instance: SqlDelightWorkspaceRepository? = null
    val repository: SqlDelightWorkspaceRepository get() = checkNotNull(instance) { "WorkspaceGraph not initialized" }

    @Synchronized
    fun init(context: Context) {
        if (instance != null) return
        val repo = SqlDelightWorkspaceRepository(ChatDatabase(ChatDriverFactory(context.applicationContext).createDriver()))
        repo.migrateIfEmpty(AiluaLocalStore.getHomeAppOrder())
        AiluaLocalStore.markWorkspaceMigrated()
        instance = repo
    }
}
