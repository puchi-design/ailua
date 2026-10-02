package com.example.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.ContactItem
import com.example.data.model.LifeEventType
import com.example.data.model.isUserActivity
import com.example.data.projection.projectPresence
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.registry.CharacterRegistry
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.AiluaSectionHeader
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme

@Composable
fun ContactsScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onStartPrivateChat: (String) -> Unit = {},
    onOpenRelations: () -> Unit = {},
    onGoHome: () -> Unit = onBackToHome,
) {
    val allRegisteredCharacters = CharacterRegistry.getAllCharacters()
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val clock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val relations by RelationshipStateRepository.states.collectAsStateWithLifecycle()
    val contacts = allRegisteredCharacters.map { profile ->
        val presence = projectPresence(profile, worldEvents)
        val latest = WorldStateRepository.eventsForCharacter(profile.id).lastOrNull { !it.isUserActivity() }
        val relation = relations.firstOrNull { setOf(it.fromCharacterId, it.toCharacterId) == setOf("user", profile.id) }
        val minutesAgo = if (latest?.worldDateLabel == clock.dateLabel) clock.minutesOfDay - latest.worldMinutesOfDay else -1
        ContactItem(
            id = "c_${profile.id}",
            characterId = profile.id,
            name = profile.name,
            englishName = profile.englishName,
            avatarId = profile.avatarId,
            shortStatus = "${presence.currentLocation} · ${presence.currentActivity} · ${if (minutesAgo >= 0) "${minutesAgo}分钟前" else latest?.time ?: "暂无动态"}",
            relationshipType = relation?.stage?.name ?: "初识",
            relationshipLevel = relation?.affinity ?: 0,
            lastActivity = if (minutesAgo >= 0) "${minutesAgo}分钟前" else latest?.time ?: "暂无动态",
            unreadCount = 0,
            onlineState = if (latest?.type == LifeEventType.SLEEP) "休息中" else if (latest != null) "生活中" else "暂无动态"
        )
    }
    val primaryCompanion = contacts.firstOrNull { it.characterId == "mira" }
    val otherCharacters = contacts.filter { it.characterId != "mira" }

    val theme = LocalAiluaTheme.current
    AiluaScreenScaffold(
        title = "联系人", onBack = onBackToHome, onGoHome = onGoHome,
        modifier = Modifier.testTag("contacts_screen"), backTestTag = "contacts_back_btn",
        trailing = { TextButton(onClick = onOpenRelations) { Text("关系", style = theme.text.secondary) } },
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = theme.layout.screenHorizontalPadding.dp),
        ) {
            if (primaryCompanion != null) {
                item { AiluaSectionHeader("常用联系人", modifier = Modifier.padding(top = 12.dp)) }
                item(key = primaryCompanion.id) {
                    ContactRowItem(primaryCompanion,
                        onOpenProfile = { onOpenProfile(primaryCompanion.characterId) },
                        onStartChat = { onStartPrivateChat(primaryCompanion.characterId) })
                }
            }
            if (otherCharacters.isNotEmpty()) {
                item { AiluaSectionHeader("其他联系人", modifier = Modifier.padding(top = theme.layout.sectionGap.dp)) }
                items(otherCharacters, key = { it.id }) { contact ->
                    ContactRowItem(contact, onOpenProfile = { onOpenProfile(contact.characterId) },
                        onStartChat = { onStartPrivateChat(contact.characterId) })
                }
            }
        }
    }
}

@Composable
private fun ContactRowItem(contact: ContactItem, onOpenProfile: () -> Unit, onStartChat: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenProfile)
                .heightIn(min = 78.dp).padding(vertical = 12.dp)
                .testTag("contact_item_${contact.characterId}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            CharacterPortrait(contact.characterId, PortraitVariant.AVATAR, Modifier.size(44.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(contact.name, style = theme.text.body, color = theme.palette.onSurface)
                Text(contact.shortStatus, style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onStartChat) { Text("消息", style = theme.text.secondary, color = theme.palette.onSurface) }
        }
        HorizontalDivider(color = theme.surfaces.divider)
    }
}
