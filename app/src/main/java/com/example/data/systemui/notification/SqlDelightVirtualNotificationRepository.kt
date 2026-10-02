package com.example.data.systemui.notification

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.Virtual_notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SqlDelightVirtualNotificationRepository(
    private val database: ChatDatabase,
) : VirtualNotificationRepository {
    private val queries = database.virtualNotificationQueries
    private val posting = Mutex()
    private val events = MutableSharedFlow<VirtualNotification>(replay = 0, extraBufferCapacity = 32)
    override val newNotifications: SharedFlow<VirtualNotification> = events.asSharedFlow()

    override fun observeActive(): Flow<List<VirtualNotification>> =
        queries.selectActiveNotifications().asFlow().mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun post(notification: VirtualNotification): Boolean = posting.withLock {
        require(notification.sourceKey.isNotBlank())
        val inserted = withContext(Dispatchers.IO) {
            database.transactionWithResult {
                queries.insertNotification(
                    id = notification.id,
                    source_key = notification.sourceKey,
                    source_app_id = notification.sourceAppId,
                    title = notification.title,
                    body = notification.body,
                    character_id = notification.characterId,
                    timestamp_epoch_ms = notification.timestampEpochMs,
                    priority = notification.priority.name,
                    category = notification.category.name,
                    route = notification.route,
                    seen = if (notification.seen) 1L else 0L,
                    dismissed = if (notification.dismissed) 1L else 0L,
                )
                queries.selectInsertedNotificationCount().executeAsOne() > 0L
            }
        }
        if (inserted && !notification.dismissed) events.emit(notification)
        inserted
    }

    override suspend fun markSeen(id: String) = withContext(Dispatchers.IO) {
        queries.markNotificationSeen(id)
    }

    override suspend fun dismiss(id: String) = withContext(Dispatchers.IO) {
        queries.dismissNotification(id)
    }

    override suspend fun clearDismissible() = withContext(Dispatchers.IO) {
        queries.clearDismissibleNotifications()
    }

    private fun Virtual_notification.toDomain() = VirtualNotification(
        id = id,
        sourceKey = source_key,
        sourceAppId = source_app_id,
        title = title,
        body = body,
        characterId = character_id,
        timestampEpochMs = timestamp_epoch_ms,
        priority = NotificationPriority.valueOf(priority),
        category = NotificationCategory.valueOf(category),
        route = route,
        seen = seen != 0L,
        dismissed = dismissed != 0L,
    )
}
