package com.example

import com.example.data.ai.prompt.MacroResolver
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.model.CharacterCardData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptMacroResolverTest
 *
 * PASS 3C-2 spec §14: only `{{char}} {{user}} {{date}} {{time}}` resolve;
 * unknown macros survive verbatim, values are caller-supplied (no platform
 * clock), and assembly applies resolution to every block uniformly.
 */
class PromptMacroResolverTest {

    private val resolver = MacroResolver(
        charName = "Mira",
        userName = "Alice",
        date = "9月27日",
        time = "15:40",
    )

    @Test
    fun resolvesTheFourSupportedMacros() {
        assertEquals(
            "Mira loves Alice on 9月27日 at 15:40.",
            resolver.resolve("{{char}} loves {{user}} on {{date}} at {{time}}."),
        )
    }

    @Test
    fun toleratesWhitespaceAndLetterCaseInsideBraces() {
        assertEquals("Mira / Alice", resolver.resolve("{{ CHAR }} / {{ User }}"))
        assertEquals("9月27日 15:40", resolver.resolve("{{date}} {{TIME}}"))
    }

    @Test
    fun unknownMacrosSurviveVerbatim() {
        assertEquals(
            "keep {{random}} and {{@inject}} untouched",
            resolver.resolve("keep {{random}} and {{@inject}} untouched"),
        )
    }

    @Test
    fun emptyValuesResolveToEmptyStrings() {
        val empty = MacroResolver(charName = "Mira", userName = "", date = "", time = "")
        assertEquals("[]", empty.resolve("[{{user}}]"))
        assertEquals("at ", empty.resolve("at {{time}}"))
    }

    @Test
    fun textWithoutMacrosPassesThroughUnchanged() {
        assertEquals("plain text", resolver.resolve("plain text"))
    }

    @Test
    fun assemblerResolvesMacrosInsideCardFields() {
        val card = CharacterCardData(
            id = "mira",
            name = "Mira",
            description = "{{char}} adores {{user}}.",
            postHistoryInstructions = "Never say {{date}} aloud.",
        )
        val result = PromptAssembler.assemble(
            PromptAssemblyInput(character = card, userName = "Alice", currentDate = "9月27日"),
        )

        assertTrue(result.messages.any { it.content.contains("Mira adores Alice.") })
        assertEquals("Never say 9月27日 aloud.", result.messages.last().content)
    }
}
