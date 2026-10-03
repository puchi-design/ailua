package com.example

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.model.CharacterCardData
import com.example.data.projection.CharacterPresence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptEmptyBlockTest
 *
 * PASS 3C-2 spec §20: empty or blank inputs never produce empty messages —
 * no blank system stubs, no orphan section headers, no whitespace-only turns.
 * Blank history turns are still reported as dropped so debugging stays honest.
 */
class PromptEmptyBlockTest {

    @Test
    fun emptyInputProducesASingleNonBlankSystemMessage() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = CharacterCardData(name = "Nami")),
        )

        assertEquals(1, result.messages.size)
        assertEquals(AiRole.SYSTEM, result.messages[0].role)
        assertTrue(result.messages[0].content.startsWith(PromptAssembler.DEFAULT_GLOBAL_SYSTEM + "\n\nCharacter: Nami"))
        assertEquals(setOf("global_system", "character_core", "ailua_behavior"), result.includedBlocks.map { it.id }.toSet())
        assertTrue(result.messages[0].content.contains("当前未启用恋爱路线"))
        assertTrue(result.messages.all { it.content.isNotBlank() })
    }

    @Test
    fun blankOptionalSectionsAddNoMessages() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(
                character = CharacterCardData(name = "Nami"),
                userPersona = "   ",
                worldState = CharacterPresence("nami", "", "", null),
                currentDate = "  ",
                currentTime = "",
                userName = "   ",
            ),
        )

        assertEquals(1, result.messages.size)
        val system = result.messages.first().content
        assertFalse(system.contains("Current world state"))
        assertFalse(system.contains("Current date"))
        assertFalse(system.contains("Current time"))
        assertFalse(system.contains("User:"))
    }

    @Test
    fun blankHistoryTurnsNeverBecomeMessagesButAreReportedAsDropped() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(
                character = CharacterCardData(name = "Nami"),
                history = listOf(
                    AiMessage(AiRole.USER, "   "),
                    AiMessage(AiRole.ASSISTANT, "Real reply."),
                ),
            ),
        )

        assertTrue(result.messages.all { it.content.isNotBlank() })
        assertFalse(result.messages.any { it.role == AiRole.USER })
        assertEquals(listOf("history:0"), result.droppedBlocks.map { it.id })
        assertTrue(result.messages.any { it.content == "Real reply." })
    }
}
