package com.example

import com.example.data.ai.model.AiRole
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.WorldChatPromptContext
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichMessagePromptHistoryTest {
    @Test fun completedCardActionsReachTheNextProviderRequest() = runBlocking {
        val f = ChatRuntimeFixture()
        val sessionId = f.sessionId("mira")
        fun settled(type: RichMessageType, label: String, status: RichMessageStatus) {
            val turn = f.repository.appendAssistantTurn(sessionId, "", VariantStatus.COMPLETE)
            val variantId = checkNotNull(turn.activeVariantId)
            f.repository.updateVariant(variantId, "", VariantStatus.COMPLETE,
                richPayloads = listOf(RichMessagePayload(type, label, 18.0, "¥", RichMessageStatus.PENDING)))
            assertTrue(f.repository.updateRichStatus(variantId, 0, status))
        }
        settled(RichMessageType.RED_PACKET, "晚饭钱", RichMessageStatus.OPENED)
        settled(RichMessageType.TRANSFER, "午饭钱", RichMessageStatus.ACCEPTED)
        settled(RichMessageType.TRANSFER, "咖啡钱", RichMessageStatus.DECLINED)
        settled(RichMessageType.GIFT, "一束花", RichMessageStatus.RECEIVED)

        val provider = FakeAiProvider.scripted("好。")
        f.use(provider)
        f.runtime.send("mira", "你都看到了吗？")
        val history = provider.requests.single().messages
            .filter { it.role == AiRole.ASSISTANT }.joinToString("\n") { it.content }
        assertTrue(history.contains("晚饭钱；用户已打开"))
        assertTrue(history.contains("午饭钱；用户已收下"))
        assertTrue(history.contains("咖啡钱；用户已退回"))
        assertTrue(history.contains("一束花；用户已收下"))
    }

    @Test fun generatedDirectiveIsPersistedAsTypedCardAndNotRawBracketText() = runBlocking {
        val f = ChatRuntimeFixture()
        f.use(FakeAiProvider.scripted("先买点吃的。[红包:30:晚饭钱]"))
        f.runtime.send("mira", "我今天加班")
        val answer = f.turns("mira").last().activeVariant!!
        assertEquals("先买点吃的。", answer.content)
        assertEquals(listOf(RichMessageType.TEXT, RichMessageType.RED_PACKET), answer.richPayloads.map { it.type })
        assertEquals(30.0, answer.richPayloads.last().amount)
    }

    @Test fun chinesePunctuationFromProviderStillPersistsAsRichCard() = runBlocking {
        val f = ChatRuntimeFixture()
        f.use(FakeAiProvider.scripted("先吃点东西。【红包：30：晚饭钱】"))
        f.runtime.send("mira", "我今天加班")
        val answer = f.turns("mira").last().activeVariant!!
        assertEquals("先吃点东西。", answer.content)
        assertEquals(RichMessageType.RED_PACKET, answer.richPayloads.last().type)
        assertEquals(30.0, answer.richPayloads.last().amount)
    }

    @Test fun quoteOfPriorAssistantTurnIsExplainedToModelWithoutFabricatedId() = runBlocking {
        val f = ChatRuntimeFixture()
        f.use(FakeAiProvider.scripted("早点睡。"))
        f.runtime.send("mira", "晚安")
        val prior = f.turns("mira").last()
        val provider = FakeAiProvider.scripted("被你发现了。")
        f.use(provider)
        f.runtime.send("mira", "你自己还不是没睡。", prior.id, "早点睡。")
        val request = provider.requests.single()
        val quoted = request.messages.last()
        assertEquals(AiRole.USER, quoted.role)
        assertTrue(quoted.content.contains("用户回复了你之前的消息"))
        assertTrue(quoted.content.contains("早点睡。"))
        assertTrue(quoted.content.contains("你自己还不是没睡。"))
        assertFalse(quoted.content.contains(prior.id))
        assertTrue(request.messages.any { it.role == AiRole.SYSTEM && it.content.contains("低频行为") })
    }

    @Test fun viewingPhoneIsNotACharacterPromptFactButChatActionIs() {
        val suffix = System.nanoTime().toString()
        val privateView = LifeEvent("private_phone_$suffix", "mira", "12:01", LifeEventType.SOCIAL,
            "用户查看了角色手机", "打开了虚拟手机", sourceAppId = "check_phone",
            metadata = mapOf("prompt_visibility" to "hidden"))
        val chatAction = LifeEvent("chat_visible_$suffix", "mira", "12:02", LifeEventType.MESSAGE,
            "用户发来消息", "今天见面吗", sourceAppId = "chat")
        WorldStateRepository.appendLifeEvent(privateView)
        WorldStateRepository.appendLifeEvent(chatAction)
        val facts = WorldChatPromptContext.lifeEvents("mira")
        assertFalse(facts.any { it.id == privateView.id })
        assertTrue(facts.any { it.id == chatAction.id })
    }
}
