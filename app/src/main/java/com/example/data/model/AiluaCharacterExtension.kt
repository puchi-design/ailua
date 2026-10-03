package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Optional AILUA behavior hints. Empty values do not impose a gender or relationship route. */
@Serializable
data class AiluaCharacterExtension(
    val schema: Int = 1,
    val identity: AiluaIdentity = AiluaIdentity(),
    val relationship: AiluaRelationship = AiluaRelationship(),
    val behavior: AiluaBehavior = AiluaBehavior(),
    val speech: AiluaSpeech = AiluaSpeech(),
    val initiative: AiluaInitiative = AiluaInitiative(),
    val life: AiluaLife = AiluaLife(),
    val visual: AiluaVisual = AiluaVisual(),
)

@Serializable
data class AiluaIdentity(
    val gender: String = "",
    val age: Int? = null,
    val occupation: String = "",
    @SerialName("height_cm") val heightCm: Int? = null,
    val birthday: String = "",
)

@Serializable
data class AiluaRelationship(
    @SerialName("route_type") val routeType: String = "",
    @SerialName("initial_relation") val initialRelation: String = "",
    @SerialName("affection_style") val affectionStyle: String = "",
    @SerialName("attachment_style") val attachmentStyle: String = "",
    val jealousy: Double = 0.0,
    val possessiveness: Double = 0.0,
    @SerialName("physical_distance") val physicalDistance: String = "",
    @SerialName("confession_threshold") val confessionThreshold: Double = 0.5,
    @SerialName("progression_style") val progressionStyle: String = "balanced",
)

@Serializable
data class AiluaBehavior(
    @SerialName("core_desire") val coreDesire: String = "",
    val flaws: List<String> = emptyList(),
    @SerialName("blind_spots") val blindSpots: List<String> = emptyList(),
    val boundaries: List<String> = emptyList(),
    val vulnerabilities: List<String> = emptyList(),
    @SerialName("care_patterns") val carePatterns: List<String> = emptyList(),
    @SerialName("flirt_patterns") val flirtPatterns: List<String> = emptyList(),
    @SerialName("jealousy_patterns") val jealousyPatterns: List<String> = emptyList(),
    @SerialName("conflict_patterns") val conflictPatterns: List<String> = emptyList(),
)

@Serializable
data class AiluaSpeech(
    @SerialName("sentence_length") val sentenceLength: String = "",
    @SerialName("emoji_frequency") val emojiFrequency: String = "",
    @SerialName("pet_names") val petNames: List<String> = emptyList(),
    @SerialName("verbal_tics") val verbalTics: List<String> = emptyList(),
    @SerialName("forbidden_phrases") val forbiddenPhrases: List<String> = emptyList(),
    val tone: List<String> = emptyList(),
)

@Serializable
data class AiluaInitiative(
    @SerialName("message_frequency") val messageFrequency: String = "",
    @SerialName("call_frequency") val callFrequency: String = "",
    @SerialName("photo_frequency") val photoFrequency: String = "",
    @SerialName("moment_frequency") val momentFrequency: String = "",
    @SerialName("preferred_triggers") val preferredTriggers: List<String> = emptyList(),
    @SerialName("letter_frequency") val letterFrequency: String = "",
    @SerialName("trigger_weights") val triggerWeights: Map<String, Double> = emptyMap(),
    @SerialName("max_text_burst") val maxTextBurst: Int = 1,
)

@Serializable
data class AiluaLife(
    val home: String = "",
    val workplace: String = "",
    @SerialName("sleep_window") val sleepWindow: String = "",
    val hobbies: List<String> = emptyList(),
    @SerialName("social_circle") val socialCircle: List<String> = emptyList(),
)

/** Asset references are data only; decoding never opens files or performs network requests. */
@Serializable
data class AiluaVisual(
    @SerialName("asset_pack") val assetPack: String = "",
    @SerialName("default_outfit") val defaultOutfit: String = "",
    val avatar: String = "",
    val portrait: String = "",
    val expressions: Map<String, String> = emptyMap(),
)
