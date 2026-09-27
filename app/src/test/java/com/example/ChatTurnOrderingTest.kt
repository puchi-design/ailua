package com.example

import com.example.data.chat.model.ChatTurnRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatTurnOrderingTest
 *
 * PASS 3C-3 spec §5: turn ordering is a stable per-session `position` counter
 * assigned inside the writing transaction — never a timestamp sort, even when
 * every row shares one frozen clock reading.
 */
class ChatTurnOrderingTest {

    @Test
    fun turnsGetStablePositionsInAppendOrder() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        h.repository.appendUserTurn(session.id, "u1")
        h.repository.appendAssistantTurn(session.id, "a1")
        h.repository.appendUserTurn(session.id, "u2")

        val turns = h.repository.observeTurns(session.id).first()
        assertEquals(listOf(0, 1, 2), turns.map { it.position })
        assertEquals(
            listOf(ChatTurnRole.USER, ChatTurnRole.ASSISTANT, ChatTurnRole.USER),
            turns.map { it.role },
        )
        assertTrue(turns.all { it.sessionId == session.id })
    }

    @Test
    fun orderingSurvivesIdenticalTimestamps() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        // The clock never advances: all rows share one epoch millisecond.
        h.repository.appendUserTurn(session.id, "u1")
        h.repository.appendAssistantTurn(session.id, "a1")
        h.repository.appendUserTurn(session.id, "u2")

        val turns = h.repository.observeTurns(session.id).first()
        assertEquals(listOf(0, 1, 2), turns.map { it.position })
        assertEquals(1, turns.map { it.createdAtEpochMs }.distinct().size)
        assertEquals(
            listOf(ChatTurnRole.USER, ChatTurnRole.ASSISTANT, ChatTurnRole.USER),
            turns.map { it.role },
        )
    }
}
