package com.example

import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ChatPersistenceRoundTripTest
 *
 * PASS 3C-3 spec §17: closing the driver and opening a completely fresh
 * repository over the same file must return the same session, turn ordering,
 * variants, and active-variant selection — the data lives in SQLite, not in
 * repository memory.
 */
class ChatPersistenceRoundTripTest {

    @Test
    fun dataSurvivesRepositoryRestart() = runBlocking {
        val file = File.createTempFile("ailua-chat-roundtrip", ".db")
        try {
            // --- first process lifetime ---
            val first = ChatTestHarness.file(file)
            val session = first.repository.getOrCreatePrivateSession("mira")
            val userTurn = first.repository.appendUserTurn(session.id, "question")
            val answer = first.repository.appendAssistantTurn(session.id, "first answer")
            val rewritten = first.repository.appendVariant(answer.id, "second answer")
            val firstVariantId = first.repository.observeVariants(answer.id).first()[0].id

            // Swipe back to the first variant, then shut everything down.
            first.repository.selectVariant(answer.id, firstVariantId)
            first.driver.close()

            // --- second process lifetime (fresh driver, database, repository) ---
            val reopened = ChatTestHarness.file(file, createSchema = false)
            val sessionBack = reopened.repository.getSession(session.id)
            assertNotNull(sessionBack)
            assertEquals("mira", sessionBack!!.characterId)
            assertEquals(session.createdAtEpochMs, sessionBack.createdAtEpochMs)

            val turns = reopened.repository.observeTurns(session.id).first()
            assertEquals(2, turns.size)
            assertEquals(listOf(0, 1), turns.map { it.position })
            assertEquals(userTurn.id, turns[0].id)
            assertEquals(answer.id, turns[1].id)
            assertEquals(firstVariantId, turns[1].activeVariantId)

            val variants = reopened.repository.observeVariants(answer.id).first()
            assertEquals(listOf("first answer", "second answer"), variants.map { it.content })
            assertEquals(rewritten.id, variants[1].id)
        } finally {
            file.delete()
        }
    }
}
