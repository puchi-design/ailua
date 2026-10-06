package com.example.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.registry.CharacterRegistry
import com.example.data.systemui.notification.NotificationCategory
import com.example.data.systemui.notification.VirtualNotification

/** Presentation projection for stored snapshots. Original persisted content is never rewritten. */
object CharacterDisplayNames {
    val officialIds = setOf("hewenchuan", "zhoujianye", "peixubai", "mira", "yuna", "noa")
    private val legacyAuthorIds = mapOf("小弥" to "mira", "Mira" to "mira", "悠奈" to "yuna", "Yuna" to "yuna", "诺亚" to "noa", "Noa" to "noa")
    private val legacyMentions = mapOf("小弥" to "苏晚宁", "悠奈" to "许朝颜", "诺亚" to "宋知微")

    fun project(characterId: String, savedName: String, currentName: String?): String =
        if (characterId in officialIds && !currentName.isNullOrBlank()) currentName else savedName

    fun knownLegacyAuthorId(savedName: String): String? = legacyAuthorIds[savedName]

    /** Read-only projection for authored legacy world snapshots; never persist the result. */
    fun projectLegacyMentions(snapshotText: String): String =
        legacyMentions.entries.fold(snapshotText) { text, (oldName, publicName) ->
            text.replace(oldName, publicName)
        }
}

@Composable
fun publicCharacterName(characterId: String, savedName: String): String {
    val cards by CharacterRegistry.allCards.collectAsState()
    return CharacterDisplayNames.project(characterId, savedName, cards[characterId]?.data?.name)
}

/** Some legacy comment models have names only; map only their exact authored labels. */
@Composable
fun publicSnapshotAuthorName(savedName: String): String {
    val id = CharacterDisplayNames.knownLegacyAuthorId(savedName) ?: return savedName
    return publicCharacterName(id, savedName)
}

@Composable
fun publicNotificationTitle(notification: VirtualNotification): String {
    val id = notification.characterId ?: return notification.title
    if (id !in CharacterDisplayNames.officialIds) return notification.title
    return when (notification.category) {
        NotificationCategory.MESSAGE -> publicCharacterName(id, notification.title)
        NotificationCategory.MAIL -> "${publicCharacterName(id, notification.title.removeSuffix("寄来一封信"))}寄来一封信"
        else -> notification.title
    }
}
