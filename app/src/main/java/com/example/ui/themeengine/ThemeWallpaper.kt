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
import com.example.ui.themeengine.external.ExternalThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Home and System UI use the same source selection and bounded Android 10 decoder. */
@Composable
fun rememberThemeWallpaperBitmap(selection: ThemeSelection = ThemeStore.selection): State<ImageBitmap?> =
    produceState<ImageBitmap?>(null, selection.wallpaperSourceId, ExternalThemeRepository.themes) {
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = selection.wallpaperSourceId?.let(ExternalThemeRepository::get)
                    ?.wallpapers?.firstOrNull()?.let(ExternalThemeRepository::assetBytes)
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

@Composable
fun ThemeWallpaper(
    modifier: Modifier = Modifier,
    runtime: AiluaThemeRuntime = LocalAiluaTheme.current
) {
    val wallpaper by rememberThemeWallpaperBitmap()
    Box(modifier.background(Brush.verticalGradient(runtime.wallpaper.colors))) {
        wallpaper?.let { bitmap ->
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
