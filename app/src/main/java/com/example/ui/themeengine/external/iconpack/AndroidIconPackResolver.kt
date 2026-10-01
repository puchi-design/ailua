package com.example.ui.themeengine.external.iconpack

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.example.ui.themeengine.icon.AiluaIconAliasRegistry
import com.example.ui.themeengine.icon.IconBitmapCache

/** Installed-pack lookup and resource loading. All failures return null so callers keep built-in icons. */
class AndroidIconPackResolver(
    context: Context,
    private val bitmapCache: IconBitmapCache = IconBitmapCache()
) {
    private val appContext = context.applicationContext
    private val parser = AndroidIconPackParser(appContext)
    private val parsedPacks = HashMap<String, ParsedAndroidIconPack?>()
    private val index = HashMap<String, Map<String, String>>()

    @Synchronized
    private fun pack(packageName: String): ParsedAndroidIconPack? {
        if (!parsedPacks.containsKey(packageName)) parsedPacks[packageName] = parser.parse(packageName)
        return parsedPacks[packageName]
    }

    @Synchronized
    private fun mappingIndex(packageName: String): Map<String, String> {
        index[packageName]?.let { return it }
        val result = LinkedHashMap<String, String>()
        val parsed = pack(packageName)
        // Package-only entries win. Otherwise the first activity entry is a stable static fallback.
        parsed?.mappings.orEmpty().filter { it.activityName == null }.forEach {
            result.putIfAbsent(it.packageName, it.drawableName)
        }
        parsed?.mappings.orEmpty().forEach { result.putIfAbsent(it.packageName, it.drawableName) }
        parsed?.calendars.orEmpty().forEach {
            result.putIfAbsent(it.packageName, it.drawableName + "1")
        }
        index[packageName] = result
        return result
    }

    fun availableIcons(packageName: String): List<AndroidIconEntry> {
        val parsed = pack(packageName) ?: return emptyList()
        if (parsed.allIcons.isNotEmpty()) return parsed.allIcons
        return parsed.mappings.map { it.drawableName }.distinct()
            .map { AndroidIconEntry(it, it) }
    }

    fun mappedDrawableName(packageName: String, iconKey: String): String? {
        val mappings = mappingIndex(packageName)
        return AiluaIconAliasRegistry.aliasesFor(iconKey).firstNotNullOfOrNull(mappings::get)
    }

    fun resolveBitmap(
        packageName: String,
        iconKey: String,
        targetSizePx: Int,
        manualDrawableName: String? = null
    ): Bitmap? {
        val manual = manualDrawableName?.takeIf { it.isNotBlank() }
        if (manual != null) loadBitmap(packageName, manual, targetSizePx)?.let { return it }
        val automatic = mappedDrawableName(packageName, iconKey) ?: return null
        return loadBitmap(packageName, automatic, targetSizePx)
    }

    fun loadBitmap(packageName: String, drawableName: String, targetSizePx: Int): Bitmap? {
        if (targetSizePx !in 1..1024 || !drawableName.matches(DRAWABLE_NAME)) return null
        bitmapCache.get(packageName, drawableName, targetSizePx)?.let { return it }
        val resources = try {
            appContext.packageManager.getResourcesForApplication(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val id = resources.getIdentifier(drawableName, "drawable", packageName)
            .takeIf { it != 0 }
            ?: resources.getIdentifier(drawableName, "mipmap", packageName)
                .takeIf { it != 0 }
            ?: return null
        val drawable = try {
            resources.getDrawable(id, null)
        } catch (_: Resources.NotFoundException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }
        val bitmap = runCatching { drawable.renderBitmap(targetSizePx) }.getOrNull() ?: return null
        bitmapCache.put(packageName, drawableName, targetSizePx, bitmap)
        return bitmap
    }

    fun clearCache() {
        synchronized(this) {
            parsedPacks.clear()
            index.clear()
        }
        bitmapCache.clear()
    }

    private fun Drawable.renderBitmap(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val copy = constantState?.newDrawable()?.mutate() ?: mutate()
        copy.setBounds(0, 0, size, size)
        copy.draw(canvas)
        return bitmap
    }

    companion object {
        private val DRAWABLE_NAME = Regex("[A-Za-z0-9_]+")
    }
}
