package com.example.data.model

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
    val category: String = "routine"
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
    val bondLevel: Int,
    val bondName: String,
    val bondProgress: Int, // 0 - 100
    val daysTogether: Int,
    val energyLevel: Int, // 0 - 100
    val personalityTags: List<String>,
    val memories: List<MemorySnippet>,
    val timeline: List<TimelineEvent>
)
