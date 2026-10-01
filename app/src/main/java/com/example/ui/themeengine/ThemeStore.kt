package com.example.ui.themeengine

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.components.HomeThemeStore

/**
 * The only preference reader for the virtual phone theme. Consumers observe [selection]
 * and obtain their drawing tokens from ThemeResolver and LocalAiluaTheme.
 */
object ThemeStore {
    private const val PREFS_NAME = "ailua_settings"
    private const val KEY_PRESET = "theme_engine_preset_id"
    private const val KEY_PALETTE = "theme_engine_palette_override_id"
    private const val KEY_WALLPAPER = "theme_engine_wallpaper_override_id"
    private const val KEY_ICON = "theme_engine_icon_style_override_id"
    private const val KEY_MIGRATED = "theme_engine_migrated"

    private var preferences: SharedPreferences? = null

    var selection by mutableStateOf(ThemeSelection())
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences = prefs
        val initial = readSelection(prefs, HomeThemeStore.selectedId)
        selection = initial
        if (!prefs.getBoolean(KEY_MIGRATED, false) || !prefs.contains(KEY_PRESET)) {
            write(prefs, initial)
        }
    }

    @Synchronized
    fun update(newSelection: ThemeSelection) {
        val safe = newSelection.normalized()
        selection = safe
        preferences?.let { write(it, safe) }
    }

    internal fun readSelection(prefs: SharedPreferences, legacyId: String? = null): ThemeSelection {
        if (prefs.contains(KEY_PRESET)) {
            return ThemeSelection(
                themePresetId = prefs.getString(KEY_PRESET, ThemeCatalog.DEFAULT_ID).orEmpty(),
                paletteOverrideId = prefs.getString(KEY_PALETTE, null),
                wallpaperOverrideId = prefs.getString(KEY_WALLPAPER, null),
                iconStyleOverrideId = prefs.getString(KEY_ICON, null)
            ).normalized()
        }
        val oldId = sequenceOf("home_theme_id", "home_theme", "selected_home_theme")
            .mapNotNull { key -> if (prefs.contains(key)) prefs.getString(key, null) else null }
            .firstOrNull()
            ?: legacyId
        return migrateLegacy(oldId)
    }

    /** Converts the former one dimensional picker selection to a Milk preset plus palette. */
    fun migrateLegacy(oldId: String?): ThemeSelection {
        val palette = when (oldId) {
            "follow", "world" -> "world"
            "cream", "sakura", "mist", "starry" -> oldId
            else -> null
        }
        return ThemeSelection(themePresetId = ThemeCatalog.DEFAULT_ID, paletteOverrideId = palette)
    }

    private fun ThemeSelection.normalized(): ThemeSelection = copy(
        themePresetId = ThemeCatalog.byId(themePresetId).id,
        paletteOverrideId = paletteOverrideId?.takeIf { id -> PaletteCatalog.palettes.any { it.id == id } },
        wallpaperOverrideId = wallpaperOverrideId?.takeIf { id -> WallpaperCatalog.options.any { it.id == id } },
        iconStyleOverrideId = iconStyleOverrideId?.takeIf { id -> IconStyleCatalog.options.any { it.id == id } }
    )

    internal fun write(prefs: SharedPreferences, value: ThemeSelection) {
        prefs.edit()
            .putString(KEY_PRESET, value.themePresetId)
            .putString(KEY_PALETTE, value.paletteOverrideId)
            .putString(KEY_WALLPAPER, value.wallpaperOverrideId)
            .putString(KEY_ICON, value.iconStyleOverrideId)
            .putBoolean(KEY_MIGRATED, true)
            .apply()
    }
}
