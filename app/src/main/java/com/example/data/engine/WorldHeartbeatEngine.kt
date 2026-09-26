package com.example.data.engine

import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.model.DayPhase
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.data.repository.MailboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScheduledActionType {
    LIFE_EVENT,
    LETTER_DELIVERY,
    INCOMING_CALL,
    GALLERY_ASSET,
    MOMENT,
    LOCATION_CHANGE
}

data class ScheduledWorldAction(
    val id: String,
    val triggerTimeMinutes: Int,
    val triggerTimeString: String,
    val type: ScheduledActionType,
    val characterId: String,
    val payloadId: String,
    val title: String,
    val description: String,
    var fired: Boolean = false
)

enum class TimeOfDayPhase(val label: String, val icon: String, val atmosphere: String) {
    DAWN("清晨薄曦", "🌅", "微光穿透薄雾，窗外风铃初鸣"),
    AFTERNOON("午后漫步", "☕", "阳光洒在木兰茶馆庭院，司康初出炉"),
    DUSK("暮色斜照", "🌇", "街角路灯次第亮起，晚风微凉"),
    RAINY_NIGHT("秋雨夜谈", "🌧️", "窗外细雨连绵，室内红茶温热，心网守候")
}

data class WorldHeartbeatState(
    val currentPhase: TimeOfDayPhase = TimeOfDayPhase.RAINY_NIGHT,
    val activePlaceId: String = "place_street_23",
    val activeCharacterId: String = "mira",
    val isProactiveTakeoverActive: Boolean = false,
    val proactiveMessage: String? = null,
    val initiativeScores: Map<String, Int> = mapOf("mira" to 88, "yuna" to 75, "noa" to 65),
    val lastPulseTime: String = "21:30"
)

/**
 * WorldHeartbeatEngine
 *
 * Core engine driving the living autonomous pulse of the companion world.
 * - Maintains dynamic reactive WorldClock with midnight advancement
 * - Evaluates scheduled timeline actions crossing time intervals
 * - Dispatches letters to Mailbox and incoming calls to CallStateEngine (sole authority)
 * - Persists clock time and fired actions to AiluaLocalStore
 */
object WorldHeartbeatEngine {

    private val _worldClock = MutableStateFlow(
        WorldClock(
            dateLabel = "9月25日",
            minutesOfDay = 21 * 60 + 30, // 21:30
            dayPhase = DayPhase.EVENING,
            weather = WeatherState.RAIN
        )
    )
    val worldClock: StateFlow<WorldClock> = _worldClock.asStateFlow()

    private val _heartbeatState = MutableStateFlow(
        WorldHeartbeatState(
            currentPhase = TimeOfDayPhase.DUSK,
            isProactiveTakeoverActive = false,
            activeCharacterId = "mira",
            lastPulseTime = "21:30",
            proactiveMessage = null
        )
    )
    val heartbeatState: StateFlow<WorldHeartbeatState> = _heartbeatState.asStateFlow()

    private val initialActions = listOf(
        ScheduledWorldAction(
            id = "sched_mira_tea_1930",
            triggerTimeMinutes = 19 * 60 + 30,
            triggerTimeString = "19:30",
            type = ScheduledActionType.LIFE_EVENT,
            characterId = "mira",
            payloadId = "event_tea_1",
            title = "小弥在飘窗前泡了一壶红茶",
            description = "傍晚的雨声中，小弥把刚煮好的大吉岭红茶倒进骨瓷杯，静静看着窗外雾气。"
        ),
        ScheduledWorldAction(
            id = "sched_mira_window_2145",
            triggerTimeMinutes = 21 * 60 + 45,
            triggerTimeString = "21:45",
            type = ScheduledActionType.LIFE_EVENT,
            characterId = "mira",
            payloadId = "event_window_rain",
            title = "小弥在书桌前整理秋雨随笔",
            description = "桌角暖灯微亮，小弥在日记本写下：‘今日青石街雨水清澈，想念随心网悄悄漫延’。"
        ),
        ScheduledWorldAction(
            id = "sched_yuna_letter_2230",
            triggerTimeMinutes = 22 * 60 + 30,
            triggerTimeString = "22:30",
            type = ScheduledActionType.LETTER_DELIVERY,
            characterId = "yuna",
            payloadId = "letter_yuna_1",
            title = "悠奈寄来了全家手写便签",
            description = "全家便利店门口避雨时写下的心绪，附带一张热豆浆的手绘明信片。"
        ),
        ScheduledWorldAction(
            id = "sched_mira_call_2245",
            triggerTimeMinutes = 22 * 60 + 45,
            triggerTimeString = "22:45",
            type = ScheduledActionType.INCOMING_CALL,
            characterId = "mira",
            payloadId = "call_mira_rain_night",
            title = "小弥打来伴生夜话通话",
            description = "窗外雨下得很大，要不要陪我听一会儿？"
        ),
        ScheduledWorldAction(
            id = "sched_noa_letter_2300",
            triggerTimeMinutes = 23 * 60 + 0,
            triggerTimeString = "23:00",
            type = ScheduledActionType.LETTER_DELIVERY,
            characterId = "noa",
            payloadId = "letter_noa_1",
            title = "诺亚寄达了银杏叶书签明信片",
            description = "月光书阁负一层投递了一枚珍藏十年的银杏叶书签明信片。"
        ),
        ScheduledWorldAction(
            id = "sched_mira_sleep_2330",
            triggerTimeMinutes = 23 * 60 + 30,
            triggerTimeString = "23:30",
            type = ScheduledActionType.LOCATION_CHANGE,
            characterId = "mira",
            payloadId = "place_street_23_bed",
            title = "小弥熄灭了飘窗台灯",
            description = "小弥合上诗集，在心网道了晚安，青石街23号二层透出柔和的夜灯。"
        )
    )

    private val _scheduledActions = MutableStateFlow<List<ScheduledWorldAction>>(emptyList())
    val scheduledActions: StateFlow<List<ScheduledWorldAction>> = _scheduledActions.asStateFlow()

    init {
        // Sync clock and actions with local store
        val savedMinutes = AiluaLocalStore.getVirtualMinutes(21 * 60 + 30)
        val savedDate = AiluaLocalStore.getVirtualDate("9月25日")
        val firedIds = AiluaLocalStore.getFiredWorldActionIds()

        val restoredActions = initialActions.map { action ->
            if (firedIds.contains(action.id)) {
                action.copy(fired = true)
            } else {
                action
            }
        }
        _scheduledActions.value = restoredActions
        updateClockInternal(savedMinutes, savedDate)
    }

    /**
     * Advances virtual time by [deltaMinutes].
     * Uses WorldTimeAdvancer to calculate exact midnight and date progression,
     * evaluates scheduled actions crossed in the interval, and persists state.
     */
    fun advanceTime(deltaMinutes: Int) {
        if (deltaMinutes <= 0) return
        val currentClock = _worldClock.value
        val startMinutes = currentClock.minutesOfDay

        val result = WorldTimeAdvancer.advance(
            currentMinutes = startMinutes,
            currentDateLabel = currentClock.dateLabel,
            deltaMinutes = deltaMinutes
        )

        updateClockInternal(result.newMinutes, result.newDateLabel)
        evaluateDueScheduledActions(startMinutes, deltaMinutes)
    }

    /**
     * Jumps directly to the timestamp of the next unfired scheduled event.
     */
    fun jumpToNextScheduledEvent() {
        val currentMinutes = _worldClock.value.minutesOfDay
        val firedIds = AiluaLocalStore.getFiredWorldActionIds()
        val nextAction = _scheduledActions.value.firstOrNull {
            !it.fired && !firedIds.contains(it.id) && it.triggerTimeMinutes > currentMinutes
        } ?: _scheduledActions.value.firstOrNull {
            !it.fired && !firedIds.contains(it.id)
        }

        if (nextAction != null) {
            val delta = if (nextAction.triggerTimeMinutes >= currentMinutes) {
                nextAction.triggerTimeMinutes - currentMinutes
            } else {
                (1440 - currentMinutes) + nextAction.triggerTimeMinutes
            }
            advanceTime(delta)
        } else {
            advanceTime(30)
        }
    }

    private fun updateClockInternal(minutes: Int, date: String) {
        val phase = when (minutes) {
            in 300..479 -> DayPhase.DAWN         // 05:00 - 07:59
            in 480..689 -> DayPhase.MORNING      // 08:00 - 11:29
            in 690..839 -> DayPhase.NOON         // 11:30 - 13:59
            in 840..1049 -> DayPhase.AFTERNOON   // 14:00 - 17:29
            in 1050..1169 -> DayPhase.DUSK       // 17:30 - 19:29
            in 1170..1319 -> DayPhase.EVENING    // 19:30 - 21:59
            in 1320..1439 -> DayPhase.NIGHT      // 22:00 - 23:59
            else -> DayPhase.LATE_NIGHT          // 00:00 - 04:59
        }

        val legacyPhase = when (phase) {
            DayPhase.DAWN, DayPhase.MORNING -> TimeOfDayPhase.DAWN
            DayPhase.NOON, DayPhase.AFTERNOON -> TimeOfDayPhase.AFTERNOON
            DayPhase.DUSK, DayPhase.EVENING -> TimeOfDayPhase.DUSK
            DayPhase.NIGHT, DayPhase.LATE_NIGHT -> TimeOfDayPhase.RAINY_NIGHT
        }

        val weather = if (minutes >= 1140 || minutes < 300) WeatherState.RAIN else WeatherState.CLOUDY

        _worldClock.value = WorldClock(
            dateLabel = date,
            minutesOfDay = minutes,
            dayPhase = phase,
            weather = weather
        )

        val formatted = _worldClock.value.timeFormatted
        _heartbeatState.value = _heartbeatState.value.copy(
            currentPhase = legacyPhase,
            lastPulseTime = formatted
        )

        AiluaLocalStore.saveVirtualMinutes(minutes)
        AiluaLocalStore.saveVirtualDate(date)

        // Sync Mailbox deliveries
        MailboxRepository.checkScheduledDeliveries(minutes)
    }

    private fun evaluateDueScheduledActions(startMinutes: Int, deltaMinutes: Int) {
        val firedIds = AiluaLocalStore.getFiredWorldActionIds()
        val updated = _scheduledActions.value.map { action ->
            if (!action.fired && !firedIds.contains(action.id) &&
                WorldTimeAdvancer.isActionTriggeredInInterval(action.triggerTimeMinutes, startMinutes, deltaMinutes)
            ) {
                executeAction(action)
                AiluaLocalStore.markWorldActionFired(action.id)
                action.copy(fired = true)
            } else {
                action
            }
        }
        _scheduledActions.value = updated
    }

    private fun executeAction(action: ScheduledWorldAction) {
        when (action.type) {
            ScheduledActionType.LIFE_EVENT, ScheduledActionType.MOMENT -> {
                WorldStateRepository.appendLifeEvent(
                    LifeEvent(
                        id = "pulse_sched_${action.id}",
                        characterId = action.characterId,
                        time = action.triggerTimeString,
                        type = LifeEventType.THOUGHT,
                        title = action.title,
                        description = action.description,
                        location = if (action.characterId == "yuna") "街角全家便利店" else if (action.characterId == "noa") "月光书阁" else "青石街23号"
                    )
                )
            }
            ScheduledActionType.LETTER_DELIVERY -> {
                MailboxRepository.checkScheduledDeliveries(action.triggerTimeMinutes)
            }
            ScheduledActionType.INCOMING_CALL -> {
                // CallStateEngine is the sole authority
                CallStateEngine.triggerIncomingCall(
                    characterId = action.characterId,
                    callerName = if (action.characterId == "yuna") "悠奈" else if (action.characterId == "noa") "诺亚" else "小弥",
                    reason = action.description,
                    timeLabel = action.triggerTimeString
                )
            }
            ScheduledActionType.LOCATION_CHANGE -> {
                WorldStateRepository.appendLifeEvent(
                    LifeEvent(
                        id = "pulse_loc_${action.id}",
                        characterId = action.characterId,
                        time = action.triggerTimeString,
                        type = LifeEventType.LOCATION_CHANGE,
                        title = action.title,
                        description = action.description,
                        location = "青石街23号 · 卧房"
                    )
                )
            }
            ScheduledActionType.GALLERY_ASSET -> {}
        }
    }

    fun cycleTimePhase() {
        advanceTime(90)
    }

    fun triggerScheduledTakeover(characterId: String = "mira"): LifeEvent {
        val character = MockData.allCharacters[characterId] ?: MockData.sampleCharacter
        val newEvent = LifeEvent(
            id = "pulse_${System.currentTimeMillis()}",
            characterId = character.id,
            time = _worldClock.value.timeFormatted,
            type = LifeEventType.THOUGHT,
            title = "${character.name}的主动牵挂",
            description = when (characterId) {
                "yuna" -> "悠奈在心网发来提示：刚才看到天边有彩虹，一定要提醒你抬头看！🌈"
                "noa" -> "诺亚在书阁整理笔记时，摘录了一句适合今晚心绪的诗句发到了你的便签。"
                else -> "小弥伸手轻触飘窗风铃，为你把保温垫上的红茶温度调整到最佳。"
            },
            location = character.location
        )
        WorldStateRepository.appendLifeEvent(newEvent)
        _heartbeatState.value = _heartbeatState.value.copy(
            isProactiveTakeoverActive = true,
            activeCharacterId = characterId,
            proactiveMessage = newEvent.description
        )
        return newEvent
    }

    fun dismissProactiveTakeover() {
        _heartbeatState.value = _heartbeatState.value.copy(
            isProactiveTakeoverActive = false,
            proactiveMessage = null
        )
    }
}
