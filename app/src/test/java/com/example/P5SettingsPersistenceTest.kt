package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firstsession.FirstSessionStore
import com.example.data.reality.RealityRepository
import com.example.data.reality.RealitySettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class P5SettingsPersistenceTest {
    @Test fun firstSessionFlagsAndRealityConsentSurviveRepositoryReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirstSessionStore.init(context)
        FirstSessionStore.completeOnboarding()
        FirstSessionStore.markLiving()
        val flags = context.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE)
        assertTrue(flags.getBoolean("onboarding", false))
        assertTrue(flags.getBoolean("living", false))

        RealityRepository.init(context)
        val disabled = RealitySettings(batteryEnabled = false, screenEnabled = false, usageEnabled = false, healthEnabled = false)
        RealityRepository.setSettings(disabled)
        RealityRepository.init(context)
        assertEquals(disabled, RealityRepository.settings.value)
        RealityRepository.setSettings(RealitySettings())
    }
}
