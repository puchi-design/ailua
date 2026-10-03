package com.example.ui.themeengine.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.ui.themeengine.IconShapeSpec
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.external.ExternalThemeRepository
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.external.iconpack.AndroidIconPackResolver

/** Mask and sampling follow the actual asset; manual/external artwork keeps its own silhouette. */
data class ResolvedIconBitmap(
    val bitmap: Bitmap,
    val isPixelArt: Boolean = false,
    val maskShape: IconShapeSpec? = null,
) {
    companion object {
        internal fun bundled(bitmap: Bitmap, resource: BundledThemeIconResource) = ResolvedIconBitmap(
            bitmap = bitmap,
            isPixelArt = resource.isPixelArt,
            maskShape = if (resource.isPixelArt) IconShapeSpec.NONE else IconShapeSpec.SQUIRCLE,
        )
    }
}

/**
 * One app-icon lookup path for Home, hotseat, previews, and the app library.
 * Explicit overrides win; missing assets fall back through official packs to AppVisualIdentity.
 */
class IconResolver private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val installedPacks = AndroidIconPackResolver(context)
    private val importedCache = IconBitmapCache()

    fun resolveBitmap(
        iconKey: String,
        selection: ThemeSelection,
        targetSizePx: Int
    ): Bitmap? = resolveIcon(iconKey, selection, targetSizePx)?.bitmap

    fun resolveIcon(
        iconKey: String,
        selection: ThemeSelection,
        targetSizePx: Int,
    ): ResolvedIconBitmap? {
        if (targetSizePx !in 1..1024) return null
        return resolveIconWithFallback(
            iconKey = iconKey,
            selection = selection,
            loadManual = { source, _, manual ->
                loadManualOverride(source, manual, targetSizePx)?.let { ResolvedIconBitmap(it) }
            },
            loadExternal = { source, key ->
                loadExternalOverride(source, key, targetSizePx)?.let { ResolvedIconBitmap(it) }
            },
            loadBundled = { resource ->
                loadBundledBitmap(resource, targetSizePx)?.let { ResolvedIconBitmap.bundled(it, resource) }
            },
        )
    }

    private fun loadManualOverride(
        source: String,
        manual: String,
        targetSizePx: Int,
    ): Bitmap? = when {
        source.startsWith("android:") ->
            installedPacks.loadBitmap(source.removePrefix("android:"), manual, targetSizePx)
        source.startsWith("theme:") -> {
            val icons = ExternalThemeRepository.get(source.removePrefix("theme:"))?.icons
            val chosen = icons?.allIcons?.firstOrNull { it.key == manual }?.asset
                ?: icons?.mappings?.get(manual)
            chosen?.let { loadAsset(it, targetSizePx) }
        }
        else -> null
    }

    private fun loadExternalOverride(source: String, iconKey: String, targetSizePx: Int): Bitmap? {
        return when {
            source.startsWith("android:") -> {
                val packageName = source.removePrefix("android:")
                installedPacks.resolveBitmap(packageName, iconKey, targetSizePx)
            }
            source.startsWith("theme:") -> {
                val id = source.removePrefix("theme:")
                val icons = ExternalThemeRepository.get(id)?.icons ?: return null
                val keys = BundledIconCatalog.lookupKeys(iconKey) + AiluaIconAliasRegistry.aliasesFor(iconKey)
                keys.firstNotNullOfOrNull { key ->
                    icons.mappings[key]?.let { loadAsset(it, targetSizePx) }
                }
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
            is ThemeAssetRef.BuiltIn -> BundledIconCatalog.resourceForAssetKey(ref.key)
                ?.let { loadBundledBitmap(it, targetSizePx) }
        }
    }

    private fun loadBundledBitmap(resource: BundledThemeIconResource, size: Int): Bitmap? {
        val name = resource.drawableName
        importedCache.get("builtin", name, size)?.let { return it }
        return runCatching {
            val resources = appContext.resources
            val id = resources.getIdentifier(name, "drawable", appContext.packageName)
            if (id == 0) return null
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inScaled = false
            }
            BitmapFactory.decodeResource(resources, id, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > size * 2 || bounds.outHeight / sample > size * 2) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inScaled = false
            }
            val decoded = BitmapFactory.decodeResource(resources, id, options) ?: return null
            val bitmap = if (decoded.width == size && decoded.height == size) decoded
            else Bitmap.createScaledBitmap(decoded, size, size, !resource.isPixelArt).also { decoded.recycle() }
            importedCache.put("builtin", name, size, bitmap)
            bitmap
        }.getOrNull()
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

/** Kept independent of Android decoding so all fallback paths can be verified with missing assets. */
internal fun <T : Any> resolveIconWithFallback(
    iconKey: String,
    selection: ThemeSelection,
    loadManual: (source: String, iconKey: String, manual: String) -> T?,
    loadExternal: (source: String, iconKey: String) -> T?,
    loadBundled: (BundledThemeIconResource) -> T?,
): T? {
    val source = selection.iconSourceOverrideId
    if (source != null) {
        val manual = BundledIconCatalog.lookupKeys(iconKey)
            .firstNotNullOfOrNull { selection.manualIconOverrides[it]?.takeIf(String::isNotBlank) }
        if (manual != null) loadManual(source, iconKey, manual)?.let { return it }
        loadExternal(source, iconKey)?.let { return it }
    }
    return BundledIconCatalog.candidates(iconKey, selection).firstNotNullOfOrNull(loadBundled)
}
