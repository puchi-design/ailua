package com.example

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatSessionRepositoryTest
 *
 * PASS 3C-3 spec §5/§13: one canonical private session per character,
 * stable ids from the injected generator, timestamps from the injected clock.
 */
class ChatSessionRepositoryTest {

    @Test
    fun createsSessionWithGeneratedIdAndClockTimestamps() {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        assertEquals("mira", session.characterId)
        assertTrue(session.id.isNotBlank())
        assertEquals(h.clock.now, session.createdAtEpochMs)
        assertEquals(h.clock.now, session.updatedAtEpochMs)
        assertEquals(session, h.repository.getSession(session.id))
    }

    @Test
    fun sameCharacterReturnsSameCanonicalSession() {
        val h = ChatTestHarness.inMemory()
        val first = h.repository.getOrCreatePrivateSession("mira")
        h.clock.now += 60_000
        val second = h.repository.getOrCreatePrivateSession("mira")

        assertEquals(first.id, second.id)
        assertEquals(first.createdAtEpochMs, second.createdAtEpochMs)
        assertEquals(1L, h.database.chatSessionQueries.countSessions().executeAsOne())
    }

    @Test
    fun differentCharactersGetSeparateSessions() {
        val h = ChatTestHarness.inMemory()
        val mira = h.repository.getOrCreatePrivateSession("mira")
        val yuna = h.repository.getOrCreatePrivateSession("yuna")

        assertNotEquals(mira.id, yuna.id)
        assertEquals(2L, h.database.chatSessionQueries.countSessions().executeAsOne())
    }

    @Test
    fun getSessionReturnsNullForUnknownId() {
        val h = ChatTestHarness.inMemory()
        assertNull(h.repository.getSession("no-such-session"))
    }

    @Test
    fun canonicalSessionIsTheEarliestCreated() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val first = h.repository.getOrCreatePrivateSession("mira")

        // A second (later) session for the same character — only reachable by
        // direct insert since the repository contract never branches.
        h.clock.now += 1_000
        h.database.chatSessionQueries.insertSession("later", "mira", h.clock.now, h.clock.now)

        val again = h.repository.getOrCreatePrivateSession("mira")
        assertEquals(first.id, again.id)
        assertEquals(2L, h.database.chatSessionQueries.countSessions().executeAsOne())
        assertNull(h.repository.observeSession("no-such-session").first())
    }
}
