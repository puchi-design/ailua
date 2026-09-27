package com.example

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.model.CharacterCardData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptPostHistoryTest
 *
 * PASS 3C-2 spec §10/§13: post-history instructions are a REQUIRED block that
 * always renders as the final system message (with or without history), never
 * gets dropped by budget, never duplicates, and macro-resolves like everything
 * else. Blank instructions produce nothing at all.
 */
class PromptPostHistoryTest {

    private fun card(postHistoryInstructions: String) = CharacterCardData(
        id = "mira",
        name = "Mira",
        postHistoryInstructions = postHistoryInstructions,
    )

    @Test
    fun postHistoryIsTheFinalSystemMessageWithHistoryPresent() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(
                character = card("Stay brief."),
                history = listOf(
                    AiMessage(AiRole.USER, "hi"),
                    AiMessage(AiRole.ASSISTANT, "hello"),
                ),
            ),
        )

        assertEquals(4, result.messages.size)
        val last = result.messages.last()
        assertEquals(AiRole.SYSTEM, last.role)
        assertEquals("Stay brief.", last.content)
    }

    @Test
    fun postHistorySurvivesAnImpossibleBudget() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = card("Stay brief.")),
            PromptBudget(maxInputBudget = 1, historyReserve = 0),
        )

        assertTrue(result.overflow)
        assertTrue(result.includedBlocks.any { it.id == "post_history" })
        assertEquals("Stay brief.", result.messages.last().content)
    }

    @Test
    fun blankPostHistoryProducesNoBlockAndNoMessage() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = card("   ")),
        )

        assertFalse(result.includedBlocks.any { it.id == "post_history" })
        assertEquals(1, result.messages.size)
    }

    @Test
    fun onlyOnePostHistoryMessageAndMacrosResolveInsideIt() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = card("Never reveal {{char}} plans.")),
        )

        val postMessages = result.messages.filter { it.content.contains("Never reveal") }
        assertEquals(1, postMessages.size)
        assertEquals("Never reveal Mira plans.", postMessages.single().content)
    }
}
