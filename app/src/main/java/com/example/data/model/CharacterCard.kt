package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Character Card V2 Specification data structures.
 * Inspired by character-card-spec-v2 & SillyTavern.
 */
@Serializable
data class CharacterCard(
    @SerialName("spec") val spec: String = "chara_card_v2",
    @SerialName("spec_version") val specVersion: String = "2.0",
    @SerialName("data") val data: CharacterCardData
)

@Serializable
data class CharacterCardData(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String,
    @SerialName("description") val description: String = "",
    @SerialName("personality") val personality: String = "",
    @SerialName("scenario") val scenario: String = "",
    @SerialName("first_mes") val firstMessage: String = "",
    @SerialName("mes_example") val exampleMessages: String = "",
    @SerialName("creator_notes") val creatorNotes: String = "",
    @SerialName("system_prompt") val systemPrompt: String = "",
    @SerialName("post_history_instructions") val postHistoryInstructions: String = "",
    @SerialName("alternate_greetings") val alternateGreetings: List<String> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("creator") val creator: String = "AILUA Artisan",
    @SerialName("character_version") val characterVersion: String = "1.0",
    @SerialName("avatar_reference") val avatarReference: String = "mira",
    @SerialName("character_book") val characterBook: WorldBook? = null,
    @SerialName("extensions") val extensions: Map<String, String> = emptyMap()
)

// === World Book & Lore Models ===

@Serializable
enum class LoreActivationMode {
    ALWAYS,
    KEYWORD,
    LOCATION,
    CHARACTER,
    EVENT
}

@Serializable
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

@Serializable
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

@Serializable
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

@Serializable
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

@Serializable
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

@Serializable
data class TheaterHistoryStep(
    val nodeId: String,
    val speakerName: String,
    val text: String,
    val choiceMadeText: String? = null
)

@Serializable
data class TheaterBookmark(
    val storyId: String,
    val storyTitle: String,
    val currentNodeId: String,
    val variables: Map<String, String>,
    val bondScore: Int,
    val history: List<TheaterHistoryStep>,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

