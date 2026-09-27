package com.example

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.FakeAiProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FakeAiProviderTest — the deterministic scripted double that lets P3C-2..P3C-6
 * run without any real API key. FakeAiProvider stays test-only (spec §11 #4).
 */
class FakeAiProviderTest {

    private fun request() = AiChatRequest(
        model = "gpt-4o-mini",
        messages = listOf(AiMessage(AiRole.USER, "hi")),
    )

    @Test
    fun scriptedDeltasEmitDeterministicSequence() = runBlocking {
        val events = FakeAiProvider.scripted("你", "好", "呀").streamChat(request()).toList()

        assertEquals(
            listOf(
                AiStreamEvent.Started,
                AiStreamEvent.Delta("你"),
                AiStreamEvent.Delta("好"),
                AiStreamEvent.Delta("呀"),
                AiStreamEvent.Completed("你好呀"),
            ),
            events,
        )
    }

    @Test
    fun requestIsRecordedForAssertions() = runBlocking {
        val provider = FakeAiProvider.scripted("ok")
        val request = request()
        provider.streamChat(request).toList()

        assertEquals(listOf(request), provider.requests)
    }

    @Test
    fun errorScriptsProduceTypedFailures() = runBlocking {
        val request = request()

        val http = FakeAiProvider.httpError(429, "slow down").streamChat(request).toList()
        assertEquals(
            AiStreamEvent.Failed(AiProviderError.Http(429, "slow down")),
            http.last(),
        )

        val unauthorized = FakeAiProvider.unauthorized().streamChat(request).toList()
        assertEquals(AiStreamEvent.Failed(AiProviderError.Unauthorized), unauthorized.last())

        val timeout = FakeAiProvider.timeout().streamChat(request).toList()
        assertEquals(AiStreamEvent.Failed(AiProviderError.Timeout), timeout.last())

        val protocol = FakeAiProvider.protocolError("bad stream").streamChat(request).toList()
        assertEquals(AiStreamEvent.Failed(AiProviderError.Protocol("bad stream")), protocol.last())
    }

    @Test
    fun failureScriptsNeverEmitCompletedOrDelta() = runBlocking {
        val request = request()
        val scripts = listOf(
            FakeAiProvider.httpError(500, "err"),
            FakeAiProvider.unauthorized(),
            FakeAiProvider.timeout(),
            FakeAiProvider.protocolError("broken"),
        )
        for (provider in scripts) {
            val events = provider.streamChat(request).toList()
            assertTrue(events.none { it is AiStreamEvent.Completed })
            assertTrue(events.none { it is AiStreamEvent.Delta })
            assertTrue(events.last() is AiStreamEvent.Failed)
        }
    }

    @Test
    fun emptyCompletionScriptEmitsEmptyCompleted() = runBlocking {
        val events = FakeAiProvider.emptyCompletion().streamChat(request()).toList()

        assertEquals(
            listOf(AiStreamEvent.Started, AiStreamEvent.Completed("")),
            events,
        )
    }
}
