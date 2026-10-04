package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AiluaLocalStore
import com.example.data.model.LetterDeliveryState
import com.example.data.model.Letter
import com.example.data.mock.OfficialCharacters
import com.example.data.repository.MailboxRepository
import com.example.data.systemui.notification.NotificationCategory
import com.example.data.systemui.notification.VirtualNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MailNotificationTest {
    @Test
    fun newlyDeliveredLettersPostOnceAfterDeliveryAndResyncDoesNotNotify() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("ailua_os_store", Context.MODE_PRIVATE).edit().clear().commit()
        AiluaLocalStore.init(context)
        AiluaLocalStore.loadFromDisk()
        // Explicit scheduled fixtures keep this delivery test independent of the authored opening time.
        @Suppress("UNCHECKED_CAST")
        val inbox = MailboxRepository::class.java.getDeclaredField("_letters").apply { isAccessible = true }
            .get(MailboxRepository) as MutableStateFlow<List<Letter>>
        val originalInbox = inbox.value
        val prefs = context.getSharedPreferences("ailua_os_store", Context.MODE_PRIVATE)
        val originalReceipts = prefs.getStringSet(AiluaLocalStore.KEY_DELIVERED_LETTERS, emptySet()).orEmpty().toSet()
        val scheduledIds = setOf("six_letter_yuna_1", "six_letter_noa_1")
        try {
        prefs.edit().putStringSet(AiluaLocalStore.KEY_DELIVERED_LETTERS, originalReceipts - scheduledIds).commit()
        AiluaLocalStore.loadFromDisk()
        inbox.value = OfficialCharacters.letters.filter { it.characterId in setOf("mira", "yuna", "noa") }
            .map { letter -> when (letter.characterId) {
                "yuna" -> letter.copy(deliverAtVirtualTimeMinutes = 22 * 60 + 30, deliverAtVirtualTimeString = "22:30")
                "noa" -> letter.copy(deliverAtVirtualTimeMinutes = 23 * 60, deliverAtVirtualTimeString = "23:00")
                else -> letter
            } }
        // Rewinding the clock must not turn persisted delivery history into a new event.
        MailboxRepository.syncWithLocalStore(21 * 60 + 30)
        assertEquals(LetterDeliveryState.DELIVERED, MailboxRepository.letters.value.single { it.id == "six_letter_mira_1" }.deliveryState)
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
        } finally {
            inbox.value = originalInbox
            prefs.edit().putStringSet(AiluaLocalStore.KEY_DELIVERED_LETTERS, originalReceipts).commit()
            AiluaLocalStore.loadFromDisk()
        }
    }
}
