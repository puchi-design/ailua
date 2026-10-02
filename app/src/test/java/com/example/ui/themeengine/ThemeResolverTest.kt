package com.example.ui.themeengine

import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.components.wallpaperPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {
    @Test
    fun allPresetsProduceDistinctVisualTreatments() {
        val runtimes = ThemeCatalog.presets.map { preset ->
            ThemeResolver.resolve(ThemeSelection(preset.id), false, DayPhase.NOON, WeatherState.CLEAR)
        }
        assertEquals(4, runtimes.map { it.icons.containerStyle }.toSet().size)
        assertEquals(4, runtimes.map { it.dock.containerMode }.toSet().size)
        assertEquals(4, runtimes.map { it.motion.editMotion }.toSet().size)
        assertEquals(4, runtimes.map { it.lockscreen.notificationStyle.cornerRadiusDp }.toSet().size)
        assertEquals(4, runtimes.map { it.shade.backgroundAlpha }.toSet().size)
        assertEquals(4, runtimes.map { it.controlCenter.tileCornerRadiusDp }.toSet().size)
        assertEquals(4, runtimes.map { it.liveActivity.expandedCornerRadiusDp }.toSet().size)
    }

    @Test
    fun paletteOverridePreservesDiaryStructureAndChangesColors() {
        val base = ThemeResolver.resolve(ThemeSelection("diary"), false, DayPhase.NOON, WeatherState.CLEAR)
        val sakura = ThemeResolver.resolve(
            ThemeSelection("diary", paletteOverrideId = "sakura"),
            false, DayPhase.NOON, WeatherState.CLEAR
        )
        assertEquals(base.icons.containerStyle, sakura.icons.containerStyle)
        assertEquals(base.widgets.backgroundStyle, sakura.widgets.backgroundStyle)
        assertEquals(base.dock.containerMode, sakura.dock.containerMode)
        assertNotEquals(base.palette.accent, sakura.palette.accent)
        assertNotEquals(base.wallpaper.colors, sakura.wallpaper.colors)
        assertEquals(base.lockscreen.notificationStyle.cornerRadiusDp, sakura.lockscreen.notificationStyle.cornerRadiusDp)
        assertEquals(base.controlCenter.tileCornerRadiusDp, sakura.controlCenter.tileCornerRadiusDp)
        assertEquals(sakura.palette.onSurface, sakura.liveActivity.foregroundColor)
    }

    @Test
    fun worldWallpaperDelegatesToExistingResolver() {
        val runtime = ThemeResolver.resolve(
            ThemeSelection("milk", wallpaperOverrideId = "world"),
            true, DayPhase.DUSK, WeatherState.RAIN
        )
        assertEquals(
            wallpaperPalette(DayPhase.DUSK, WeatherState.RAIN, true).colors,
            runtime.wallpaper.colors
        )
    }

    @Test
    fun unknownPresetFallsBackToMilk() {
        val runtime = ThemeResolver.resolve(
            ThemeSelection("not-a-preset"), false, DayPhase.NOON, WeatherState.CLEAR
        )
        assertEquals("milk", runtime.id)
    }

    @Test
    fun legacyThemeIdsBecomeMilkPaletteSelections() {
        assertEquals("sakura", ThemeStore.migrateLegacy("sakura").paletteOverrideId)
        assertEquals("world", ThemeStore.migrateLegacy("follow").paletteOverrideId)
        assertEquals("milk", ThemeStore.migrateLegacy("follow").themePresetId)
        assertTrue(ThemeStore.migrateLegacy("unknown").paletteOverrideId == null)
    }
}
