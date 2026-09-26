package com.example.data.model

import kotlinx.serialization.Serializable

enum class MessageSender {
    USER,
    CHARACTER,
    SYSTEM
}

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    ACTION_NARRATIVE,
    MEMORY_CARD
}

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val senderCharacterId: String = "mira",
    val senderName: String = "Mira",
    val type: MessageType = MessageType.TEXT,
    val text: String,
    val timestamp: String,
    val voiceDurationSeconds: Int = 0,
    val imageDescription: String = "",
    val reactions: List<String> = emptyList(),
    val isSavedToMemory: Boolean = false,
    val memoryTag: String = ""
)

data class MomentComment(
    val id: String,
    val author: String,
    val isUser: Boolean = false,
    val content: String,
    val timestamp: String
)

data class MomentPost(
    val id: String,
    val authorId: String = "mira",
    val authorName: String,
    val timestamp: String,
    val moodTag: String,
    val locationContext: String,
    val content: String,
    val imageType: String = "",
    val likesCount: Int,
    val isLiked: Boolean = false,
    val comments: List<MomentComment> = emptyList()
)

data class TimelineEvent(
    val id: String,
    val time: String,
    val title: String,
    val description: String,
    val isCurrent: Boolean = false,
    val category: String = "routine",
    val location: String? = null,
    val mood: String? = null,
    val relatedCharacterIds: List<String> = emptyList()
)

data class MemorySnippet(
    val id: String,
    val title: String,
    val snippet: String,
    val date: String,
    val tag: String,
    val resonanceLevel: Int = 3
)

enum class AppStatus {
    AVAILABLE,
    BETA,
    COMING_SOON,
    PREVIEW
}

data class AiluaApp(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val status: AppStatus = AppStatus.AVAILABLE,
    val badge: String? = null,
    val iconKey: String,
    val route: String? = null
)

// === Proactive Life Pulse & Unified Engine ===

@Serializable
enum class LifeEventType {
    WAKE_UP,
    MEAL,
    TRAVEL,
    THOUGHT,
    MEMORY,
    SOCIAL,
    PHOTO,
    MOMENT,
    MESSAGE,
    DIARY,
    SLEEP,
    SURPRISE,
    LOCATION_CHANGE
}

@Serializable
data class LifeEvent(
    val id: String,
    val characterId: String,
    val time: String,
    val type: LifeEventType,
    val title: String,
    val description: String,
    val location: String? = null,
    val visibility: String = "PUBLIC",
    val relatedCharacterIds: List<String> = emptyList(),
    val imageReference: String? = null
)

// === Multi-Character & Contacts Models ===

data class CharacterProfile(
    val id: String,
    val name: String,
    val englishName: String,
    val title: String,
    val bio: String,
    val currentActivity: String,
    val mood: String,
    val location: String,
    val contextualQuote: String,
    val bondLevel: Int = 1,
    val bondName: String = "知交初契",
    val bondProgress: Int = 20, // 0 - 100
    val daysTogether: Int = 1,
    val energyLevel: Int = 85, // 0 - 100
    val personalityTags: List<String> = emptyList(),
    val memories: List<MemorySnippet> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val avatarId: String = "mira",
    val isOnline: Boolean = true,
    val relationshipType: String = "伴生心契"
)

data class ContactItem(
    val id: String,
    val characterId: String,
    val name: String,
    val englishName: String,
    val avatarId: String,
    val shortStatus: String,
    val relationshipType: String,
    val relationshipLevel: Int,
    val lastActivity: String,
    val unreadCount: Int = 0,
    val onlineState: String = "心网在线"
)

// === Conversation List & Group Chat Models ===

enum class ConversationType {
    PRIVATE,
    GROUP
}

data class Conversation(
    val id: String,
    val type: ConversationType,
    val title: String,
    val characterId: String? = null,
    val memberIds: List<String> = emptyList(),
    val latestMessage: String,
    val latestTime: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val avatarId: String? = null,
    val characterStatus: String? = null
)

// === Social Graph / Relations Model ===

data class RelationLink(
    val id: String,
    val fromCharacterId: String,
    val toCharacterId: String,
    val fromName: String,
    val toName: String,
    val relationshipLabel: String,
    val closeness: Int, // 0 - 100
    val recentInteraction: String,
    val sharedMemory: String
)

// === Check Phone (查手机) Models ===

data class MusicTrack(
    val title: String,
    val artist: String,
    val albumCoverType: String,
    val duration: String,
    val isPlaying: Boolean = false
)

data class PrivatePhoto(
    val title: String,
    val time: String,
    val imageType: String,
    val note: String
)

data class CheckPhoneData(
    val searchHistory: List<String>,
    val unsentDrafts: List<String>,
    val notes: List<String>,
    val recentlyPlayed: List<MusicTrack>,
    val privateGallery: List<PrivatePhoto>,
    val browsingHistory: List<String>,
    val savedItems: List<String>,
    val hiddenThoughts: List<String>
)

// === Diary (日记) Model ===

data class DiaryEntry(
    val id: String,
    val characterId: String,
    val authorName: String,
    val date: String,
    val weather: String,
    val mood: String,
    val title: String,
    val content: String,
    val excerpt: String,
    val imageReference: String? = null,
    val relatedMemoryIds: List<String> = emptyList()
)
