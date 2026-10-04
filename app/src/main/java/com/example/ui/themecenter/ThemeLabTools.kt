package com.example.ui.themecenter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.iconShapeFor
import com.example.ui.themeengine.AiluaThemeRuntime
import com.example.ui.themeengine.ThemeCatalog
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.builtInWallpaperResource
import com.example.ui.themeengine.external.ExternalThemeRepository
import com.example.ui.themeengine.icon.BundledIconCatalog
import com.example.ui.themeengine.icon.IconResolver
import com.example.ui.themeengine.icon.ResolvedIconBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.MessageDigest

data class ThemeAssetInspection(
    val key: String,
    val source: String,
    val width: Int,
    val height: Int,
    val sha256: String,
    val byteCount: Int,
    val isBitmap: Boolean = true,
)

/** Read-only production tools: no Workspace, characters, chat, or provider credentials. */
object ThemeLabTools {
    suspend fun inspect(
        context: Context, selection: ThemeSelection, runtime: AiluaThemeRuntime,
    ): List<ThemeAssetInspection> = withContext(Dispatchers.IO) {
        val wallpaper = wallpaper(context, selection, runtime)
        buildList {
            add(inspectImage("壁纸", wallpaper.second, wallpaper.first))
            val resolver = IconResolver.get(context)
            val requestedSource = selection.iconSourceOverrideId ?: "官方 ${runtime.id}"
            for (key in BundledIconCatalog.iconKeys) {
                val resolved = resolver.resolveIcon(key, selection, 256)
                if (resolved == null) {
                    add(ThemeAssetInspection(key, "矢量 fallback；请求来源：$requestedSource",
                        0, 0, "", 0, isBitmap = false))
                    continue
                }
                // The hash describes the decoded result, including the actual fallback and mask.
                // Requested source is labelled as such because an absent external icon can fall back.
                add(inspectImage(key, "实际渲染；请求来源：$requestedSource", iconPng(resolved)))
            }
        }
    }

    suspend fun export(
        context: Context, selection: ThemeSelection, runtime: AiluaThemeRuntime, output: OutputStream,
    ) = withContext(Dispatchers.IO) {
        val wallpaper = wallpaper(context, selection, runtime).first
        val resolver = IconResolver.get(context)
        val icons = buildMap {
            for (key in BundledIconCatalog.iconKeys) {
                resolver.resolveIcon(key, selection, 256)?.let { put(key, iconPng(it)) }
            }
        }
        AiluaThemeArchiveWriter.write(
            output = output,
            id = "ailua-${runtime.id}-mix",
            name = ThemeCatalog.byId(runtime.id).name + " · 当前搭配",
            basePreset = runtime.id,
            palette = runtime.palette.id,
            wallpaper = wallpaper,
            icons = icons,
            iconStyle = ThemeResolver.resolveIconStyleId(selection),
            wallpaperStyle = wallpaperStyleForExport(selection, runtime),
        )
    }

    private fun wallpaper(
        context: Context, selection: ThemeSelection, runtime: AiluaThemeRuntime,
    ): Pair<ByteArray, String> {
        selection.wallpaperSourceId?.let(ExternalThemeRepository::get)?.let { theme ->
            theme.wallpapers.firstOrNull()?.let(ExternalThemeRepository::assetBytes)?.let {
                return it to "已导入：${theme.name}"
            }
        }
        builtInWallpaperResource(runtime.wallpaper.key)?.let { id ->
            return context.resources.openRawResource(id).use { it.readBytes() } to runtime.wallpaper.key
        }
        val bitmap = Bitmap.createBitmap(1080, 2340, Bitmap.Config.ARGB_8888)
        try {
            val colors = runtime.wallpaper.colors.map { it.toArgb() }.toIntArray()
            val paint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, 2340f, colors, null, Shader.TileMode.CLAMP)
            }
            Canvas(bitmap).drawRect(0f, 0f, 1080f, 2340f, paint)
            return png(bitmap) to "当前渐变：${runtime.wallpaper.key}"
        } finally { bitmap.recycle() }
    }

    private fun iconPng(resolved: ResolvedIconBitmap): ByteArray {
        val bitmap = resolved.bitmap
        val mask = resolved.maskShape ?: return png(bitmap)
        val outline = iconShapeFor(mask).createOutline(
            Size(bitmap.width.toFloat(), bitmap.height.toFloat()), LayoutDirection.Ltr, Density(1f),
        )
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        }
        val masked = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        try {
            Canvas(masked).apply {
                clipPath(path.asAndroidPath())
                drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
            }
            return png(masked)
        } finally { masked.recycle() } // Resolver owns and caches the original bitmap.
    }

    private fun png(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use {
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "无法编码图片" }
        it.toByteArray()
    }

    private fun inspectImage(key: String, source: String, bytes: ByteArray): ThemeAssetInspection {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return ThemeAssetInspection(key, source, bounds.outWidth, bounds.outHeight, hash, bytes.size)
    }
}
