package com.example

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.provider.OpenAiCompatibleProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AiProviderCancellationTest — P3C-1 correction #1.
 *
 * Cancellation is strictly request-scoped: cancelling the collector Job cancels
 * the Flow, which cancels the underlying HTTP call — and produces neither a
 * Failed event nor any fabricated text. The scripted Cancel step is the only
 * path that emits the terminal `Cancelled` event.
 */
class AiProviderCancellationTest {

    private fun request() = AiChatRequest(
        model = "gpt-4o-mini",
        messages = listOf(AiMessage(AiRole.USER, "hi")),
    )

    @Test
    fun collectorCancellationCancelsCallWithoutTerminalFailure() = runBlocking {
        val call = FakeHttpCall(
            lines = listOf(
                """data: {"choices":[{"delta":{"content":"a"}}]}""",
                "",
                """data: {"choices":[{"delta":{"content":"b"}}]}""",
                "",
                "data: [DONE]",
                "",
            ),
            pauseAt = 2,
        )
        val factory = FakeHttpCallFactory(call)
        val provider = OpenAiCompatibleProvider("https://api.test/v1", "sk-test", factory)

        val events = CopyOnWriteArrayList<AiStreamEvent>()
        val job = launch(Dispatchers.Default) {
            provider.streamChat(request()).collect { events += it }
        }

        withTimeout(10_000) {
            while (events.none { it is AiStreamEvent.Delta }) delay(10)
        }
        job.cancelAndJoin()

        assertTrue(call.executed)
        assertTrue(call.cancelled)
        assertTrue(events.any { it is AiStreamEvent.Delta })
        assertTrue(events.none { it is AiStreamEvent.Failed })
        assertTrue(events.none { it is AiStreamEvent.Completed })
        assertTrue(events.none { it is AiStreamEvent.Cancelled })
    }

    @Test
    fun cancellingPausedFakeLeavesNoTerminalEvent() = runBlocking {
        val provider = FakeAiProvider.pause(60_000, "late")

        val events = CopyOnWriteArrayList<AiStreamEvent>()
        val job = launch(Dispatchers.Default) {
            provider.streamChat(request()).collect { events += it }
        }

        withTimeout(10_000) {
            while (events.none { it is AiStreamEvent.Started }) delay(10)
        }
        job.cancelAndJoin()

        assertTrue(events.none { it is AiStreamEvent.Failed })
        assertTrue(events.none { it is AiStreamEvent.Completed })
        assertTrue(events.none { it is AiStreamEvent.Cancelled })
    }

    @Test
    fun scriptedCancelDuringSecondChunkEmitsCancelled() = runBlocking {
        val events = FakeAiProvider.cancelAfter(2).streamChat(request()).toList()

        assertEquals(
            listOf(
                AiStreamEvent.Started,
                AiStreamEvent.Delta("chunk0"),
                AiStreamEvent.Delta("chunk1"),
                AiStreamEvent.Cancelled,
            ),
            events,
        )
        assertTrue(events.none { it is AiStreamEvent.Completed })
        assertFalse(events.any { it is AiStreamEvent.Failed })
    }
}
