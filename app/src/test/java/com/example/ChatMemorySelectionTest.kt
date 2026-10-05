package com.example

import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.chat.latestSaveableChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatMemorySelectionTest {
    @Test
    fun selectsLatestCompletedUserOrCharacterMessage() {
        val earlier = message("user-1", MessageSender.USER, "今天有件开心事")
        val latest = message("character-1", MessageSender.CHARACTER, "说给我听听")

        assertEquals(latest, latestSaveableChatMessage(listOf(earlier, latest)))
        assertEquals(earlier, latestSaveableChatMessage(listOf(earlier)))
    }

    @Test
    fun skipsSystemBlankAndUnfinishedMessages() {
        val valid = message("user-1", MessageSender.USER, "值得记住")
        val messages = listOf(
            valid,
            message("system-1", MessageSender.SYSTEM, "系统提示"),
            message("character-blank", MessageSender.CHARACTER, "   "),
            message("character-failed", MessageSender.CHARACTER, "片段", "FAILED"),
            message("character-cancelled", MessageSender.CHARACTER, "片段", "CANCELLED"),
            message("", MessageSender.USER, "没有持久 ID"),
        )

        assertEquals(valid, latestSaveableChatMessage(messages))
        assertNull(latestSaveableChatMessage(messages.drop(1)))
        assertNull(latestSaveableChatMessage(emptyList()))
    }

    private fun message(
        id: String,
        sender: MessageSender,
        text: String,
        statusLabel: String? = null,
    ) = ChatMessage(id = id, sender = sender, text = text, timestamp = "12:00", statusLabel = statusLabel)
}
