package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ai.security.ApiSecretStore
import com.example.data.ai.model.ProviderProfile
import com.example.data.ai.onboarding.ProviderSetup
import com.example.data.ai.repository.ProviderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in device QA: consumes an app-private, one-time key file and uses the production secret store. */
@RunWith(AndroidJUnit4::class)
class V61RealProviderSetupTest {
    @Test
    fun switchApprovedProviderToResponsiveModel() {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("qaRealProviderSetup"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ProviderRepository.create(context)
        val profile = checkNotNull(repository.activeProfile())
        assertEquals("v61_approved_vibework", profile.id)
        repository.saveProfile(profile.copy(model = "gpt-5.6-luna"))
        assertTrue(context.getSharedPreferences(ProviderRepository.PREFS_FILE, 0).edit().commit())
        assertEquals("gpt-5.6-luna", ProviderRepository.create(context).activeProfile()?.model)
    }

    @Test
    fun connectAndStoreApprovedChatProvider() = runBlocking {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("qaRealProviderSetup"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyFile = context.getFileStreamPath("qa_chat_provider_key")
        val key = try {
            check(keyFile.isFile) { "One-time app-private credential file is missing" }
            keyFile.readText().trim()
        } finally {
            keyFile.delete()
        }
        check(key.isNotBlank()) { "One-time credential is blank" }

        val baseUrl = "https://www.vibework.live/v1"
        val model = "gpt-6-luna"
        assertEquals("连接成功", ProviderSetup.test(baseUrl, model, key))
        val repository = ProviderRepository.create(context)
        val id = "v61_approved_vibework"
        repository.saveProfile(ProviderProfile(id, "VibeWork", baseUrl, model))
        assertTrue(repository.setApiKey(id, key))
        repository.setActiveProfile(id)
        assertEquals(id, repository.activeProfileId.value)
        assertNotNull(repository.maskedKeyLabel(id))
        assertTrue(context.getSharedPreferences(ProviderRepository.PREFS_FILE, 0).edit().commit())
        assertTrue(context.getSharedPreferences(ApiSecretStore.PREFS_FILE, 0).edit().commit())
        val reopened = ProviderRepository.create(context)
        assertEquals(id, reopened.activeProfileId.value)
        assertNotNull(reopened.resolveApiKey(id))
    }
}
