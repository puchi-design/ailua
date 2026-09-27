package com.example

import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptBudget
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptLifeEventIsolationTest
 *
 * PASS 3C-2 spec §12: only the assembled character's recent life events enter
 * the prompt, ordered chronologically by virtual world time, formatted solely
 * by the assembler — and under budget pressure the OLDEST event yields first.
 */
class PromptLifeEventIsolationTest {

    private val miraCard = CharacterCardData(id = "mira", name = "Mira")

    private fun event(
        id: String,
        characterId: String = "mira",
        dateLabel: String,
        minutes: Int,
        time: String,
        title: String,
        description: String,
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = LifeEventType.SOCIAL,
        title = title,
        description = description,
        worldDateLabel = dateLabel,
        worldMinutesOfDay = minutes,
    )

    @Test
    fun eventsRenderChronologicallyRegardlessOfInputOrder() {
        val day26 = event(
            id = "e-day26", dateLabel = "9月26日", minutes = 600, time = "10:00",
            title = "Yesterday tea", description = "A quiet morning.",
        )
        val day27 = event(
            id = "e-day27", dateLabel = "9月27日", minutes = 840, time = "14:00",
            title = "Today tea", description = "A rainy afternoon.",
        )
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = miraCard, recentLifeEvents = listOf(day27, day26)),
        )

        val system = result.messages.first().content
        val yesterday = system.indexOf("Yesterday tea")
        val today = system.indexOf("Today tea")
        assertTrue(yesterday in 0 until today)
    }

    @Test
    fun onlyTheAssembledCharactersEventsAreIncluded() {
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(
                character = miraCard,
                recentLifeEvents = listOf(
                    event(
                        id = "e-mira", dateLabel = "9月27日", minutes = 840, time = "14:00",
                        title = "Mira serves tea", description = "A guest arrives.",
                    ),
                    event(
                        id = "e-yuna", characterId = "yuna",
                        dateLabel = "9月27日", minutes = 900, time = "15:00",
                        title = "Yuna hidden rite", description = "Incense burns.",
                    ),
                ),
            ),
        )

        assertTrue(result.includedBlocks.any { it.id == "event:e-mira" })
        assertFalse(result.includedBlocks.any { it.id == "event:e-yuna" })
        assertFalse(result.messages.any { it.content.contains("Yuna") })
    }

    @Test
    fun oldestEventYieldsFirstUnderBudgetPressureAndHeaderStaysLast() {
        val old = event(
            id = "e-old", dateLabel = "9月26日", minutes = 600, time = "10:00",
            title = "Ancient event",
            description = "A long description of yesterday's business at the tea house.".padEnd(400, '.'),
        )
        val fresh = event(
            id = "e-new", dateLabel = "9月27日", minutes = 840, time = "14:00",
            title = "Fresh event", description = "Just happened.",
        )
        val input = PromptAssemblyInput(
            character = miraCard,
            recentLifeEvents = listOf(old, fresh),
        )
        val full = PromptAssembler.assemble(input, PromptBudget(Int.MAX_VALUE, Int.MAX_VALUE))
        val result = PromptAssembler.assemble(input, PromptBudget(full.estimatedCost - 10, Int.MAX_VALUE))

        assertEquals(listOf("event:e-old"), result.droppedBlocks.map { it.id })
        assertTrue(result.includedBlocks.any { it.id == "life_events_header" })
        assertTrue(result.includedBlocks.any { it.id == "event:e-new" })
        assertFalse(result.messages.any { it.content.contains("Ancient event") })
        assertTrue(result.messages.any { it.content.contains("Fresh event") })
    }
}
