package com.example.data.engine

import com.example.data.local.AiluaLocalStore
import com.example.data.model.CallAction
import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.data.model.CallType
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.relationship.romance.RomanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CallStateEngine
 *
 * Single authority for virtual companion call state machine.
 * Manages virtual companion voice sessions without system Telecom / WebRTC bloat.
 * Persists completed sessions in AiluaLocalStore and emits life events on call end.
 */
object CallStateEngine {

    private val _currentCall = MutableStateFlow<CallSession?>(null)
    val currentCall: StateFlow<CallSession?> = _currentCall.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeaker = MutableStateFlow(true)
    val isSpeaker: StateFlow<Boolean> = _isSpeaker.asStateFlow()

    private val defaultHistory = listOf(
        CallSession(
            id = "call_past_1",
            characterId = "mira",
            callerName = "小弥",
            avatarId = "mira",
            type = CallType.VOICE,
            state = CallState.ENDED,
            reason = "傍晚散步时的随心轻语",
            scheduledAtMinutes = 18 * 60 + 20,
            scheduledAtTime = "18:20",
            durationSeconds = 142
        )
    )

    private val _callHistory = MutableStateFlow<List<CallSession>>(initialHistory())
    val callHistory: StateFlow<List<CallSession>> = _callHistory.asStateFlow()

    private fun initialHistory(): List<CallSession> {
        val saved = AiluaLocalStore.savedCallHistory.value
        return if (saved.isNotEmpty()) saved else defaultHistory
    }

    fun syncWithLocalStore() {
        val saved = AiluaLocalStore.savedCallHistory.value
        if (saved.isNotEmpty()) {
            _callHistory.value = (saved + _callHistory.value).distinctBy { it.id }
        }
    }

    fun triggerIncomingCall(
        characterId: String = "mira",
        callerName: String = "小弥",
        reason: String = "窗外雨下得很大，要不要陪我听一会儿？",
        timeLabel: String = "22:45",
        userInitiated: Boolean = false,
    ): CallSession {
        val session = CallSession(
            id = "call_${System.currentTimeMillis()}",
            characterId = characterId,
            callerName = callerName,
            avatarId = characterId,
            type = CallType.VOICE,
            state = CallState.INCOMING,
            reason = reason,
            scheduledAtMinutes = 22 * 60 + 45,
            scheduledAtTime = timeLabel,
            userInitiated = userInitiated,
        )
        _currentCall.value = session
        return session
    }

    /**
     * Single action handling API for all call state transitions.
     */
    fun handleAction(action: CallAction, nowEpochMs: Long = System.currentTimeMillis()) {
        val session = _currentCall.value ?: return
        when (action) {
            CallAction.ANSWER -> {
                _currentCall.value = session.copy(
                    state = CallState.CONNECTED,
                    startedAt = nowEpochMs
                )
            }
            CallAction.DECLINE -> {
                val finished = session.copy(
                    state = CallState.DECLINED,
                    endedAt = nowEpochMs
                )
                _currentCall.value = null
                val updated = listOf(finished) + _callHistory.value
                _callHistory.value = updated
                AiluaLocalStore.appendCallHistory(finished)
                AiluaLocalStore.saveCallHistory(updated)
            }
            CallAction.END -> {
                val endedAt = nowEpochMs
                val actualDuration = if (session.state == CallState.CONNECTED && session.startedAt > 0L)
                    ((endedAt - session.startedAt).coerceAtLeast(0L) / 1_000L).toInt() else 0
                val finished = session.copy(
                    state = CallState.ENDED,
                    endedAt = endedAt,
                    durationSeconds = actualDuration,
                )
                _currentCall.value = null
                val updated = listOf(finished) + _callHistory.value
                _callHistory.value = updated
                AiluaLocalStore.appendCallHistory(finished)
                AiluaLocalStore.saveCallHistory(updated)
                RomanceRepository.recordUserCall(finished.characterId, finished.id, actualDuration, finished.userInitiated, endedAt)

                // Record life event into unified timeline
                WorldStateRepository.appendLifeEvent(
                    LifeEvent(
                        id = "pulse_call_${finished.id}",
                        characterId = finished.characterId,
                        time = finished.scheduledAtTime,
                        type = LifeEventType.SOCIAL,
                        title = if (actualDuration > 0) "与${finished.callerName}结束了通话" else "与${finished.callerName}的通话已取消",
                        description = if (actualDuration > 0) "通话持续了 ${finished.durationSeconds} 秒。" else "未建立持续通话。",
                        sourceAppId = "call",
                        sourceRefId = finished.id,
                        relatedCharacterIds = listOf("user")
                    )
                )
            }
            CallAction.TOGGLE_MUTE -> {
                _isMuted.value = !_isMuted.value
            }
            CallAction.TOGGLE_SPEAKER -> {
                _isSpeaker.value = !_isSpeaker.value
            }
        }
    }

    fun incrementDuration(seconds: Int = 1) {
        val session = _currentCall.value ?: return
        if (session.state == CallState.CONNECTED) {
            _currentCall.value = session.copy(durationSeconds = session.durationSeconds + seconds)
        }
    }

    fun clearCurrentCall() {
        _currentCall.value = null
    }
}
