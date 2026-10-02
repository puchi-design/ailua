package com.example.data.systemui.live

import com.example.data.engine.CallStateEngine
import com.example.data.model.CallSession
import com.example.data.model.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

enum class LiveActivityType { CALL, AI_RESPONSE, MEDIA, WORLD }

data class VirtualLiveActivity(
    val id: String,
    val type: LiveActivityType,
    val title: String,
    val subtitle: String? = null,
    val startedAtEpochMs: Long? = null,
    val sourceAppId: String,
    val route: String? = null,
    val priority: Int = 0,
)

/** A projection, not another call state machine or a persisted notification. */
object LiveActivityProjection {
    fun fromCall(call: CallSession?): List<VirtualLiveActivity> {
        if (call?.state != CallState.CONNECTED) return emptyList()
        return listOf(VirtualLiveActivity(
            id = call.id,
            type = LiveActivityType.CALL,
            title = call.callerName,
            subtitle = "心契通话中",
            startedAtEpochMs = call.startedAt.takeIf { it > 0L },
            sourceAppId = "companion_call",
            route = "call/${call.characterId}",
            priority = 100,
        ))
    }

    /** Keep stable tie ordering when future producers add media or response activities. */
    fun ordered(activities: List<VirtualLiveActivity>): List<VirtualLiveActivity> =
        activities.distinctBy { it.id }.sortedByDescending { it.priority }

    fun durationLabel(activity: VirtualLiveActivity, nowEpochMs: Long): String {
        val start = activity.startedAtEpochMs ?: return "00:00"
        val seconds = ((nowEpochMs - start).coerceAtLeast(0L) / 1_000L)
        return if (seconds >= 3_600L) {
            String.format(Locale.US, "%d:%02d:%02d", seconds / 3_600L, seconds / 60L % 60L, seconds % 60L)
        } else {
            String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L)
        }
    }
}

object LiveActivityCenter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val activities: StateFlow<List<VirtualLiveActivity>> = CallStateEngine.currentCall
        .map(LiveActivityProjection::fromCall)
        .stateIn(scope, SharingStarted.Eagerly, LiveActivityProjection.fromCall(CallStateEngine.currentCall.value))
}
