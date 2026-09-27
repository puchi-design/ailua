package com.example

import com.example.data.ai.model.AiProviderError
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.VariantStatus
import com.example.data.ai.runtime.SendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatNoFakeFallbackTest — P3C-4 §13: on failure the runtime NEVER fabricates
 * a reply. Assistant content is exactly what the provider streamed — nothing
 * from the old canned keyword table can leak in.
 */
class ChatNoFakeFallbackTest {

    private val fixture = ChatRuntimeFixture()

    private val cannedPhrases = listOf(
        "嗯，我在听。",
        "累了就快停下来",
        "从前有一座静悄悄的钟表镇",
        "嘿嘿，听你说话好开心",
        "静听长夜细雨",
        "我刚才在看窗户上滑下来的雨珠",
    )

    @Test
    fun failureNeverProducesCannedOrFabricatedText() = runBlocking {
        fixture.use(FakeAiProvider.unauthorized())

        val result = fixture.runtime.send("mira", "今天好累")
        assertEquals(SendResult.Failed(AiProviderError.Unauthorized), result)

        val assistant = fixture.turns("mira").last { it.role == ChatTurnRole.ASSISTANT }
        assertEquals(VariantStatus.FAILED, assistant.activeVariant?.status)
        assertEquals("", assistant.activeVariant?.content)
        cannedPhrases.forEach { phrase ->
            assertTrue(
                "no canned phrase allowed: $phrase",
                !assistant.activeVariant!!.content.contains(phrase),
            )
        }
    }

    @Test
    fun successContentIsExactlyProviderOutput() = runBlocking {
        fixture.use(FakeAiProvider.scripted("真实", "回复", "内容"))

        val result = fixture.runtime.send("mira", "hi")
        assertEquals(SendResult.Completed, result)

        val assistant = fixture.turns("mira").last()
        assertEquals("真实回复内容", assistant.activeVariant?.content)
        assertEquals(VariantStatus.COMPLETE, assistant.activeVariant?.status)
        cannedPhrases.forEach { phrase ->
            assertTrue(!assistant.activeVariant!!.content.contains(phrase))
        }
    }

    @Test
    fun cancelledTerminalNeverBecomesComplete() = runBlocking {
        fixture.use(FakeAiProvider.cancelAfter(2))

        val result = fixture.runtime.send("mira", "hi")
        assertEquals(SendResult.Cancelled, result)

        val assistant = fixture.turns("mira").last()
        assertEquals(VariantStatus.CANCELLED, assistant.activeVariant?.status)
        assertEquals("chunk0chunk1", assistant.activeVariant?.content)
    }
}
