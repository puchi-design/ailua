package com.example.ui.home.edit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeDisplayPreferencesTest {
    @Test
    fun displayOptionsSurvivePreferenceReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("ailua_home_display", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        HomeDisplayPreferencesStore.initialize(context)
        val selected = HomeDisplayPreferences(
            showLabels = false,
            iconScale = 1.1f,
            showPageIndicator = false,
        )

        HomeDisplayPreferencesStore.update(selected)

        assertEquals(selected, HomeDisplayPreferencesStore.read(prefs))
    }
}
