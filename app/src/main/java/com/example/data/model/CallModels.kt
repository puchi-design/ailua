package com.example.data.model

import kotlinx.serialization.Serializable

enum class CallType(val label: String) {
    VOICE("心契音频通话"),
    VIDEO_PLACEHOLDER("全息伴生视界")
}

enum class CallState {
    INCOMING,
    RINGING,
    CONNECTED,
    ENDED,
    MISSED,
    DECLINED
}

enum class CallAction {
    ANSWER,
    DECLINE,
    END,
    TOGGLE_MUTE,
    TOGGLE_SPEAKER
}

@Serializable
data class CallSession(
    val id: String,
    val characterId: String,
    val callerName: String,
    val avatarId: String = "mira",
    val type: CallType = CallType.VOICE,
    var state: CallState = CallState.INCOMING,
    val reason: String = "窗外雨下得很大，要不要陪我听一会儿？",
    val scheduledAtMinutes: Int = 22 * 60 + 45, // 22:45
    val scheduledAtTime: String = "22:45",
    var startedAt: Long = 0L,
    var endedAt: Long = 0L,
    var durationSeconds: Int = 0
)
