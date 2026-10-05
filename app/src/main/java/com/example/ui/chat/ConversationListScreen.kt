package com.example.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.GroupMessage
import com.example.data.chat.model.VariantStatus
import com.example.data.engine.WorldStateRepository
import com.example.data.registry.CharacterRegistry
import com.example.data.context.CharacterContext
import com.example.data.projection.projectPresence
import com.example.data.model.Conversation
import com.example.data.model.ConversationType
import com.example.ui.designsystem.AiluaScreenScaffold
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationListScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenMiraChat: () -> Unit = {},
    onOpenGroupChat: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onSelectCharacterChat: (String) -> Unit = {},
    onGoHome: () -> Unit = onBackToHome,
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val selectedCharacterId by CharacterContext.selectedId.collectAsStateWithLifecycle()
    val registeredCards by CharacterRegistry.allCards.collectAsStateWithLifecycle()
    val conversations by produceState<List<Conversation>>(initialValue = emptyList(), worldEvents, selectedCharacterId, registeredCards) {
        value = withContext(Dispatchers.IO) {
            val driver = ChatDriverFactory(context.applicationContext).createDriver()
            try {
                val repository = SqlDelightChatRepository(ChatDatabase(driver), UuidIdGenerator(), SystemEpochClock())
                val privateConversations = CharacterRegistry.getAllCharacters().map { character ->
                    val session = repository.getOrCreatePrivateSession(character.id)
                    val latest = repository.getResolvedTurns(session.id).lastOrNull {
                        val variant = it.activeVariant
                        variant?.status == VariantStatus.COMPLETE && !variant.content.isNullOrBlank()
                    }
                    val presence = projectPresence(character, worldEvents)
                    Conversation(
                        id = "conv_${character.id}", type = ConversationType.PRIVATE,
                        title = character.name, characterId = character.id,
                        latestMessage = latest?.activeVariant?.content?.replace('\n', ' ')?.take(100)
                            ?: "尚无对话 · ${presence.currentActivity}",
                        latestTime = latest?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.createdAtEpochMs)) }.orEmpty(),
                        isPinned = character.id == selectedCharacterId, avatarId = character.id,
                        characterStatus = presence.currentActivity,
                    )
                }
                val groupSession = repository.getOrCreateGroupSession(
                    GroupChatViewModel.GROUP_ID, GroupChatViewModel.PARTICIPANTS)
                val latestGroupTurn = repository.getResolvedTurns(groupSession.id).lastOrNull {
                    it.activeVariant?.let { variant ->
                        variant.status == VariantStatus.COMPLETE && variant.content.isNotBlank()
                    } == true
                }
                val groupPreview = latestGroupTurn?.let { turn ->
                    val content = turn.activeVariant!!.content
                    val readable = if (turn.role == ChatTurnRole.ASSISTANT) {
                        GroupMessage.decode(content, GroupChatViewModel.PARTICIPANTS)?.content ?: content
                    } else content
                    readable.replace('\n', ' ').take(100)
                } ?: "还没有群聊消息"
                privateConversations + Conversation(
                    id = "conv_group", type = ConversationType.GROUP, title = "雨夜茶会",
                    latestMessage = groupPreview,
                    latestTime = latestGroupTurn?.let {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.createdAtEpochMs))
                    }.orEmpty(),
                )
            } finally { driver.close() }
        }
    }

    val filteredConversations = conversations.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.latestMessage.contains(searchQuery, ignoreCase = true)
    }

    val pinnedList = filteredConversations.filter { it.isPinned }
    val recentList = filteredConversations.filter { !it.isPinned }

    val theme = LocalAiluaTheme.current
    AiluaScreenScaffold(
        title = "消息", onBack = onBackToHome, onGoHome = onGoHome,
        modifier = Modifier.testTag("conversation_list_screen"), backTestTag = "messages_back_btn",
        trailing = { TextButton(onClick = onOpenContacts) { Text("联系人", style = theme.text.secondary) } },
    ) {
        TextField(
            value = searchQuery, onValueChange = { searchQuery = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 8.dp),
            placeholder = { Text("搜索", style = theme.text.body) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            textStyle = theme.text.body,
            shape = RoundedCornerShape(theme.shapes.medium.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.surfaces.inset, unfocusedContainerColor = theme.surfaces.inset,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp)) {
            items(pinnedList + recentList, key = { it.id }) { conv ->
                ConversationItemCard(conv, conv.isPinned) {
                    if (conv.type == ConversationType.GROUP) onOpenGroupChat()
                    else if (conv.id == "conv_mira" || conv.characterId == "mira") onOpenMiraChat()
                    else onSelectCharacterChat(conv.characterId ?: selectedCharacterId)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ConversationItemCard(conversation: Conversation, isPinned: Boolean, onClick: () -> Unit) {
    val theme = LocalAiluaTheme.current
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 78.dp)
                .padding(vertical = 12.dp).testTag("conv_item_${conversation.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(theme.layout.itemGap.dp),
        ) {
            CharacterPortrait(conversation.avatarId ?: if (conversation.type == ConversationType.GROUP) "group_tea" else "mira",
                PortraitVariant.AVATAR, Modifier.size(46.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(conversation.title, style = theme.text.body, color = theme.palette.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (isPinned) Icon(Icons.Default.PushPin, "已置顶", Modifier.size(12.dp), tint = theme.palette.onSurfaceMuted)
                }
                Text(conversation.latestMessage, style = theme.text.secondary, color = theme.palette.onSurfaceMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(conversation.latestTime, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                if (conversation.unreadCount > 0) {
                    Text("● ${conversation.unreadCount}", style = theme.text.caption, color = theme.palette.accent)
                }
            }
        }
        HorizontalDivider(color = theme.surfaces.divider)
    }
}
