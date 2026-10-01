package com.example.ui.themeengine.external.iconpack

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipFile

data class AndroidIconMapping(
    val packageName: String,
    val activityName: String?,
    val drawableName: String
)

data class AndroidIconEntry(
    val drawableName: String,
    val displayName: String? = null,
    val category: String? = null
)

data class ParsedAndroidIconPack(
    val mappings: List<AndroidIconMapping>,
    val calendars: List<AndroidIconMapping> = emptyList(),
    val dynamicClockDrawables: Set<String> = emptySet(),
    val allIcons: List<AndroidIconEntry> = emptyList()
)

/**
 * Reads the ADW appfilter format from an installed pack. Android Resources handle compiled XML;
 * assets are read as text. APK entry enumeration is only used to discover resource variants.
 */
class AndroidIconPackParser(private val context: Context) {
    fun parse(packageName: String): ParsedAndroidIconPack? {
        val pm = context.packageManager
        val resources = try {
            pm.getResourcesForApplication(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val names = discoverCandidateNames(packageName, resources)
        val mapping = names.asSequence()
            .mapNotNull { name -> openParser(resources, packageName, name)?.useParser(::parseAppFilter) }
            .firstOrNull { it.mappings.isNotEmpty() || it.calendars.isNotEmpty() }
            ?: return null
        val allIcons = openParser(resources, packageName, "drawable")?.useParser(::parseDrawableList)
            .orEmpty()
        return mapping.copy(allIcons = allIcons)
    }

    private fun discoverCandidateNames(packageName: String, resources: Resources): List<String> {
        val found = linkedSetOf("appfilter")
        val assetVariants = linkedSetOf<String>()
        val resourceVariants = linkedSetOf<String>()
        val applicationInfo = runCatching {
            context.packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull()
        applicationInfo?.sourceDir?.let { source ->
            runCatching {
                ZipFile(source).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val path = entries.nextElement().name
                        val directory = path.substringBeforeLast('/', "")
                        if (directory != "assets" && !directory.startsWith("res/xml") &&
                            !directory.startsWith("res/raw")) continue
                        val name = path.substringAfterLast('/').removeSuffix(".xml")
                        if (path.endsWith(".xml") &&
                            (name.startsWith("appfilter_") || name == "app_filter" || name == "icon_config")
                        ) {
                            if (directory == "assets") assetVariants += name else resourceVariants += name
                        }
                    }
                }
            }
        }
        found += assetVariants.sorted()
        found += resourceVariants.sorted()
        // Resource identifiers can be present even when the APK filename was resource-shrunk.
        listOf("app_filter", "icon_config").forEach { name ->
            if (resources.getIdentifier(name, "xml", packageName) != 0 ||
                resources.getIdentifier(name, "raw", packageName) != 0
            ) found += name
        }
        found += "app_filter"
        found += "icon_config"
        return found.toList()
    }

    private fun openParser(resources: Resources, packageName: String, name: String): XmlPullParser? {
        // assets/appfilter.xml takes priority over compiled res/xml and res/raw.
        runCatching { resources.assets.open("$name.xml") }.getOrNull()?.let { stream ->
            return streamToParser(stream)
        }
        for (type in listOf("xml", "raw")) {
            val id = resources.getIdentifier(name, type, packageName)
            if (id == 0) continue
            if (type == "xml") {
                runCatching { resources.getXml(id) }.getOrNull()?.let { return it }
            } else {
                runCatching { resources.openRawResource(id) }.getOrNull()?.let { stream ->
                    return streamToParser(stream)
                }
            }
        }
        return null
    }

    private fun streamToParser(stream: InputStream): XmlPullParser? {
        return try {
            XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(stream, "UTF-8")
                // The parser does not own the stream; close it when END_DOCUMENT is reached below.
            }.also { openStreams[it] = stream }
        } catch (_: Exception) {
            stream.close()
            null
        }
    }

    private inline fun <T> XmlPullParser.useParser(block: (XmlPullParser) -> T): T? {
        return try {
            block(this)
        } catch (_: Exception) {
            null
        } finally {
            (this as? AutoCloseable)?.close()
            openStreams.remove(this)?.close()
        }
    }

    private fun parseAppFilter(parser: XmlPullParser): ParsedAndroidIconPack {
        val mappings = ArrayList<AndroidIconMapping>()
        val calendars = ArrayList<AndroidIconMapping>()
        val clocks = LinkedHashSet<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "item", "calendar" -> {
                    val component = parser.getAttributeValue(null, "component") ?: continue
                    val drawable = parser.getAttributeValue(
                        null, if (parser.name == "calendar") "prefix" else "drawable"
                    )?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                    val target = parseComponent(component, drawable) ?: continue
                    if (parser.name == "calendar") calendars += target else mappings += target
                }
                "dynamic-clock" -> parser.getAttributeValue(null, "drawable")
                    ?.takeIf { it.isNotBlank() }?.let(clocks::add)
            }
        }
        return ParsedAndroidIconPack(mappings, calendars, clocks)
    }

    private fun parseDrawableList(parser: XmlPullParser): List<AndroidIconEntry> {
        val icons = LinkedHashMap<String, AndroidIconEntry>()
        var category: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "category" -> category = parser.getAttributeValue(null, "title")
                "item" -> {
                    val name = parser.getAttributeValue(null, "drawable")
                        ?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                    icons.putIfAbsent(name, AndroidIconEntry(name,
                        parser.getAttributeValue(null, "name") ?: name, category))
                }
            }
        }
        return icons.values.toList()
    }

    companion object {
        private val openStreams = java.util.Collections.synchronizedMap(
            java.util.WeakHashMap<XmlPullParser, InputStream>()
        )

        /** Accepts ComponentInfo{package/.Activity}, package/activity and package-only aliases. */
        fun parseComponent(value: String, drawableName: String): AndroidIconMapping? {
            val cleaned = value.trim().removePrefix("ComponentInfo{").removeSuffix("}")
            val packageName = cleaned.substringBefore('/').trim()
            if (packageName.isEmpty() || !packageName.contains('.') ||
                packageName.any { it.isWhitespace() }) return null
            val rawActivity = cleaned.substringAfter('/', "").trim()
            val activity = when {
                rawActivity.isEmpty() -> null
                rawActivity.startsWith('.') -> packageName + rawActivity
                rawActivity.contains('.') -> rawActivity
                else -> packageName + "." + rawActivity
            }
            return AndroidIconMapping(packageName, activity, drawableName)
        }

        /** Pure XML entry point for small synthetic parser tests. */
        fun parseXml(input: InputStream): ParsedAndroidIconPack {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(input, "UTF-8")
            val mappings = ArrayList<AndroidIconMapping>()
            val calendars = ArrayList<AndroidIconMapping>()
            val clocks = LinkedHashSet<String>()
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                val tag = parser.name
                if (tag == "dynamic-clock") {
                    parser.getAttributeValue(null, "drawable")
                        ?.takeIf { it.isNotBlank() }?.let(clocks::add)
                    continue
                }
                if (tag != "item" && tag != "calendar") continue
                val component = parser.getAttributeValue(null, "component") ?: continue
                val drawable = parser.getAttributeValue(
                    null, if (tag == "calendar") "prefix" else "drawable"
                )?.takeIf { it.isNotBlank() } ?: continue
                val mapping = parseComponent(component, drawable) ?: continue
                if (tag == "calendar") calendars += mapping else mappings += mapping
            }
            return ParsedAndroidIconPack(mappings, calendars, clocks)
        }
    }
}
