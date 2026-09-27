package com.example

import com.example.data.ai.model.AiProviderError
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.provider.FakeAiStep
import com.example.data.ai.runtime.SendResult
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatGenerationFailureTest — P3C-4 §13: typed errors persist a FAILED
 * variant with the partial text; the assistant never "pretends" to reply.
 */
class ChatGenerationFailureTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun httpFailurePersistsFailedVariantWithPartialText() = runBlocking {
        fixture.use(
            FakeAiProvider(
                listOf(
                    FakeAiStep.Emit("抱歉，"),
                    FakeAiStep.Fail(AiProviderError.Http(500, "boom")),
                ),
            ),
        )

        val result = fixture.runtime.send("mira", "hi")

        assertTrue(result is SendResult.Failed)
        assertEquals(
            AiProviderError.Http(500, "boom"),
            (result as SendResult.Failed).error,
        )

        val turns = fixture.turns("mira")
        assertEquals(2, turns.size)
        val assistant = turns[1]
        assertEquals(ChatTurnRole.ASSISTANT, assistant.role)
        assertEquals(VariantStatus.FAILED, assistant.activeVariant?.status)
        assertEquals("抱歉，", assistant.activeVariant?.content)
        assertEquals("HTTP", assistant.activeVariant?.errorType)
        assertTrue(assistant.activeVariant?.errorMessage?.contains("500") == true)
    }

    @Test
    fun unauthorizedFailureSurfacesTypedErrorAndEmptyContent() = runBlocking {
        fixture.use(FakeAiProvider.unauthorized())

        val result = fixture.runtime.send("mira", "hi")

        assertEquals(SendResult.Failed(AiProviderError.Unauthorized), result)
        val assistant = fixture.turns("mira")[1]
        assertEquals(VariantStatus.FAILED, assistant.activeVariant?.status)
        assertEquals("", assistant.activeVariant?.content)
        assertEquals("UNAUTHORIZED", assistant.activeVariant?.errorType)
    }

    @Test
    fun emptyCompletionFailsWithoutFabricatedText() = runBlocking {
        fixture.use(FakeAiProvider.emptyCompletion())

        val result = fixture.runtime.send("mira", "hi")

        assertTrue(result is SendResult.Failed)
        assertTrue((result as SendResult.Failed).error is AiProviderError.Protocol)
        val assistant = fixture.turns("mira")[1]
        assertEquals(VariantStatus.FAILED, assistant.activeVariant?.status)
        assertEquals("", assistant.activeVariant?.content)
    }
}
