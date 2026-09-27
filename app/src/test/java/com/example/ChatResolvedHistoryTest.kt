package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.provider.FakeAiStep
import com.example.data.ai.model.AiProviderError
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatResolvedHistoryTest — P3C-4 §7 prompt-history rules: only COMPLETE
 * turns via their ACTIVE variant enter the prompt; FAILED / CANCELLED /
 * STREAMING never do.
 */
class ChatResolvedHistoryTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun failedAssistantTurnNeverEntersPromptHistory() = runBlocking {
        val failing = FakeAiProvider(
            listOf(
                FakeAiStep.Emit("坏回复"),
                FakeAiStep.Fail(AiProviderError.Http(502, "bad gateway")),
            ),
        )
        fixture.use(failing)
        fixture.runtime.send("mira", "第一条")

        val ok = FakeAiProvider.scripted("好回复")
        fixture.use(ok)
        fixture.runtime.send("mira", "第二条")

        val messages = ok.requests.single().messages
        val allText = messages.joinToString("\n") { it.content }
        assertFalse(allText.contains("坏回复"))
        assertTrue(allText.contains("第一条"))
        assertTrue(allText.contains("第二条"))
    }

    @Test
    fun streamingAndCancelledTurnsNeverEnterPromptHistory() = runBlocking {
        val session = fixture.sessionId("mira")
        fixture.repository.appendAssistantTurn(
            sessionId = session,
            content = "卡住的流式文本",
            status = VariantStatus.STREAMING,
        )
        fixture.repository.appendAssistantTurn(
            sessionId = session,
            content = "被取消的文本",
            status = VariantStatus.CANCELLED,
        )

        val provider = FakeAiProvider.scripted("回答")
        fixture.use(provider)
        fixture.runtime.send("mira", "继续")

        val allText = provider.requests.single().messages.joinToString("\n") { it.content }
        assertFalse(allText.contains("卡住的流式文本"))
        assertFalse(allText.contains("被取消的文本"))
    }

    @Test
    fun onlyActiveVariantContentEntersPromptHistory() = runBlocking {
        val ok = FakeAiProvider.scripted("甲变体")
        fixture.use(ok)
        fixture.runtime.send("mira", "问题")

        val session = fixture.sessionId("mira")
        val assistantTurn = fixture.repository.getResolvedTurns(session)
            .last { it.role == com.example.data.chat.model.ChatTurnRole.ASSISTANT }
        val originalVariantId = assistantTurn.activeVariantId!!

        fixture.repository.appendVariant(
            turnId = assistantTurn.id,
            content = "乙变体",
            status = VariantStatus.COMPLETE,
        )

        val second = FakeAiProvider.scripted("继续")
        fixture.use(second)
        fixture.runtime.send("mira", "追问")

        var text = second.requests.single().messages.joinToString("\n") { it.content }
        assertTrue(text.contains("乙变体"))
        assertFalse(text.contains("甲变体"))

        fixture.repository.selectVariant(assistantTurn.id, originalVariantId)

        val third = FakeAiProvider.scripted("再继续")
        fixture.use(third)
        fixture.runtime.send("mira", "再追问")

        text = third.requests.single().messages.joinToString("\n") { it.content }
        assertTrue(text.contains("甲变体"))
        assertFalse(text.contains("乙变体"))
    }

    @Test
    fun resolvedReadsExposeActiveStatusAndCount() = runBlocking {
        fixture.use(FakeAiProvider.scripted("v1"))
        fixture.runtime.send("mira", "hi")
        val session = fixture.sessionId("mira")
        val turn = fixture.repository.getResolvedTurns(session).last()
        assertEquals(VariantStatus.COMPLETE, turn.activeVariant?.status)
        assertEquals(1, turn.variantCount)
        assertEquals(turn.activeVariantId, turn.activeVariant?.id)
    }
}
