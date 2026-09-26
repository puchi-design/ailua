package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.components.HomeThemeCatalog
import com.example.ui.components.HomeThemeStore
import com.example.ui.components.themeWallpaper
import com.example.ui.components.wallpaperPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeThemeTest {

    private fun luminance(color: Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    @Test
    fun themeIdsAreUnique() {
        val ids = HomeThemeCatalog.themes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun defaultThemeFollowsWorldClock() {
        val defaultTheme = HomeThemeCatalog.byId(HomeThemeCatalog.DEFAULT_ID)
        assertTrue(defaultTheme.followsWorldClock)
        assertEquals(HomeThemeCatalog.DEFAULT_ID, HomeThemeStore.selectedId)
    }

    @Test
    fun fixedThemesProvideThreeOpaqueColorsInBothModes() {
        HomeThemeCatalog.themes.filter { !it.followsWorldClock }.forEach { theme ->
            assertEquals("${theme.id} light", 3, theme.lightColors.size)
            assertEquals("${theme.id} dark", 3, theme.darkColors.size)
            (theme.lightColors + theme.darkColors).forEach {
                assertTrue("${theme.id} alpha", it.alpha == 1f)
            }
        }
    }

    @Test
    fun fixedDarkWallpaperIsDarkerThanLight() {
        HomeThemeCatalog.themes.filter { !it.followsWorldClock }.forEach { theme ->
            val light = themeWallpaper(theme, DayPhase.NOON, WeatherState.CLEAR, false)
            val dark = themeWallpaper(theme, DayPhase.NOON, WeatherState.CLEAR, true)
            val lightAverage = light.colors.map { luminance(it) }.average()
            val darkAverage = dark.colors.map { luminance(it) }.average()
            assertTrue("$theme light=$lightAverage dark=$darkAverage", darkAverage < lightAverage)
        }
    }

    @Test
    fun followThemeDelegatesToWorldClockWallpaper() {
        val follow = HomeThemeCatalog.byId("follow")
        assertEquals(
            wallpaperPalette(DayPhase.DUSK, WeatherState.SNOW, true),
            themeWallpaper(follow, DayPhase.DUSK, WeatherState.SNOW, true)
        )
    }

    @Test
    fun everyThemeHasAnAccent() {
        HomeThemeCatalog.themes.forEach { theme ->
            assertTrue("${theme.id} accent", theme.accent.alpha == 1f)
        }
    }

    @Test
    fun unknownIdFallsBackToDefault() {
        assertEquals(HomeThemeCatalog.DEFAULT_ID, HomeThemeCatalog.byId("nope").id)
    }
}
