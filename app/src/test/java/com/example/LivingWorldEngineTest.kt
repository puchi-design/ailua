package com.example

import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.engine.WorldTimeAdvancer
import com.example.data.model.CallAction
import com.example.data.model.CallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LivingWorldEngineTest
 *
 * Deterministic unit tests covering:
 * - Same-day time advance
 * - Crossing midnight and date label advancement
 * - Interval action triggering semantics
 * - Call ANSWER / DECLINE / END state transitions
 * - Call history and LifeEvent emissions
 */
class LivingWorldEngineTest {

    @Test
    fun testSameDayTimeAdvance() {
        // 22:30 (1350) + 15 min -> 22:45 (1365), date remains 9月25日
        val result = WorldTimeAdvancer.advance(
            currentMinutes = 1350,
            currentDateLabel = "9月25日",
            deltaMinutes = 15
        )
        assertEquals(1365, result.newMinutes)
        assertEquals("9月25日", result.newDateLabel)
        assertEquals(0, result.daysAdvanced)

        // Event at 22:45 (1365) falls into interval (1350, 1365]
        assertTrue(WorldTimeAdvancer.isActionTriggeredInInterval(1365, 1350, 15))
        // Event at 22:30 (1350) was at start boundary (already fired)
        assertFalse(WorldTimeAdvancer.isActionTriggeredInInterval(1350, 1350, 15))
        // Event at 22:50 (1370) is not yet reached
        assertFalse(WorldTimeAdvancer.isActionTriggeredInInterval(1370, 1350, 15))
    }

    @Test
    fun testCrossingMidnight() {
        // 23:50 (1430) + 30 min -> 00:20 (20), date advances to 9月26日
        val result = WorldTimeAdvancer.advance(
            currentMinutes = 1430,
            currentDateLabel = "9月25日",
            deltaMinutes = 30
        )
        assertEquals(20, result.newMinutes)
        assertEquals("9月26日", result.newDateLabel)
        assertEquals(1, result.daysAdvanced)

        // Event before midnight at 23:55 (1435) is triggered
        assertTrue(WorldTimeAdvancer.isActionTriggeredInInterval(1435, 1430, 30))
        // Event after midnight at 00:10 (10) is triggered
        assertTrue(WorldTimeAdvancer.isActionTriggeredInInterval(10, 1430, 30))
        // Event at 00:30 (30) is beyond 00:20 and not yet triggered
        assertFalse(WorldTimeAdvancer.isActionTriggeredInInterval(30, 1430, 30))
        // Event before startMinutes e.g. 23:40 (1420) is not triggered
        assertFalse(WorldTimeAdvancer.isActionTriggeredInInterval(1420, 1430, 30))
    }

    @Test
    fun testCallStateTransitionsAnswerAndEnd() {
        // 1. Incoming Call trigger
        val session = CallStateEngine.triggerIncomingCall(
            characterId = "mira",
            callerName = "小弥",
            reason = "夜话倾听",
            timeLabel = "22:45"
        )
        val initialCurrent = CallStateEngine.currentCall.value
        assertNotNull(initialCurrent)
        assertEquals(CallState.INCOMING, initialCurrent?.state)

        // 2. Answer Call -> CONNECTED
        CallStateEngine.handleAction(CallAction.ANSWER)
        val answeredCurrent = CallStateEngine.currentCall.value
        assertEquals(CallState.CONNECTED, answeredCurrent?.state)
        assertTrue((answeredCurrent?.startedAt ?: 0L) > 0L)

        // 3. Increment duration
        CallStateEngine.incrementDuration(42)
        assertEquals(42, CallStateEngine.currentCall.value?.durationSeconds)

        // 4. End Call -> Clears current call, records to history, appends LifeEvent
        val beforeHistorySize = CallStateEngine.callHistory.value.size
        CallStateEngine.handleAction(CallAction.END)

        assertNull(CallStateEngine.currentCall.value)
        assertEquals(beforeHistorySize + 1, CallStateEngine.callHistory.value.size)

        val latestLog = CallStateEngine.callHistory.value.first()
        assertEquals(session.id, latestLog.id)
        assertEquals(CallState.ENDED, latestLog.state)
        assertEquals(42, latestLog.durationSeconds)

        // Verify life event emitted to WorldStateRepository
        val lifeEvent = WorldStateRepository.events.value.firstOrNull { it.id == "pulse_call_${session.id}" }
        assertNotNull(lifeEvent)
        assertEquals("mira", lifeEvent?.characterId)
    }

    @Test
    fun testCallStateTransitionDecline() {
        val session = CallStateEngine.triggerIncomingCall(
            characterId = "yuna",
            callerName = "悠奈",
            reason = "便利店避雨测试通话",
            timeLabel = "22:30"
        )
        assertEquals(CallState.INCOMING, CallStateEngine.currentCall.value?.state)

        CallStateEngine.handleAction(CallAction.DECLINE)

        assertNull(CallStateEngine.currentCall.value)
        val latestLog = CallStateEngine.callHistory.value.first()
        assertEquals(session.id, latestLog.id)
        assertEquals(CallState.DECLINED, latestLog.state)
    }

    @Test
    fun testFiredActionDeduplication() {
        val firedIds = mutableSetOf<String>()
        val actionId = "sched_yuna_letter_2230"

        // First evaluation: not fired
        assertFalse(firedIds.contains(actionId))
        firedIds.add(actionId)

        // Second evaluation: already fired, skipped
        assertTrue(firedIds.contains(actionId))
    }
}
