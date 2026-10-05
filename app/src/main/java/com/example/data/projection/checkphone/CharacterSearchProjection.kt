package com.example.data.projection.checkphone

import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.model.CheckPhoneData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.data.mock.MockData
import com.example.data.projection.eligiblePhoneEvents

/** Explicit runtime searches precede character-specific authored history. */
fun projectCharacterSearchHistory(
    characterId: String,
    events: List<LifeEvent>,
    combined: CheckPhoneData,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
    profile: CharacterRuntimeProfile? = null,
    clock: WorldClock? = null,
): List<PhoneSearchRecord> {
    val relevantEvents = eligiblePhoneEvents(characterId, events, seedEventIds)
    val runtime = relevantEvents.mapNotNull { event ->
        event.metadata["search_query"]?.trim()?.takeIf(String::isNotEmpty)?.let { query ->
            PhoneSearchRecord("search:${event.id}", query, event.time, PhoneTraceSource.RUNTIME_EVENT)
        }
    }
    // First-version rule projection: these queries follow the character's current work,
    // location and actual recent events. A source tag distinguishes them from recorded searches.
    val derived = buildList {
        val latest = relevantEvents.firstOrNull { it.location?.isNotBlank() == true }
        latest?.let { event ->
            val place = event.location!!.trim().take(24)
            val query = when (event.type) {
                LifeEventType.PHOTO -> "$place 附近照片冲印"
                LifeEventType.MEAL -> "$place 附近晚餐"
                LifeEventType.TRAVEL, LifeEventType.LOCATION_CHANGE -> "$place 附近营业时间"
                else -> null
            }
            if (query != null) add(PhoneSearchRecord("rule-location:${event.id}", query,
                null, PhoneTraceSource.RULE_PROJECTION))
        }
        val occupation = profile?.identity?.occupation.orEmpty()
        if (clock?.weather in setOf(WeatherState.RAIN, WeatherState.HEAVY_RAIN)) {
            val workQuery = when {
                "建筑" in occupation || "空间" in occupation -> "雨天木材防潮怎么处理"
                "摄影" in occupation -> "雨天室内人像拍摄光线"
                "烘焙" in occupation -> "潮湿天气面团醒发时间"
                "鼓手" in occupation -> "雨天鼓皮保养"
                "声音" in occupation -> "雨天室外录音设备防潮"
                else -> null
            }
            if (workQuery != null) add(PhoneSearchRecord("rule-weather:$characterId:${clock?.dateLabel.orEmpty()}",
                workQuery, null, PhoneTraceSource.RULE_PROJECTION))
        }
        val workplace = profile?.life?.workplace.orEmpty()
        if (workplace.isNotBlank() && clock != null && clock.minutesOfDay in 19 * 60..23 * 60 &&
            relevantEvents.any { it.location == workplace }) {
            add(PhoneSearchRecord("rule-evening:$characterId:${clock.dateLabel}",
                "${workplace.take(18)}附近咖啡店几点关门", null, PhoneTraceSource.RULE_PROJECTION))
        }
    }.take(2)
    val authored = combined.searchHistory.drop(runtime.size).mapIndexed { index, query ->
        PhoneSearchRecord("authored-search:$characterId:$index", query, null, PhoneTraceSource.AUTHORED)
    }
    return (runtime + derived + authored).distinctBy { it.query }.take(20)
}
