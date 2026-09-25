package com.example.data.engine

import com.example.data.mock.MockData
import com.example.data.mock.WorldData
import com.example.data.model.CharacterProfile
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WorldHeartbeatEngine
 *
 * Translates concepts from SillyTavern-GroupWorld into a lightweight,
 * reactive Kotlin Companion OS heartbeat.
 *
 * Core mechanics:
 * - Round State & Orchestration
 * - Speaker Initiative calculation (energy, mood, bond, time context)
 * - Takeover Scheduler (proactive autonomous presence)
 * - Time-of-Day and Ambient Weather Provider
 */
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
    val lastPulseTime: String = "21:40"
)

object WorldHeartbeatEngine {

    private val _heartbeatState = MutableStateFlow(WorldHeartbeatState())
    val heartbeatState: StateFlow<WorldHeartbeatState> = _heartbeatState.asStateFlow()

    /**
     * Advances world round or cycles time-of-day phase.
     */
    fun cycleTimePhase() {
        val nextPhase = when (_heartbeatState.value.currentPhase) {
            TimeOfDayPhase.DAWN -> TimeOfDayPhase.AFTERNOON
            TimeOfDayPhase.AFTERNOON -> TimeOfDayPhase.DUSK
            TimeOfDayPhase.DUSK -> TimeOfDayPhase.RAINY_NIGHT
            TimeOfDayPhase.RAINY_NIGHT -> TimeOfDayPhase.DAWN
        }

        // Recalculate speaker initiative based on phase
        val newInitiative = when (nextPhase) {
            TimeOfDayPhase.DAWN -> mapOf("mira" to 90, "yuna" to 70, "noa" to 40)
            TimeOfDayPhase.AFTERNOON -> mapOf("yuna" to 95, "mira" to 80, "noa" to 60)
            TimeOfDayPhase.DUSK -> mapOf("mira" to 85, "yuna" to 80, "noa" to 75)
            TimeOfDayPhase.RAINY_NIGHT -> mapOf("mira" to 92, "noa" to 88, "yuna" to 65)
        }

        val topSpeaker = newInitiative.maxByOrNull { it.value }?.key ?: "mira"

        _heartbeatState.value = _heartbeatState.value.copy(
            currentPhase = nextPhase,
            activeCharacterId = topSpeaker,
            initiativeScores = newInitiative,
            proactiveMessage = getProactiveAnnouncement(topSpeaker, nextPhase)
        )
    }

    /**
     * Simulates scheduled takeover event (proactive companion presence).
     */
    fun triggerScheduledTakeover(characterId: String = "mira"): LifeEvent {
        val character = MockData.allCharacters[characterId] ?: MockData.sampleCharacter
        val newEvent = LifeEvent(
            id = "pulse_${System.currentTimeMillis()}",
            characterId = character.id,
            time = "刚刚",
            type = LifeEventType.THOUGHT,
            title = "${character.name}的主动牵挂",
            description = when (characterId) {
                "yuna" -> "悠奈在心网发来提示：刚才看到天边有彩虹，一定要提醒你抬头看！🌈"
                "noa" -> "诺亚在书阁整理笔记时，摘录了一句适合今晚心绪的诗句发到了你的便签。"
                else -> "小弥伸手轻触飘窗风铃，为你把保温垫上的红茶温度调整到最佳。"
            },
            location = character.location
        )

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

    private fun getProactiveAnnouncement(characterId: String, phase: TimeOfDayPhase): String {
        return when (characterId) {
            "yuna" -> "悠奈：${phase.label}到啦！今天也要元气满满哦～"
            "noa" -> "诺亚：${phase.label}，此时此刻心绪最适合回归宁静。"
            else -> "小弥：${phase.label}，我一直在这里守候着你。"
        }
    }
}
