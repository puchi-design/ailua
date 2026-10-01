package com.example.ui.themeengine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeStorePersistenceTest {
    @Test
    fun savedSelectionCanBeReadFromPreferencesAgain() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("theme_store_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val selected = ThemeSelection(
            themePresetId = "diary",
            paletteOverrideId = "sakura",
            wallpaperOverrideId = "world",
            iconStyleOverrideId = "mono"
        )

        ThemeStore.write(prefs, selected)

        assertEquals(selected, ThemeStore.readSelection(prefs))
    }

    @Test
    fun oldSelectionMigratesOnFirstReadAndUnknownIdsFallBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("theme_store_migration_test", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("home_theme_id", "sakura").commit()
        assertEquals(ThemeSelection("milk", paletteOverrideId = "sakura"), ThemeStore.readSelection(prefs))

        prefs.edit().clear()
            .putString("theme_engine_preset_id", "does-not-exist")
            .putString("theme_engine_palette_override_id", "does-not-exist")
            .commit()
        assertEquals(ThemeSelection("milk"), ThemeStore.readSelection(prefs))
    }
}
