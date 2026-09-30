package com.example

import com.example.data.ai.model.AiProviderError
import com.example.data.ai.onboarding.ProviderSetup
import com.example.data.firstsession.FirstSessionPolicy
import com.example.data.firstsession.FirstSessionState
import org.junit.Assert.*
import org.junit.Test

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
}
