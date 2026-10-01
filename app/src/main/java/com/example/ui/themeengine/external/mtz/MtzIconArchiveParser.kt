package com.example.ui.themeengine.external.mtz

import com.example.ui.themeengine.IconSource
import com.example.ui.themeengine.external.ExternalIconEntry
import com.example.ui.themeengine.external.ExternalIconSet
import com.example.ui.themeengine.external.SafeThemeArchive
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.external.assetPath
import com.example.ui.themeengine.external.normalizeExternalIconName

data class ParsedMtzIcons(
    val iconSet: ExternalIconSet?,
    val assets: Map<String, ByteArray>
)

/** Reads package-named static icons from the inner `icons` ZIP and directory layouts. */
object MtzIconArchiveParser {
    fun parse(outer: SafeThemeArchive, themeId: String): ParsedMtzIcons {
        val candidates = ArrayList<IconCandidate>()
        val innerPath = outer.listEntries().firstOrNull {
            it.equals("icons", ignoreCase = true) || it.equals("icons.zip", ignoreCase = true)
        }
        innerPath?.let { path ->
            // A broken OEM module should not discard a usable wallpaper or preview.
            runCatching { outer.openNestedArchive(path) }.getOrNull()?.let { inner ->
                candidates += scan(inner, prefix = "")
            }
        }
        candidates += scan(outer, prefix = "icons/")

        val bestByKey = linkedMapOf<String, IconCandidate>()
        for (candidate in candidates.sortedWith(compareByDescending<IconCandidate> { it.score }.thenBy { it.path })) {
            bestByKey.putIfAbsent(candidate.key, candidate)
        }
        if (bestByKey.isEmpty()) return ParsedMtzIcons(null, emptyMap())

        val assets = linkedMapOf<String, ByteArray>()
        val mappings = linkedMapOf<String, ThemeAssetRef>()
        val entries = ArrayList<ExternalIconEntry>()
        bestByKey.values.forEachIndexed { index, candidate ->
            val ref = ThemeAssetRef.LocalFile(
                assetPath(themeId, "icons", "icon_${index.toString().padStart(4, '0')}.${candidate.extension}")
            )
            assets[ref.relativePath] = candidate.bytes
            mappings.putIfAbsent(candidate.key, ref)
            candidate.packageAlias?.let { mappings.putIfAbsent(it, ref) }
            entries += ExternalIconEntry(candidate.key, candidate.displayName, ref)
        }
        return ParsedMtzIcons(
            ExternalIconSet("$themeId-icons", "MIUI icons", IconSource.ThemePackage(themeId), mappings, entries),
            assets
        )
    }

    private fun scan(archive: SafeThemeArchive, prefix: String): List<IconCandidate> {
        val result = ArrayList<IconCandidate>()
        for (path in archive.listEntries()) {
            if (prefix.isNotEmpty() && !path.startsWith(prefix, ignoreCase = true)) continue
            val localPath = if (prefix.isEmpty()) path else path.substring(prefix.length)
            if (!isIconLocation(localPath)) continue
            val bytes = archive.readBytes(path) ?: continue
            val extension = MtzImageSupport.extension(bytes) ?: continue
            val basename = localPath.substringAfterLast('/')
            val parent = localPath.substringBeforeLast('/', "")
            val layered = basename.equals("1.png", true) || basename.equals("1.webp", true) ||
                basename.equals("1.jpg", true) || basename.equals("1.jpeg", true)
            val knownSuffix = basename.substringAfterLast('.', "").lowercase() in setOf("png", "webp", "jpg", "jpeg")
            val rawName = if (layered) parent.substringAfterLast('/') else if (knownSuffix) basename.substringBeforeLast('.') else basename
            val key = if (rawName.startsWith("ComponentInfo{", ignoreCase = true)) {
                rawName.substringAfter('{').substringBefore('/').lowercase()
            } else {
                normalizeExternalIconName("$rawName.$extension")
            }
            if (!isUsableKey(key)) continue
            val packageAlias = rawName.substringBeforeLast('.', "").takeIf {
                rawName.substringAfterLast('.').firstOrNull()?.isUpperCase() == true &&
                    it.contains('.') && isUsableKey(it.lowercase())
            }?.lowercase()
            result += IconCandidate(
                path = path, bytes = bytes, extension = extension, key = key,
                packageAlias = packageAlias, displayName = rawName,
                score = locationScore(localPath) + if (layered) -20 else 0
            )
        }
        return result
    }

    private fun isIconLocation(path: String): Boolean {
        val directory = path.substringBeforeLast('/', "")
        if (directory.isEmpty()) return true
        val segments = directory.split('/')
        if (segments.firstOrNull().equals("res", true)) {
            return segments.getOrNull(1)?.let {
                it.startsWith("drawable", true) || it.startsWith("mipmap", true)
            } == true
        }
        return segments.firstOrNull()?.let {
            it.startsWith("drawable", true) || it.startsWith("mipmap", true)
        } == true
    }

    private fun locationScore(path: String): Int = when {
        path.contains("drawable-xxhdpi", true) -> 80
        path.contains("drawable-xxxhdpi", true) -> 75
        path.contains("drawable-xhdpi", true) -> 70
        path.contains("mipmap-xxhdpi", true) -> 65
        path.contains("drawable", true) -> 60
        path.contains("mipmap", true) -> 50
        else -> 40
    }

    private fun isUsableKey(key: String): Boolean = key.isNotBlank() && key.length <= 180 &&
        key.matches(Regex("[a-z0-9][a-z0-9._-]*")) &&
        key !in setOf("0", "1", "icon", "background", "mask", "transform_config")

    private data class IconCandidate(
        val path: String,
        val bytes: ByteArray,
        val extension: String,
        val key: String,
        val packageAlias: String?,
        val displayName: String,
        val score: Int
    )
}



