package com.example

import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.ai.prompt.PromptCategory
import com.example.data.ai.prompt.estimateTokens
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.LoreActivationResult
import com.example.data.model.LoreEntry
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
        LoreActivationResult(
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
        // Soft reserve (P3C-2.1): phase 3 cuts even history before overflow is
        // declared, so the survivors are exactly the required blocks.
        assertEquals(
            setOf("global_system", "character_core", "post_history"),
            result.includedBlocks.map { it.id }.toSet(),
        )
        assertFalse(result.includedBlocks.any { it.id == "lore:lore1" })
        assertFalse(result.includedBlocks.any { it.id == "event:e1" })
        assertTrue(result.messages.any { it.content.contains("Character: Mira") })
        assertTrue(result.messages.any { it.content.contains("Keep replies short.") })
    }

    @Test
    fun historyFallsBelowReserveBeforeOverflow() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            activeLore = listOf(loreActivation("optional", "O".repeat(400), 10)),
            history = listOf(
                AiMessage(AiRole.USER, "u".repeat(200)),
                AiMessage(AiRole.ASSISTANT, "a".repeat(200)),
            ),
        )
        val full = PromptAssembler.assemble(input, unlimited())
        // Deficit exceeds every optional block: optionals go first (phase 2),
        // then history must fall below its 150-token reserve (phase 3) instead
        // of declaring overflow.
        val requiredCost = full.includedBlocks.filter { it.required }.sumOf { estimateTokens(it.content) }
        val budget = PromptBudget(requiredCost + 10, 150)
        val result = PromptAssembler.assemble(input, budget)

        assertFalse(result.overflow)
        assertEquals(
            setOf("global_system", "character_core", "post_history"),
            result.includedBlocks.map { it.id }.toSet(),
        )
        assertTrue(result.droppedBlocks.any { it.id == "lore:optional" })
        assertTrue(result.droppedBlocks.any { it.id == "ailua_behavior" })
        assertTrue(result.droppedBlocks.any { it.id == "history:0" })
        assertTrue(result.droppedBlocks.any { it.id == "history:1" })
    }

    @Test
    fun overflowOnlyWhenRequiredBlocksCannotFit() {
        val input = PromptAssemblyInput(
            character = miraCard(),
            activeLore = listOf(loreActivation("lore1", "Some lore.", 10)),
            history = listOf(
                AiMessage(AiRole.USER, "hello"),
                AiMessage(AiRole.ASSISTANT, "hi"),
            ),
        )
        val requiredCost = PromptAssembler
            .assemble(PromptAssemblyInput(character = miraCard()), unlimited())
            .includedBlocks.filter { it.required }.sumOf { estimateTokens(it.content) }

        // Exactly the required budget: everything else drops, no overflow.
        val exact = PromptAssembler.assemble(input, PromptBudget(requiredCost, 0))
        assertFalse(exact.overflow)
        assertEquals(
            setOf("global_system", "character_core", "post_history"),
            exact.includedBlocks.map { it.id }.toSet(),
        )

        // One token below the required set: overflow, still nothing but required.
        val impossible = PromptAssembler.assemble(input, PromptBudget(requiredCost - 1, 0))
        assertTrue(impossible.overflow)
        assertTrue(impossible.includedBlocks.all { it.required })
        assertFalse(impossible.includedBlocks.any { it.category == PromptCategory.HISTORY })
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
