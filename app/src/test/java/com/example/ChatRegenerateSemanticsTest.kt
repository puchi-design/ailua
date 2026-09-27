package com.example

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatRegenerateSemanticsTest
 *
 * PASS 3C-3 spec §11 (locked semantics): regeneration appends a NEW variant to
 * the EXISTING assistant turn (`variantIndex = max + 1`) and activates it.
 * It never creates a new logical turn, never rewrites the user turn, and never
 * changes any position. Exercised now so P3C-4's AI call only swaps the
 * content source.
 */
class ChatRegenerateSemanticsTest {

    @Test
    fun regenerationAppendsVariantWithoutCreatingNewTurn() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")

        h.repository.appendUserTurn(session.id, "hi")
        h.repository.appendAssistantTurn(session.id, "original answer")
        h.repository.appendUserTurn(session.id, "tell me more")
        val target = h.repository.appendAssistantTurn(session.id, "second original")

        val before = h.repository.observeTurns(session.id).first()
        assertEquals(4, before.size)
        assertEquals(listOf(0, 1, 2, 3), before.map { it.position })

        val regenerated = h.repository.appendVariant(target.id, "rewritten answer")

        // Structure is untouched: same turns, same ids, same positions.
        val after = h.repository.observeTurns(session.id).first()
        assertEquals(before.map { it.id }, after.map { it.id })
        assertEquals(before.map { it.position }, after.map { it.position })
        assertEquals(4, after.size)

        // The regeneration lives on the SAME assistant turn as a new variant.
        val variants = h.repository.observeVariants(target.id).first()
        assertEquals(listOf(0, 1), variants.map { it.variantIndex })
        assertEquals(listOf("second original", "rewritten answer"), variants.map { it.content })

        // New variant is active; user turns keep their single variant.
        assertEquals(regenerated.id, after[3].activeVariantId)
        assertEquals(1, h.repository.observeVariants(after[0].id).first().size)
        assertEquals(1, h.repository.observeVariants(after[2].id).first().size)
    }

    @Test
    fun repeatedRegenerationKeepsOneTurnGrowing() = runBlocking {
        val h = ChatTestHarness.inMemory()
        val session = h.repository.getOrCreatePrivateSession("mira")
        h.repository.appendUserTurn(session.id, "question")
        val answer = h.repository.appendAssistantTurn(session.id, "v1")

        h.repository.appendVariant(answer.id, "v2")
        val third = h.repository.appendVariant(answer.id, "v3")

        val turns = h.repository.observeTurns(session.id).first()
        assertEquals(2, turns.size)
        assertEquals(listOf(0, 1), turns.map { it.position })
        assertEquals(third.id, turns[1].activeVariantId)
        assertEquals(
            listOf(0, 1, 2),
            h.repository.observeVariants(answer.id).first().map { it.variantIndex },
        )
    }
}
