package com.example

import com.example.ui.components.OsChromeFormat
import com.example.ui.components.OsChromeState
import com.example.ui.components.OsNetworkKind
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic coverage for the P3A-1 OS chrome state model.
 *
 * Deliberately avoids Android runtime APIs: everything asserted here is the
 * formatting/scheduling logic that decides what the status bar renders.
 */
class OsChromeStateTest {

    @Test
    fun timeLabel_padsSingleDigits() {
        assertEquals("07:05", OsChromeFormat.timeLabel(7, 5))
        assertEquals("00:00", OsChromeFormat.timeLabel(0, 0))
        assertEquals("21:48", OsChromeFormat.timeLabel(21, 48))
        assertEquals("23:59", OsChromeFormat.timeLabel(23, 59))
    }

    @Test
    fun timeLabel_clampsOutOfRangeComponents() {
        assertEquals("00:00", OsChromeFormat.timeLabel(-1, -1))
        assertEquals("23:59", OsChromeFormat.timeLabel(24, 99))
    }

    @Test
    fun timeLabel_fromEpochFollowsProvidedTimeZone() {
        // epoch 0 = 1970-01-01T00:00:00Z
        assertEquals("00:00", OsChromeFormat.timeLabel(0L, TimeZone.getTimeZone("UTC")))
        assertEquals("08:00", OsChromeFormat.timeLabel(0L, TimeZone.getTimeZone("GMT+08:00")))
        assertEquals("19:00", OsChromeFormat.timeLabel(0L, TimeZone.getTimeZone("America/New_York")))

        // 1970-01-01T13:37:00Z -> 21:37 in GMT+08:00
        val epoch = 13 * 3_600_000L + 37 * 60_000L
        assertEquals("21:37", OsChromeFormat.timeLabel(epoch, TimeZone.getTimeZone("GMT+08:00")))
    }

    @Test
    fun millisUntilNextMinute_returnsDelayToNextBoundary() {
        // Already on a boundary: must not return 0 (busy loop).
        assertEquals(60_000L, OsChromeFormat.millisUntilNextMinute(0L))
        assertEquals(60_000L, OsChromeFormat.millisUntilNextMinute(60_000L))

        // 1ms past a boundary -> 59_999ms left.
        assertEquals(59_999L, OsChromeFormat.millisUntilNextMinute(60_001L))

        // 123_456ms is 3_456ms into its minute -> 56_544ms left.
        assertEquals(56_544L, OsChromeFormat.millisUntilNextMinute(123_456L))

        // Pre-epoch timestamps must still yield a positive delay.
        assertEquals(1L, OsChromeFormat.millisUntilNextMinute(-1L))
    }

    @Test
    fun millisUntilNextMinute_alwaysLandsOnMinuteBoundary() {
        for (epoch in listOf(-123_457L, -1L, 0L, 1L, 59_999L, 60_000L, 123_456L, 8_639_999L)) {
            val delay = OsChromeFormat.millisUntilNextMinute(epoch)
            val next = epoch + delay
            assertEquals("boundary for $epoch", 0L, next % 60_000L)
            assertTrue("delay for $epoch must be positive", delay > 0L)
        }
    }

    @Test
    fun batteryLabel_rendersClampedPercentage() {
        assertEquals("92%", OsChromeFormat.batteryLabel(92))
        assertEquals("0%", OsChromeFormat.batteryLabel(0))
        assertEquals("100%", OsChromeFormat.batteryLabel(100))
        assertEquals("0%", OsChromeFormat.batteryLabel(-5))
        assertEquals("100%", OsChromeFormat.batteryLabel(150))
    }

    @Test
    fun networkLabel_mapsConnectivityKinds() {
        assertEquals("心网 WiFi", OsChromeFormat.networkLabel(OsNetworkKind.WIFI))
        assertEquals("心网 5G", OsChromeFormat.networkLabel(OsNetworkKind.CELLULAR))
        assertEquals("心网 离线", OsChromeFormat.networkLabel(OsNetworkKind.NONE))
    }

    @Test
    fun previewBaseline_isStableAcrossRenders() {
        val baseline = OsChromeState.PreviewBaseline
        assertEquals("21:48", baseline.timeLabel)
        assertEquals(92, baseline.batteryPercent)
        assertEquals("92%", baseline.batteryLabel)
        assertEquals("心网 5G", baseline.networkLabel)
    }
}
