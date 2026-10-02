package com.example.ui.systemui.lockscreen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenGestureTest {
    @Test
    fun unlockDistanceUsesDeviceDensity() {
        assertFalse(LockScreenGesture.shouldUnlock(359f, 0f, 3f))
        assertTrue(LockScreenGesture.shouldUnlock(360f, 0f, 3f))
    }

    @Test
    fun upwardFlingRequiresIntentionalTravelAndDirection() {
        assertTrue(LockScreenGesture.shouldUnlock(72f, -2700f, 3f))
        assertFalse(LockScreenGesture.shouldUnlock(12f, -6000f, 3f))
        assertFalse(LockScreenGesture.shouldUnlock(72f, 3000f, 3f))
    }
}
