package com.example

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.OpenAiCompatibleProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * AiProviderHttpErrorTest — P3C-1 typed error gate.
 *
 * 401 -> Unauthorized, 500 -> Http, timeout -> Timeout, network -> Network,
 * broken stream -> Protocol. In every failure case the stream ends with
 * Failed(...) and NEVER with a fabricated/canned character reply.
 */
class AiProviderHttpErrorTest {

    private val apiKey = "sk-SECRET-abcdef123456"

    private fun request() = AiChatRequest(
        model = "gpt-4o-mini",
        messages = listOf(AiMessage(AiRole.USER, "hi")),
    )

    private fun collect(
        call: FakeHttpCall,
        key: String? = apiKey,
    ): List<AiStreamEvent> {
        val factory = FakeHttpCallFactory(call)
        val provider = OpenAiCompatibleProvider("https://api.test/v1", key, factory)
        return runBlocking { provider.streamChat(request()).toList() }
    }

    @Test
    fun unauthorizedMapsToTypedUnauthorized() {
        val events = collect(FakeHttpCall(code = 401, lines = listOf("""{"error":"invalid key"}""")))

        assertEquals(listOf<AiStreamEvent>(AiStreamEvent.Failed(AiProviderError.Unauthorized)), events)
    }

    @Test
    fun serverErrorMapsToTypedHttp() {
        val events = collect(FakeHttpCall(code = 500, lines = listOf("boom")))

        assertEquals(
            listOf<AiStreamEvent>(AiStreamEvent.Failed(AiProviderError.Http(500, "boom"))),
            events,
        )
    }

    @Test
    fun otherClientErrorMapsToTypedHttp() {
        val events = collect(FakeHttpCall(code = 404, lines = listOf("missing")))

        assertEquals(
            listOf<AiStreamEvent>(AiStreamEvent.Failed(AiProviderError.Http(404, "missing"))),
            events,
        )
    }

    @Test
    fun timeoutMapsToTypedTimeout() {
        val events = collect(FakeHttpCall(executeError = SocketTimeoutException("timeout")))

        assertEquals(listOf<AiStreamEvent>(AiStreamEvent.Failed(AiProviderError.Timeout)), events)
    }

    @Test
    fun networkFailureMapsToTypedNetwork() {
        val events = collect(FakeHttpCall(executeError = IOException("connection refused")))

        assertEquals(
            listOf<AiStreamEvent>(AiStreamEvent.Failed(AiProviderError.Network("connection refused"))),
            events,
        )
    }

    @Test
    fun apiKeyNeverAppearsInHttpErrorMessage() {
        val events = collect(FakeHttpCall(code = 500, lines = listOf("upstream rejected key $apiKey")))

        val error = (events.single() as AiStreamEvent.Failed).error as AiProviderError.Http
        assertFalse(error.message!!.contains(apiKey))
        assertTrue(error.message!!.contains("••••"))
    }

    @Test
    fun apiKeyNeverAppearsInNetworkErrorMessage() {
        val events = collect(FakeHttpCall(executeError = IOException("failed while sending $apiKey")))

        val error = (events.single() as AiStreamEvent.Failed).error as AiProviderError.Network
        assertFalse(error.message!!.contains(apiKey))
        assertTrue(error.message!!.contains("••••"))
    }

    @Test
    fun malformedSseFailsAsProtocolAndNeverCompletes() {
        val events = collect(
            FakeHttpCall(
                lines = listOf(
                    "data: {broken json",
                    "",
                    "data: [DONE]",
                    "",
                ),
            ),
        )

        assertTrue(events.first() is AiStreamEvent.Started)
        val error = (events.last() as AiStreamEvent.Failed).error as AiProviderError.Protocol
        assertTrue(error.message.contains("invalid JSON"))
        assertFalse(events.any { it is AiStreamEvent.Completed })
    }

    @Test
    fun providerErrorPayloadFailsAsProtocol() {
        val events = collect(
            FakeHttpCall(
                lines = listOf(
                    """data: {"error":{"message":"overloaded"}}""",
                    "",
                    "",
                ),
            ),
        )

        val error = (events.last() as AiStreamEvent.Failed).error as AiProviderError.Protocol
        assertTrue(error.message.startsWith("provider error"))
        assertFalse(events.any { it is AiStreamEvent.Completed })
    }

    @Test
    fun nonObjectPayloadFailsAsProtocol() {
        val events = collect(FakeHttpCall(lines = listOf("data: 12345", "", "")))

        val error = (events.last() as AiStreamEvent.Failed).error as AiProviderError.Protocol
        assertTrue(error.message.contains("not a JSON object"))
    }

    @Test
    fun emptyCompletionFailsAsProtocol() {
        val events = collect(FakeHttpCall(lines = listOf("data: [DONE]", "")))

        assertEquals(
            listOf<AiStreamEvent>(
                AiStreamEvent.Started,
                AiStreamEvent.Failed(AiProviderError.Protocol("empty completion")),
            ),
            events,
        )
    }

    @Test
    fun emptyStringContentCountsAsEmptyCompletion() {
        val events = collect(
            FakeHttpCall(
                lines = listOf(
                    """data: {"choices":[{"delta":{"content":""}}]}""",
                    "",
                    "data: [DONE]",
                    "",
                ),
            ),
        )

        assertEquals(
            AiStreamEvent.Failed(AiProviderError.Protocol("empty completion")),
            events.last(),
        )
    }

    @Test
    fun failureNeverProducesCannedText() {
        val failures = listOf(
            FakeHttpCall(code = 401),
            FakeHttpCall(code = 500, lines = listOf("err")),
            FakeHttpCall(executeError = SocketTimeoutException("timeout")),
            FakeHttpCall(executeError = IOException("boom")),
            FakeHttpCall(lines = listOf("data: {bad", "", "")),
        )
        for (call in failures) {
            val events = collect(call)
            assertTrue(events.none { it is AiStreamEvent.Completed })
            assertTrue(events.none { it is AiStreamEvent.Delta })
            assertTrue(events.last() is AiStreamEvent.Failed)
        }
    }
}
