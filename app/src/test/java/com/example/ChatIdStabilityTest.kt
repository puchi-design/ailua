package com.example

import com.example.data.chat.repository.UuidIdGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatIdStabilityTest
 *
 * PASS 3C-3 spec §8: ids come from the injected IdGenerator — never from the
 * clock — so two writes in the same millisecond can never collide.
 */
class ChatIdStabilityTest {

    @Test
    fun uuidGeneratorNeverCollides() {
        val generator = UuidIdGenerator()
        val ids = (1..1_000).map { generator.newId() }

        assertEquals(1_000, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
    }

    @Test
    fun idsAreNotDerivedFromTheClock() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        // Frozen clock: identical timestamps, yet every id must be unique.
        val first = h.repository.appendUserTurn(session.id, "one")
        val second = h.repository.appendUserTurn(session.id, "two")

        assertNotEquals(first.id, second.id)
        assertNotEquals(first.activeVariantId, second.activeVariantId)
        assertEquals(first.createdAtEpochMs, second.createdAtEpochMs)
    }

    @Test
    fun repositoryUsesTheInjectedIdGenerator() {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val turn = h.repository.appendUserTurn(session.id, "hi")

        assertEquals("id-1", session.id)
        assertEquals("id-2", turn.id)
        assertEquals("id-3", turn.activeVariantId)
    }
}
