package com.example.ui.themeengine.external

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.ThemeStore

/** In-memory registry backed by each theme's normalized manifest. */
object ExternalThemeRepository {
    private var store: ThemeAssetStore? = null
    var themes by mutableStateOf<List<ExternalThemePackage>>(emptyList())
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (store != null) return
        store = ThemeAssetStore(context)
        themes = store!!.loadAll()
    }

    fun get(id: String): ExternalThemePackage? = themes.firstOrNull { it.id == id }
    fun assetBytes(ref: ThemeAssetRef): ByteArray? = store?.read(ref)

    @Synchronized
    fun install(preview: ThemeImportPreview): ExternalThemePackage {
        val installed = requireNotNull(store) { "Theme repository not initialized" }.install(preview)
        themes = themes.filterNot { it.id == installed.id } + installed
        return installed
    }

    @Synchronized
    fun delete(id: String): Boolean {
        val current = ThemeStore.selection
        val wallpaperUsed = current.wallpaperSourceId == id
        val iconsUsed = current.iconSourceOverrideId == "theme:$id"
        if (wallpaperUsed || iconsUsed) ThemeStore.update(current.copy(
            wallpaperSourceId = if (wallpaperUsed) null else current.wallpaperSourceId,
            iconSourceOverrideId = if (iconsUsed) null else current.iconSourceOverrideId,
            manualIconOverrides = if (iconsUsed) emptyMap() else current.manualIconOverrides
        ))
        val deleted = store?.delete(id) ?: false
        if (deleted) themes = themes.filterNot { it.id == id }
        return deleted
    }

    fun apply(id: String, preserveShell: Boolean = true) {
        val theme = get(id) ?: return
        val current = ThemeStore.selection
        ThemeStore.update(current.copy(
            themePresetId = if (preserveShell) current.themePresetId else "milk",
            paletteOverrideId = theme.preferredPaletteId ?: current.paletteOverrideId,
            wallpaperSourceId = id.takeIf { theme.wallpapers.isNotEmpty() } ?: current.wallpaperSourceId,
            iconSourceOverrideId = "theme:$id".takeIf { theme.icons != null } ?: current.iconSourceOverrideId,
            manualIconOverrides = emptyMap()
        ))
    }
}
