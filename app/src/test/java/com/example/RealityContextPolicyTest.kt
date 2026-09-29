package com.example

import com.example.data.reality.RealityContextPolicy
import com.example.data.reality.RealitySettings
import com.example.data.reality.RealitySnapshot
import org.junit.Assert.*
import org.junit.Test

class RealityContextPolicyTest {
    @Test fun missingOrDisabledSignalsNeverBecomeZeroOrPromptText() {
        assertNull(RealityContextPolicy.context(RealitySnapshot(1L), RealitySettings()))
        assertNull(RealityContextPolicy.context(RealitySnapshot(1L, batteryPercent = 8, charging = false), RealitySettings(enabled = false)))
    }

    @Test fun lowSensitivitySummaryRequiresOptInsAndThresholds() {
        val snapshot = RealitySnapshot(1L, batteryPercent = 12, charging = false, todayScreenTimeMinutes = 325,
            recentAppCategory = "社交", todaySteps = 8215)
        val defaultText = RealityContextPolicy.context(snapshot, RealitySettings())!!
        assertTrue(defaultText.contains("12%"))
        assertFalse(defaultText.contains("使用手机"))
        assertFalse(defaultText.contains("步"))
        val allText = RealityContextPolicy.context(snapshot, RealitySettings(usageEnabled = true, healthEnabled = true))!!
        assertTrue(allText.contains("5 小时"))
        assertTrue(allText.contains("8000 步"))
        assertFalse(allText.contains("社交"))
    }
}
