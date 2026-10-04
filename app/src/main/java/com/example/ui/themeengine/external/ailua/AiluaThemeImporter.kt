package com.example.ui.themeengine.external.ailua

import com.example.ui.themeengine.IconSource
import com.example.ui.themeengine.external.*
import org.json.JSONObject

/** Imports schema-v1 AILUA packages, including the earlier `preset` object form. */
class AiluaThemeImporter : ParsingThemeImporter() {
    override val format = ExternalThemeFormat.AILUA

    override fun parse(source: ImportedThemeSource): ThemeImportPreview =
        parse(source, SafeThemeArchive(source.bytes))

    fun parse(source: ImportedThemeSource, archive: SafeThemeArchive): ThemeImportPreview {
        val manifest = JSONObject(String(requireNotNull(archive.readBytes("manifest.json")) { "缺少 manifest.json" }, Charsets.UTF_8))
        require(manifest.optInt("schema", -1) == 1) { "不支持的 AILUA 主题版本" }
        val settings = JSONObject(String(requireNotNull(archive.readBytes("theme.json")) { "缺少 theme.json" }, Charsets.UTF_8))
        val oldPreset = settings.optJSONObject("preset")
        val requestedId = manifest.optString("id", "imported")
        val id = externalThemeId(requestedId, source.bytes)
        val assets = mutableMapOf<String, ByteArray>()
        val previews = mutableListOf<ThemeAssetRef>()
        val wallpapers = mutableListOf<ThemeAssetRef.LocalFile>()
        val iconEntries = mutableListOf<ExternalIconEntry>()
        val mappings = mutableMapOf<String, ThemeAssetRef>()
        val iconPrefix = settings.optJSONObject("icons")?.optString("path", "icons/") ?: "icons/"
        for (entry in archive.listEntries()) {
            val category = when {
                entry.startsWith("wallpaper/") -> "wallpaper"
                entry.startsWith("preview/") -> "preview"
                entry.startsWith(iconPrefix) -> "icons"
                else -> null
            } ?: continue
            if (!entry.matches(Regex(".*\\.(png|jpe?g|webp)", RegexOption.IGNORE_CASE))) continue
            val bytes = archive.readBytes(entry) ?: continue
            val path = assetPath(id, category, entry.substringAfterLast('/'))
            assets[path] = bytes
            val ref = ThemeAssetRef.LocalFile(path)
            when (category) {
                "wallpaper" -> wallpapers += ref
                "preview" -> previews += ref
                "icons" -> {
                    val key = normalizeExternalIconName(entry)
                    mappings[key] = ref
                    iconEntries += ExternalIconEntry(key, null, ref)
                }
            }
        }
        val requestedWallpaper = settings.optString("wallpaper").ifBlank { oldPreset?.optString("wallpaper").orEmpty() }
        if (requestedWallpaper.isNotBlank()) {
            val exact = wallpapers.firstOrNull { it.relativePath.endsWith(requestedWallpaper.substringAfterLast('/')) }
            if (exact != null) { wallpapers.remove(exact); wallpapers.add(0, exact) }
        }
        val icons = iconEntries.takeIf { it.isNotEmpty() }?.let {
            ExternalIconSet("$id/icons", "Icons", IconSource.ThemePackage(id), mappings, it)
        }
        val basePreset = settings.optString("basePreset").ifBlank { oldPreset?.optString("skin").orEmpty() }
        val palette = settings.optString("palette").ifBlank { oldPreset?.optString("palette").orEmpty() }
        // Optional schema-v1 UI metadata. The apply UI validates it against its own catalog.
        val uiMetadata = buildMap {
            for (key in listOf("iconStyle", "wallpaperStyle")) {
                settings.optString(key).takeIf { it.isNotBlank() }?.let { put(key, it) }
            }
        }
        val theme = ExternalThemePackage(
            id = id, format = format, name = manifest.optString("name", "AILUA Theme"),
            author = manifest.optString("author").takeIf { it.isNotBlank() },
            version = manifest.optString("version").takeIf { it.isNotBlank() },
            previewAssets = previews, wallpapers = wallpapers, icons = icons,
            basePresetId = basePreset.takeIf { it.isNotBlank() },
            preferredPaletteId = palette.takeIf { it.isNotBlank() },
            metadata = uiMetadata,
        )
        return ThemeImportPreview(theme, assets)
    }
}
