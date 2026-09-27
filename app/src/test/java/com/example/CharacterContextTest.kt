package com.example

import com.example.data.context.CharacterContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * CharacterContextTest
 *
 * P3B-3 active companion contract:
 * - bootstrap default is Mira
 * - selection is observable and programmatic
 * - profiles resolve through CharacterRegistry: an unknown character ID keeps its
 *   own identity and NEVER falls back to Mira
 */
class CharacterContextTest {

    @After
    fun reset() {
        CharacterContext.select(CharacterContext.DEFAULT_CHARACTER_ID)
    }

    @Test
    fun bootstrapDefaultIsMira() {
        assertEquals("mira", CharacterContext.selectedId.value)
        assertEquals("mira", CharacterContext.currentCharacter().id)
    }

    @Test
    fun selectChangesCurrentIdAndProfile() {
        CharacterContext.select("yuna")

        assertEquals("yuna", CharacterContext.selectedId.value)
        assertEquals("yuna", CharacterContext.currentCharacter().id)
        assertEquals("yuna", CharacterContext.currentId())
    }

    @Test
    fun blankSelectionIsIgnored() {
        CharacterContext.select("noa")
        CharacterContext.select("   ")

        assertEquals("noa", CharacterContext.currentId())
    }

    @Test
    fun unknownCharacterNeverFallsBackToMira() {
        CharacterContext.select("custom_starlight")

        val profile = CharacterContext.currentCharacter()
        assertEquals("custom_starlight", profile.id)
        assertNotEquals("mira", profile.id)
        assertNotEquals("小弥", profile.name)
    }

    @Test
    fun builtinCharactersResolveThroughRegistry() {
        CharacterContext.select("noa")
        assertEquals("noa", CharacterContext.currentCharacter().id)

        CharacterContext.select("mira")
        assertEquals("mira", CharacterContext.currentCharacter().id)
    }
}
