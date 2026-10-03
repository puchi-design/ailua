package com.example.data.engine

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WorldClock

/** Shared weak contact guard for planned calls/messages and proactive sends. */
object UserContactCooldown {
    fun isContact(event: LifeEvent): Boolean =
        (event.metadata["proactive_content_type"] != null && event.metadata["proactive_content_type"] != "MOMENT") ||
        (event.sourceAppId == "heartbeat" &&
            (event.type == LifeEventType.MESSAGE || "user" in event.relatedCharacterIds || event.sourceRefId == "proactive_message"))

    fun recentlyContacted(clock: WorldClock, events: List<LifeEvent>, minutes: Int = 90): Boolean =
        events.any { event ->
            if (!isContact(event) || event.worldMinutesOfDay !in 0..1439) return@any false
            val offset = WorldPlanValidator.dayOffset(event.worldDateLabel, clock.dateLabel) ?: return@any false
            val elapsed = offset * 1440 + clock.minutesOfDay - event.worldMinutesOfDay
            elapsed in 0 until minutes
        }
}
