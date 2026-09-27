package com.example

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.ai.prompt.PromptMemory
import com.example.data.model.CharacterCardData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptDeterminismTest
 *
 * PASS 3C-2 spec §20: assembling the same input twice must produce
 * byte-identical messages, block lists, cost estimates — and budget drops must
 * be reproducible too, so a prompt can be diffed and debugged.
 */
class PromptDeterminismTest {

    private fun fixture() = PromptAssemblyInput(
        character = CharacterCardData(
            id = "mira",
            name = "Mira",
            description = "A tea house keeper.",
            personality = "Warm.",
            scenario = "Rainy afternoon.",
            postHistoryInstructions = "Stay concise.",
        ),
        userPersona = "An engineer.",
        memories = listOf(
            PromptMemory(id = "m1", content = "Mira remembers your order."),
            PromptMemory(id = "m2", content = "You like oolong."),
        ),
        history = listOf(
            AiMessage(AiRole.USER, "Hello."),
            AiMessage(AiRole.ASSISTANT, "Welcome."),
            AiMessage(AiRole.USER, "Hello again."),
        ),
        currentDate = "9月27日",
        currentTime = "15:40",
        userName = "Alice",
    )

    @Test
    fun identicalInputProducesIdenticalOutput() {
        val input = fixture()
        val first = PromptAssembler.assemble(input)

        repeat(3) {
            val next = PromptAssembler.assemble(input)
            assertEquals(first.messages, next.messages)
            assertEquals(first.includedBlocks.map { it.id }, next.includedBlocks.map { it.id })
            assertEquals(first.droppedBlocks.map { it.id }, next.droppedBlocks.map { it.id })
            assertEquals(first.estimatedCost, next.estimatedCost)
            assertEquals(first.overflow, next.overflow)
        }
    }

    @Test
    fun separatelyConstructedEqualInputsProduceIdenticalOutput() {
        assertEquals(PromptAssembler.assemble(fixture()), PromptAssembler.assemble(fixture()))
    }

    @Test
    fun budgetDropsAreDeterministicAcrossRuns() {
        val input = fixture()
        val full = PromptAssembler.assemble(input, PromptBudget(Int.MAX_VALUE, Int.MAX_VALUE))
        val budget = PromptBudget(full.estimatedCost - 10, 0)

        val first = PromptAssembler.assemble(input, budget)
        val second = PromptAssembler.assemble(input, budget)

        assertTrue(first.droppedBlocks.isNotEmpty())
        assertEquals(first.droppedBlocks.map { it.id }, second.droppedBlocks.map { it.id })
        assertEquals(first.messages, second.messages)
        assertEquals(first.estimatedCost, second.estimatedCost)
    }
}
