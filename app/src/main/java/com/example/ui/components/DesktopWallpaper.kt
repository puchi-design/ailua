package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState

data class WallpaperPalette(
    val key: String,
    val colors: List<Color>
)

private val lightBase: Map<DayPhase, List<Color>> = mapOf(
    DayPhase.DAWN to listOf(Color(0xFFFEF8F4), Color(0xFFFBF2ED), Color(0xFFF4E9E4)),
    DayPhase.MORNING to listOf(Color(0xFFFFFCF4), Color(0xFFF9F7EC), Color(0xFFF1EFE1)),
    DayPhase.NOON to listOf(Color(0xFFFFFEF9), Color(0xFFFAFAF2), Color(0xFFF3F3E8)),
    DayPhase.AFTERNOON to listOf(Color(0xFFFFFAF0), Color(0xFFF8F2E4), Color(0xFFF0E9D9)),
    DayPhase.DUSK to listOf(Color(0xFFFDF7EE), Color(0xFFF7EFE1), Color(0xFFEEE4D2)),
    DayPhase.EVENING to listOf(Color(0xFFF9F6F0), Color(0xFFF4F0E8), Color(0xFFEBE6DC)),
    DayPhase.NIGHT to listOf(Color(0xFFF7F6F5), Color(0xFFF2F1F1), Color(0xFFE9E8E9)),
    DayPhase.LATE_NIGHT to listOf(Color(0xFFF6F6FA), Color(0xFFF1F1F6), Color(0xFFE7E7EF))
)

private val darkBase: Map<DayPhase, List<Color>> = mapOf(
    DayPhase.DAWN to listOf(Color(0xFF1A151D), Color(0xFF1F1923), Color(0xFF251E2A)),
    DayPhase.MORNING to listOf(Color(0xFF14161D), Color(0xFF181A21), Color(0xFF1E2027)),
    DayPhase.NOON to listOf(Color(0xFF12141B), Color(0xFF16181F), Color(0xFF1C1E25)),
    DayPhase.AFTERNOON to listOf(Color(0xFF171511), Color(0xFF1C1A16), Color(0xFF221F1A)),
    DayPhase.DUSK to listOf(Color(0xFF1A1418), Color(0xFF1F181D), Color(0xFF251D22)),
    DayPhase.EVENING to listOf(Color(0xFF13121C), Color(0xFF191724), Color(0xFF1F1B2F)),
    DayPhase.NIGHT to listOf(Color(0xFF10101A), Color(0xFF14141F), Color(0xFF181825)),
    DayPhase.LATE_NIGHT to listOf(Color(0xFF0D0D15), Color(0xFF11111B), Color(0xFF15151F))
)

private val weatherTint: Map<WeatherState, Color> = mapOf(
    WeatherState.CLEAR to Color(0xFFFFF4DE),
    WeatherState.CLOUDY to Color(0xFFEFF0F2),
    WeatherState.RAIN to Color(0xFFE4EAF2),
    WeatherState.HEAVY_RAIN to Color(0xFFD9E1EC),
    WeatherState.SNOW to Color(0xFFF2F5FA)
)

private val darkWeatherTint: Map<WeatherState, Color> = mapOf(
    WeatherState.CLEAR to Color(0xFF241F14),
    WeatherState.CLOUDY to Color(0xFF1C1E22),
    WeatherState.RAIN to Color(0xFF171C26),
    WeatherState.HEAVY_RAIN to Color(0xFF141922),
    WeatherState.SNOW to Color(0xFF1E222B)
)

internal fun mixColor(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount
)

fun wallpaperPalette(
    dayPhase: DayPhase,
    weather: WeatherState,
    isDarkTheme: Boolean
): WallpaperPalette {
    val base = (if (isDarkTheme) darkBase else lightBase).getValue(dayPhase)
    val tint = (if (isDarkTheme) darkWeatherTint else weatherTint).getValue(weather)
    val amount = if (isDarkTheme) 0.16f else 0.10f
    return WallpaperPalette(
        key = "$dayPhase/$weather/${if (isDarkTheme) "dark" else "light"}",
        colors = base.map { mixColor(it, tint, amount) }
    )
}
