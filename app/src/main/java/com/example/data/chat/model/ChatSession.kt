package com.example.data.chat.model

/**
 * ChatSession — the canonical PRIVATE chat session for one character (P3C-3 §5).
 *
 * Exactly one persistent session exists per characterId (the earliest-created
 * row is canonical, chosen by [com.example.data.chat.repository.ChatRepository.getOrCreatePrivateSession]).
 * Group chat and branching are out of scope for this pass; private sessions are
 * completely isolated from one another.
 *
 * Pure Kotlin on purpose: no Android, no Room, no driver types — this model is
 * the KMP-ready layer and only epoch milliseconds are used for time.
 */
data class ChatSession(
    val id: String,
    val characterId: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val type: ChatSessionType get() = if (GroupChatIdentity.isGroup(characterId)) ChatSessionType.GROUP else ChatSessionType.PRIVATE
    val participantCharacterIds: List<String> get() = GroupChatIdentity.participants(characterId)
}
