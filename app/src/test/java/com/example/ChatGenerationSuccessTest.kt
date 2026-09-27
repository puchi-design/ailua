package com.example

import com.example.data.ai.runtime.SendResult
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatGenerationSuccessTest — P3C-4 happy path E2E with a scripted fake
 * provider (spec §17): USER hi → stream 你/好/呀 → ASSISTANT 你好呀 → COMPLETE.
 */
class ChatGenerationSuccessTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun sendStreamsAndPersistsCompleteAssistantReply() = runBlocking {
        val provider = FakeAiProvider.scripted("你", "好", "呀")
        fixture.use(provider)

        val result = fixture.runtime.send("mira", "hi")

        assertEquals(SendResult.Completed, result)
        val turns = fixture.turns("mira")
        assertEquals(2, turns.size)

        val user = turns[0]
        assertEquals(ChatTurnRole.USER, user.role)
        assertEquals("hi", user.activeVariant?.content)
        assertEquals(VariantStatus.COMPLETE, user.activeVariant?.status)

        val assistant = turns[1]
        assertEquals(ChatTurnRole.ASSISTANT, assistant.role)
        assertEquals("你好呀", assistant.activeVariant?.content)
        assertEquals(VariantStatus.COMPLETE, assistant.activeVariant?.status)
        assertEquals("prof-1", assistant.activeVariant?.providerProfileId)
        assertEquals("test-model", assistant.activeVariant?.model)
        assertEquals(1, assistant.variantCount)

        assertNotNull(assistant.activeVariantId)
        assertNull(fixture.runtime.streaming.value)
    }

    @Test
    fun positionsAreStableCountersAcrossSends() = runBlocking {
        fixture.use(FakeAiProvider.scripted("A"))
        fixture.runtime.send("mira", "m1")
        fixture.runtime.send("mira", "m2")
        fixture.runtime.send("mira", "m3")

        val turns = fixture.turns("mira")
        assertEquals(6, turns.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), turns.map { it.position })
        assertEquals(
            listOf(
                ChatTurnRole.USER, ChatTurnRole.ASSISTANT,
                ChatTurnRole.USER, ChatTurnRole.ASSISTANT,
                ChatTurnRole.USER, ChatTurnRole.ASSISTANT,
            ),
            turns.map { it.role },
        )
        assertTrue(turns.all { it.activeVariant?.status == VariantStatus.COMPLETE })
    }
}
