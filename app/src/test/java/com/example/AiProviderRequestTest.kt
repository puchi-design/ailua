package com.example

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.model.AiUsage
import com.example.data.ai.provider.OpenAiCompatibleProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiProviderRequestTest — P3C-1 wire contract.
 *
 * Verifies exactly what leaves the device: URL, headers (Bearer auth) and the
 * minimal request body — nothing beyond model/messages/stream/temperature/max_tokens.
 */
class AiProviderRequestTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun request(
        stream: Boolean = true,
        temperature: Double? = null,
        maxTokens: Int? = null,
    ) = AiChatRequest(
        model = "gpt-4o-mini",
        messages = listOf(
            AiMessage(AiRole.SYSTEM, "system prompt"),
            AiMessage(AiRole.USER, "hello"),
            AiMessage(AiRole.ASSISTANT, "hi there"),
        ),
        stream = stream,
        temperature = temperature,
        maxTokens = maxTokens,
    )

    private fun streamingCall() = FakeHttpCall(
        lines = listOf(
            """data: {"choices":[{"delta":{"content":"你"}}]}""",
            "",
            """data: {"choices":[{"delta":{"content":"好"}}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}""",
            "",
            "data: [DONE]",
            "",
        ),
    )

    private fun provider(
        baseUrl: String = "https://api.example.com/v1/",
        apiKey: String? = "sk-test-key",
        call: FakeHttpCall = streamingCall(),
    ): Pair<OpenAiCompatibleProvider, FakeHttpCallFactory> {
        val factory = FakeHttpCallFactory(call)
        return OpenAiCompatibleProvider(baseUrl, apiKey, factory) to factory
    }

    @Test
    fun urlIsAlwaysTheCanonicalChatCompletionsPath() = runBlocking {
        val (provider, factory) = provider(baseUrl = "https://api.example.com/v1/chat/completions/")
        provider.streamChat(request()).toList()

        assertEquals("https://api.example.com/v1/chat/completions", factory.lastUrl)
    }

    @Test
    fun headersCarryBearerAuthJsonAndEventStreamAccept() = runBlocking {
        val (provider, factory) = provider()
        provider.streamChat(request()).toList()

        assertEquals("Bearer sk-test-key", factory.lastHeaders["Authorization"])
        assertTrue(factory.lastHeaders.getValue("Content-Type").startsWith("application/json"))
        assertEquals("text/event-stream", factory.lastHeaders["Accept"])
    }

    @Test
    fun missingApiKeySendsNoAuthorizationHeader() = runBlocking {
        val (provider, factory) = provider(apiKey = null)
        provider.streamChat(request()).toList()

        assertFalse(factory.lastHeaders.containsKey("Authorization"))
    }

    @Test
    fun bodyContainsOnlyTheMinimalContract() = runBlocking {
        val (provider, factory) = provider()
        provider.streamChat(request()).toList()

        val root = json.parseToJsonElement(factory.lastBody!!) as JsonObject
        assertEquals(setOf("model", "messages", "stream"), root.keys)
        assertEquals("gpt-4o-mini", (root["model"] as JsonPrimitive).content)
        assertTrue((root["stream"] as JsonPrimitive).content.toBoolean())
        // no tools / tool_calls / vision / audio / reasoning / response_format
        assertFalse(root.containsKey("tools"))
    }

    @Test
    fun samplingFieldsAppearOnlyWhenProvided() = runBlocking {
        val bare = provider(call = streamingCall())
        bare.first.streamChat(request()).toList()
        val bareRoot = json.parseToJsonElement(bare.second.lastBody!!) as JsonObject
        assertFalse(bareRoot.containsKey("temperature"))
        assertFalse(bareRoot.containsKey("max_tokens"))

        val tuned = provider(call = streamingCall())
        tuned.first.streamChat(request(temperature = 0.7, maxTokens = 32)).toList()
        val tunedRoot = json.parseToJsonElement(tuned.second.lastBody!!) as JsonObject
        assertEquals(0.7, (tunedRoot["temperature"] as JsonPrimitive).content.toDouble(), 0.0)
        assertEquals("32", (tunedRoot["max_tokens"] as JsonPrimitive).content)
    }

    @Test
    fun rolesAreSerializedLowercaseInOrder() = runBlocking {
        val (provider, factory) = provider()
        provider.streamChat(request()).toList()

        val root = json.parseToJsonElement(factory.lastBody!!) as JsonObject
        val messages = root["messages"] as JsonArray
        val roles = messages.map { ((it as JsonObject)["role"] as JsonPrimitive).content }
        val contents = messages.map { ((it as JsonObject)["content"] as JsonPrimitive).content }
        assertEquals(listOf("system", "user", "assistant"), roles)
        assertEquals(listOf("system prompt", "hello", "hi there"), contents)
    }

    @Test
    fun streamingResponseEmitsStartedDeltasUsageCompleted() = runBlocking {
        val (provider, _) = provider()
        val events = provider.streamChat(request()).toList()

        assertEquals(
            listOf(
                AiStreamEvent.Started,
                AiStreamEvent.Delta("你"),
                AiStreamEvent.Delta("好"),
                AiStreamEvent.Usage(AiUsage(3, 2, 5)),
                AiStreamEvent.Completed("你好"),
            ),
            events,
        )
    }

    @Test
    fun nonStreamRequestEmitsSingleDeltaAndCompleted() = runBlocking {
        val call = FakeHttpCall(
            lines = listOf(
                """{"choices":[{"message":{"content":"单发"}}],"usage":{"prompt_tokens":1,"completion_tokens":2,"total_tokens":3}}""",
            ),
        )
        val (provider, factory) = provider(call = call)
        val events = provider.streamChat(request(stream = false)).toList()

        assertEquals(
            listOf(
                AiStreamEvent.Started,
                AiStreamEvent.Delta("单发"),
                AiStreamEvent.Usage(AiUsage(1, 2, 3)),
                AiStreamEvent.Completed("单发"),
            ),
            events,
        )
        assertTrue(factory.lastBody!!.contains("\"stream\":false"))
    }
}
