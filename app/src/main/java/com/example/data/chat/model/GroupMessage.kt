package com.example.data.chat.model

data class GroupReply(val characterId: String, val content: String)

object GroupMessage {
    fun speaker(raw: String, participants: List<String>): String? =
        raw.substringBefore('\n').takeIf { it in participants }

    fun encode(reply: GroupReply, participants: List<String>): String {
        require(reply.characterId in participants && reply.content.isNotBlank())
        return "${reply.characterId}\n${reply.content.trim()}"
    }

    fun decode(raw: String, participants: List<String>): GroupReply? {
        val id = speaker(raw, participants) ?: return null
        val content = raw.substringAfter('\n', "").trim()
        return if (id in participants && content.isNotBlank()) GroupReply(id, content) else null
    }
}

object GroupSpeakerPlanner {
    /** One voice by default, a second only when the user addresses two members. */
    fun choose(text: String, participants: List<String>, names: Map<String, String>, previousAssistantCount: Int): List<String> {
        if (participants.isEmpty()) return emptyList()
        val addressed = participants.filter { id -> names[id]?.let { text.contains(it) } == true }
        if (addressed.isNotEmpty()) return addressed.take(2)
        return listOf(participants[previousAssistantCount % participants.size])
    }
}
