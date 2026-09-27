package com.example.data.context

import com.example.data.model.CharacterProfile
import com.example.data.registry.CharacterRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CharacterContext
 *
 * The app-wide active companion selection. Every screen that is scoped to one
 * companion (Home, Living, Diary, CheckPhone, Memories, navigation targets)
 * reads the selection here instead of hardcoding Mira.
 *
 * Contract:
 * - bootstrap default is "mira" (first launch = Mira is the intended default)
 * - profiles are always resolved through CharacterRegistry; an unknown ID yields
 *   a profile derived from that ID and NEVER falls back to Mira
 * - selection is programmatic (tests and future companion-switching UI);
 *   opening a chat or profile does not silently change the active companion
 */
object CharacterContext {

    const val DEFAULT_CHARACTER_ID = "mira"

    private val _selectedId = MutableStateFlow(DEFAULT_CHARACTER_ID)
    val selectedId: StateFlow<String> = _selectedId.asStateFlow()

    fun select(characterId: String) {
        if (characterId.isBlank()) return
        _selectedId.value = characterId
    }

    fun currentId(): String = _selectedId.value

    fun currentCharacter(): CharacterProfile = CharacterRegistry.getCharacter(_selectedId.value)
}
