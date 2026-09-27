package com.example

import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatVariantTest
 *
 * PASS 3C-3 spec §5/§7: variants carry content + status + optional provider
 * metadata, ordered by a stable per-turn `variantIndex` counter.
 */
class ChatVariantTest {

    @Test
    fun assistantTurnStartsWithItsFirstVariant() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        val turn = h.repository.appendAssistantTurn(
            sessionId = session.id,
            content = "hello there",
            providerProfileId = "profile-1",
            model = "model-x",
        )

        val variants = h.repository.observeVariants(turn.id).first()
        assertEquals(1, variants.size)
        val variant = variants[0]
        assertEquals(0, variant.variantIndex)
        assertEquals("hello there", variant.content)
        assertEquals(VariantStatus.COMPLETE, variant.status)
        assertEquals("profile-1", variant.providerProfileId)
        assertEquals("model-x", variant.model)
        assertEquals(null, variant.errorType)
        assertEquals(null, variant.errorMessage)
        assertEquals(h.clock.now, variant.createdAtEpochMs)
        assertEquals(h.clock.now, variant.updatedAtEpochMs)
        assertEquals(turn.activeVariantId, variant.id)
    }

    @Test
    fun appendVariantAssignsNextStableIndex() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        val turn = h.repository.appendAssistantTurn(session.id, "original")

        val second = h.repository.appendVariant(
            turnId = turn.id,
            content = "rewritten",
            status = VariantStatus.FAILED,
            errorType = "network",
            errorMessage = "connection lost",
        )
        val third = h.repository.appendVariant(turn.id, "third attempt")

        assertEquals(1, second.variantIndex)
        assertEquals(VariantStatus.FAILED, second.status)
        assertEquals("network", second.errorType)
        assertEquals("connection lost", second.errorMessage)
        assertEquals(2, third.variantIndex)

        val variants = h.repository.observeVariants(turn.id).first()
        assertEquals(listOf(0, 1, 2), variants.map { it.variantIndex })
        assertEquals(listOf("original", "rewritten", "third attempt"), variants.map { it.content })
    }

    @Test
    fun appendVariantRejectsUnknownTurn() {
        val h = ChatTestHarness.inMemory()
        assertThrows(IllegalArgumentException::class.java) {
            h.repository.appendVariant("no-such-turn", "content")
        }
    }
}
