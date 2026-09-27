package com.example

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptCategory
import com.example.data.ai.prompt.PromptMemory
import com.example.data.model.CharacterCardData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptDedupeTest
 *
 * PASS 3C-2 spec §16: same fact reached twice must render once —
 * identical block ids collapse, trim-identical content collapses — while chat
 * history (where repeats are legitimate) is exempt from content dedupe.
 */
class PromptDedupeTest {

    private fun miraCard() = CharacterCardData(
        id = "mira",
        name = "Mira",
        description = "A tea house keeper.",
    )

    @Test
    fun duplicateBlockIdsKeepOnlyTheFirstOccurrence() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            memories = listOf(
                PromptMemory(id = "m1", content = "First memory wins."),
                PromptMemory(id = "m1", content = "Second memory loses."),
            ),
        )
        val result = PromptAssembler.assemble(input)

        assertEquals(1, result.includedBlocks.count { it.id == "m1" })
        assertEquals(1, result.droppedBlocks.count { it.id == "m1" })
        assertTrue(result.messages.any { it.content.contains("First memory wins.") })
        assertFalse(result.messages.any { it.content.contains("Second memory loses.") })
    }

    @Test
    fun identicalTrimmedContentCollapsesToOneBlock() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            memories = listOf(
                PromptMemory(id = "m2", content = "Mira likes oolong tea."),
                PromptMemory(id = "m3", content = "Mira likes oolong tea.  "),
                PromptMemory(id = "m4", content = "A different fact entirely."),
            ),
        )
        val result = PromptAssembler.assemble(input)

        val factBlocks = result.includedBlocks.filter {
            it.category == PromptCategory.MEMORY && it.id != "memory_header"
        }
        assertEquals(2, factBlocks.size)
        assertEquals(1, factBlocks.count { it.content.contains("Mira likes oolong tea.") })
        assertTrue(factBlocks.any { it.content.contains("A different fact entirely.") })
        assertTrue(result.droppedBlocks.any { it.id == "m3" })
    }

    @Test
    fun repeatedChatTurnsAreNotDeduplicated() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            history = listOf(
                AiMessage(AiRole.USER, "ok"),
                AiMessage(AiRole.ASSISTANT, "ok"),
                AiMessage(AiRole.USER, "ok"),
            ),
        )
        val result = PromptAssembler.assemble(input)

        val historyBlocks = result.includedBlocks.filter { it.category == PromptCategory.HISTORY }
        assertEquals(3, historyBlocks.size)
        assertEquals(2, result.messages.count { it.role == AiRole.USER })
        assertEquals(1, result.messages.count { it.role == AiRole.ASSISTANT })
    }
}
