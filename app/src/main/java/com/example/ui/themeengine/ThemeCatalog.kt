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
        // Official themes share the existing selection chain. Legacy defaults stay valid.
        AiluaThemePreset(
            id = "default", name = "AILUA Default", nameEn = "清透日常",
            paletteId = "mist", wallpaperId = "default", iconStyleId = "default_icons",
            widgetStyleId = "light_glass", dockStyleId = "light_glass", typographyId = "milk",
            statusBarStyleId = "light_home", motionStyleId = "milk"
        ),
        AiluaThemePreset(
            id = "soft_home", name = "Soft Home", nameEn = "花园里的日常",
            paletteId = "mist", wallpaperId = "soft_home", iconStyleId = "soft_home_icons",
            widgetStyleId = "soft_glass", dockStyleId = "soft_glass", typographyId = "milk",
            statusBarStyleId = "light_home", motionStyleId = "milk"
        ),
        AiluaThemePreset(
            id = "rainy_study", name = "Rainy Study", nameEn = "湖畔花田",
            paletteId = "rainy", wallpaperId = "rainy_study", iconStyleId = "rainy_study_icons",
            widgetStyleId = "rain_glass", dockStyleId = "rain_glass", typographyId = "milk",
            statusBarStyleId = "glass", motionStyleId = "glass"
        ),
        AiluaThemePreset(
            id = "sakura_diary", name = "Sakura Diary", nameEn = "樱花手账",
            paletteId = "sakura", wallpaperId = "sakura_diary", iconStyleId = "sakura_icons",
            widgetStyleId = "sakura_paper", dockStyleId = "sakura_paper", typographyId = "diary",
            statusBarStyleId = "light_home", motionStyleId = "diary"
        ),
        AiluaThemePreset(
            id = "y2k_love", name = "Y2K Love PC", nameEn = "粉紫旧电脑",
            paletteId = "y2k", wallpaperId = "y2k_love", iconStyleId = "y2k_icons",
            widgetStyleId = "y2k_panel", dockStyleId = "y2k_taskbar", typographyId = "y2k",
            statusBarStyleId = "light_home", motionStyleId = "mono"
        ),
        AiluaThemePreset(
            id = "midnight_glass", name = "Midnight Glass", nameEn = "深蓝微光",
            paletteId = "mist", wallpaperId = "midnight_glass", iconStyleId = "midnight_icons",
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
