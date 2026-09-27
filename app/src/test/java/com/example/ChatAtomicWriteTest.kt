package com.example

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatAtomicWriteTest
 *
 * PASS 3C-3 spec §10: a failure inside an append (here: a forced primary-key
 * collision on the variant, and an FK violation on an unknown session) rolls
 * back the ENTIRE transaction — no half-written turn, no orphan variant, no
 * partial session touch.
 */
class ChatAtomicWriteTest {

    @Test
    fun failedVariantInsertRollsBackTheWholeTurn() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val first = h.repository.appendUserTurn(session.id, "one")
        // ids so far: session=id-1, turn=id-2, variant=id-3
        val turnsBefore = h.database.chatTurnQueries.countTurns().executeAsOne()
        val variantsBefore = h.database.chatVariantQueries.countVariants().executeAsOne()

        // Next append: the turn insert succeeds, then the variant insert hits a
        // duplicate primary key -> the whole transaction must roll back.
        h.idGenerator.forcedIds.add("forced-turn-x")
        h.idGenerator.forcedIds.add(first.activeVariantId!!)

        assertThrows(Exception::class.java) {
            h.repository.appendUserTurn(session.id, "two")
        }

        assertEquals(turnsBefore, h.database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(variantsBefore, h.database.chatVariantQueries.countVariants().executeAsOne())
        assertEquals(1, h.repository.observeTurns(session.id).first().size)
    }

    @Test
    fun appendTurnOnUnknownSessionLeavesNoOrphans() = runBlocking {
        val h = ChatTestHarness.inMemory()

        // Foreign-key violation on the turn insert: nothing may be written.
        assertThrows(Exception::class.java) {
            h.repository.appendUserTurn("ghost-session", "hi")
        }

        assertEquals(0L, h.database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(0L, h.database.chatVariantQueries.countVariants().executeAsOne())
        assertEquals(0L, h.database.chatSessionQueries.countSessions().executeAsOne())
    }

    @Test
    fun appendVariantOnUnknownTurnDoesNotTouchAnything() {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val turnsBefore = h.database.chatTurnQueries.countTurns().executeAsOne()

        assertThrows(IllegalArgumentException::class.java) {
            h.repository.appendVariant("ghost-turn", "content")
        }

        assertEquals(0L, turnsBefore)
        assertEquals(0L, h.database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(1L, h.database.chatSessionQueries.countSessions().executeAsOne())
    }
}
