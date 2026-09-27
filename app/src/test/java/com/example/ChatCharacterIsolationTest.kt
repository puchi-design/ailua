package com.example

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ChatCharacterIsolationTest
 *
 * PASS 3C-3 spec §13: private sessions of different characters are fully
 * isolated — separate session ids, no shared turns, and a write under one
 * character never touches the other character's session row.
 */
class ChatCharacterIsolationTest {

    @Test
    fun privateSessionsOfDifferentCharactersNeverMix() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val mira = h.repository.getOrCreatePrivateSession("mira")
        val yuna = h.repository.getOrCreatePrivateSession("yuna")
        assertNotEquals(mira.id, yuna.id)

        h.repository.appendUserTurn(mira.id, "mira says hi")
        h.repository.appendAssistantTurn(mira.id, "mira replies")
        h.repository.appendUserTurn(yuna.id, "yuna says hi")

        val miraTurns = h.repository.observeTurns(mira.id).first()
        val yunaTurns = h.repository.observeTurns(yuna.id).first()

        assertEquals(2, miraTurns.size)
        assertEquals(1, yunaTurns.size)
        assertEquals(listOf(mira.id, mira.id), miraTurns.map { it.sessionId })
        assertEquals(listOf(yuna.id), yunaTurns.map { it.sessionId })
        assertNotEquals(miraTurns[0].id, yunaTurns[0].id)
    }

    @Test
    fun writeUnderOneCharacterDoesNotTouchOtherSession() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val mira = h.repository.getOrCreatePrivateSession("mira")
        val yuna = h.repository.getOrCreatePrivateSession("yuna")
        // Seed yuna's own data first, then prove a mira write leaves it alone.
        h.repository.appendUserTurn(yuna.id, "yuna seed")
        val yunaBefore = h.repository.getSession(yuna.id)!!.updatedAtEpochMs

        h.clock.now += 60_000
        h.repository.appendUserTurn(mira.id, "mira only")

        assertEquals(yunaBefore, h.repository.getSession(yuna.id)!!.updatedAtEpochMs)
        assertEquals(1, h.repository.observeTurns(yuna.id).first().size)
        assertNull(h.repository.observeTurns(yuna.id).first().singleOrNull { it.sessionId == mira.id })
    }
}
