package com.example

import com.example.data.chat.local.SqlDelightMemoryRepository
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.chat.memorySourceRefId
import com.example.ui.chat.memorySourceRefIdForSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ChatMemorySourceRefTest {
    @Test
    fun firstReplyAndUserRetainTheirLegacyTurnKeys() {
        val firstReply = message("turn-1", MessageSender.CHARACTER, variantIndex = 0, variantCount = 3)
        val user = message("turn-2", MessageSender.USER, variantIndex = 2, variantCount = 3)

        assertEquals("turn-1", memorySourceRefId(firstReply))
        assertEquals("turn-2", memorySourceRefId(user))
    }

    @Test
    fun regeneratedRepliesHaveDistinctStableKeys() {
        val original = message("turn-1", MessageSender.CHARACTER, variantIndex = 0)
        val regenerated = message("turn-1", MessageSender.CHARACTER, variantIndex = 1)
        val later = message("turn-1", MessageSender.CHARACTER, variantIndex = 2)

        assertNotEquals(memorySourceRefId(original), memorySourceRefId(regenerated))
        assertNotEquals(memorySourceRefId(regenerated), memorySourceRefId(later))
        assertEquals(
            memorySourceRefId(regenerated),
            memorySourceRefId(regenerated.copy(text = "重新选择的回复", variantCount = 4)),
        )
    }

    @Test
    fun originalAndRegeneratedRepliesCanBothPersistWithoutDuplicatingTheSameVariant() {
        val fixture = ChatTestHarness.inMemory()
        try {
            val memories = SqlDelightMemoryRepository(fixture.database, fixture.idGenerator, fixture.clock)
            val original = message("turn-1", MessageSender.CHARACTER, variantIndex = 0)
            val regenerated = message("turn-1", MessageSender.CHARACTER, variantIndex = 1)

            listOf(original, regenerated, regenerated).forEach { message ->
                val key = memorySourceRefIdForSave(message, memories.getMemories("mira"))
                if (key != null) memories.saveMemory("mira", message.text, "chat", key)
            }

            assertEquals(
                setOf(memorySourceRefId(original), memorySourceRefId(regenerated)),
                memories.getMemories("mira").map { it.sourceRefId }.toSet(),
            )
            assertEquals(2, memories.getMemories("mira").size)
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun legacyVariantSaveDoesNotDuplicateAndOriginalCanUseAlternateKey() {
        val fixture = ChatTestHarness.inMemory()
        try {
            val memories = SqlDelightMemoryRepository(fixture.database, fixture.idGenerator, fixture.clock)
            val regenerated = message("turn-1", MessageSender.CHARACTER, variantIndex = 1)
            val original = message("turn-1", MessageSender.CHARACTER, variantIndex = 0)
            memories.saveMemory("mira", regenerated.text, "chat", regenerated.id)

            // Before variant-aware keys, variant 1 was stored under the raw turn ID.
            assertNull(memorySourceRefIdForSave(regenerated, memories.getMemories("mira")))
            assertEquals(1, memories.getMemories("mira").size)

            val originalKey = memorySourceRefIdForSave(original, memories.getMemories("mira"))
            assertEquals("turn-1:variant:0", originalKey)
            memories.saveMemory("mira", original.text, "chat", originalKey)

            assertNull(memorySourceRefIdForSave(original, memories.getMemories("mira")))
            assertEquals(2, memories.getMemories("mira").size)
            assertEquals(
                setOf("turn-1", "turn-1:variant:0"),
                memories.getMemories("mira").map { it.sourceRefId }.toSet(),
            )
            assertEquals(
                "turn-1:variant:1",
                memorySourceRefIdForSave(regenerated.copy(text = "另一段回复"), memories.getMemories("mira")),
            )
        } finally {
            fixture.driver.close()
        }
    }

    @Test
    fun occupiedCanonicalKeyWithDifferentContentFailsInsteadOfReportingSuccess() {
        val fixture = ChatTestHarness.inMemory()
        try {
            val memories = SqlDelightMemoryRepository(fixture.database, fixture.idGenerator, fixture.clock)
            val regenerated = message("turn-1", MessageSender.CHARACTER, variantIndex = 1)
            memories.saveMemory("mira", regenerated.text, "chat", memorySourceRefId(regenerated))

            assertThrows(IllegalStateException::class.java) {
                memorySourceRefIdForSave(
                    regenerated.copy(text = "发生变化的另一段回复"),
                    memories.getMemories("mira"),
                )
            }
            assertNull(memorySourceRefIdForSave(regenerated, memories.getMemories("mira")))

            memories.saveMemory("mira", "旧版回复", "chat", "turn-1")
            memories.saveMemory("mira", "另一段原始回复", "chat", "turn-1:variant:0")
            assertThrows(IllegalStateException::class.java) {
                memorySourceRefIdForSave(
                    message("turn-1", MessageSender.CHARACTER, variantIndex = 0),
                    memories.getMemories("mira"),
                )
            }
            assertEquals(3, memories.getMemories("mira").size)
        } finally {
            fixture.driver.close()
        }
    }

    private fun message(
        id: String,
        sender: MessageSender,
        variantIndex: Int,
        variantCount: Int = 1,
    ) = ChatMessage(
        id = id,
        sender = sender,
        text = "第 $variantIndex 版回复",
        timestamp = "12:00",
        variantIndex = variantIndex,
        variantCount = variantCount,
    )
}
