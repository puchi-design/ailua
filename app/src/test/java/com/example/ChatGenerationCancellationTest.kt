package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.provider.FakeAiStep
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatGenerationCancellationTest — P3C-4 §12: cancel = Job cancel → flow
 * cancel → HttpStreamCall.cancel; the partial text is persisted as CANCELLED
 * exactly once, then the cancellation propagates.
 */
class ChatGenerationCancellationTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun jobCancellationPersistsPartialTextAsCancelled() = runBlocking {
        fixture.use(
            FakeAiProvider(
                listOf(
                    FakeAiStep.Emit("你"),
                    FakeAiStep.Pause(60_000),
                    FakeAiStep.Emit("好"),
                    FakeAiStep.Complete,
                ),
            ),
        )

        val job = launch { fixture.runtime.send("mira", "hi") }
        withTimeout(5_000) { fixture.runtime.streaming.first { it != null && it.text == "你" } }

        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertNull(fixture.runtime.streaming.value)

        val turns = fixture.turns("mira")
        assertEquals(2, turns.size)
        val assistant = turns[1]
        assertEquals(VariantStatus.CANCELLED, assistant.activeVariant?.status)
        assertEquals("你", assistant.activeVariant?.content)
        assertEquals("CANCELLED", assistant.activeVariant?.errorType)
    }

    @Test
    fun cancelBeforeFirstDeltaPersistsEmptyCancelledVariant() = runBlocking {
        fixture.use(
            FakeAiProvider(
                listOf(FakeAiStep.Pause(60_000), FakeAiStep.Complete),
            ),
        )

        val job = launch { fixture.runtime.send("mira", "hi") }
        withTimeout(5_000) { fixture.runtime.streaming.first { it != null } }

        job.cancelAndJoin()
        assertNull(fixture.runtime.streaming.value)

        val assistant = fixture.turns("mira")[1]
        assertEquals(VariantStatus.CANCELLED, assistant.activeVariant?.status)
        assertEquals("", assistant.activeVariant?.content)
    }

    @Test
    fun providerEmittedCancelledEventPersistsCancelledVariant() = runBlocking {
        fixture.use(FakeAiProvider.cancelAfter(1))

        val result = fixture.runtime.send("mira", "hi")

        assertEquals(com.example.data.ai.runtime.SendResult.Cancelled, result)
        val assistant = fixture.turns("mira")[1]
        assertEquals(VariantStatus.CANCELLED, assistant.activeVariant?.status)
        assertEquals("chunk0", assistant.activeVariant?.content)
    }
}
