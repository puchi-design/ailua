package com.example.ui.themeengine

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.ui.themeengine.external.ExternalThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Home and System UI use the same source selection and bounded Android 10 decoder. */
@Composable
fun rememberThemeWallpaperBitmap(
    selection: ThemeSelection = ThemeStore.selection,
    runtime: AiluaThemeRuntime = LocalAiluaTheme.current,
): State<ImageBitmap?> {
    val resources = LocalContext.current.resources
    return produceState<ImageBitmap?>(null, selection.wallpaperSourceId, runtime.wallpaper.key, ExternalThemeRepository.themes) {
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching {
                val importedBytes = selection.wallpaperSourceId?.let(ExternalThemeRepository::get)
                    ?.wallpapers?.firstOrNull()?.let(ExternalThemeRepository::assetBytes)
                val bytes = importedBytes ?: builtInWallpaperResource(runtime.wallpaper.key)
                    ?.let { resource -> resources.openRawResource(resource).use { it.readBytes() } }
                    ?: return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
}

/** Asset lookup is part of the existing wallpaper renderer, not a second theme store. */
internal fun builtInWallpaperResource(key: String): Int? = when (key) {
    "builtin/default" -> R.drawable.wallpaper_default
    "builtin/soft_home" -> R.drawable.wallpaper_soft_home
    "builtin/midnight_glass" -> R.drawable.wallpaper_midnight_glass
    else -> null
}

@Composable
fun ThemeWallpaper(
    modifier: Modifier = Modifier,
    runtime: AiluaThemeRuntime = LocalAiluaTheme.current,
    selection: ThemeSelection = ThemeStore.selection,
) {
    val wallpaper by rememberThemeWallpaperBitmap(selection, runtime)
    Box(modifier.background(Brush.verticalGradient(runtime.wallpaper.colors))) {
        wallpaper?.let { bitmap ->
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
