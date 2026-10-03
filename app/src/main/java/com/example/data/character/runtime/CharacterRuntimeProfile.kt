package com.example.data.character.runtime

/** Immutable, normalized projection. Runtime consumers never need the extension wire format. */
data class CharacterRuntimeProfile(
    val characterId: String,
    val name: String,
    val source: RuntimeSource,
    val identity: CharacterIdentityRuntime,
    val relationship: RelationshipRuntime,
    val behavior: BehaviorRuntime,
    val speech: SpeechRuntime,
    val initiative: InitiativeRuntime,
    val life: LifeRuntime,
    val visual: VisualRuntime,
)

enum class RuntimeSource { OFFICIAL, IMPORTED, STANDARD, FUTURE }

enum class InitiativeTrigger(val wireName: String) {
    USER_ABSENT("user_absent"), LATE_NIGHT("late_night"), MORNING("morning"), RAIN("rain"),
    BIRTHDAY("birthday"), SHARED_MEMORY("shared_memory"), RECENT_CONFLICT("recent_conflict"),
    RECENT_GOOD_EVENT("recent_good_event"), LOCATION_CHANGE("location_change");
    companion object { fun fromWire(value: String): InitiativeTrigger? = entries.firstOrNull { it.wireName == value } }
}

data class CharacterIdentityRuntime(
    val gender: String = "", val age: Int? = null, val occupation: String = "",
    val heightCm: Int? = null, val birthday: String = "",
)
data class RelationshipRuntime(
    val routeType: String = "companion", val initialRelation: String = "",
    val affectionStyle: String = "", val attachmentStyle: String = "",
    val jealousy: Double = 0.0, val possessiveness: Double = 0.0,
    val physicalDistance: String = "", val confessionThreshold: Double = 0.5,
    val progressionStyle: String = "balanced",
) { val romanceEnabled: Boolean get() = routeType == "romance" }

data class BehaviorRuntime(
    val coreDesire: String = "", val flaws: List<String> = emptyList(), val blindSpots: List<String> = emptyList(),
    val boundaries: List<String> = emptyList(), val vulnerabilities: List<String> = emptyList(),
    val carePatterns: List<String> = emptyList(), val flirtPatterns: List<String> = emptyList(),
    val jealousyPatterns: List<String> = emptyList(), val conflictPatterns: List<String> = emptyList(),
)
data class SpeechRuntime(
    val sentenceLength: String = "", val emojiFrequency: String = "", val petNames: List<String> = emptyList(),
    val verbalTics: List<String> = emptyList(), val forbiddenPhrases: List<String> = emptyList(), val tone: List<String> = emptyList(),
)
data class InitiativeRuntime(
    val messageFrequency: String = "medium", val callFrequency: String = "low", val photoFrequency: String = "low",
    val momentFrequency: String = "low", val letterFrequency: String = "low",
    val messageProbability: Double = .5, val callProbability: Double = .18, val photoProbability: Double = .18,
    val momentProbability: Double = .18, val letterProbability: Double = .18,
    val preferredTriggers: List<String> = emptyList(),
    val triggerWeights: Map<InitiativeTrigger, Double> = emptyMap(), val maxTextBurst: Int = 1,
)
data class LifeRuntime(
    val home: String = "", val workplace: String = "", val sleepWindow: String = "00:00-08:00",
    val hobbies: List<String> = emptyList(), val socialCircle: List<String> = emptyList(),
)
data class VisualRuntime(
    val assetPack: String = "", val defaultOutfit: String = "", val avatar: String = "", val portrait: String = "",
    val expressions: Map<String, String> = emptyMap(),
)
