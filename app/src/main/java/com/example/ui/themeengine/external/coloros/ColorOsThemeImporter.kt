package com.example.ui.themeengine.external.coloros

import com.example.ui.themeengine.IconSource
import com.example.ui.themeengine.external.*
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/** Imports static visual assets from a local OPPO/OPlus .theme ZIP. */
class ColorOsThemeImporter : ParsingThemeImporter() {
    override val format = ExternalThemeFormat.COLOROS_THEME

    override fun parse(source: ImportedThemeSource): ThemeImportPreview {
        val archive = SafeThemeArchive(source.bytes)
        val infoPath = archive.listEntries().firstOrNull { it.equals("themeInfo.xml", true) }
            ?: archive.listEntries().firstOrNull { it.endsWith("/themeInfo.xml", true) }
            ?: throw IllegalArgumentException("缺少 themeInfo.xml")
        val info = readInfo(requireNotNull(archive.readBytes(infoPath)))
        val id = externalThemeId("coloros", source.bytes)
        val assets = linkedMapOf<String, ByteArray>()
        val previews = mutableListOf<RankedAsset>()
        val wallpapers = mutableListOf<RankedAsset>()
        val iconCandidates = mutableListOf<IconCandidate>()
        var sequence = 0

        fun save(kind: Kind, origin: String, bytes: ByteArray, extension: String?): ThemeAssetRef.LocalFile {
            val category = when (kind) {
                Kind.PREVIEW -> "preview"
                Kind.WALLPAPER -> "wallpaper"
                Kind.ICON -> "icons"
                Kind.OTHER -> "raw"
            }
            val base = origin.substringAfterLast('!').substringAfterLast('/')
                .replace(Regex("[^A-Za-z0-9._-]"), "_").take(60).ifBlank { "asset" }
            val filename = if (extension == null) base else base.substringBeforeLast('.', base) + extension
            val digest = MessageDigest.getInstance("SHA-256").digest(origin.toByteArray())
                .take(4).joinToString("") { "%02x".format(it) }
            val path = assetPath(id, category, "${sequence++}_${digest}_$filename")
            assets[path] = bytes
            return ThemeAssetRef.LocalFile(path)
        }

        fun scan(current: SafeThemeArchive, prefix: String, inherited: Kind, depth: Int) {
            current.listEntries().forEach { entry ->
                if (depth == 0 && entry == infoPath) return@forEach
                val bytes = current.readBytes(entry) ?: return@forEach
                val origin = if (prefix.isEmpty()) entry else "$prefix!$entry"
                val kind = classify(entry, inherited)
                val imageType = imageExtension(bytes)
                if (imageType != null) {
                    val ref = save(kind, origin, bytes, imageType)
                    when (kind) {
                        Kind.PREVIEW -> previews += RankedAsset(ref, previewRank(origin))
                        Kind.WALLPAPER -> wallpapers += RankedAsset(ref, wallpaperRank(origin))
                        Kind.ICON -> iconCandidates += IconCandidate(entry, ref)
                        Kind.OTHER -> Unit
                    }
                } else {
                    // Opaque resources remain available in the installed asset set.
                    save(Kind.OTHER, origin, bytes, null)
                    if (depth < 2 && kind != Kind.OTHER && isZip(bytes)) {
                        runCatching { current.openNestedArchive(entry) }.getOrNull()?.let {
                            scan(it, origin, kind, depth + 1)
                        }
                    }
                }
            }
        }

        scan(archive, "", Kind.OTHER, 0)
        val mappings = linkedMapOf<String, ThemeAssetRef>()
        val icons = iconCandidates.sortedWith(compareBy<IconCandidate> {
            normalizeColorOsIconName(it.sourceName)?.activityName != null
        }.thenBy { it.sourceName }).map { candidate ->
            val identity = normalizeColorOsIconName(candidate.sourceName)
            if (identity != null) {
                mappings.putIfAbsent(identity.packageName, candidate.asset)
                identity.activityName?.let { mappings.putIfAbsent("${identity.packageName}/$it", candidate.asset) }
            }
            ExternalIconEntry(
                key = identity?.packageName ?: candidate.sourceName.substringAfterLast('/').substringBeforeLast('.'),
                displayName = candidate.sourceName.substringAfterLast('/'), asset = candidate.asset
            )
        }
        val fallbackName = source.filename?.substringAfterLast('/')?.substringAfterLast('\\')
            ?.removeSuffix(".theme")?.takeIf { it.isNotBlank() } ?: "ColorOS theme"
        val name = info.fields["Name"] ?: info.fields["ThemeName"] ?: info.fields["Summary"] ?: fallbackName
        val metadata = linkedMapOf("rootElement" to info.rootName)
        metadata.putAll(info.fields)
        if (info.resolutions.isNotEmpty()) metadata["resolutionInfo"] = info.resolutions.joinToString(", ")
        if (info.packages.isNotEmpty()) metadata["packageInfo"] = info.packages.joinToString(", ")
        return ThemeImportPreview(
            ExternalThemePackage(
                id = id, format = format, name = name,
                author = info.fields["Author"],
                version = info.fields["VersionName"] ?: info.fields["VersionCode"],
                description = info.fields["Description"],
                previewAssets = previews.sortedBy { it.rank }.map { it.asset },
                wallpapers = wallpapers.sortedBy { it.rank }.map { it.asset },
                icons = icons.takeIf { it.isNotEmpty() }?.let {
                    ExternalIconSet("$id-icons", "$name icons", IconSource.ThemePackage(id), mappings, it)
                },
                metadata = metadata
            ), assets
        )
    }

    private fun readInfo(bytes: ByteArray): Info {
        require(bytes.size <= 1024 * 1024) { "themeInfo.xml 过大" }
        val xml = bytes.toString(Charsets.UTF_8)
        require(!Regex("<!\\s*(DOCTYPE|ENTITY)", RegexOption.IGNORE_CASE).containsMatchIn(xml)) {
            "不支持包含 DTD 的主题元数据"
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        val root = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement
        val rootName = localName(root)
        require(rootName.equals("OppoSmartPhoneThemeInfo", true) ||
            rootName.equals("OplusSmartPhoneThemeInfo", true)) { "不支持的 ColorOS 主题元数据" }
        val fields = linkedMapOf<String, String>()
        for (field in FIELDS) {
            directChild(root, field)?.textContent?.trim()?.takeIf { it.isNotEmpty() }?.let { fields[field] = it }
        }
        val resolutions = directChild(root, "resolutionInfo")?.let { parent ->
            children(parent).filter { localName(it).equals("resolution", true) }
                .mapNotNull { it.textContent?.trim()?.takeIf(String::isNotEmpty) }
        }.orEmpty()
        val packages = directChild(root, "packageInfo")?.let { parent ->
            children(parent).filter { localName(it).equals("package", true) }.mapNotNull { node ->
                val name = node.getAttribute("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
                node.getAttribute("version").takeIf(String::isNotBlank)?.let { "$name@$it" } ?: name
            }
        }.orEmpty()
        return Info(rootName, fields, resolutions, packages)
    }

    private fun directChild(root: Element, name: String): Element? =
        children(root).firstOrNull { localName(it).equals(name, true) }

    private fun children(parent: Element): List<Element> = buildList {
        val nodes = parent.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) add(node as Element)
        }
    }

    private fun localName(element: Element) = element.localName ?: element.tagName.substringAfterLast(':')

    private fun classify(path: String, inherited: Kind): Kind {
        val normalized = path.lowercase(Locale.ROOT).replace('\\', '/')
        val first = normalized.substringBefore('/')
        return when {
            first in setOf("picture", "preview", "previews") -> Kind.PREVIEW
            first in setOf("wallpaper", "wallpapers") || normalized.contains("wallpaper") -> Kind.WALLPAPER
            first in setOf("icons", "icon", "launcher", "com.oppo.launcher", "com.android.launcher") -> Kind.ICON
            inherited != Kind.OTHER -> inherited
            else -> Kind.OTHER
        }
    }

    private fun previewRank(path: String) = when {
        path.contains("home", true) || path.contains("launcher", true) -> 0
        path.contains("lock", true) -> 2
        else -> 1
    }

    private fun wallpaperRank(path: String) = when {
        path.contains("home", true) || path.contains("default", true) || path.contains("launcher", true) -> 0
        path.contains("lock", true) -> 2
        else -> 1
    }

    private fun isZip(bytes: ByteArray) = bytes.size >= 4 && bytes[0] == 0x50.toByte() &&
        bytes[1] == 0x4b.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

    private fun imageExtension(bytes: ByteArray): String? = when {
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(PNG) -> ".png"
        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() &&
            bytes[2] == 0xff.toByte() -> ".jpg"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
            bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()) -> ".webp"
        else -> null
    }

    private data class Info(val rootName: String, val fields: Map<String, String>,
        val resolutions: List<String>, val packages: List<String>)
    private data class RankedAsset(val asset: ThemeAssetRef.LocalFile, val rank: Int)
    private data class IconCandidate(val sourceName: String, val asset: ThemeAssetRef.LocalFile)
    private enum class Kind { OTHER, PREVIEW, WALLPAPER, ICON }

    companion object {
        private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        private val FIELDS = listOf("Name", "ThemeName", "Author", "Summary", "Description", "UUID",
            "PackageName", "VersionName", "VersionCode", "EditorVersion", "LastModifyTime")
    }
}

data class NormalizedColorOsIconName(val packageName: String, val activityName: String?)

/** Normalizes package icons and ComponentInfo{package/activity} names. */
fun normalizeColorOsIconName(filename: String): NormalizedColorOsIconName? {
    val component = Regex("(?i)ComponentInfo\\{\\s*([a-z0-9_]+(?:\\.[a-z0-9_]+)+)(?:/([^}]+))?\\s*}")
        .find(filename)
    if (component != null) {
        val pkg = component.groupValues[1].lowercase(Locale.ROOT)
        val activity = component.groupValues[2].trim().takeIf(String::isNotEmpty)
            ?.let { if (it.startsWith('.')) pkg + it else it }
        return NormalizedColorOsIconName(pkg, activity)
    }
    val base = filename.replace('\\', '/').substringAfterLast('/')
    val stem = base.substringBeforeLast('.', base)
    if (!PACKAGE_OR_COMPONENT.matches(stem)) return null
    val parts = stem.split('.')
    val hasActivity = parts.size >= 4 && parts.last().firstOrNull()?.isUpperCase() == true
    val pkg = (if (hasActivity) parts.dropLast(1) else parts).joinToString(".").lowercase(Locale.ROOT)
    return NormalizedColorOsIconName(pkg, if (hasActivity) stem else null)
}

private val PACKAGE_OR_COMPONENT = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")

