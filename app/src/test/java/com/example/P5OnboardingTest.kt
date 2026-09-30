package com.example

import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.onboarding.ProviderSetup
import com.example.data.firstsession.FirstSessionPolicy
import com.example.data.firstsession.FirstSessionState
import com.example.data.model.WorldClock
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.data.model.LifeEventType
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

class P5OnboardingTest {
    @Test fun providerValidationRejectsUnsafeOrIncompleteConfiguration() {
        assertNull(ProviderSetup.validate("https://api.example.com/v1", "some-model", true))
        assertNotNull(ProviderSetup.validate("http://api.example.com/v1", "some-model", true))
        assertNotNull(ProviderSetup.validate("https://name:secret@api.example.com/v1", "some-model", true))
        assertEquals("请输入模型名称", ProviderSetup.validate("https://api.example.com/v1", "", true))
        assertEquals("请输入 API Key", ProviderSetup.validate("https://api.example.com/v1", "model", false))
    }

    @Test fun providerErrorsAreReadableAndNeverEchoServerSecrets() {
        assertEquals("认证失败，请检查 API Key", ProviderSetup.friendly(AiProviderError.Unauthorized))
        assertEquals("模型或接口地址不存在", ProviderSetup.friendly(AiProviderError.Http(404, "secret")))
        assertFalse(ProviderSetup.friendly(AiProviderError.Network("secret" )).contains("secret"))
    }

    @Test fun firstSessionRequiresReplyLivingAndFact() {
        val start = FirstSessionState()
        assertFalse(start.journeyComplete)
        assertFalse(start.copy(receivedFirstReply = true, viewedLiving = true).journeyComplete)
        assertTrue(start.copy(receivedFirstReply = true, viewedLiving = true, continuationCreated = true).journeyComplete)
    }

    @Test fun onlyExplicitNameBecomesFirstMemory() {
        assertEquals("用户希望被称为小雨", FirstSessionPolicy.firstMemory("我叫小雨"))
        assertNull(FirstSessionPolicy.firstMemory("哈哈"))
        assertNull(FirstSessionPolicy.firstMemory("你现在在做什么？"))
        assertNull(FirstSessionPolicy.firstMemory("我叫"))
    }

    @Test fun connectionProbeUsesMinimalRequestAndMapsFailure() = runBlocking {
        var calls = 0
        val provider = object : AiProvider {
            override fun streamChat(request: AiChatRequest) = flow {
                calls++
                assertEquals("model", request.model)
                assertFalse(request.stream)
                assertEquals(8, request.maxTokens)
                emit(AiStreamEvent.Failed(AiProviderError.Unauthorized))
            }
        }
        assertEquals("认证失败，请检查 API Key", ProviderSetup.test("https://api.example.com/v1", "model", "bad", provider))
        assertEquals(1, calls)
        assertEquals("请输入 API Key", ProviderSetup.test("https://api.example.com/v1", "model", "", provider))
        assertEquals(1, calls)
    }

    @Test fun firstContinuationIsARealCharacterOwnedFact() {
        val clock = WorldClock("9月25日", 1320, DayPhase.NIGHT, WeatherState.RAIN)
        val event = FirstSessionPolicy.continuation("yuna", "悠奈", "街角便利店", clock)
        assertEquals("first_session_continuation_yuna", event.id)
        assertEquals("yuna", event.characterId)
        assertEquals(LifeEventType.THOUGHT, event.type)
        assertEquals("街角便利店", event.location)
        assertFalse(event.title.contains("用户"))
    }
}
