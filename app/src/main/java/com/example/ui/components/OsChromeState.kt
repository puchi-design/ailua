package com.example.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * State model for the Virtual Phone's OS chrome (status bar).
 *
 * Layering:
 * ```
 * Android runtime -> OsChromeRuntimeSource -> OsChromeState -> Compose UI
 * PreviewFixtures -> OsChromeState                        -> Compose UI
 * ```
 * The chrome composable never knows whether the state came from the device or a
 * preview fixture.
 */
data class OsChromeState(
    val timeLabel: String,
    val batteryPercent: Int,
    val networkLabel: String
) {
    val batteryLabel: String
        get() = OsChromeFormat.batteryLabel(batteryPercent)

    companion object {
        /**
         * Deterministic baseline used when no runtime source is installed
         * (Compose Preview). Keeps preview screenshots stable across renders.
         */
        val PreviewBaseline = OsChromeState(
            timeLabel = "21:48",
            batteryPercent = 92,
            networkLabel = "心网 5G"
        )
    }
}

enum class OsNetworkKind {
    WIFI,
    CELLULAR,
    NONE
}

/** Pure formatting helpers for OS chrome. Safe to call from plain JVM unit tests. */
object OsChromeFormat {

    fun timeLabel(hour: Int, minute: Int): String = String.format(
        Locale.US,
        "%02d:%02d",
        hour.coerceIn(0, 23),
        minute.coerceIn(0, 59)
    )

    fun timeLabel(epochMillis: Long, timeZone: TimeZone): String {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = epochMillis
        return timeLabel(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    fun timeLabel(epochMillis: Long): String = timeLabel(epochMillis, TimeZone.getDefault())

    /**
     * Milliseconds until the wall clock next crosses a minute boundary, so the
     * chrome refreshes exactly when the displayed minute changes.
     * Returns 60_000 when already sitting on a boundary to avoid a busy loop.
     */
    fun millisUntilNextMinute(epochMillis: Long): Long {
        val remainder = epochMillis % MINUTE_MILLIS
        val positiveRemainder = if (remainder < 0) remainder + MINUTE_MILLIS else remainder
        return if (positiveRemainder == 0L) MINUTE_MILLIS else MINUTE_MILLIS - positiveRemainder
    }

    fun batteryLabel(percent: Int): String = "${percent.coerceIn(0, 100)}%"

    fun networkLabel(kind: OsNetworkKind): String = when (kind) {
        OsNetworkKind.WIFI -> "心网 WiFi"
        OsNetworkKind.CELLULAR -> "心网 5G"
        OsNetworkKind.NONE -> "心网 离线"
    }

    private const val MINUTE_MILLIS = 60_000L
}

/**
 * OS chrome state visible to the UI tree. Defaults to the deterministic
 * baseline so Compose Previews never depend on a running device.
 */
val LocalOsChromeState = staticCompositionLocalOf { OsChromeState.PreviewBaseline }
