package com.example

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatRepositoryFlowTest
 *
 * PASS 3C-3 spec §17: the observe* Flows emit the current state on collection
 * and re-emit after every committed write (SQLDelight query listeners).
 */
class ChatRepositoryFlowTest {

    @Test
    fun observeTurnsEmitsEmptyThenTheNewTurn() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        val collector = async { h.repository.observeTurns(session.id).take(2).toList() }
        delay(250)
        h.repository.appendUserTurn(session.id, "hi")

        val emissions = withTimeout(5_000) { collector.await() }
        assertEquals(2, emissions.size)
        assertTrue(emissions[0].isEmpty())
        assertEquals(1, emissions[1].size)
        assertEquals("hi", h.repository.observeVariants(emissions[1][0].id).first().single().content)
    }

    @Test
    fun observeSessionEmitsOnTouch() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        val collector = async { h.repository.observeSession(session.id).take(2).toList() }
        delay(250)
        h.clock.now += 60_000
        h.repository.appendUserTurn(session.id, "hi")

        val emissions = withTimeout(5_000) { collector.await() }
        assertEquals(2, emissions.size)
        assertEquals(session.updatedAtEpochMs, emissions[0]!!.updatedAtEpochMs)
        assertEquals(h.clock.now, emissions[1]!!.updatedAtEpochMs)
    }

    @Test
    fun observeVariantsReflectsRegeneration() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val answer = h.repository.appendAssistantTurn(session.id, "original")

        val collector = async { h.repository.observeVariants(answer.id).take(2).toList() }
        delay(250)
        h.repository.appendVariant(answer.id, "rewritten")

        val emissions = withTimeout(5_000) { collector.await() }
        assertEquals(2, emissions.size)
        assertEquals(1, emissions[0].size)
        assertEquals(2, emissions[1].size)
        assertEquals(listOf(0, 1), emissions[1].map { it.variantIndex })
    }
}
