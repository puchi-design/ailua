package com.example.data.model

/**
 * Character Card V2 Specification data structures.
 * Inspired by character-card-spec-v2 & SillyTavern.
 */
data class CharacterCard(
    val spec: String = "chara_card_v2",
    val specVersion: String = "2.0",
    val data: CharacterCardData
)

data class CharacterCardData(
    val id: String = "",
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val exampleMessages: String = "",
    val creatorNotes: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val creator: String = "AILUA Artisan",
    val characterVersion: String = "1.0",
    val avatarReference: String = "mira",
    val characterBook: WorldBook? = null
)

// === World Book & Lore Models ===

enum class LoreActivationMode {
    ALWAYS,
    KEYWORD,
    LOCATION,
    CHARACTER,
    EVENT
}

data class LoreEntry(
    val id: String,
    val title: String,
    val content: String,
    val keywords: List<String> = emptyList(),
    val secondaryKeywords: List<String> = emptyList(),
    val enabled: Boolean = true,
    val priority: Int = 10,
    val characterIds: List<String> = emptyList(),
    val locationIds: List<String> = emptyList(),
    val activationMode: LoreActivationMode = LoreActivationMode.KEYWORD,
    val category: String = "世界设定", // 世界设定, 地点, 人物关系, 事件, 习惯, 秘密, 共同记忆
    val notes: String = ""
)

data class WorldBook(
    val id: String,
    val name: String,
    val description: String,
    val scanDepth: Int = 2,
    val tokenBudget: Int = 500,
    val recursiveScanning: Boolean = false,
    val entries: List<LoreEntry> = emptyList()
)

// === Virtual Places Models ===

data class VirtualPlace(
    val id: String,
    val name: String,
    val description: String,
    val type: String, // 居所, 茶歇, 书阁, 花卉, 街区, 秘境
    val mood: String, // 宁静, 温暖, 沉思, 元气, 悠闲
    val residentCharacterIds: List<String> = emptyList(),
    val connectedPlaceIds: List<String> = emptyList(),
    val currentCharacterIds: List<String> = emptyList(),
    val recentEvents: List<String> = emptyList(),
    val visualReference: String = "place_street",
    val ambientAudioNote: String = "雨打青石台阶声与风铃微鸣",
    val coordinateX: Float = 0.5f,
    val coordinateY: Float = 0.5f
)

// === Branching Narrative / Theater Models (Inspired by inkle/ink) ===

data class TheaterChoice(
    val id: String,
    val text: String,
    val targetNodeId: String,
    val requiredVariableKey: String? = null,
    val requiredVariableValue: String? = null,
    val setVariableKey: String? = null,
    val setVariableValue: String? = null,
    val bondIncrease: Int = 1
)

data class TheaterDialogueNode(
    val id: String,
    val speakerId: String,
    val speakerName: String,
    val avatarId: String,
    val text: String,
    val emotion: String = "温柔",
    val choices: List<TheaterChoice> = emptyList(),
    val isEnding: Boolean = false,
    val endingTitle: String? = null
)

data class TheaterStory(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val characterIds: List<String>,
    val initialNodeId: String,
    val nodes: Map<String, TheaterDialogueNode>,
    val initialVariables: Map<String, String> = emptyMap(),
    val coverTag: String = "雨夜茶话"
)
