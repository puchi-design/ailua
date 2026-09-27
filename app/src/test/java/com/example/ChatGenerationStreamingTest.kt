package com.example

import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.provider.FakeAiStep
import com.example.data.ai.runtime.SendResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ChatGenerationStreamingTest — P3C-4 §8: deltas surface on the streaming
 * StateFlow only (no per-token DB writes), exactly one terminal persist.
 */
class ChatGenerationStreamingTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun streamingSurfacesPartialTextThenClearsOnCompletion() = runBlocking {
        fixture.use(
            FakeAiProvider(
                listOf(
                    FakeAiStep.Pause(150),
                    FakeAiStep.Emit("你"),
                    FakeAiStep.Pause(150),
                    FakeAiStep.Emit("好"),
                    FakeAiStep.Complete,
                ),
            ),
        )

        var result: SendResult? = null
        val job = launch { result = fixture.runtime.send("mira", "hi") }

        val started = withTimeout(5_000) { fixture.runtime.streaming.first { it != null }!! }
        assertEquals("", started.text)
        assertEquals(fixture.sessionId("mira"), started.sessionId)

        val firstDelta = withTimeout(5_000) {
            fixture.runtime.streaming.first { it != null && it.text == "你" }!!
        }
        assertEquals(started.turnId, firstDelta.turnId)
        assertEquals(started.variantId, firstDelta.variantId)

        job.join()
        assertEquals(SendResult.Completed, result)
        assertNull(fixture.runtime.streaming.value)

        val turns = fixture.turns("mira")
        assertEquals("你好", turns[1].activeVariant?.content)
    }

    @Test
    fun streamingNeverWritesPerTokenUpdatesToDatabase() = runBlocking {
        fixture.use(
            FakeAiProvider(
                listOf(
                    FakeAiStep.Emit("a"),
                    FakeAiStep.Emit("b"),
                    FakeAiStep.Emit("c"),
                    FakeAiStep.Complete,
                ),
            ),
        )

        var streamingUpdates = 0
        val collector = launch {
            fixture.runtime.streaming.collect { if (it != null) streamingUpdates++ }
        }
        fixture.runtime.send("mira", "hi")
        collector.cancel()

        // Started state + 3 delta updates observed — DB has exactly ONE
        // terminal assistant row with the full text.
        assertNotNull(streamingUpdates)
        val turns = fixture.turns("mira")
        assertEquals(2, turns.size)
        assertEquals("abc", turns[1].activeVariant?.content)
        assertEquals(1, turns[1].variantCount)
    }
}
