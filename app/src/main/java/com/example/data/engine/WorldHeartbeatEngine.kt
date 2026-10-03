package com.example.data.engine

import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.model.DayPhase
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WorldPlan
import com.example.data.repository.MailboxRepository
import com.example.data.registry.CharacterRegistry
import com.example.data.context.CharacterContext
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.initiative.CharacterInitiativeQuota
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@kotlinx.serialization.Serializable
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
    val lifeEventType: LifeEventType = LifeEventType.THOUGHT,
    var fired: Boolean = false,
    val worldDate: String = "",
    val location: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val imageReference: String? = null,
    val relatedCharacterIds: List<String> = emptyList(),
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
    val activeCharacterId: String = CharacterContext.currentId(),
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

    const val DEFAULT_MINUTES_OF_DAY = 21 * 60 + 30

    private val _worldClock = MutableStateFlow(
        WorldClock(
            dateLabel = "9月25日",
            minutesOfDay = DEFAULT_MINUTES_OF_DAY,
            dayPhase = DayPhase.EVENING,
            weather = WeatherState.RAIN
        )
    )
    val worldClock: StateFlow<WorldClock> = _worldClock.asStateFlow()

    private val _heartbeatState = MutableStateFlow(
        WorldHeartbeatState(
            currentPhase = TimeOfDayPhase.DUSK,
            isProactiveTakeoverActive = false,
            activeCharacterId = CharacterContext.currentId(),
            lastPulseTime = "21:30",
            proactiveMessage = null
        )
    )
    val heartbeatState: StateFlow<WorldHeartbeatState> = _heartbeatState.asStateFlow()

    private val _scheduledActions = MutableStateFlow<List<ScheduledWorldAction>>(emptyList())
    val scheduledActions: StateFlow<List<ScheduledWorldAction>> = _scheduledActions.asStateFlow()

    init {
        // Sync clock and actions with local store
        val savedMinutes = AiluaLocalStore.getVirtualMinutes(DEFAULT_MINUTES_OF_DAY)
        val savedDate = AiluaLocalStore.getVirtualDate("9月25日")
        val firedIds = AiluaLocalStore.getFiredWorldActionIds()

        val hasProvider = com.example.data.ai.repository.ProviderGraph.isReady &&
            com.example.data.ai.runtime.ActiveProfileProviderResolver(com.example.data.ai.repository.ProviderGraph.repository).resolve() != null
        val officialCharacter = CharacterContext.currentId() in CharacterRegistry.OFFICIAL_ROMANCE_IDS
        val savedPlan = AiluaLocalStore.savedWorldPlan.value ?: if (!hasProvider && officialCharacter) {
            // futureActions() reads the persisted plan. Keeping this only in _scheduledActions
            // would let cold-start maybePlan() replace the introductory schedule immediately.
            OfficialDemoWorldPlan.createPlan(savedDate, savedMinutes).takeIf(AiluaLocalStore::saveWorldPlan)
        } else null
        val sourceActions = savedPlan?.actions?.map { it.toScheduledAction() }
            ?: if (hasProvider || officialCharacter) emptyList() else DemoSeedWorldPlan.actions
        val restoredActions = sourceActions.map { action ->
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

        evaluateDueScheduledActions(currentClock.dateLabel, startMinutes, deltaMinutes)
        updateClockInternal(result.newMinutes, result.newDateLabel)
    }

    /**
     * Jumps directly to the timestamp of the next unfired scheduled event.
     */
    fun jumpToNextScheduledEvent() {
        val currentClock = _worldClock.value
        val currentMinutes = currentClock.minutesOfDay
        val firedIds = AiluaLocalStore.getFiredWorldActionIds()
        val nextAction = _scheduledActions.value.filter { !it.fired && !firedIds.contains(it.id) }
            .mapNotNull { action ->
                val distance = if (action.worldDate.isNotBlank()) {
                    val offset = WorldPlanValidator.dayOffset(currentClock.dateLabel, action.worldDate) ?: return@mapNotNull null
                    offset * 1440 + action.triggerTimeMinutes - currentMinutes
                } else if (action.triggerTimeMinutes > currentMinutes) action.triggerTimeMinutes - currentMinutes
                else 1440 - currentMinutes + action.triggerTimeMinutes
                if (distance > 0) action to distance else null
            }.minByOrNull { it.second }

        if (nextAction != null) {
            advanceTime(nextAction.second)
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

    private fun evaluateDueScheduledActions(startDate: String, startMinutes: Int, deltaMinutes: Int) {
        val firedIds = AiluaLocalStore.getFiredWorldActionIds()
        val updated = _scheduledActions.value.map { action ->
            if (!action.fired && !firedIds.contains(action.id) &&
                (if (action.worldDate.isNotBlank()) {
                    val offset = WorldPlanValidator.dayOffsetWithin(startDate, action.worldDate, deltaMinutes / 1440 + 1)
                    offset != null && (offset * 1440 + action.triggerTimeMinutes - startMinutes) in 1..deltaMinutes
                } else WorldTimeAdvancer.isActionTriggeredInInterval(action.triggerTimeMinutes, startMinutes, deltaMinutes))
            ) {
                // A now-ineligible plan is consumed without fabricating a fact; future plans are resolved afresh.
                executeAction(action)
                AiluaLocalStore.markWorldActionFired(action.id)
                action.copy(fired = true)
            } else {
                action
            }
        }
        _scheduledActions.value = updated
    }

    /**
     * Data-driven mapping from a scheduled action to the LifeEvent type it must record.
     * The semantic type is declared on the action itself ([ScheduledWorldAction.lifeEventType]);
     * MOMENT and LOCATION_CHANGE schedules are forced to their own types so a
     * "发动态" schedule can never degrade into THOUGHT.
     */
    internal fun lifeEventTypeFor(action: ScheduledWorldAction): LifeEventType = when (action.type) {
        ScheduledActionType.MOMENT -> LifeEventType.MOMENT
        ScheduledActionType.LOCATION_CHANGE -> LifeEventType.LOCATION_CHANGE
        else -> action.lifeEventType
    }

    internal fun executeAction(action: ScheduledWorldAction): Boolean = synchronized(CharacterInitiativeQuota) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val profile = CharacterRuntimeResolver.resolve(action.characterId)
        if (!CharacterInitiativeQuota.eligibleScheduled(action, profile, _worldClock.value, WorldStateRepository.events.value,
                AiluaLocalStore.getProactiveSettings(), AiluaLocalStore.getProactiveState(), now,
                calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE), today, CallStateEngine.currentCall.value != null)) return@synchronized false
        val romance = com.example.data.relationship.romance.RomanceRepository.triggerFacts(action.characterId, now)
        val contact = action.type == ScheduledActionType.INCOMING_CALL || action.type == ScheduledActionType.LETTER_DELIVERY ||
            action.lifeEventType == LifeEventType.MESSAGE || "user" in action.relatedCharacterIds
        if (contact && romance.recentConflict && !romance.readyToReconnect) return@synchronized false
        when (action.type) {
            ScheduledActionType.LIFE_EVENT, ScheduledActionType.MOMENT -> {
                WorldStateRepository.appendLifeEvent(
                    scheduledLifeEvent(
                        action = action,
                        eventId = "pulse_sched_${action.id}",
                        type = lifeEventTypeFor(action),
                        location = action.location ?: CharacterRegistry.getCharacter(action.characterId).location
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
                    callerName = CharacterRegistry.getCharacter(action.characterId).name,
                    reason = action.description,
                    timeLabel = action.triggerTimeString
                )
                WorldStateRepository.appendLifeEvent(
                    scheduledLifeEvent(
                        action = action,
                        eventId = "pulse_call_plan_${action.id}",
                        type = LifeEventType.MESSAGE,
                        location = action.location ?: com.example.data.registry.CharacterRegistry.getCharacter(action.characterId).location,
                    ).copy(relatedCharacterIds = (action.relatedCharacterIds + "user").distinct())
                )
            }
            ScheduledActionType.LOCATION_CHANGE -> {
                WorldStateRepository.appendLifeEvent(
                    scheduledLifeEvent(
                        action = action,
                        eventId = "pulse_loc_${action.id}",
                        type = LifeEventType.LOCATION_CHANGE,
                        location = action.location ?: CharacterRegistry.getCharacter(action.characterId).location
                    )
                )
            }
            ScheduledActionType.GALLERY_ASSET -> {}
        }
        if (contact && action.type != ScheduledActionType.LETTER_DELIVERY) {
            AiluaLocalStore.saveProactiveState(ProactiveRules.withSuccess(AiluaLocalStore.getProactiveState(), now, today))
        }
        true
    }

    private fun scheduledLifeEvent(
        action: ScheduledWorldAction,
        eventId: String,
        type: LifeEventType,
        location: String
    ): LifeEvent {
        return LifeEvent(
            id = eventId,
            characterId = action.characterId,
            time = action.triggerTimeString,
            type = type,
            title = action.title,
            description = action.description,
            location = location,
            worldDateLabel = action.worldDate.ifBlank { _worldClock.value.dateLabel },
            worldMinutesOfDay = action.triggerTimeMinutes,
            sourceAppId = "heartbeat",
            visibility = if (action.type == ScheduledActionType.INCOMING_CALL || type == LifeEventType.MESSAGE || "user" in action.relatedCharacterIds) "PRIVATE" else "PUBLIC",
            sourceRefId = action.id,
            metadata = action.metadata,
            imageReference = action.imageReference,
            relatedCharacterIds = action.relatedCharacterIds,
        )
    }

    fun installPlan(plan: WorldPlan): Boolean {
        val clock = _worldClock.value
        val existing = AiluaLocalStore.savedWorldPlan.value?.actions.orEmpty().filter {
            it.id !in AiluaLocalStore.getFiredWorldActionIds() &&
                WorldPlanValidator.dayOffset(clock.dateLabel, it.triggerWorldDate)?.let { offset -> offset * 1440 + it.triggerMinutes > clock.minutesOfDay } == true
        }
        if (!WorldPlanValidator.validate(plan, clock, existing, WorldStateRepository.events.value)) return false
        val merged = WorldPlan(plan.createdWorldDate, plan.createdMinutes, existing + plan.actions)
        if (!AiluaLocalStore.saveWorldPlan(merged)) return false
        _scheduledActions.value = merged.actions.map { it.toScheduledAction() }
        return true
    }

    fun futureActions(): List<PlannedWorldAction> = AiluaLocalStore.savedWorldPlan.value?.actions.orEmpty()
        .filter { it.id !in AiluaLocalStore.getFiredWorldActionIds() }

    private fun PlannedWorldAction.toScheduledAction() = ScheduledWorldAction(
        id = id, triggerTimeMinutes = triggerMinutes,
        triggerTimeString = "%02d:%02d".format(triggerMinutes / 60, triggerMinutes % 60),
        type = type, characterId = characterId, payloadId = id, title = title,
        description = description, lifeEventType = lifeEventType,
        worldDate = triggerWorldDate, location = location, metadata = metadata,
        imageReference = imageReference, relatedCharacterIds = relatedCharacterIds,
    )

    fun cycleTimePhase() {
        advanceTime(90)
    }

    fun triggerScheduledTakeover(characterId: String = CharacterContext.currentId()): LifeEvent {
        val character = CharacterRegistry.getCharacter(characterId)
        val newEvent = LifeEvent(
            id = "pulse_${System.currentTimeMillis()}",
            characterId = character.id,
            time = _worldClock.value.timeFormatted,
            type = LifeEventType.THOUGHT,
            title = "${character.name}的主动牵挂",
            description = when (characterId) {
                "yuna" -> "悠奈在心网发来提示：刚才看到天边有彩虹，一定要提醒你抬头看！🌈"
                "noa" -> "诺亚在书阁整理笔记时，摘录了一句适合今晚心绪的诗句发到了你的便签。"
                "mira" -> "小弥伸手轻触飘窗风铃，为你把保温垫上的红茶温度调整到最佳。"
                else -> "${character.name}${character.currentActivity}。"
            },
            location = character.location,
            worldDateLabel = _worldClock.value.dateLabel,
            worldMinutesOfDay = _worldClock.value.minutesOfDay,
            sourceAppId = "heartbeat",
            sourceRefId = "proactive_takeover"
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
