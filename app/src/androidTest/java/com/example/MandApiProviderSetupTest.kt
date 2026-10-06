package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ai.model.ProviderProfile
import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiMessage
import com.example.data.ai.model.AiRole
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.onboarding.ProviderSetup
import com.example.data.ai.provider.OpenAiCompatibleProvider
import com.example.data.ai.repository.ProviderRepository
import com.example.data.ai.security.ApiSecretStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/** Opt-in local QA. The key is supplied through an app-private file and never baked into the APK. */
@RunWith(AndroidJUnit4::class)
class MandApiProviderSetupTest {
    @Test fun endpointIsReachableFromDevice() {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("qaMandApiProviderSetup"))
        val connection = URL("https://api.mandapi.com/v1/models").openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        try {
            assertTrue("Expected an HTTP response from MandAPI", connection.responseCode in 200..499)
        } finally {
            connection.disconnect()
        }
    }

    @Test fun appTransportRespondsWithinExtendedDiagnosticWindow() = runBlocking {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("qaMandApiProviderSetup"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyFile = context.getFileStreamPath("qa_mandapi_probe_key")
        val key = try {
            check(keyFile.isFile) { "One-time diagnostic credential file is missing" }
            keyFile.readText().trim()
        } finally {
            keyFile.delete()
        }
        val startedAt = System.currentTimeMillis()
        val terminal = withTimeoutOrNull(70_000) {
            OpenAiCompatibleProvider("https://api.mandapi.com/v1", key).streamChat(
                AiChatRequest("deepseek-v4.1-flash", listOf(AiMessage(AiRole.USER, "请只回复：好")),
                    stream = false, maxTokens = 8)
            ).first { it is AiStreamEvent.Completed || it is AiStreamEvent.Failed || it is AiStreamEvent.Cancelled }
        }
        println("MandAPI app transport terminal=${terminal?.javaClass?.simpleName} elapsedMs=${System.currentTimeMillis() - startedAt}")
        assertTrue("App transport failed: ${terminal?.javaClass?.simpleName}", terminal is AiStreamEvent.Completed)
    }

    @Test fun connectAndStoreApprovedMandApiProvider() = runBlocking {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("qaMandApiProviderSetup"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyFile = context.getFileStreamPath("qa_mandapi_provider_key")
        val key = try {
            check(keyFile.isFile) { "One-time app-private credential file is missing" }
            keyFile.readText().trim()
        } finally {
            keyFile.delete()
        }
        check(key.isNotBlank()) { "One-time credential is blank" }

        val id = "mandapi_deepseek_v41_flash"
        val baseUrl = "https://api.mandapi.com/v1"
        val model = "deepseek-v4.1-flash"
        assertEquals("连接成功", ProviderSetup.test(baseUrl, model, key))

        val repository = ProviderRepository.create(context)
        repository.saveProfile(ProviderProfile(id, "MandAPI DeepSeek V4.1 Flash", baseUrl, model))
        assertTrue(repository.setApiKey(id, key))
        repository.setActiveProfile(id)
        assertTrue(context.getSharedPreferences(ProviderRepository.PREFS_FILE, 0).edit().commit())
        assertTrue(context.getSharedPreferences(ApiSecretStore.PREFS_FILE, 0).edit().commit())

        val reopened = ProviderRepository.create(context)
        assertEquals(id, reopened.activeProfileId.value)
        assertEquals(model, reopened.activeProfile()?.model)
        assertNotNull(reopened.resolveApiKey(id))
    }
}
