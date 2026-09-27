package com.example.data.model

/**
 * ProactiveSettings — the user-facing knobs of P3D-2 proactive messages.
 *
 * All timing rules run on REAL device time (not the virtual world clock,
 * which freezes in the background): [intervalHours] between two messages,
 * [quietStartMinute]..[quietEndMinute] of day when nothing is sent
 * (wrap-aware, defaults 23:00-08:00), and [dailyLimit] per real day.
 * Defaults follow the P3D-2 spec: disabled until the user opts in.
 */
data class ProactiveSettings(
    val enabled: Boolean = false,
    val intervalHours: Int = 6,
    val quietStartMinute: Int = 23 * 60,
    val quietEndMinute: Int = 8 * 60,
    val dailyLimit: Int = 3,
)

/**
 * ProactiveState — durable runtime marks of the proactive scheduler
 * (not user settings): when the last message succeeded and how many
 * were sent on the current real day.
 */
data class ProactiveState(
    val lastSuccessAtEpochMs: Long = 0L,
    val sentDate: String = "",
    val sentCount: Int = 0,
)
