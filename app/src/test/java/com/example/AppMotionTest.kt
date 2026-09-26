package com.example

import com.example.ui.motion.AppMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionTest {

    @Test
    fun `open and close durations stay inside the motion window`() {
        assertTrue(AppMotion.OPEN_DURATION_MS in AppMotion.MIN_DURATION_MS..AppMotion.MAX_DURATION_MS)
        assertTrue(AppMotion.CLOSE_DURATION_MS in AppMotion.MIN_DURATION_MS..AppMotion.MAX_DURATION_MS)
    }

    @Test
    fun `scale anchors are the agreed scale value`() {
        assertEquals(0.96f, AppMotion.OPEN_SCALE, 0.0001f)
        assertEquals(0.96f, AppMotion.CLOSE_SCALE, 0.0001f)
        assertTrue(AppMotion.OPEN_SCALE < 1f)
        assertTrue(AppMotion.CLOSE_SCALE < 1f)
    }

    @Test
    fun `easing is monotonic and never overshoots`() {
        var previous = -1f
        for (step in 0..100) {
            val t = step / 100f
            val value = AppMotion.EASING.transform(t)
            assertTrue("value out of [0,1] at t=$t", value in 0f..1f)
            assertTrue("non monotonic at t=$t", value >= previous)
            previous = value
        }
    }

    @Test
    fun `easing starts and ends exactly at the edges`() {
        assertEquals(0f, AppMotion.EASING.transform(0f), 0.0001f)
        assertEquals(1f, AppMotion.EASING.transform(1f), 0.0001f)
    }

    @Test
    fun `transition specs carry the shared duration and easing`() {
        val enter = AppMotion.enterSpec()
        val exit = AppMotion.exitSpec()
        assertEquals(AppMotion.OPEN_DURATION_MS, enter.durationMillis)
        assertEquals(AppMotion.CLOSE_DURATION_MS, exit.durationMillis)
        assertEquals(AppMotion.EASING, enter.easing)
        assertEquals(AppMotion.EASING, exit.easing)
    }
}
