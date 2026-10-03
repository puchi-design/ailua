package com.example.data.character.runtime

import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.mock.OfficialCharacters
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.CharacterCardData
import com.example.data.registry.CharacterRegistry

/** The only behavior boundary that interprets cards. No writes, cached stale edits, or gender inference. */
object CharacterRuntimeResolver {
    fun resolve(characterId: String): CharacterRuntimeProfile =
        CharacterRegistry.getCard(characterId)?.data?.let(::resolve)
            ?: resolve(CharacterCardData(id = characterId, name = characterId))

    fun resolve(data: CharacterCardData): CharacterRuntimeProfile {
        val extension = AiluaCharacterExtensionCodec.readOrNull(data)
        val source = when {
            AiluaCharacterExtensionCodec.hasFutureSchema(data) -> RuntimeSource.FUTURE
            extension == null -> RuntimeSource.STANDARD
            OfficialCharacters.cards.any { it.data == data } -> RuntimeSource.OFFICIAL
            else -> RuntimeSource.IMPORTED
        }
        val projected = fromExtension(extension ?: AiluaCharacterExtension(), data.id, data.name)
        return projected.copy(
            source = source,
            // Tags may describe style but never establish gender, romance or facts of a shared past.
            speech = if (extension == null) projected.speech.copy(tone = data.tags.take(6)) else projected.speech,
            visual = projected.visual.copy(avatar = projected.visual.avatar.ifBlank { data.avatarReference }),
        )
    }

    /** Typed compatibility adapter for authoring helpers and old tests; production resolves the current card. */
    fun fromExtension(e: AiluaCharacterExtension, characterId: String = "", name: String = ""): CharacterRuntimeProfile {
        val i = e.identity; val r = e.relationship; val b = e.behavior; val s = e.speech; val a = e.initiative; val l = e.life; val v = e.visual
        val message = frequency(a.messageFrequency, "medium"); val call = frequency(a.callFrequency, "low")
        val photo = frequency(a.photoFrequency, "low"); val moment = frequency(a.momentFrequency, "low")
        val letter = frequency(a.letterFrequency, "low")
        val weights = if (a.triggerWeights.isNotEmpty()) a.triggerWeights.mapNotNull { (key, value) ->
            InitiativeTrigger.fromWire(key)?.takeIf { value.isFinite() }?.let { it to value.coerceIn(0.0, 1.0) }
        }.toMap() else defaultTriggers
        return CharacterRuntimeProfile(
            characterId, name, RuntimeSource.IMPORTED,
            CharacterIdentityRuntime(i.gender, i.age?.takeIf { it >= 0 }, i.occupation, i.heightCm?.takeIf { it > 0 }, i.birthday),
            RelationshipRuntime(r.routeType.ifBlank { "companion" }, r.initialRelation, r.affectionStyle, r.attachmentStyle,
                ratio(r.jealousy, 0.0), ratio(r.possessiveness, 0.0), r.physicalDistance, ratio(r.confessionThreshold, .5), r.progressionStyle),
            BehaviorRuntime(b.coreDesire, b.flaws, b.blindSpots, b.boundaries, b.vulnerabilities, b.carePatterns, b.flirtPatterns, b.jealousyPatterns, b.conflictPatterns),
            SpeechRuntime(s.sentenceLength.ifBlank { "适中，遵循角色描述和对话示例" }, s.emojiFrequency.ifBlank { "少量，遵循示例" }, s.petNames, s.verbalTics, s.forbiddenPhrases, s.tone),
            InitiativeRuntime(message, call, photo, moment, letter, probability(message), probability(call), probability(photo), probability(moment), probability(letter), a.preferredTriggers, weights, a.maxTextBurst.coerceIn(1, 3)),
            LifeRuntime(l.home, l.workplace, normalizeSleep(l.sleepWindow), l.hobbies, l.socialCircle),
            VisualRuntime(v.assetPack, v.defaultOutfit, v.avatar, v.portrait, v.expressions),
        )
    }

    private val defaultTriggers = mapOf(InitiativeTrigger.USER_ABSENT to .35, InitiativeTrigger.SHARED_MEMORY to .5)
    private fun ratio(value: Double, default: Double) = if (value.isFinite()) value.coerceIn(0.0, 1.0) else default
    private fun frequency(value: String, default: String): String = when (val normalized = value.trim().lowercase()) {
        "none", "off", "never" -> "none"
        "very_low", "low", "low_medium", "medium", "medium_high", "high" -> normalized
        else -> default
    }
    fun probability(frequency: String): Double = when (frequency) {
        "none" -> 0.0; "very_low" -> .06; "low" -> .18; "low_medium" -> .35
        "medium" -> .5; "medium_high" -> .65; "high" -> .8; else -> 0.0
    }
    private fun normalizeSleep(value: String): String {
        val match = Regex("^(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})$").matchEntire(value) ?: return "00:00-08:00"
        val n = match.groupValues.drop(1).map { it.toInt() }
        return if (n[0] in 0..23 && n[2] in 0..23 && n[1] in 0..59 && n[3] in 0..59 && n.take(2) != n.drop(2)) value else "00:00-08:00"
    }
}
