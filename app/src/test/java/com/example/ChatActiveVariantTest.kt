package com.example

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatActiveVariantTest
 *
 * PASS 3C-3 spec §7: `activeVariantId` follows generation (assistant turn
 * activates variant 0), regeneration (last variant wins), and explicit
 * selection — and the value is read back from the database, not memory.
 */
class ChatActiveVariantTest {

    @Test
    fun assistantTurnActivatesItsFirstVariant() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        val turn = h.repository.appendAssistantTurn(session.id, "first reply")

        val readBack = h.repository.observeTurns(session.id).first().single()
        assertEquals(turn.activeVariantId, readBack.activeVariantId)
        val variant = h.repository.observeVariants(turn.id).first().single()
        assertEquals(variant.id, readBack.activeVariantId)
    }

    @Test
    fun appendVariantSwitchesActiveToTheNewVariant() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val turn = h.repository.appendAssistantTurn(session.id, "original")

        val regenerated = h.repository.appendVariant(turn.id, "rewritten")

        val readBack = h.repository.observeTurns(session.id).first().single()
        assertEquals(regenerated.id, readBack.activeVariantId)
        assertEquals(turn.activeVariantId, h.repository.observeVariants(turn.id).first()[0].id)
    }

    @Test
    fun selectVariantSwitchesBackToPreviousVariant() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val turn = h.repository.appendAssistantTurn(session.id, "original")
        val firstVariantId = turn.activeVariantId!!
        val regenerated = h.repository.appendVariant(turn.id, "rewritten")
        assertEquals(regenerated.id, h.repository.observeTurns(session.id).first().single().activeVariantId)

        h.repository.selectVariant(turn.id, firstVariantId)

        val readBack = h.repository.observeTurns(session.id).first().single()
        assertEquals(firstVariantId, readBack.activeVariantId)
    }

    @Test
    fun selectVariantRejectsVariantOfAnotherTurn() = runBlocking<Unit> {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val turnA = h.repository.appendAssistantTurn(session.id, "a")
        val turnB = h.repository.appendAssistantTurn(session.id, "b")

        assertThrows(IllegalArgumentException::class.java) {
            h.repository.selectVariant(turnA.id, turnB.activeVariantId!!)
        }
        assertThrows(IllegalArgumentException::class.java) {
            h.repository.selectVariant(turnA.id, "no-such-variant")
        }
    }
}
