package com.example.data.character.initiative

import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.character.runtime.InitiativeTrigger
import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.model.Letter
import com.example.data.model.LetterDeliveryState
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.data.model.ProactiveSettings
import java.security.MessageDigest

enum class ProactiveContentType { TEXT, PHOTO, CALL_INVITE, LETTER, MOMENT }

data class InitiativeEvidence(val trigger: InitiativeTrigger, val key: String, val description: String)
data class InitiativeDecision(
    val type: ProactiveContentType,
    val evidence: InitiativeEvidence,
    val windowKey: String,
)

/** Facts supplied by the real chat, memory and romance repositories, never inferred from a bio. */
data class InitiativeFacts(
    val lastUserAtEpochMs: Long? = null,
    val importantMemoryId: String? = null,
    val importantMemory: String? = null,
    val conflictEventId: String? = null,
    val readyToReconnect: Boolean = false,
    val recentGoodEventAtEpochMs: Long? = null,
)

object CharacterInitiativeRuntime {
    const val WINDOW_MS = 30 * 60_000L

    /** The most active allowed medium sets the outer contact envelope; a disabled text channel cannot disable letters. */
    fun contactSettings(settings: ProactiveSettings, profile: CharacterRuntimeProfile): ProactiveSettings {
        val initiative = profile.initiative
        val frequency = listOf(initiative.messageFrequency, initiative.callFrequency, initiative.photoFrequency,
            initiative.momentFrequency, initiative.letterFrequency).maxBy { com.example.data.character.runtime.CharacterRuntimeResolver.probability(it) }
        return CharacterBehaviorRuntime.proactiveSettings(settings, profile.copy(initiative = initiative.copy(messageFrequency = frequency)))
    }

    fun evidence(
        profile: CharacterRuntimeProfile, now: Long, today: String, clock: WorldClock,
        facts: InitiativeFacts, events: List<LifeEvent>,
    ): List<InitiativeEvidence> = buildList {
        val id = profile.characterId
        facts.lastUserAtEpochMs?.takeIf { now - it >= 24 * 3_600_000L }?.let {
            add(InitiativeEvidence(InitiativeTrigger.USER_ABSENT, "absence:$it", "用户距上次主动聊天已超过一天；不要责怪或催促。"))
        }
        if (clock.minutesOfDay in 6 * 60 until 10 * 60) add(InitiativeEvidence(InitiativeTrigger.MORNING, "morning:$today", "世界时间是早晨。"))
        if (clock.minutesOfDay >= 22 * 60 || clock.minutesOfDay < 5 * 60) add(InitiativeEvidence(InitiativeTrigger.LATE_NIGHT, "night:$today", "世界时间已深夜；尊重休息。"))
        if (clock.weather in setOf(WeatherState.RAIN, WeatherState.HEAVY_RAIN)) add(InitiativeEvidence(InitiativeTrigger.RAIN, "rain:$today", "世界天气正在下雨。"))
        val birthday = Regex("(?:\\d{4}-)?(\\d{2})-(\\d{2})").matchEntire(profile.identity.birthday)?.let { "${it.groupValues[1]}-${it.groupValues[2]}" }
        if (birthday != null && today.takeLast(5) == birthday) add(InitiativeEvidence(InitiativeTrigger.BIRTHDAY, "birthday:$today", "今天是角色自己的生日；不要假称是用户生日。"))
        if (facts.importantMemoryId != null && !facts.importantMemory.isNullOrBlank()) add(InitiativeEvidence(InitiativeTrigger.SHARED_MEMORY, "memory:${facts.importantMemoryId}:$today", "记得这件真实记录的小事：${facts.importantMemory.take(220)}"))
        if (facts.readyToReconnect && facts.conflictEventId != null) add(InitiativeEvidence(InitiativeTrigger.RECENT_CONFLICT, "conflict:${facts.conflictEventId}", "一次实际冲突尚未解决，已经留出空间；可以尝试重新联系，不能假定对方已原谅。"))
        facts.recentGoodEventAtEpochMs?.takeIf { now - it in 0..24 * 3_600_000L }?.let {
            add(InitiativeEvidence(InitiativeTrigger.RECENT_GOOD_EVENT, "good:$it", "近期有一次真实的积极互动；不要编造新的共同经历。"))
        }
        events.firstOrNull { it.characterId == id && it.type == LifeEventType.LOCATION_CHANGE && it.worldDateLabel == clock.dateLabel && clock.minutesOfDay - it.worldMinutesOfDay in 0..180 }?.let {
            add(InitiativeEvidence(InitiativeTrigger.LOCATION_CHANGE, "location:${it.id}", "刚发生的位置变化：${it.title.take(120)} ${it.location.orEmpty().take(80)}"))
        }
    }

    /** Every evaluation in the same character/time window uses exactly the same draw, including after restart. */
    fun decide(
        profile: CharacterRuntimeProfile, now: Long, evidence: List<InitiativeEvidence>,
        events: List<LifeEvent>, worldDate: String? = null, sample: (String) -> Double = ::stableSample,
    ): InitiativeDecision? {
        val window = "${profile.characterId}:${now / WINDOW_MS}"
        if (events.any { it.characterId == profile.characterId && it.metadata["initiative_window"] == window }) return null
        val eligible = evidence.filter { candidate ->
            (profile.initiative.triggerWeights[candidate.trigger] ?: 0.0) > 0.0 &&
                events.none { it.characterId == profile.characterId && it.metadata["initiative_evidence"] == candidate.key }
        }.sortedWith(compareByDescending<InitiativeEvidence> { profile.initiative.triggerWeights[it.trigger] ?: 0.0 }.thenBy { it.key })
        val trigger = eligible.firstOrNull() ?: return null
        val weights = contentWeights(profile).filter { (type, weight) ->
            val kind = CharacterInitiativeQuota.kind(type)
            val used = if (worldDate != null) CharacterInitiativeQuota.used(profile.characterId, worldDate, kind, events)
            else events.count {
                it.characterId == profile.characterId && CharacterInitiativeQuota.kind(it) == kind &&
                    it.metadata["initiative_at_epoch_ms"]?.toLongOrNull()?.let { time -> now - time in 0 until 24 * 3_600_000L } == true
            }
            weight > 0.0 && used < CharacterInitiativeQuota.cap(profile, kind)
        }
        if (weights.isEmpty()) return null
        val threshold = weights.values.maxOrNull()!! * (profile.initiative.triggerWeights[trigger.trigger] ?: 0.0)
        if (sample("$window:gate") >= threshold) return null
        val position = sample("$window:content").coerceIn(0.0, .999999999) * weights.values.sum()
        var cumulative = 0.0
        val type = weights.entries.first { cumulative += it.value; position < cumulative }.key
        return InitiativeDecision(type, trigger, window)
    }

    fun contentWeights(profile: CharacterRuntimeProfile): Map<ProactiveContentType, Double> = profile.initiative.let {
        linkedMapOf(ProactiveContentType.TEXT to it.messageProbability, ProactiveContentType.PHOTO to it.photoProbability,
            ProactiveContentType.CALL_INVITE to it.callProbability, ProactiveContentType.LETTER to it.letterProbability,
            ProactiveContentType.MOMENT to it.momentProbability)
    }

    fun stableSample(key: String): Double {
        val bytes = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        val bits = bytes.take(4).fold(0L) { value, byte -> (value shl 8) or (byte.toLong() and 255) }
        return bits / 4_294_967_296.0
    }

    fun splitText(content: String, maxBurst: Int): List<String> {
        val parts = content.split(Regex("\\n\\s*\\n|\\n")).map(String::trim).filter(String::isNotEmpty)
        val limit = maxBurst.coerceIn(1, 3)
        if (parts.size <= 1 || limit == 1) return listOf(content.trim())
        return parts.take(limit - 1) + parts.drop(limit - 1).joinToString("\n")
    }

    fun fact(profile: CharacterRuntimeProfile, decision: InitiativeDecision, content: String, now: Long, clock: WorldClock): LifeEvent {
        val type = decision.type
        val source = when (type) { ProactiveContentType.LETTER -> "mailbox"; ProactiveContentType.MOMENT -> "moments"; ProactiveContentType.PHOTO -> "gallery"; else -> "heartbeat" }
        val title = when (type) { ProactiveContentType.TEXT -> "主动发来消息"; ProactiveContentType.PHOTO -> "分享了一张照片"; ProactiveContentType.CALL_INVITE -> "问你是否方便通话"; ProactiveContentType.LETTER -> "写来一封信"; ProactiveContentType.MOMENT -> "分享了动态" }
        return LifeEvent(
            id = "proactive_${now}_${profile.characterId}", characterId = profile.characterId, time = clock.timeFormatted,
            type = when (type) { ProactiveContentType.PHOTO -> LifeEventType.PHOTO; ProactiveContentType.MOMENT -> LifeEventType.MOMENT; else -> LifeEventType.MESSAGE },
            title = "${profile.name.substringBefore(" (")}$title", description = content,
            location = profile.life.workplace.ifBlank { profile.life.home }.ifBlank { null },
            visibility = if (type == ProactiveContentType.MOMENT) "PUBLIC" else "PRIVATE",
            relatedCharacterIds = if (type == ProactiveContentType.MOMENT) emptyList() else listOf("user"),
            imageReference = if (type == ProactiveContentType.PHOTO) "character_asset:${profile.visual.assetPack.ifBlank { profile.characterId }}/gallery/${decision.evidence.trigger.wireName}" else null,
            worldDateLabel = clock.dateLabel, worldMinutesOfDay = clock.minutesOfDay, sourceAppId = source,
            sourceRefId = "proactive_${type.name.lowercase()}",
            metadata = mapOf("initiative_window" to decision.windowKey, "initiative_evidence" to decision.evidence.key,
                "initiative_trigger" to decision.evidence.trigger.wireName, "proactive_content_type" to type.name,
                "initiative_at_epoch_ms" to now.toString(), "sender_name" to profile.name.substringBefore(" (")),
        )
    }
}

/** Restores delivered letters from their durable world fact, including after process death. */
fun projectInitiativeLetters(events: List<LifeEvent>, openedIds: Set<String> = emptySet()): List<Letter> = events
    .filter { it.metadata["proactive_content_type"] == ProactiveContentType.LETTER.name }
    .distinctBy { it.id }.map { event ->
        Letter(id = event.id, characterId = event.characterId, senderName = event.metadata["sender_name"] ?: event.characterId,
            subject = "写给你", body = event.description, createdAtVirtualTime = event.time,
            deliverAtVirtualTimeMinutes = event.worldMinutesOfDay, deliverAtVirtualTimeString = event.time,
            deliveryState = if (event.id in openedIds) LetterDeliveryState.OPENED else LetterDeliveryState.DELIVERED,
            relatedLifeEventId = event.id)
    }
