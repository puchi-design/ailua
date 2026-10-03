package com.example.data.registry

import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.mock.OfficialCharacters
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * CharacterRegistry
 *
 * Single source of truth for all AILUA companion profiles.
 * Integrates the official romance catalog, optional legacy characters
 * with user-imported and custom Character Card V2 characters.
 */
object CharacterRegistry {

    const val DEFAULT_CHARACTER_ID = "yan"
    val OFFICIAL_ROMANCE_IDS: List<String> = OfficialCharacters.romanceIds

    // Built-in cards mapped to CharacterCard models
    private val builtInCards = WorldData.allCards.associateBy { it.data.id }

    // Dynamic registry combining built-ins and custom cards
    private val _allCards = MutableStateFlow<Map<String, CharacterCard>>(builtInCards)
    val allCards: StateFlow<Map<String, CharacterCard>> = _allCards.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val merged = builtInCards.toMutableMap()
        AiluaLocalStore.customCards.value.forEach { card ->
            val id = card.data.id.ifBlank { card.data.name.lowercase().replace(" ", "_") }
            merged[id] = card.copy(data = card.data.copy(id = id))
        }
        _allCards.value = merged
    }

    /**
     * Resolves CharacterProfile for any character ID (built-in or imported).
     * If not found, generates a fallback profile from ID instead of silently reverting to Mira.
     */
    fun getCharacter(characterId: String): CharacterProfile {
        refresh()
        return resolveProfile(characterId)
    }

    private fun resolveProfile(characterId: String): CharacterProfile {
        // A saved/imported edit wins even when it retains a built-in ID.
        val saved = AiluaLocalStore.customCards.value.lastOrNull { card ->
            card.data.id.ifBlank { card.data.name.lowercase().replace(" ", "_") } == characterId
        }
        if (saved != null) {
            return WorldData.cardToProfile(saved.copy(data = saved.data.copy(id = characterId)))
        }
        // Unedited built-ins retain their authored timelines and legacy memories.
        MockData.allCharacters[characterId]?.let { return it }

        // 2. Check registered cards (e.g. Luna or custom imported)
        val card = _allCards.value[characterId]
        if (card != null) {
            return WorldData.cardToProfile(card)
        }

        // 3. Fallback: construct profile based on the requested ID
        return CharacterProfile(
            id = characterId,
            name = characterId.replaceFirstChar { it.uppercase() },
            englishName = characterId.replaceFirstChar { it.uppercase() },
            title = "自定义角色",
            bio = "尚未添加角色资料。",
            currentActivity = "暂无近况",
            mood = "平静",
            location = "尚未设定",
            contextualQuote = "很高兴在 AILUA 与你相遇。",
            avatarId = characterId,
            relationshipType = "初识"
        )
    }

    fun getCard(characterId: String): CharacterCard? {
        refresh()
        return _allCards.value[characterId]
    }

    /** Saved built-in edits must not inherit the original persona through the shared world book. */
    fun isUnmodifiedBuiltIn(characterId: String): Boolean =
        characterId in builtInCards && AiluaLocalStore.customCards.value.none { card ->
            card.data.id.ifBlank { card.data.name.lowercase().replace(" ", "_") } == characterId
        }

    fun getAllCharacters(): List<CharacterProfile> {
        refresh()
        return _allCards.value.keys.map(::resolveProfile)
    }

    fun getOfficialRomanceCharacters(): List<CharacterProfile> {
        refresh()
        return OFFICIAL_ROMANCE_IDS.map(::resolveProfile)
    }

    fun getAllCards(): List<CharacterCard> {
        refresh()
        return _allCards.value.values.toList()
    }

    fun saveCharacterCard(card: CharacterCard): CharacterProfile {
        val targetId = if (card.data.id.isNotBlank()) {
            card.data.id.lowercase().replace(" ", "_")
        } else {
            "custom_" + card.data.name.lowercase().replace(" ", "_").ifBlank { System.currentTimeMillis().toString() }
        }

        val finalizedCard = card.copy(
            data = card.data.copy(
                id = targetId,
                avatarReference = card.data.avatarReference.ifBlank { targetId }
            )
        )

        AiluaLocalStore.saveCustomCard(finalizedCard)
        refresh()
        return getCharacter(targetId)
    }

    fun deleteCustomCharacter(characterId: String) {
        if (characterId in builtInCards) return
        AiluaLocalStore.deleteCustomCard(characterId)
        refresh()
    }

    fun duplicateCharacter(characterId: String): CharacterCard {
        val source = getCard(characterId) ?: WorldData.cardYan
        val newId = "${source.data.id}_copy_${UUID.randomUUID()}"
        val copyCard = source.copy(
            data = source.data.copy(
                id = newId,
                name = "${source.data.name} (副本)"
            )
        )
        saveCharacterCard(copyCard)
        return copyCard
    }
}
