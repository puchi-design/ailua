package com.example.ui.themeengine.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.external.ExternalThemeRepository
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.external.iconpack.AndroidIconPackResolver

/**
 * One app-icon lookup path for Home, hotseat, previews, and the app library.
 * Missing packages or damaged theme files resolve to null and keep the built-in AppVisualIdentity.
 */
class IconResolver private constructor(context: Context) {
    private val installedPacks = AndroidIconPackResolver(context)
    private val importedCache = IconBitmapCache()

    fun resolveBitmap(
        iconKey: String,
        selection: ThemeSelection,
        targetSizePx: Int
    ): Bitmap? {
        if (targetSizePx !in 1..1024) return null
        val source = selection.iconSourceOverrideId ?: return null
        val manual = selection.manualIconOverrides[iconKey]
        return when {
            source.startsWith("android:") -> {
                val packageName = source.removePrefix("android:")
                installedPacks.resolveBitmap(packageName, iconKey, targetSizePx, manual)
            }
            source.startsWith("theme:") -> {
                val id = source.removePrefix("theme:")
                val icons = ExternalThemeRepository.get(id)?.icons ?: return null
                val automatic = icons.mappings[iconKey]
                    ?: AiluaIconAliasRegistry.aliasesFor(iconKey)
                        .firstNotNullOfOrNull { alias -> icons.mappings[alias] }
                val chosen = manual?.let { selected ->
                    icons.allIcons.firstOrNull { it.key == selected }?.asset
                        ?: icons.mappings[selected]
                }
                chosen?.let { loadAsset(it, targetSizePx) }
                    ?: automatic?.let { loadAsset(it, targetSizePx) }
            }
            else -> null
        }
    }

    fun loadAsset(ref: ThemeAssetRef, targetSizePx: Int): Bitmap? {
        if (targetSizePx !in 1..1024) return null
        return when (ref) {
        is ThemeAssetRef.InstalledAndroidResource ->
            installedPacks.loadBitmap(ref.packageName, ref.drawableName, targetSizePx)
        is ThemeAssetRef.LocalFile -> {
            importedCache.get("theme", ref.relativePath, targetSizePx)?.let { return it }
            val bytes = ExternalThemeRepository.assetBytes(ref) ?: return null
            val decoded = decodeSized(bytes, targetSizePx) ?: return null
            importedCache.put("theme", ref.relativePath, targetSizePx, decoded)
            decoded
        }
        is ThemeAssetRef.BuiltIn -> null
        }
    }

    private fun decodeSized(bytes: ByteArray, size: Int): Bitmap? {
        return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > size * 2 ||
            bounds.outHeight / sample > size * 2
        ) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        if (decoded.width == size && decoded.height == size) decoded
        else Bitmap.createScaledBitmap(decoded, size, size, true).also { decoded.recycle() }
        }.getOrNull()
    }

    companion object {
        @Volatile private var instance: IconResolver? = null

        fun get(context: Context): IconResolver =
            instance ?: synchronized(this) {
                instance ?: IconResolver(context.applicationContext).also { instance = it }
            }
    }
}
