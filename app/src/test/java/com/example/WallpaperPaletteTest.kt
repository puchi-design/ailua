package com.example

import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.components.wallpaperPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPaletteTest {

    private fun luminance(color: androidx.compose.ui.graphics.Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    @Test
    fun paletteAlwaysHasThreeOpaqueColors() {
        for (phase in DayPhase.values()) {
            for (weather in WeatherState.values()) {
                for (dark in listOf(false, true)) {
                    val palette = wallpaperPalette(phase, weather, dark)
                    assertEquals(3, palette.colors.size)
                    palette.colors.forEach { assertTrue(it.alpha == 1f) }
                }
            }
        }
    }

    @Test
    fun everyDayPhaseHasItsOwnLightPalette() {
        val keys = DayPhase.values().map {
            wallpaperPalette(it, WeatherState.CLEAR, false).colors.toString()
        }.toSet()
        assertEquals(DayPhase.values().size, keys.size)
    }

    @Test
    fun darkPaletteIsDarkerThanLightPalette() {
        for (phase in DayPhase.values()) {
            val light = wallpaperPalette(phase, WeatherState.RAIN, false)
            val dark = wallpaperPalette(phase, WeatherState.RAIN, true)
            val lightAverage = light.colors.map { luminance(it) }.average()
            val darkAverage = dark.colors.map { luminance(it) }.average()
            assertTrue("$phase light=$lightAverage dark=$darkAverage", darkAverage < lightAverage)
        }
    }

    @Test
    fun weatherTintShiftsTheSamePhase() {
        for (phase in DayPhase.values()) {
            val clear = wallpaperPalette(phase, WeatherState.CLEAR, false)
            val rain = wallpaperPalette(phase, WeatherState.RAIN, false)
            assertNotEquals(clear.colors, rain.colors)
        }
    }

    @Test
    fun paletteKeyEncodesInputs() {
        assertEquals(
            "EVENING/RAIN/dark",
            wallpaperPalette(DayPhase.EVENING, WeatherState.RAIN, true).key
        )
        assertEquals(
            "NOON/CLEAR/light",
            wallpaperPalette(DayPhase.NOON, WeatherState.CLEAR, false).key
        )
    }

    @Test
    fun tintIsDeterministic() {
        val first = wallpaperPalette(DayPhase.DUSK, WeatherState.SNOW, false)
        val second = wallpaperPalette(DayPhase.DUSK, WeatherState.SNOW, false)
        assertEquals(first, second)
    }
}
