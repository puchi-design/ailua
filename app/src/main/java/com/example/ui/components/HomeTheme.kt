package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.theme.AiluaMoonGold

data class HomeTheme(
    val id: String,
    val name: String,
    val nameEn: String,
    val accent: Color,
    val followsWorldClock: Boolean,
    val lightColors: List<Color>,
    val darkColors: List<Color>
)

object HomeThemeCatalog {
    const val DEFAULT_ID = "follow"

    val themes: List<HomeTheme> = listOf(
        HomeTheme(
            id = "follow",
            name = "跟随世界",
            nameEn = "Follow World",
            accent = AiluaMoonGold,
            followsWorldClock = true,
            lightColors = emptyList(),
            darkColors = emptyList()
        ),
        HomeTheme(
            id = "cream",
            name = "奶油",
            nameEn = "Cream",
            accent = Color(0xFFC2A166),
            followsWorldClock = false,
            lightColors = listOf(Color(0xFFFFFCF6), Color(0xFFF8F1E5), Color(0xFFEFE5D4)),
            darkColors = listOf(Color(0xFF17130E), Color(0xFF1D1811), Color(0xFF241E15))
        ),
        HomeTheme(
            id = "sakura",
            name = "樱色",
            nameEn = "Sakura",
            accent = Color(0xFFCB8697),
            followsWorldClock = false,
            lightColors = listOf(Color(0xFFFFF7F7), Color(0xFFFAECEE), Color(0xFFF2E0E4)),
            darkColors = listOf(Color(0xFF1A1216), Color(0xFF20161B), Color(0xFF271B21))
        ),
        HomeTheme(
            id = "mist",
            name = "雾蓝",
            nameEn = "Mist Blue",
            accent = Color(0xFF7FA6C4),
            followsWorldClock = false,
            lightColors = listOf(Color(0xFFF4F8FB), Color(0xFFEAF1F6), Color(0xFFDDE7EE)),
            darkColors = listOf(Color(0xFF0F141A), Color(0xFF131920), Color(0xFF171E26))
        ),
        HomeTheme(
            id = "starry",
            name = "星夜",
            nameEn = "Starry",
            accent = Color(0xFF9C8FD0),
            followsWorldClock = false,
            lightColors = listOf(Color(0xFFF6F5FC), Color(0xFFEEEDF8), Color(0xFFE3E2F2)),
            darkColors = listOf(Color(0xFF0E0D18), Color(0xFF12101F), Color(0xFF161327))
        )
    )

    fun byId(id: String): HomeTheme = themes.firstOrNull { it.id == id } ?: themes.first()
}

object HomeThemeStore {
    var selectedId by mutableStateOf(HomeThemeCatalog.DEFAULT_ID)
}

fun themeWallpaper(
    theme: HomeTheme,
    dayPhase: DayPhase,
    weather: WeatherState,
    isDarkTheme: Boolean
): WallpaperPalette {
    if (theme.followsWorldClock) {
        return wallpaperPalette(dayPhase, weather, isDarkTheme)
    }
    val colors = if (isDarkTheme) theme.darkColors else theme.lightColors
    return WallpaperPalette(
        key = "${theme.id}/${if (isDarkTheme) "dark" else "light"}",
        colors = colors
    )
}
