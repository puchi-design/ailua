package com.example.ui.themeengine

import androidx.compose.ui.graphics.Color

data class PaletteOption(
    val id: String,
    val name: String,
    val nameEn: String,
    val accent: Color,
    val lightColors: List<Color>,
    val darkColors: List<Color>
)

object PaletteCatalog {
    const val DEFAULT_ID = "cream"

    val palettes = listOf(
        PaletteOption(
            "cream", "奶油", "Cream", Color(0xFFC2A166),
            listOf(Color(0xFFFFFCF6), Color(0xFFF8F1E5), Color(0xFFEFE5D4)),
            listOf(Color(0xFF17130E), Color(0xFF1D1811), Color(0xFF241E15))
        ),
        PaletteOption(
            "sakura", "樱色", "Sakura", Color(0xFFCB8697),
            listOf(Color(0xFFFFF7F7), Color(0xFFFAECEE), Color(0xFFF2E0E4)),
            listOf(Color(0xFF1A1216), Color(0xFF20161B), Color(0xFF271B21))
        ),
        PaletteOption(
            "mist", "雾蓝", "Mist Blue", Color(0xFF7FA6C4),
            listOf(Color(0xFFF4F8FB), Color(0xFFEAF1F6), Color(0xFFDDE7EE)),
            listOf(Color(0xFF0F141A), Color(0xFF131920), Color(0xFF171E26))
        ),
        PaletteOption(
            "starry", "星夜", "Starry", Color(0xFF9C8FD0),
            listOf(Color(0xFFF6F5FC), Color(0xFFEEEDF8), Color(0xFFE3E2F2)),
            listOf(Color(0xFF0E0D18), Color(0xFF12101F), Color(0xFF161327))
        ),
        PaletteOption(
            "mono", "灰阶", "Monochrome", Color(0xFF4E5052),
            listOf(Color(0xFFF9F9F7), Color(0xFFF3F3F1), Color(0xFFE8E8E5)),
            listOf(Color(0xFF111214), Color(0xFF191A1C), Color(0xFF222326))
        ),
        PaletteOption(
            "world", "跟随世界", "World", Color(0xFFC7AD74),
            listOf(Color(0xFFFFFCF4), Color(0xFFF9F7EC), Color(0xFFF1EFE1)),
            listOf(Color(0xFF10101A), Color(0xFF14141F), Color(0xFF181825))
        )
    )

    fun byId(id: String?): PaletteOption =
        palettes.firstOrNull { it.id == id } ?: palettes.first()

    fun resolve(id: String?, darkMode: Boolean, worldColors: List<Color>): PaletteSpec {
        val option = byId(id)
        val colors = if (option.id == "world") worldColors else if (darkMode) option.darkColors else option.lightColors
        val primary = colors.first()
        val secondary = colors.getOrElse(1) { primary }
        val onSurface = if (darkMode) Color(0xFFF5F2EC) else Color(0xFF352C27)
        val muted = if (darkMode) Color(0xFFBEB8AF) else Color(0xFF756D66)
        val surface = if (darkMode) mix(Color(0xFF252329), option.accent, 0.10f) else mix(Color(0xFFFFFCF8), option.accent, 0.07f)
        val surfaceVariant = if (darkMode) mix(Color(0xFF333039), option.accent, 0.14f) else mix(Color(0xFFF5F0E8), option.accent, 0.12f)
        return PaletteSpec(
            id = option.id,
            name = option.name,
            accent = option.accent,
            backgroundPrimary = primary,
            backgroundSecondary = secondary,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onSurface = onSurface,
            onSurfaceMuted = muted,
            border = if (darkMode) mix(Color(0xFF6A6265), option.accent, 0.25f) else mix(Color(0xFFDDD1C7), option.accent, 0.26f),
            highlight = if (darkMode) Color(0xFFFEF8F3) else Color.White
        )
    }

    private fun mix(from: Color, to: Color, amount: Float): Color = Color(
        red = from.red + (to.red - from.red) * amount,
        green = from.green + (to.green - from.green) * amount,
        blue = from.blue + (to.blue - from.blue) * amount
    )
}

data class WallpaperOption(val id: String, val name: String, val nameEn: String)

object WallpaperCatalog {
    val options = listOf(
        WallpaperOption("world", "跟随世界", "World Reactive"),
        WallpaperOption("cream", "奶油渐变", "Cream"),
        WallpaperOption("sakura", "樱色渐变", "Sakura"),
        WallpaperOption("mist", "雾蓝渐变", "Mist Blue"),
        WallpaperOption("starry", "星夜渐变", "Starry"),
        WallpaperOption("glass", "深色玻璃", "Glass"),
        WallpaperOption("diary", "暖纸张", "Diary Paper"),
        WallpaperOption("mono", "极简", "Mono"),
        WallpaperOption("default", "清透日常", "AILUA Default"),
        WallpaperOption("soft_home", "书与茶的午后", "Soft Home"),
        WallpaperOption("midnight_glass", "深蓝微光", "Midnight Glass")
    )
    fun byId(id: String?): WallpaperOption =
        options.firstOrNull { it.id == id } ?: options.first()
}

data class IconStyleOption(val id: String, val name: String, val nameEn: String)

object IconStyleCatalog {
    val options = listOf(
        IconStyleOption("milk", "柔和渐变", "Milk"),
        IconStyleOption("glass", "玻璃配套图标", "Glass"),
        IconStyleOption("diary", "手账纸页", "Diary"),
        IconStyleOption("mono", "极简图形", "Mono"),
        IconStyleOption("identity", "经典应用图标", "Identity")
    )
    fun byId(id: String?): IconStyleOption =
        options.firstOrNull { it.id == id } ?: options.first()
}
