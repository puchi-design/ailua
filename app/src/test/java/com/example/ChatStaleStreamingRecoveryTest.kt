package com.example

import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatStaleStreamingRecoveryTest — P3C-4 §9: an app killed mid-generation
 * leaves STREAMING rows; opening the session recovers them to
 * CANCELLED/INTERRUPTED so the UI never shows a permanent "typing" state.
 */
class ChatStaleStreamingRecoveryTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun staleStreamingVariantRecoversToCancelledInterrupted() = runBlocking {
        val session = fixture.sessionId("mira")
        fixture.repository.appendAssistantTurn(
            sessionId = session,
            content = "",
            status = VariantStatus.STREAMING,
        )

        assertEquals(VariantStatus.STREAMING, fixture.repository.getResolvedTurns(session).last().activeVariant?.status)

        val recovered = fixture.repository.recoverInterruptedVariants(session)
        assertEquals(1, recovered)

        val turn = fixture.repository.getResolvedTurns(session).last()
        assertEquals(VariantStatus.CANCELLED, turn.activeVariant?.status)
        assertEquals("INTERRUPTED", turn.activeVariant?.errorType)
    }

    @Test
    fun recoveryIsIdempotent() = runBlocking {
        val session = fixture.sessionId("mira")
        fixture.repository.appendAssistantTurn(session, "", VariantStatus.STREAMING)

        assertEquals(1, fixture.repository.recoverInterruptedVariants(session))
        assertEquals(0, fixture.repository.recoverInterruptedVariants(session))
        assertEquals(VariantStatus.CANCELLED, fixture.repository.getResolvedTurns(session).last().activeVariant?.status)
    }

    @Test
    fun recoveryOnlyTouchesTheGivenSession() = runBlocking {
        val sessionA = fixture.sessionId("mira")
        val sessionB = fixture.sessionId("yuna")
        fixture.repository.appendAssistantTurn(sessionA, "", VariantStatus.STREAMING)
        fixture.repository.appendAssistantTurn(sessionB, "", VariantStatus.STREAMING)

        fixture.repository.recoverInterruptedVariants(sessionA)

        assertEquals(VariantStatus.CANCELLED, fixture.repository.getResolvedTurns(sessionA).last().activeVariant?.status)
        assertEquals(VariantStatus.STREAMING, fixture.repository.getResolvedTurns(sessionB).last().activeVariant?.status)
    }

    @Test
    fun recoveredInterruptedTurnNeverEntersPromptAgain() = runBlocking {
        val session = fixture.sessionId("mira")
        fixture.repository.appendAssistantTurn(session, "半截文本", VariantStatus.STREAMING)
        fixture.repository.recoverInterruptedVariants(session)

        val provider = com.example.data.ai.provider.FakeAiProvider.scripted("新回复")
        fixture.use(provider)
        fixture.runtime.send("mira", "继续")

        val allText = provider.requests.single().messages.joinToString("\n") { it.content }
        org.junit.Assert.assertFalse(allText.contains("半截文本"))
    }
}
