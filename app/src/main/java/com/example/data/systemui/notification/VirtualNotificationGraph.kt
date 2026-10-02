package com.example.data.systemui.notification

import android.content.Context
import android.util.Log
import com.example.data.memory.repository.MemoryGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Shares the process-wide SQLDelight connection, including its query invalidations. */
object VirtualNotificationGraph {
    @Volatile private var instance: VirtualNotificationRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: VirtualNotificationRepository
        get() = checkNotNull(instance) { "VirtualNotificationGraph.init(context) must run before System UI" }

    @Synchronized
    fun init(context: Context) {
        if (instance != null) return
        MemoryGraph.init(context.applicationContext)
        instance = SqlDelightVirtualNotificationRepository(MemoryGraph.database)
    }

    /** A presentation write must not undo an already committed message or letter. */
    suspend fun post(notification: VirtualNotification): Boolean {
        val current = instance ?: return false
        return try {
            current.post(notification)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("VirtualNotification", "Unable to persist ${notification.sourceKey}", error)
            false
        }
    }

    fun postAsync(notification: VirtualNotification) {
        scope.launch { post(notification) }
    }
}
