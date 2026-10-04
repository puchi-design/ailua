package com.example.ui.themecenter

import android.content.Context
import com.example.ui.themeengine.ThemeCatalog
import com.example.ui.themeengine.AiluaThemeRuntime
import com.example.ui.themeengine.IconStyleCatalog
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.WallpaperCatalog
import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ExternalThemePackage

/** Consumer descriptions only; appearance is resolved through the existing catalogs. */
internal data class ThemeProduct(val id: String, val title: String, val description: String, val materials: String)

internal val officialThemeProducts = listOf(
    ThemeProduct("default", "AILUA Default", "清透的日常，留一点自己的颜色。", "现代 · 清晰 · 轻玻璃"),
    ThemeProduct("soft_home", "Soft Home", "花园里的柔软日常", "油画 · 粉花 · 轻玻璃"),
    ThemeProduct("rainy_study", "Rainy Study", "湖边的花，慢慢盛开。", "油画 · 湖蓝 · 清透玻璃"),
    ThemeProduct("sakura_diary", "Sakura Diary", "把喜欢的日子贴进手账。", "纸张 · 樱花 · 手绘贴纸"),
    ThemeProduct("y2k_love", "Y2K Love PC", "在粉紫色旧电脑里，收到你的消息。", "像素 · 粉紫 · 桌面面板"),
    ThemeProduct("midnight_glass", "Midnight Glass", "深蓝夜色里的微光", "夜色 · 深蓝 · 玻璃"),
)

/** Full official theme preserves external and per-app icon choices. */
internal fun ThemeSelection.withOfficialTheme(id: String): ThemeSelection = copy(
    themePresetId = id, paletteOverrideId = null, wallpaperOverrideId = null,
    wallpaperSourceId = null, iconStyleOverrideId = null,
)

internal fun officialPreviewSelection(id: String): ThemeSelection = ThemeSelection(id)

/** Preserve the wallpaper's foreground treatment while the archive stores its actual pixels. */
internal fun wallpaperStyleForExport(selection: ThemeSelection, runtime: AiluaThemeRuntime): String? {
    val explicit = selection.wallpaperOverrideId?.takeIf { id -> WallpaperCatalog.options.any { it.id == id } }
    if (explicit != null) return explicit
    val key = runtime.wallpaper.key
    val resolved = when (key.substringBefore('/')) {
        "builtin", "gradient" -> key.substringAfter('/').substringBefore('/')
        else -> key.substringBefore('/')
    }
    return resolved.takeIf { id -> WallpaperCatalog.options.any { it.id == id } }
}

/** Only AILUA archives describe an AILUA shell; OEM packages remain asset adapters. */
internal fun ThemeSelection.withImportedTheme(theme: ExternalThemePackage): ThemeSelection {
    val basePreset = theme.basePresetId?.takeIf { id ->
        theme.format == ExternalThemeFormat.AILUA && ThemeCatalog.presets.any { it.id == id }
    }
    val iconStyle = theme.metadata["iconStyle"]?.takeIf { id ->
        theme.format == ExternalThemeFormat.AILUA && IconStyleCatalog.options.any { it.id == id }
    }
    val wallpaperStyle = theme.metadata["wallpaperStyle"]?.takeIf { id ->
        theme.format == ExternalThemeFormat.AILUA && WallpaperCatalog.options.any { it.id == id }
    }
    return copy(
        themePresetId = basePreset ?: themePresetId,
        paletteOverrideId = theme.preferredPaletteId ?: if (basePreset != null) null else paletteOverrideId,
        wallpaperOverrideId = wallpaperStyle ?: if (basePreset != null) null else wallpaperOverrideId,
        iconStyleOverrideId = iconStyle ?: if (basePreset != null) null else iconStyleOverrideId,
        wallpaperSourceId = theme.id.takeIf { theme.wallpapers.isNotEmpty() }
            ?: if (basePreset != null) null else wallpaperSourceId,
        iconSourceOverrideId = "theme:${theme.id}".takeIf { theme.icons != null }
            ?: if (basePreset != null) null else iconSourceOverrideId,
        manualIconOverrides = if (theme.icons != null || basePreset != null) emptyMap() else manualIconOverrides,
    )
}

/** UI-only recent choices, separate from ThemeStore and Workspace persistence. */
internal object ThemeRecentHistory {
    private const val PREFS = "ailua_theme_center_ui"
    private const val KEY = "recent_theme_ids"
    fun read(context: Context): List<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY, "").orEmpty().split(',').filter { id ->
            id.isNotBlank() && ThemeCatalog.presets.any { it.id == id }
        }.distinct().take(8)
    fun record(context: Context, id: String) {
        if (ThemeCatalog.presets.none { it.id == id }) return
        val ids = (listOf(id) + read(context)).distinct().take(8)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, ids.joinToString(",")).apply()
    }
}
