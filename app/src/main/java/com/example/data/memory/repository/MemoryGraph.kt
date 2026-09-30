package com.example.data.memory.repository

import android.content.Context
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightMemoryRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator

/**
 * MemoryGraph — process-wide holder for the single [MemoryRepository]
 * instance (P3C-5 §2), mirroring `ProviderGraph`.
 *
 * MainActivity calls [init] once; the chat runtime (prompt injection) and the
 * Memories page share the same repository — a memory saved in chat shows up
 * in the Memories screen without a reload.
 *
 * It owns its own driver for the same `ailua_chat.db` file: SQLite handles
 * cross-connection access with normal locking, and keeping the graph's
 * connection process-wide means the Memories flow never dies with a screen.
 * The Workspace repository reuses this connection.
 */
object MemoryGraph {

    @Volatile
    private var instance: MemoryRepository? = null
    @Volatile private var sharedDatabase: ChatDatabase? = null

    val database: ChatDatabase get() = checkNotNull(sharedDatabase) { "MemoryGraph not initialized" }

    fun init(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    val driver = ChatDriverFactory(context.applicationContext).createDriver()
                    val database = ChatDatabase(driver)
                    sharedDatabase = database
                    instance = SqlDelightMemoryRepository(
                        database = database,
                        idGenerator = UuidIdGenerator(),
                        clock = SystemEpochClock(),
                    )
                }
            }
        }
    }

    val repository: MemoryRepository
        get() = instance
            ?: error("MemoryGraph.init(context) must be called from MainActivity.onCreate before use")
}
