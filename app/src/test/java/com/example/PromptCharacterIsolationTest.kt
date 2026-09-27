package com.example

import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptMemory
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.LoreEntry
import com.example.data.model.WorldBook
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptCharacterIsolationTest
 *
 * PASS 3C-2 spec §20: assembling Mira must never leak another character's
 * private life events, memories, or character-book lore into the prompt —
 * and the assembler must not scan lore on its own.
 */
class PromptCharacterIsolationTest {

    private val miraCard = CharacterCardData(
        id = "mira",
        name = "Mira",
        description = "A tea house keeper named Mira.",
        characterBook = WorldBook(
            id = "wb-mira",
            name = "Mira Book",
            description = "",
            entries = listOf(
                LoreEntry(
                    id = "yuna_secret",
                    title = "YunaSecret",
                    content = "Yuna keeps a hidden shrine.",
                    characterIds = listOf("yuna"),
                ),
            ),
        ),
    )

    private fun event(id: String, characterId: String, title: String, description: String) =
        LifeEvent(
            id = id,
            characterId = characterId,
            time = "14:00",
            type = LifeEventType.SOCIAL,
            title = title,
            description = description,
            worldDateLabel = "9月27日",
            worldMinutesOfDay = 840,
        )

    @Test
    fun otherCharactersLifeEventsNeverEnterThePrompt() {
        val input = PromptAssemblyInput(
            character = miraCard,
            recentLifeEvents = listOf(
                event("ye1", "yuna", "Yuna secret ritual", "Yuna lights incense at the shrine."),
                event("me1", "mira", "Mira opens the tea house", "Rain taps the awning."),
            ),
        )
        val result = PromptAssembler.assemble(input)

        assertFalse(result.messages.any { it.content.contains("Yuna") })
        assertFalse(result.includedBlocks.any { it.id == "event:ye1" })
        assertTrue(result.messages.any { it.content.contains("Mira opens the tea house") })
    }

    @Test
    fun memoriesOwnedByAnotherCharacterAreFiltered() {
        val input = PromptAssemblyInput(
            character = miraCard,
            memories = listOf(
                PromptMemory(id = "g1", content = "A global memory stays."),
                PromptMemory(id = "y1", content = "Yuna private memory.", characterIds = listOf("yuna")),
                PromptMemory(id = "m1", content = "Mira memory stays.", characterIds = listOf("mira")),
            ),
        )
        val result = PromptAssembler.assemble(input)

        assertTrue(result.messages.any { it.content.contains("A global memory stays.") })
        assertTrue(result.messages.any { it.content.contains("Mira memory stays.") })
        assertFalse(result.messages.any { it.content.contains("Yuna private memory.") })
        assertFalse(result.includedBlocks.any { it.id == "y1" })
    }

    @Test
    fun characterBookEntriesRenderOnlyWhenTheCallerActivatesThem() {
        // The card carries a Yuna-only lore entry in its book, but activation is
        // WorldData's job — the assembler must never scan characterBook itself.
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = miraCard, activeLore = emptyList()),
        )

        assertFalse(result.messages.any { it.content.contains("hidden shrine") })
        assertFalse(result.includedBlocks.any { it.id.startsWith("lore:") })
    }
}
