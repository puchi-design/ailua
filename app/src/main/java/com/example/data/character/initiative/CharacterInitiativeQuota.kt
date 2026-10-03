package com.example.data.character.initiative

import com.example.data.character.CharacterBehaviorRuntime
import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.engine.ProactiveRules
import com.example.data.engine.ScheduledActionType
import com.example.data.engine.ScheduledWorldAction
import com.example.data.engine.UserContactCooldown
import com.example.data.model.*

/** Shared virtual-day content caps; the user's aggregate contact limit still uses real time. */
object CharacterInitiativeQuota {
    fun kind(type: ProactiveContentType): String = when (type) {
        ProactiveContentType.TEXT -> "message"; ProactiveContentType.PHOTO -> "photo"
        ProactiveContentType.CALL_INVITE -> "call"; ProactiveContentType.LETTER -> "letter"; ProactiveContentType.MOMENT -> "moment"
    }
    fun kind(action: PlannedWorldAction): String? = when {
        action.type == ScheduledActionType.INCOMING_CALL -> "call"
        action.type == ScheduledActionType.LETTER_DELIVERY -> "letter"
        action.type == ScheduledActionType.MOMENT || action.lifeEventType == LifeEventType.MOMENT -> "moment"
        action.lifeEventType == LifeEventType.PHOTO -> "photo"
        action.lifeEventType == LifeEventType.MESSAGE -> "message"
        else -> null
    }
    fun kind(event: LifeEvent): String? = when {
        event.isUserActivity() -> null // Saving a character's photo is the user's action, not another character photo.
        event.metadata["proactive_content_type"] == ProactiveContentType.CALL_INVITE.name || event.id.startsWith("pulse_call_plan_") -> "call"
        event.metadata["proactive_content_type"] == ProactiveContentType.LETTER.name -> "letter"
        event.type == LifeEventType.PHOTO -> "photo"
        event.type == LifeEventType.MOMENT -> "moment"
        event.type == LifeEventType.MESSAGE && event.sourceAppId == "heartbeat" -> "message"
        else -> null // A later sourceAppId=call summary is not a second autonomous call.
    }
    fun cap(profile: CharacterRuntimeProfile, kind: String): Int = if (kind == "message") when (profile.initiative.messageFrequency) {
        "none" -> 0; "very_low", "low" -> 1; "low_medium", "medium" -> 2; else -> 4
    } else CharacterBehaviorRuntime.dailyActionLimit(profile, kind)

    fun used(characterId: String, date: String, contentKind: String, events: List<LifeEvent>): Int = events.distinctBy { it.id }.count {
        it.characterId == characterId && it.worldDateLabel == date && kind(it) == contentKind
    }

    fun available(profile: CharacterRuntimeProfile, date: String, kind: String, events: List<LifeEvent>): Boolean =
        used(profile.characterId, date, kind, events) < cap(profile, kind)

    fun eligibleScheduled(
        action: ScheduledWorldAction, profile: CharacterRuntimeProfile, clock: WorldClock,
        events: List<LifeEvent>, settings: ProactiveSettings, state: ProactiveState,
        nowEpochMs: Long, realMinute: Int, realDate: String, hasCurrentCall: Boolean,
    ): Boolean {
        val date = action.worldDate.ifBlank { clock.dateLabel }
        val type = when {
            action.type == ScheduledActionType.INCOMING_CALL -> "call"
            action.type == ScheduledActionType.LETTER_DELIVERY -> "letter"
            action.type == ScheduledActionType.MOMENT || action.lifeEventType == LifeEventType.MOMENT -> "moment"
            action.lifeEventType == LifeEventType.PHOTO -> "photo"
            action.lifeEventType == LifeEventType.MESSAGE -> "message"
            else -> null
        }
        if (type != null && (CharacterBehaviorRuntime.isSleeping(profile, action.triggerTimeMinutes) ||
                    !available(profile, date, type, events))) return false
        val contact = type in setOf("call", "letter", "message") || "user" in action.relatedCharacterIds
        if (contact) {
            if (hasCurrentCall || CharacterBehaviorRuntime.isSleeping(profile, action.triggerTimeMinutes)) return false
            if (!ProactiveRules.shouldFire(CharacterInitiativeRuntime.contactSettings(settings, profile), state, nowEpochMs, realMinute, realDate)) return false
            if (UserContactCooldown.recentlyContacted(clock.copy(dateLabel = date, minutesOfDay = action.triggerTimeMinutes), events)) return false
        }
        return true
    }
}
