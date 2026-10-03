package com.example.data.relationship.romance

import com.example.data.character.runtime.CharacterRuntimeProfile
import kotlin.math.max

/** Pure reducer. Distinct real experiences matter; replay, spam and seed history cannot manufacture a romance. */
object RomanceReducer {
    const val DAY_MS = 86_400_000L

    fun apply(record: RomanceRecord, event: RomanceEvent, profile: CharacterRuntimeProfile): RomanceRecord {
        if (event.id.isBlank() || event.occurredAtEpochMs <= 0 || event.id in record.processedEventIds) return record
        if (event.occurredAtEpochMs < maxOf(record.lastAppliedAtEpochMs, record.lastMeaningfulAtEpochMs ?: 0L)) {
            return record.copy(processedEventIds = record.processedEventIds + event.id)
        }
        val day = event.occurredAtEpochMs / DAY_MS
        // Reordered delivery may add evidence, but must not reset today's diminishing-return budget.
        val counts = if (day > record.rewardDay) emptyMap() else record.dailyTypeCounts
        val count = counts[event.type.name] ?: 0
        val repeated = event.fingerprint in record.rewardedFingerprints
        val multiplier = if (repeated || day < record.rewardDay) 0f else when (count) { 0 -> 1f; 1 -> .25f; 2 -> .05f; else -> 0f }
        val old = record.state.sanitized()
        val base = when (event.type) {
            RomanceEventType.IMPORTANT_MEMORY -> RomanceState(.025f, .06f, .01f, .02f)
            RomanceEventType.SUSTAINED_CONVERSATION -> RomanceState(.05f, .025f, .02f, .02f)
            RomanceEventType.USER_CALL -> RomanceState(.035f, .025f, .025f, .04f)
            RomanceEventType.SHARED_STORY -> RomanceState(.05f, .04f, .025f, .035f)
            RomanceEventType.LETTER -> RomanceState(.02f, .04f, .015f, .025f)
            RomanceEventType.ANNIVERSARY -> RomanceState(.015f, .04f, .025f, .06f)
            RomanceEventType.CONFLICT -> RomanceState(tension = .17f)
            RomanceEventType.CONFLICT_RESOLVED -> if (record.recentConflict?.resolvedAtEpochMs == null && record.recentConflict != null)
                RomanceState(trust = .045f, intimacy = .025f, tension = -.25f) else RomanceState()
            RomanceEventType.MUTUAL_AFFECTION -> RomanceState(attraction = .04f, intimacy = .025f)
            RomanceEventType.MUTUAL_COMMITMENT -> RomanceState(trust = .04f, intimacy = .04f)
            RomanceEventType.GOOD_EVENT -> RomanceState(familiarity = .015f, trust = .01f)
            RomanceEventType.BOUNDARY_RESET -> RomanceState()
        }
        val style = profile.relationship.progressionStyle
        val familiarityScale = if (style == "reserved") .55f else 1f
        val trustScale = if (style == "slow_trust") 1.3f else 1f
        val attractionScale = when (style) { "slow_trust" -> .5f; "expressive" -> 1.65f; "reserved" -> .55f; else -> 1f }
        val intimacyScale = when (style) { "expressive" -> 1.5f; "reserved" -> if (old.trust >= .65f) 1.8f else .45f; else -> 1f }
        val tensionScale = if (style == "expressive") 1.3f else 1f
        val romance = profile.relationship.romanceEnabled && (!record.romanceDeclined || event.type == RomanceEventType.MUTUAL_AFFECTION)
        val next = RomanceState(
            old.familiarity + base.familiarity * multiplier * familiarityScale,
            old.trust + base.trust * multiplier * trustScale,
            if (romance) old.attraction + base.attraction * multiplier * attractionScale else 0f,
            old.intimacy + base.intimacy * multiplier * intimacyScale,
            old.tension + base.tension * multiplier * tensionScale,
        ).sanitized()
        val isReset = event.type == RomanceEventType.BOUNDARY_RESET
        val conflict = when {
            event.type == RomanceEventType.CONFLICT && !repeated -> RomanceConflict(event.id, event.occurredAtEpochMs)
            event.type == RomanceEventType.CONFLICT_RESOLVED && record.recentConflict != null -> record.recentConflict.copy(resolvedAtEpochMs = event.occurredAtEpochMs)
            else -> record.recentConflict
        }
        val romanticAt = when {
            isReset || !romance -> null
            event.type == RomanceEventType.MUTUAL_AFFECTION -> record.romanticConfirmedAtEpochMs ?: event.occurredAtEpochMs
            else -> record.romanticConfirmedAtEpochMs
        }
        val commitmentAt = when {
            isReset || !romance -> null
            event.type == RomanceEventType.MUTUAL_COMMITMENT && romanticAt != null -> record.commitmentConfirmedAtEpochMs ?: event.occurredAtEpochMs
            else -> record.commitmentConfirmedAtEpochMs
        }
        return record.copy(
            state = if (isReset) next.copy(attraction = 0f) else next,
            processedEventIds = record.processedEventIds + event.id,
            rewardedFingerprints = record.rewardedFingerprints + event.fingerprint,
            rewardDay = max(day, record.rewardDay),
            dailyTypeCounts = counts + (event.type.name to count + 1),
            recentConflict = conflict,
            romanticConfirmedAtEpochMs = romanticAt,
            commitmentConfirmedAtEpochMs = commitmentAt,
            romanceDeclined = if (isReset) true else if (event.type == RomanceEventType.MUTUAL_AFFECTION && romance) false else record.romanceDeclined,
            lastMeaningfulAtEpochMs = if (multiplier > 0f) max(event.occurredAtEpochMs, record.lastMeaningfulAtEpochMs ?: 0L) else record.lastMeaningfulAtEpochMs,
            lastAppliedAtEpochMs = max(event.occurredAtEpochMs, record.lastAppliedAtEpochMs),
            lastGoodEventAtEpochMs = if (event.type in setOf(RomanceEventType.GOOD_EVENT, RomanceEventType.CONFLICT_RESOLVED, RomanceEventType.ANNIVERSARY)) event.occurredAtEpochMs else record.lastGoodEventAtEpochMs,
        )
    }

    fun stage(record: RomanceRecord, profile: CharacterRuntimeProfile): RomanceStage {
        val s = record.state.sanitized()
        val romance = profile.relationship.romanceEnabled && !record.romanceDeclined
        // A card's route means possibility, never the user's consent or a shared romantic past.
        if (romance && s.tension < .45f && record.commitmentConfirmedAtEpochMs != null && record.romanticConfirmedAtEpochMs != null &&
            s.familiarity >= .7f && s.trust >= .8f && s.attraction >= .6f && s.intimacy >= .75f) return RomanceStage.COMMITTED
        val trustThreshold = max(.55f, profile.relationship.confessionThreshold.toFloat().coerceIn(0f, 1f))
        if (romance && s.tension < .55f && record.romanticConfirmedAtEpochMs != null && s.familiarity >= .45f &&
            s.trust >= trustThreshold && s.attraction >= .4f && s.intimacy >= .35f) return RomanceStage.ROMANTIC
        if (romance && s.tension < .6f && s.familiarity >= .4f && s.trust >= .4f && s.attraction >= .3f && s.intimacy >= .25f) return RomanceStage.AMBIGUOUS
        if (s.familiarity >= .3f && s.trust >= .35f) return RomanceStage.CLOSE
        if (s.familiarity >= .12f && s.trust >= .1f) return RomanceStage.FAMILIAR
        return RomanceStage.STRANGER
    }

    private fun RomanceState.sanitized() = RomanceState(familiarity.unit(), trust.unit(), attraction.unit(), intimacy.unit(), tension.unit())
    private fun Float.unit() = if (isFinite()) coerceIn(0f, 1f) else 0f
}
