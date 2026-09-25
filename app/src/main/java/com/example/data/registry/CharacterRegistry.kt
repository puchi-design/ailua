package com.example.data.registry

import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.mock.WorldData
import com.example.data.model.CharacterCard
import com.example.data.model.CharacterProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CharacterRegistry
 *
 * Single source of truth for all AILUA companion profiles.
 * Seamlessly integrates built-in core companions (Mira, Yuna, Noa)
 * with user-imported and custom Character Card V2 characters.
 */
object CharacterRegistry {

    // Built-in cards mapped to CharacterCard models
    private val builtInCards = mapOf(
        "mira" to WorldData.cardMira,
        "yuna" to WorldData.cardYuna,
        "noa" to WorldData.cardNoa
    )

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
        // 1. Check built-in mock characters first for rich timeline/memories
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
            title = "自定义伴生者",
            bio = "通过伴生工坊导入的专属灵魂知己。",
            currentActivity = "正在心网世界静候连通…",
            mood = "温存",
            location = "青石街23号",
            contextualQuote = "很高兴在 AILUA 与你相遇。",
            avatarId = "mira",
            relationshipType = "伴生者"
        )
    }

    fun getCard(characterId: String): CharacterCard? {
        refresh()
        return _allCards.value[characterId]
    }

    fun getAllCharacters(): List<CharacterProfile> {
        refresh()
        val profiles = mutableListOf<CharacterProfile>()
        // Built-ins in canonical order
        profiles.add(MockData.sampleCharacter)
        MockData.allCharacters["yuna"]?.let { profiles.add(it) }
        MockData.allCharacters["noa"]?.let { profiles.add(it) }

        // Append custom imported characters
        _allCards.value.forEach { (id, card) ->
            if (id != "mira" && id != "yuna" && id != "noa") {
                profiles.add(WorldData.cardToProfile(card))
            }
        }
        return profiles
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
                avatarReference = card.data.avatarReference.ifBlank { "mira" }
            )
        )

        AiluaLocalStore.saveCustomCard(finalizedCard)
        refresh()
        return getCharacter(targetId)
    }

    fun deleteCustomCharacter(characterId: String) {
        if (characterId == "mira" || characterId == "yuna" || characterId == "noa") return
        AiluaLocalStore.deleteCustomCard(characterId)
        refresh()
    }

    fun duplicateCharacter(characterId: String): CharacterCard {
        val source = getCard(characterId) ?: WorldData.cardMira
        val newId = "${source.data.id}_copy_${System.currentTimeMillis() % 1000}"
        val copyCard = source.copy(
            data = source.data.copy(
                id = newId,
                name = "${source.data.name} (副本)",
                creatorNotes = "基于 ${source.data.name} 复制创建"
            )
        )
        saveCharacterCard(copyCard)
        return copyCard
    }
}
