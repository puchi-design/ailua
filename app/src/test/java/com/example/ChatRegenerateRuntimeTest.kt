package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.SendResult
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatRegenerateRuntimeTest — P3C-4 §10: regenerate rebuilds the LATEST
 * assistant turn as a NEW VARIANT on the SAME turn (P3C-3 §11 semantics);
 * prompt history stops strictly before the target turn.
 */
class ChatRegenerateRuntimeTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun regenerateAppendsSecondVariantToSameTurn() = runBlocking {
        fixture.use(FakeAiProvider.scripted("第一版"))
        fixture.runtime.send("mira", "hi")

        fixture.use(FakeAiProvider.scripted("第二版"))
        val result = fixture.runtime.regenerate("mira")
        assertEquals(SendResult.Completed, result)

        val turns = fixture.turns("mira")
        assertEquals(2, turns.size)
        assertEquals(listOf(0, 1), turns.map { it.position })

        val assistant = turns[1]
        assertEquals(ChatTurnRole.ASSISTANT, assistant.role)
        assertEquals(2, assistant.variantCount)
        assertEquals("第二版", assistant.activeVariant?.content)
        assertEquals(1, assistant.activeVariant?.variantIndex)
        assertEquals(VariantStatus.COMPLETE, assistant.activeVariant?.status)
    }

    @Test
    fun regeneratePromptExcludesTargetTurnContent() = runBlocking {
        fixture.use(FakeAiProvider.scripted("第一版"))
        fixture.runtime.send("mira", "hi")

        val regenerated = FakeAiProvider.scripted("第二版")
        fixture.use(regenerated)
        fixture.runtime.regenerate("mira")

        val messages = regenerated.requests.single().messages
        val allText = messages.joinToString("\n") { it.content }
        assertFalse(allText.contains("第一版"))
        assertTrue(allText.contains("hi"))
        assertTrue(messages.last().content.contains("hi"))
    }

    @Test
    fun regenerateOnFreshSessionReturnsNothingToRegenerate() = runBlocking {
        fixture.use(FakeAiProvider.scripted("回复"))

        val result = fixture.runtime.regenerate("mira")

        assertEquals(SendResult.NothingToRegenerate, result)
        assertEquals(0L, fixture.chat.database.chatTurnQueries.countTurns().executeAsOne())
    }

    @Test
    fun repeatedRegeneratesGrowVariantCountOnly() = runBlocking {
        fixture.use(FakeAiProvider.scripted("v0"))
        fixture.runtime.send("mira", "hi")

        fixture.use(FakeAiProvider.scripted("v1"))
        fixture.runtime.regenerate("mira")
        fixture.use(FakeAiProvider.scripted("v2"))
        fixture.runtime.regenerate("mira")

        val turns = fixture.turns("mira")
        assertEquals(2, turns.size)
        val assistant = turns[1]
        assertEquals(3, assistant.variantCount)
        assertEquals("v2", assistant.activeVariant?.content)
        assertEquals(2, assistant.activeVariant?.variantIndex)
    }
}
