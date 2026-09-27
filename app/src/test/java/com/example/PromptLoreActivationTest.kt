package com.example

import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.model.CharacterCardData
import com.example.data.model.LoreEntry
import com.example.data.mock.WorldData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptLoreActivationTest
 *
 * PASS 3C-2 spec §11: lore arrives pre-activated from `WorldData.getActiveLore()`
 * and renders as `[title] + content` per entry, higher effective priority first;
 * under budget pressure the lowest-priority entry yields first, while blank or
 * disabled entries never enter the stack at all.
 */
class PromptLoreActivationTest {

    private val miraCard = CharacterCardData(id = "mira", name = "Mira")

    private fun activation(title: String, content: String, priority: Int) =
        WorldData.LoreActivationResult(
            entry = LoreEntry(
                id = title.lowercase(),
                title = title,
                content = content,
                priority = priority,
            ),
            activationReasons = listOf("test"),
            effectivePriority = priority,
        )

    @Test
    fun activatedLoreRendersWithTitleAndContent() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(
                character = miraCard,
                activeLore = listOf(
                    activation("TeaHouse", "The tea house never closes before dusk.", 20),
                ),
            ),
        )

        val system = result.messages.first().content
        assertTrue(system.contains("[TeaHouse]"))
        assertTrue(system.contains("The tea house never closes before dusk."))
    }

    @Test
    fun higherEffectivePriorityRendersEarlier() {
        val low = activation("LowLore", "Low lore content.", 5)
        val high = activation("HighLore", "High lore content.", 50)
        // Deliberately pass low first — priority, not list order, decides.
        val system = PromptAssembler.assemble(
            PromptAssemblyInput(character = miraCard, activeLore = listOf(low, high)),
        ).messages.first().content

        assertTrue(system.indexOf("High lore content.") < system.indexOf("Low lore content."))
    }

    @Test
    fun lowPriorityLoreYieldsFirstUnderBudgetPressure() {
        val input = PromptAssemblyInput(
            character = miraCard,
            activeLore = listOf(
                activation("low", "L".repeat(800), 5),
                activation("high", "H".repeat(800), 50),
            ),
        )
        val full = PromptAssembler.assemble(input, PromptBudget(Int.MAX_VALUE, Int.MAX_VALUE))
        val result = PromptAssembler.assemble(input, PromptBudget(full.estimatedCost - 10, Int.MAX_VALUE))

        assertEquals(listOf("lore:low"), result.droppedBlocks.map { it.id })
        assertTrue(result.includedBlocks.any { it.id == "lore:high" })
        assertFalse(result.messages.any { it.content.contains("LLLLL") })
        assertTrue(result.messages.any { it.content.contains("HHHHH") })
    }

    @Test
    fun blankOrDisabledEntriesNeverEnterTheStack() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(
                character = miraCard,
                activeLore = listOf(
                    WorldData.LoreActivationResult(
                        entry = LoreEntry(id = "blank", title = "BlankEntry", content = "   "),
                        activationReasons = listOf("test"),
                        effectivePriority = 10,
                    ),
                    WorldData.LoreActivationResult(
                        entry = LoreEntry(
                            id = "off",
                            title = "DisabledEntry",
                            content = "Should not appear.",
                            enabled = false,
                        ),
                        activationReasons = listOf("test"),
                        effectivePriority = 10,
                    ),
                ),
            ),
        )

        assertFalse(result.messages.any { it.content.contains("BlankEntry") })
        assertFalse(result.messages.any { it.content.contains("Should not appear.") })
        assertFalse(result.includedBlocks.any { it.id.startsWith("lore:") })
        assertFalse(result.droppedBlocks.any { it.id.startsWith("lore:") })
    }
}
