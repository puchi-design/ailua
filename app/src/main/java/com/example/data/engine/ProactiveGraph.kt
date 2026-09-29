package com.example.data.engine

import android.content.Context
import com.example.data.ai.runtime.ActiveProfileProviderResolver
import com.example.data.ai.repository.ProviderGraph
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph
import com.example.data.reality.RealityContextPolicy
import com.example.data.reality.RealityRepository

/**
 * ProactiveGraph — process-wide holder for the single [ProactiveMessageEngine]
 * (P3D-2), mirroring [MemoryGraph] / [ProviderGraph].
 *
 * It owns its own driver on the same `ailua_chat.db` (SQLite handles the
 * cross-connection locking), so the heartbeat ticker can append a proactive
 * message while a chat screen holds its own connection.
 */
object ProactiveGraph {

    @Volatile
    private var instance: ProactiveMessageEngine? = null

    fun init(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    val database = ChatDatabase(
                        ChatDriverFactory(context.applicationContext).createDriver(),
                    )
                    instance = ProactiveMessageEngine(
                        chatRepository = SqlDelightChatRepository(
                            database = database,
                            idGenerator = UuidIdGenerator(),
                            clock = SystemEpochClock(),
                        ),
                        memoryRepository = MemoryGraph.repository,
                        providerResolver = ActiveProfileProviderResolver(ProviderGraph.repository),
                        clock = SystemEpochClock(),
                        loadSettings = { AiluaLocalStore.getProactiveSettings() },
                        loadState = { AiluaLocalStore.getProactiveState() },
                        saveState = { state -> AiluaLocalStore.saveProactiveState(state) },
                        realityContext = { RealityContextPolicy.context(RealityRepository.refresh(), RealityRepository.settings.value) },
                    )
                }
            }
        }
    }

    /** Heartbeat ticker entry — non-blocking, rules checked inside. */
    fun maybeFire() {
        instance?.maybeFire()
    }

    /** Dev-console test entry: skips timing rules, performs a real send. */
    fun forceFire() {
        instance?.forceFire()
    }

    /** Test/preview seam. */
    internal fun swapForTesting(engine: ProactiveMessageEngine?) {
        instance = engine
    }
}
