package com.example.ui.themeengine.external

import com.example.ui.themeengine.IconSource
import java.security.MessageDigest

sealed interface ThemeAssetRef {
    data class BuiltIn(val key: String) : ThemeAssetRef
    data class InstalledAndroidResource(val packageName: String, val drawableName: String) : ThemeAssetRef
    data class LocalFile(val relativePath: String) : ThemeAssetRef
}

enum class ExternalThemeFormat { AILUA, ANDROID_ICON_PACK, MIUI_MTZ, COLOROS_THEME }

data class ExternalIconEntry(val key: String, val displayName: String?, val asset: ThemeAssetRef)
data class ExternalIconSet(
    val id: String, val name: String, val source: IconSource,
    val mappings: Map<String, ThemeAssetRef>, val allIcons: List<ExternalIconEntry>
)
data class ExternalThemePackage(
    val id: String, val format: ExternalThemeFormat, val name: String,
    val author: String? = null, val version: String? = null, val description: String? = null,
    val previewAssets: List<ThemeAssetRef> = emptyList(),
    val wallpapers: List<ThemeAssetRef> = emptyList(),
    val icons: ExternalIconSet? = null,
    val metadata: Map<String, String> = emptyMap(),
    val basePresetId: String? = null, val preferredPaletteId: String? = null
)
data class ImportedThemeSource(val filename: String?, val bytes: ByteArray)
data class ThemeImportPreview(val theme: ExternalThemePackage, val assets: Map<String, ByteArray>)
sealed interface ThemeImportResult {
    data class Success(val theme: ExternalThemePackage) : ThemeImportResult
    data class Failure(val message: String) : ThemeImportResult
}
interface ThemeImporter {
    suspend fun inspect(source: ImportedThemeSource): ThemeImportPreview
    suspend fun import(source: ImportedThemeSource, assetStore: ThemeAssetStore): ThemeImportResult
}
abstract class ParsingThemeImporter : ThemeImporter {
    abstract val format: ExternalThemeFormat
    abstract fun parse(source: ImportedThemeSource): ThemeImportPreview
    override suspend fun inspect(source: ImportedThemeSource): ThemeImportPreview = parse(source)
    override suspend fun import(source: ImportedThemeSource, assetStore: ThemeAssetStore): ThemeImportResult =
        try { ThemeImportResult.Success(assetStore.install(parse(source))) }
        catch (error: Exception) { ThemeImportResult.Failure(error.message ?: "导入失败") }
}
fun externalThemeId(prefix: String, bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return "${prefix.lowercase().replace(Regex("[^a-z0-9_-]"), "-")}-${digest.take(8).joinToString("") { "%02x".format(it) }}"
}
fun assetPath(id: String, category: String, filename: String): String {
    require(id.matches(Regex("[a-zA-Z0-9._-]+")))
    require(category in setOf("wallpaper", "icons", "preview", "raw"))
    val basename = filename.substringAfterLast('/').substringAfterLast('\\')
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
    require(basename.isNotBlank() && basename != "." && basename != "..")
    return "$id/$category/$basename"
}
fun normalizeExternalIconName(filename: String): String {
    val name = filename.substringAfterLast('/').substringBeforeLast('.')
    return name.removePrefix("ComponentInfo{").removeSuffix("}")
        .substringBefore('/').lowercase().trim()
}
