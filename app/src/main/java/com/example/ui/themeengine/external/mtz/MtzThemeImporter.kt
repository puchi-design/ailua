package com.example.ui.themeengine.external.mtz

import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ExternalThemePackage
import com.example.ui.themeengine.external.ImportedThemeSource
import com.example.ui.themeengine.external.ParsingThemeImporter
import com.example.ui.themeengine.external.SafeThemeArchive
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.external.ThemeImportPreview
import com.example.ui.themeengine.external.assetPath
import com.example.ui.themeengine.external.externalThemeId

/** Projects local MIUI / HyperOS MTZ visual resources into the shared AILUA package model. */
class MtzThemeImporter : ParsingThemeImporter() {
    override val format: ExternalThemeFormat = ExternalThemeFormat.MIUI_MTZ

    override fun parse(source: ImportedThemeSource): ThemeImportPreview =
        parse(source, SafeThemeArchive(source.bytes))

    fun parse(source: ImportedThemeSource, archive: SafeThemeArchive): ThemeImportPreview {
        val paths = archive.listEntries()
        val descriptionPath = paths.firstOrNull { it.equals("description.xml", ignoreCase = true) }
        val recognized = descriptionPath != null || paths.any {
            val lower = it.lowercase()
            lower == "icons" || lower == "icons.zip" || lower.startsWith("icons/") ||
                lower.startsWith("wallpaper/") || lower.startsWith("preview/") ||
                lower == "lockscreen" || lower.startsWith("lockscreen/")
        }
        require(recognized) { "不是可识别的 MTZ 主题" }

        val description = descriptionPath?.let { path ->
            archive.readBytes(path)?.let { runCatching { MtzDescriptionParser.parse(it) }.getOrNull() }
        } ?: MtzDescription()
        val id = externalThemeId("mtz", source.bytes)
        val assets = linkedMapOf<String, ByteArray>()
        val wallpapers = collectImages(archive, id, "wallpaper", paths.filter {
            it.startsWith("wallpaper/", ignoreCase = true)
        }, ::wallpaperRank, assets)
        val previews = collectImages(archive, id, "preview", paths.filter {
            it.startsWith("preview/", ignoreCase = true)
        }, ::previewRank, assets)
        val icons = MtzIconArchiveParser.parse(archive, id)
        assets.putAll(icons.assets)
        require(wallpapers.isNotEmpty() || previews.isNotEmpty() || icons.iconSet != null) {
            "MTZ 未包含可使用的图片资源"
        }

        val sourceName = source.filename?.substringAfterLast('/')?.substringAfterLast('\\')
            ?.substringBeforeLast('.', missingDelimiterValue = "")
            ?.trim()?.takeIf(String::isNotEmpty)
        val title = description.title?.takeIf(String::isNotBlank) ?: sourceName ?: "MIUI theme"
        val metadata = description.metadata.toMutableMap()
        if (description.designer != null) metadata["designer"] = description.designer
        wallpapers.firstOrNull { it.originalPath.contains("lock", true) }?.let {
            metadata["lockWallpaper"] = it.ref.relativePath
        }

        return ThemeImportPreview(
            theme = ExternalThemePackage(
                id = id,
                format = format,
                name = title,
                author = description.author ?: description.designer,
                version = description.version,
                description = description.description,
                previewAssets = previews.map { it.ref },
                wallpapers = wallpapers.map { it.ref },
                icons = icons.iconSet,
                metadata = metadata,
                basePresetId = "milk"
            ),
            assets = assets
        )
    }

    private fun collectImages(
        archive: SafeThemeArchive,
        id: String,
        category: String,
        paths: List<String>,
        rank: (String) -> Int,
        output: MutableMap<String, ByteArray>
    ): List<ImportedImage> {
        val images = paths.mapNotNull { path ->
            val bytes = archive.readBytes(path) ?: return@mapNotNull null
            val extension = MtzImageSupport.extension(bytes) ?: return@mapNotNull null
            path to (bytes to extension)
        }.sortedWith(compareBy<Pair<String, Pair<ByteArray, String>>> { rank(it.first) }.thenBy { it.first })
        return images.mapIndexed { index, (path, image) ->
            val label = when {
                category == "wallpaper" && path.contains("lock", true) -> "lock_${index.toString().padStart(3, '0')}"
                category == "wallpaper" && index == 0 -> "home"
                category == "preview" && index == 0 -> "launcher"
                else -> "asset_${index.toString().padStart(3, '0')}"
            }
            val ref = ThemeAssetRef.LocalFile(assetPath(id, category, "$label.${image.second}"))
            output[ref.relativePath] = image.first
            ImportedImage(path, ref)
        }
    }

    private fun wallpaperRank(path: String): Int {
        val name = path.substringAfterLast('/').lowercase()
        return when {
            name.startsWith("default_wallpaper.") -> 0
            name.startsWith("home") || name.startsWith("launcher") -> 1
            name.contains("lock") -> 100
            else -> 10
        }
    }

    private fun previewRank(path: String): Int {
        val name = path.substringAfterLast('/').lowercase()
        return when {
            name.contains("launcher") || name.contains("home") -> 0
            name.contains("icons") -> 1
            name.contains("lock") -> 2
            name.contains("status") -> 3
            else -> 10
        }
    }

    private data class ImportedImage(val originalPath: String, val ref: ThemeAssetRef.LocalFile)
}



