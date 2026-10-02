package com.example

import com.example.data.systemui.notification.NotificationCategory
import com.example.data.systemui.notification.NotificationHeadsUpPolicy
import com.example.data.systemui.notification.NotificationPriority
import com.example.data.systemui.notification.SqlDelightVirtualNotificationRepository
import com.example.data.systemui.notification.VirtualNotification
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VirtualNotificationRepositoryTest {
    private fun notification(key: String = "message:session:turn", time: Long = 100L) = VirtualNotification(
        sourceKey = key,
        sourceAppId = "chat",
        title = "小弥",
        body = "真正写入成功的消息",
        characterId = "mira",
        timestampEpochMs = time,
        category = NotificationCategory.MESSAGE,
        route = "chat/mira",
    )

    @Test
    fun sourceKeyIsDeduplicatedAcrossRestartAndDismissedHistoryDoesNotReplay() = runBlocking {
        val file = File.createTempFile("ailua_notifications", ".db")
        try {
            val first = ChatTestHarness.file(file)
            val message = notification()
            try {
                val repository = SqlDelightVirtualNotificationRepository(first.database)
                val liveEvent = async(start = CoroutineStart.UNDISPATCHED) { repository.newNotifications.first() }
                assertTrue(repository.post(message))
                assertEquals(message, liveEvent.await())
                assertFalse(repository.post(message.copy(id = "other-id", body = "重复事件")))
                assertEquals(listOf(message), repository.observeActive().first())
                repository.dismiss(message.id)
            } finally { first.driver.close() }

            val reopened = ChatTestHarness.file(file, createSchema = false)
            try {
                val repository = SqlDelightVirtualNotificationRepository(reopened.database)
                assertTrue(repository.observeActive().first().isEmpty())
                assertTrue(repository.newNotifications.replayCache.isEmpty())
                assertFalse(repository.post(message.copy(id = "third-id")))
                val historical = reopened.database.virtualNotificationQueries.selectNotificationBySourceKey(message.sourceKey).executeAsOne()
                assertEquals(message.id, historical.id)
                assertEquals(1L, historical.dismissed)
            } finally { reopened.driver.close() }
        } finally { file.delete() }
    }

    @Test
    fun seenDoesNotDismissAndClearAllKeepsDedupeHistory() = runBlocking {
        val f = ChatTestHarness.inMemory()
        try {
            val repository = SqlDelightVirtualNotificationRepository(f.database)
            val older = notification("mail:one", 10L)
            val newer = notification("message:two", 20L)
            repository.post(older)
            repository.post(newer)
            repository.markSeen(newer.id)
            val active = repository.observeActive().first()
            assertEquals(listOf(newer.id, older.id), active.map { it.id })
            assertTrue(active.first().seen)
            assertFalse(active.first().dismissed)
            assertFalse(active.last().seen)
            repository.clearDismissible()
            assertTrue(repository.observeActive().first().isEmpty())
            assertFalse(repository.post(older.copy(id = "again")))
            assertFalse(repository.post(newer.copy(id = "again-newer")))
        } finally { f.driver.close() }
    }

    @Test
    fun headsUpOnlyUsesEligibleEventsAndRespectsFocusLockShadeAndIncomingCall() {
        val message = notification()
        assertTrue(NotificationHeadsUpPolicy.shouldShow(message, false, false, false))
        assertFalse(NotificationHeadsUpPolicy.shouldShow(message, false, false, true))
        assertFalse(NotificationHeadsUpPolicy.shouldShow(message, true, false, false))
        assertFalse(NotificationHeadsUpPolicy.shouldShow(message, false, true, false))
        assertFalse(NotificationHeadsUpPolicy.shouldShow(message, false, false, false, isIncomingCall = true))
        val mail = message.copy(category = NotificationCategory.MAIL)
        assertFalse(NotificationHeadsUpPolicy.shouldShow(mail, false, false, false))
        assertTrue(NotificationHeadsUpPolicy.shouldShow(mail.copy(priority = NotificationPriority.HIGH), false, false, false))
    }
}
