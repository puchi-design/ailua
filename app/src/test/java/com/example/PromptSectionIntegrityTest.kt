package com.example

import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.ai.prompt.PromptMemory
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptSectionIntegrityTest
 *
 * PASS 3C-2.1 orphan-header rule: `Relevant memories:` / `Recent world events:`
 * must never render headless. A header drops together with the last surviving
 * entry of its section, and whenever the section survives so does its header.
 */
class PromptSectionIntegrityTest {

    private val miraCard = CharacterCardData(id = "mira", name = "Mira")

    private fun unlimited(): PromptBudget = PromptBudget(Int.MAX_VALUE, Int.MAX_VALUE)

    @Test
    fun memoryHeaderDroppedWhenAllMemoriesDropped() {
        val input = PromptAssemblyInput(
            character = miraCard,
            memories = listOf(PromptMemory(id = "m1", content = "M".repeat(400))),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        // Deficit smaller than the entry (500) but far bigger than the header
        // (510): the budget removes the entry and would strand the header.
        val result = PromptAssembler.assemble(input, PromptBudget(full.estimatedCost - 10, Int.MAX_VALUE))

        assertFalse(result.includedBlocks.any { it.id == "m1" })
        assertFalse(result.includedBlocks.any { it.id == "memory_header" })
        assertTrue(result.droppedBlocks.any { it.id == "m1" })
        assertTrue(result.droppedBlocks.any { it.id == "memory_header" })
        assertFalse(result.messages.any { it.content.contains("Relevant memories:") })
    }

    @Test
    fun lifeEventHeaderDroppedWhenAllEventsDropped() {
        val input = PromptAssemblyInput(
            character = miraCard,
            recentLifeEvents = listOf(
                LifeEvent(
                    id = "e1",
                    characterId = "mira",
                    time = "14:00",
                    type = LifeEventType.SOCIAL,
                    title = "Tea served",
                    description = "D".repeat(400),
                    worldDateLabel = "9月27日",
                    worldMinutesOfDay = 840,
                ),
            ),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        val result = PromptAssembler.assemble(input, PromptBudget(full.estimatedCost - 10, Int.MAX_VALUE))

        assertFalse(result.includedBlocks.any { it.id == "event:e1" })
        assertFalse(result.includedBlocks.any { it.id == "life_events_header" })
        assertTrue(result.droppedBlocks.any { it.id == "event:e1" })
        assertTrue(result.droppedBlocks.any { it.id == "life_events_header" })
        assertFalse(result.messages.any { it.content.contains("Recent world events:") })
    }

    @Test
    fun headerAndAtLeastOneEntryRemainTogetherWhenSectionSurvives() {
        val input = PromptAssemblyInput(
            character = miraCard,
            memories = listOf(
                PromptMemory(id = "m1", content = "F".repeat(400)),
                PromptMemory(id = "m2", content = "The surviving memory."),
            ),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        // Deficit sits between m2's cost and m1's cost: only m1 drops, and the
        // section must keep header + m2 together.
        val result = PromptAssembler.assemble(input, PromptBudget(full.estimatedCost - 40, Int.MAX_VALUE))

        assertTrue(result.includedBlocks.any { it.id == "memory_header" })
        assertTrue(result.includedBlocks.any { it.id == "m2" })
        assertFalse(result.includedBlocks.any { it.id == "m1" })

        val system = result.messages.first().content
        assertTrue(system.contains("Relevant memories:"))
        assertTrue(system.contains("The surviving memory."))
        assertTrue(system.indexOf("Relevant memories:") < system.indexOf("The surviving memory."))
        assertFalse(system.contains("F".repeat(50)))
    }
}
