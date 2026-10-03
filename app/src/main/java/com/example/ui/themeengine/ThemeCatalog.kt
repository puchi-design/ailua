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
        ),
        // First visual-review samples. Existing selections and the legacy default stay valid.
        AiluaThemePreset(
            id = "default", name = "AILUA Default", nameEn = "清透日常",
            paletteId = "mist", wallpaperId = "default", iconStyleId = "identity",
            widgetStyleId = "light_glass", dockStyleId = "light_glass", typographyId = "milk",
            statusBarStyleId = "light_home", motionStyleId = "milk"
        ),
        AiluaThemePreset(
            id = "soft_home", name = "Soft Home", nameEn = "书与茶的午后",
            paletteId = "cream", wallpaperId = "soft_home", iconStyleId = "identity",
            widgetStyleId = "soft_glass", dockStyleId = "soft_glass", typographyId = "milk",
            statusBarStyleId = "light_home", motionStyleId = "milk"
        ),
        AiluaThemePreset(
            id = "midnight_glass", name = "Midnight Glass", nameEn = "深蓝微光",
            paletteId = "mist", wallpaperId = "midnight_glass", iconStyleId = "identity",
            widgetStyleId = "dark_glass", dockStyleId = "dark_glass", typographyId = "milk",
            statusBarStyleId = "glass", motionStyleId = "glass"
        )
    )

    fun byId(id: String): AiluaThemePreset =
        presets.firstOrNull { it.id == id } ?: presets.first()

    val defaultRuntime: AiluaThemeRuntime by lazy {
        ThemeResolver.resolve(ThemeSelection(), false, DayPhase.NOON, WeatherState.CLEAR)
    }
}
