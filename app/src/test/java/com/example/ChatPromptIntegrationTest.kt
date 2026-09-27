package com.example

import com.example.data.ai.model.AiRole
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.SendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatPromptIntegrationTest — P3C-4 §6: the request that reaches the provider
 * is built ONLY from real sources: CharacterRegistry card (fixture),
 * presence, life events, active lore, DB history — memories empty (P3C-5).
 */
class ChatPromptIntegrationTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun promptCarriesCharacterPresenceLoreEventsAndUserText() = runBlocking {
        fixture.promptContext.lore = listOf(
            runtimeLore("雨夜茶馆", "茶馆只在雨夜对旅人开放。"),
        )
        fixture.promptContext.lifeEvents = listOf(
            runtimeLifeEvent("mira", "雨中散步归来"),
        )
        val provider = FakeAiProvider.scripted("嗯")
        fixture.use(provider)

        val result = fixture.runtime.send("mira", "今天好累")
        assertEquals(SendResult.Completed, result)

        val request = provider.requests.single()
        assertEquals("test-model", request.model)
        assertTrue(request.stream)

        val system = request.messages.first()
        assertEquals(AiRole.SYSTEM, system.role)
        assertTrue(system.content.contains("Mira"))
        assertTrue(system.content.contains("雨天茶馆的主人"))
        assertTrue(system.content.contains("琉璃茶馆"))
        assertTrue(system.content.contains("茶馆只在雨夜对旅人开放。"))
        assertTrue(system.content.contains("雨中散步归来"))

        val last = request.messages.last()
        assertEquals(AiRole.USER, last.role)
        assertEquals("今天好累", last.content)
    }

    @Test
    fun promptHistoryCarriesPreviousExchangeInOrder() = runBlocking {
        val first = FakeAiProvider.scripted("第一句")
        fixture.use(first)
        fixture.runtime.send("mira", "第一条消息")

        val second = FakeAiProvider.scripted("第二句")
        fixture.use(second)
        fixture.runtime.send("mira", "第二条消息")

        val messages = second.requests.single().messages
        val userTexts = messages.filter { it.role == AiRole.USER }.map { it.content }
        assertEquals(2, userTexts.size)
        assertTrue(userTexts[0].contains("第一条消息"))
        assertTrue(userTexts[1].contains("第二条消息"))

        val assistantTexts = messages.filter { it.role == AiRole.ASSISTANT }.map { it.content }
        assertEquals(listOf("第一句"), assistantTexts)
    }

    @Test
    fun promptStartsWithSystemAndEndsWithLatestUserMessage() = runBlocking {
        val provider = FakeAiProvider.scripted("好")
        fixture.use(provider)

        fixture.runtime.send("mira", "最末的用户消息")

        val messages = provider.requests.single().messages
        assertEquals(AiRole.SYSTEM, messages.first().role)
        assertEquals(AiRole.USER, messages.last().role)
        assertEquals("最末的用户消息", messages.last().content)
    }
}
