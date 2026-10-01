package com.example.ui.themeengine

import com.example.data.model.DayPhase
import com.example.data.model.WeatherState

object ThemeCatalog {
    const val DEFAULT_ID = "milk"

    val presets = listOf(
        AiluaThemePreset(
            id = "milk", name = "AILUA 奶油", nameEn = "AILUA Milk",
            paletteId = "cream", wallpaperId = "cream", iconStyleId = "milk",
            widgetStyleId = "milk", dockStyleId = "milk", typographyId = "milk",
            statusBarStyleId = "milk", motionStyleId = "milk"
        ),
        AiluaThemePreset(
            id = "glass", name = "AILUA 玻璃", nameEn = "AILUA Glass",
            paletteId = "mist", wallpaperId = "glass", iconStyleId = "glass",
            widgetStyleId = "glass", dockStyleId = "glass", typographyId = "glass",
            statusBarStyleId = "glass", motionStyleId = "glass"
        ),
        AiluaThemePreset(
            id = "diary", name = "AILUA 手账", nameEn = "AILUA Diary",
            paletteId = "cream", wallpaperId = "diary", iconStyleId = "diary",
            widgetStyleId = "diary", dockStyleId = "diary", typographyId = "diary",
            statusBarStyleId = "diary", motionStyleId = "diary"
        ),
        AiluaThemePreset(
            id = "mono", name = "AILUA 极简", nameEn = "AILUA Mono",
            paletteId = "mono", wallpaperId = "mono", iconStyleId = "mono",
            widgetStyleId = "mono", dockStyleId = "mono", typographyId = "mono",
            statusBarStyleId = "mono", motionStyleId = "mono"
        )
    )

    fun byId(id: String): AiluaThemePreset =
        presets.firstOrNull { it.id == id } ?: presets.first()

    val defaultRuntime: AiluaThemeRuntime by lazy {
        ThemeResolver.resolve(ThemeSelection(), false, DayPhase.NOON, WeatherState.CLEAR)
    }
}
