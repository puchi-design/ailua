package com.example

import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.data.systemui.live.LiveActivityProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveActivityProjectionTest {
    @Test
    fun onlyConnectedCallsAppearAndUseConnectionTimeUntilEnded() {
        val incoming = CallSession(id = "call_yuna", characterId = "yuna", callerName = "悠奈")
        assertTrue(LiveActivityProjection.fromCall(incoming).isEmpty())
        val connected = incoming.copy(state = CallState.CONNECTED, startedAt = 10_000L,
            durationSeconds = 999) // The screen's incremental duration is not the timer authority.
        val activity = LiveActivityProjection.fromCall(connected).single()
        assertEquals("call/yuna", activity.route)
        assertEquals(10_000L, activity.startedAtEpochMs ?: 0L)
        assertEquals("02:14", LiveActivityProjection.durationLabel(activity, 144_000L))
        assertEquals("00:00", LiveActivityProjection.durationLabel(activity, 5_000L))
        assertEquals(listOf("call_yuna", "background"), LiveActivityProjection.ordered(listOf(
            activity.copy(id = "background", priority = 1), activity,
        )).map { it.id })
        assertTrue(LiveActivityProjection.fromCall(connected.copy(state = CallState.ENDED)).isEmpty())
        assertTrue(LiveActivityProjection.fromCall(null).isEmpty())
    }
}
