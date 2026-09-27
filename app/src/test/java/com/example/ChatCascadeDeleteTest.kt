package com.example

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ChatCascadeDeleteTest
 *
 * PASS 3C-3 spec §12: clearing a session removes its turns and every variant
 * underneath them — zero orphan rows — while other sessions stay intact.
 */
class ChatCascadeDeleteTest {

    @Test
    fun clearSessionRemovesTurnsAndVariantsWithZeroOrphans() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val mira = h.repository.getOrCreatePrivateSession("mira")
        val yuna = h.repository.getOrCreatePrivateSession("yuna")

        h.repository.appendUserTurn(mira.id, "m1")
        val miraAnswer = h.repository.appendAssistantTurn(mira.id, "a1")
        h.repository.appendVariant(miraAnswer.id, "a1 rewritten")
        h.repository.appendUserTurn(yuna.id, "y1")

        assertEquals(3L, h.database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(4L, h.database.chatVariantQueries.countVariants().executeAsOne())
        assertEquals(2L, h.database.chatSessionQueries.countSessions().executeAsOne())

        h.repository.clearSession(mira.id)

        // Removed: session, turns, and every variant — no orphans anywhere.
        assertNull(h.repository.getSession(mira.id))
        assertEquals(0, h.repository.observeTurns(mira.id).first().size)
        assertEquals(0, h.repository.observeVariants(miraAnswer.id).first().size)
        // Global counts equal EXACTLY yuna's surviving rows: any mira orphan
        // (turn or variant) would inflate these.
        assertEquals(1L, h.database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(1L, h.database.chatVariantQueries.countVariants().executeAsOne())
        assertEquals(1L, h.database.chatSessionQueries.countSessions().executeAsOne())

        // The other character survives untouched.
        assertEquals("yuna", h.repository.getSession(yuna.id)!!.characterId)
        assertEquals(1, h.repository.observeTurns(yuna.id).first().size)
        assertEquals(1L, h.database.chatVariantQueries.countVariants().executeAsOne())
    }

    @Test
    fun clearingUnknownSessionIsANoOp() {
        val h = ChatTestHarness.inMemory()
        h.repository.clearSession("no-such-session")
        assertEquals(0L, h.database.chatSessionQueries.countSessions().executeAsOne())
    }
}
