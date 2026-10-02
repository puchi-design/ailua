package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AiluaLocalStore
import com.example.data.model.LetterDeliveryState
import com.example.data.repository.MailboxRepository
import com.example.data.systemui.notification.NotificationCategory
import com.example.data.systemui.notification.VirtualNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MailNotificationTest {
    @Test
    fun newlyDeliveredLettersPostOnceAfterDeliveryAndResyncDoesNotNotify() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("ailua_os_store", Context.MODE_PRIVATE).edit().clear().commit()
        AiluaLocalStore.init(context)
        AiluaLocalStore.loadFromDisk()
        // The seed inbox already contains Mira's delivered letter at 21:30.
        // Rewinding the clock must not turn persisted delivery history into a new event.
        MailboxRepository.syncWithLocalStore(21 * 60 + 30)
        assertEquals(LetterDeliveryState.DELIVERED, MailboxRepository.letters.value.single { it.id == "letter_mira_1" }.deliveryState)
        val scheduledIds = setOf("letter_yuna_1", "letter_noa_1")
        assertEquals(scheduledIds, MailboxRepository.letters.value.filter { it.deliveryState == LetterDeliveryState.SCHEDULED }.map { it.id }.toSet())
        val notifications = mutableListOf<VirtualNotification>()
        val newlyDelivered = MailboxRepository.checkScheduledDeliveries(
            currentVirtualMinutes = 23 * 60,
            nowEpochMs = 1_234L,
            postNotification = { notification ->
                assertTrue(MailboxRepository.letters.value.any {
                    "mail:${it.id}" == notification.sourceKey && it.deliveryState == LetterDeliveryState.DELIVERED
                })
                notifications += notification
            },
        )
        assertEquals(scheduledIds, newlyDelivered.map { it.id }.toSet())
        assertEquals(2, notifications.size)
        assertEquals(scheduledIds.map { "mail:$it" }.toSet(), notifications.map { it.sourceKey }.toSet())
        assertTrue(notifications.all { it.category == NotificationCategory.MAIL && it.route == "mailbox" && it.timestampEpochMs == 1_234L })
        MailboxRepository.syncWithLocalStore(23 * 60)
        val repeated = MailboxRepository.checkScheduledDeliveries(23 * 60, { notifications += it })
        assertTrue(repeated.isEmpty())
        assertEquals(2, notifications.size)
    }
}
