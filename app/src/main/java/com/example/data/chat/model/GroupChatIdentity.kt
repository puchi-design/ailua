package com.example.data.chat.model

enum class ChatSessionType { PRIVATE, GROUP }

/** Group metadata is encoded in the existing session key, avoiding a second store or schema migration. */
object GroupChatIdentity {
    private const val PREFIX = "__ailua_group__:"
    fun key(groupId: String, participants: List<String>): String {
        require(groupId.matches(Regex("[a-z0-9_]+")))
        require(participants.size in 2..8 && participants.distinct().size == participants.size)
        require(participants.all { it.matches(Regex("[a-z0-9_]+")) })
        return "$PREFIX$groupId:${participants.joinToString(",")}"
    }
    fun participants(key: String): List<String> =
        if (key.startsWith(PREFIX)) key.substringAfterLast(':').split(',').filter { it.isNotBlank() } else emptyList()
    fun isGroup(key: String): Boolean = key.startsWith(PREFIX)
}
