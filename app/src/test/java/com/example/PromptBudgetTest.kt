package com.example

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.ai.prompt.estimateTokens
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.LoreEntry
import com.example.data.mock.WorldData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptBudgetTest
 *
 * PASS 3C-2 spec §15: one budget controls the whole prompt.
 * - required blocks (global system, character core, post-history) never drop;
 * - oldest history yields first, but only down to `historyReserve`;
 * - after that the lowest-priority optional blocks yield (lore before events);
 * - content is NEVER substring-mangled — blocks drop whole or survive whole;
 * - an impossible budget keeps the required set and reports `overflow`.
 */
class PromptBudgetTest {

    private fun miraCard(description: String = "A tea house keeper.") = CharacterCardData(
        id = "mira",
        name = "Mira",
        description = description,
        personality = "Warm.",
        scenario = "Rainy afternoon.",
        postHistoryInstructions = "Keep replies short.",
    )

    private fun loreActivation(id: String, content: String, priority: Int) =
        WorldData.LoreActivationResult(
            entry = LoreEntry(id = id, title = id, content = content, priority = priority),
            activationReasons = listOf("test"),
            effectivePriority = priority,
        )

    private fun event(id: String, title: String, description: String) = LifeEvent(
        id = id,
        characterId = "mira",
        time = "14:00",
        type = LifeEventType.SOCIAL,
        title = title,
        description = description,
        worldDateLabel = "9月27日",
        worldMinutesOfDay = 840,
    )

    private fun unlimited(): PromptBudget = PromptBudget(Int.MAX_VALUE, Int.MAX_VALUE)

    @Test
    fun oldestHistoryIsDroppedFirstAndRespectTheReserve() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            history = listOf(
                // 1200 chars ≈ 300 tokens
                AiMessage(AiRole.USER, "oldest-history-marker " + "x".repeat(1178)),
                // 400 chars ≈ 100 tokens
                AiMessage(AiRole.ASSISTANT, "newest-history-marker " + "y".repeat(378)),
            ),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        // Over by 50 tokens; reserve 150 → the 300-token turn goes, the 100-token one stays.
        val budget = PromptBudget(full.estimatedCost - 50, 150)
        val result = PromptAssembler.assemble(input, budget)

        assertFalse(result.overflow)
        assertEquals(listOf("history:0"), result.droppedBlocks.map { it.id })
        assertFalse(result.messages.any { it.content.contains("oldest-history-marker") })
        assertTrue(result.messages.any { it.content.contains("newest-history-marker") })
    }

    @Test
    fun loreYieldsBeforeLifeEvents() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            activeLore = listOf(loreActivation("biglore", "L".repeat(4000), 10)),
            recentLifeEvents = listOf(event("e1", "Tea served", "A guest ordered oolong.")),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        // Reserve far above any history → phase two must cut lore (310) before events (400).
        val budget = PromptBudget(full.estimatedCost - 10, Int.MAX_VALUE)
        val result = PromptAssembler.assemble(input, budget)

        assertFalse(result.overflow)
        assertEquals(listOf("lore:biglore"), result.droppedBlocks.map { it.id })
        assertFalse(result.messages.any { it.content.contains("LLLLL") })
        assertTrue(result.messages.any { it.content.contains("Tea served") })
    }

    @Test
    fun requiredBlocksSurviveAnImpossibleBudgetAndFlagOverflow() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            activeLore = listOf(loreActivation("lore1", "Some lore.", 10)),
            recentLifeEvents = listOf(event("e1", "Tea served", "Guest arrived.")),
            history = listOf(
                AiMessage(AiRole.USER, "hello"),
                AiMessage(AiRole.ASSISTANT, "hi"),
            ),
        )
        val result = PromptAssembler.assemble(input, PromptBudget(5, 2500))

        assertTrue(result.overflow)
        val ids = result.includedBlocks.map { it.id }
        assertTrue("global_system must survive", ids.contains("global_system"))
        assertTrue("character_core must survive", ids.contains("character_core"))
        assertTrue("post_history must survive", ids.contains("post_history"))
        assertTrue("history within reserve must survive", ids.containsAll(listOf("history:0", "history:1")))
        assertFalse(ids.contains("lore:lore1"))
        assertFalse(ids.contains("event:e1"))
        assertTrue(result.messages.any { it.content.contains("Character: Mira") })
        assertTrue(result.messages.any { it.content.contains("Keep replies short.") })
    }

    @Test
    fun budgetNeverSubstringManglesContent() {
        val longDescription = "MiraDescription " + "abcdefghijklmnopqrstuvwxyz".repeat(30)
        val input = PromptAssemblyInput(
            character = miraCard(description = longDescription),
            activeLore = listOf(
                loreActivation("cutcheck", "LoreContentMarker " + "0123456789".repeat(200), 10),
            ),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        val result = PromptAssembler.assemble(input, PromptBudget(full.estimatedCost - 5, 0))

        assertFalse(result.overflow)
        assertEquals(listOf("lore:cutcheck"), result.droppedBlocks.map { it.id })
        // The required character description survives byte-for-byte — no truncation.
        assertTrue(result.messages.any { it.content.contains(longDescription) })
        assertTrue(result.messages.none { it.content.contains("…") })
    }

    @Test
    fun estimatedCostEqualsTheSumOfIncludedBlocks() {
        val result = PromptAssembler.assemble(PromptAssemblyInput(character = miraCard()))

        val expected = result.includedBlocks.sumOf { estimateTokens(it.content) }
        assertEquals(expected, result.estimatedCost)
        assertTrue(result.includedBlocks.isNotEmpty())
        assertTrue(result.droppedBlocks.isEmpty())
    }
}
