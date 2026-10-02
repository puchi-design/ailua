package com.example.data.systemui.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface VirtualNotificationRepository {
    fun observeActive(): Flow<List<VirtualNotification>>

    /** Ephemeral event stream. Existing persisted history never replays as heads-up. */
    val newNotifications: SharedFlow<VirtualNotification>

    /** Returns false for any previously posted sourceKey, including dismissed history. */
    suspend fun post(notification: VirtualNotification): Boolean
    suspend fun markSeen(id: String)
    suspend fun dismiss(id: String)
    suspend fun clearDismissible()
}
