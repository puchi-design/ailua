package com.example.data.systemui.notification

import com.example.data.model.Letter
import java.util.UUID

enum class NotificationPriority { LOW, DEFAULT, HIGH }
enum class NotificationCategory { MESSAGE, MAIL, CALL, WORLD, SYSTEM }

data class VirtualNotification(
    val id: String = UUID.randomUUID().toString(),
    val sourceKey: String,
    val sourceAppId: String,
    val title: String,
    val body: String,
    val characterId: String? = null,
    val timestampEpochMs: Long,
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val category: NotificationCategory,
    val route: String? = null,
    val seen: Boolean = false,
    val dismissed: Boolean = false,
)

/** These factories project committed AILUA events; they do not generate sample events. */
object NotificationEvents {
    fun proactiveMessage(
        sessionId: String,
        turnId: String,
        characterId: String,
        characterName: String,
        content: String,
        timestampEpochMs: Long,
    ) = VirtualNotification(
        sourceKey = "message:$sessionId:$turnId",
        sourceAppId = "chat",
        title = characterName,
        body = content,
        characterId = characterId,
        timestampEpochMs = timestampEpochMs,
        category = NotificationCategory.MESSAGE,
        route = "chat/$characterId",
    )

    fun mailDelivered(letter: Letter, timestampEpochMs: Long) = VirtualNotification(
        sourceKey = "mail:${letter.id}",
        sourceAppId = "mailbox",
        title = "${letter.senderName}寄来一封信",
        body = letter.subject,
        characterId = letter.characterId,
        timestampEpochMs = timestampEpochMs,
        category = NotificationCategory.MAIL,
        route = "mailbox",
    )
}

object NotificationHeadsUpPolicy {
    fun shouldShow(
        notification: VirtualNotification,
        isLocked: Boolean,
        isShadeOpen: Boolean,
        focusMode: Boolean,
        isIncomingCall: Boolean = false,
    ): Boolean = !isLocked && !isShadeOpen && !focusMode && !isIncomingCall && !notification.dismissed &&
        (notification.priority == NotificationPriority.HIGH || notification.category == NotificationCategory.MESSAGE)
}
