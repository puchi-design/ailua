package com.example.ui.themecenter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.AiluaThemeRuntime
import com.example.ui.themeengine.external.ExternalThemePackage
import com.example.ui.themeengine.external.ExternalThemeRepository
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.icon.AiluaIconAliasRegistry
import com.example.ui.themeengine.icon.IconResolver
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composes a mini AILUA desktop from the current shell plus actual assets in the chosen package.
 * Draft assets come from the inspected file; installed assets come from ThemeAssetStore.
 */
@Composable
fun ImportedThemePreview(
    theme: ExternalThemePackage,
    runtime: AiluaThemeRuntime,
    draftAssets: Map<String, ByteArray>? = null,
    modifier: Modifier = Modifier
) {
    val labels = runtime.icons.labelColor
    val colors = runtime.wallpaper.colors.ifEmpty {
        listOf(runtime.palette.backgroundPrimary, runtime.palette.backgroundSecondary)
    }
    val apps = listOf("chat" to "消息", "gallery" to "相册",
        "moments" to "动态", "living" to "生活")
    val wallpaper = theme.wallpapers.firstOrNull()
    Box(modifier.clip(RoundedCornerShape(runtime.shapes.medium.dp))
        .background(Brush.verticalGradient(colors))) {
        if (wallpaper != null) {
            ExternalAssetImage(wallpaper, draftAssets, 640,
                Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("9:41", color = labels, style = runtime.text.caption, fontWeight = FontWeight.Bold)
                Text("●  ▰", color = labels, style = runtime.text.caption)
            }
            Spacer(Modifier.height(8.dp))
            Text("今天也和你一起", color = labels, style = runtime.text.caption,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                apps.forEachIndexed { index, (key, name) ->
                    PreviewExternalApp(theme, runtime, draftAssets, key, name, index, 30)
                }
            }
            Spacer(Modifier.weight(1f))
            val widget = runtime.widgets
            val widgetShape = RoundedCornerShape(widget.cornerRadiusDp.dp.coerceAtLeast(0.dp))
            Column(Modifier.fillMaxWidth().height(44.dp).clip(widgetShape)
                .background(widget.backgroundColor.copy(alpha = widget.surfaceAlpha))
                .border(widget.border.widthDp.dp, widget.border.color, widgetShape)
                .padding(horizontal = 9.dp, vertical = 4.dp)) {
                Text("10月 · AILUA", color = widget.foregroundColor,
                    style = runtime.text.caption, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("把喜欢的日子收进这里 ✦", color = widget.foregroundColor, style = runtime.text.caption, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            val dock = runtime.dock
            val dockShape = RoundedCornerShape(dock.cornerRadiusDp.dp.coerceAtLeast(0.dp))
            Row(Modifier.fillMaxWidth().height(33.dp).clip(dockShape)
                .background(dock.backgroundColor.copy(alpha = dock.surfaceAlpha))
                .border(dock.border.widthDp.dp, dock.border.color, dockShape),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                apps.forEachIndexed { index, (key, _) ->
                    PreviewExternalIcon(theme, runtime, draftAssets, key, index, 24)
                }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun PreviewExternalApp(
    theme: ExternalThemePackage, runtime: AiluaThemeRuntime,
    draftAssets: Map<String, ByteArray>?, key: String, name: String,
    index: Int, sizeDp: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PreviewExternalIcon(theme, runtime, draftAssets, key, index, sizeDp)
        Spacer(Modifier.height(2.dp))
        Text(name, color = runtime.icons.labelColor, style = runtime.text.caption, maxLines = 1)
    }
}

@Composable
private fun PreviewExternalIcon(
    theme: ExternalThemePackage, runtime: AiluaThemeRuntime,
    draftAssets: Map<String, ByteArray>?, key: String, index: Int, sizeDp: Int
) {
    val icons = theme.icons
    val chosen = icons?.mappings?.get(key)
        ?: AiluaIconAliasRegistry.aliasesFor(key)
            .firstNotNullOfOrNull { alias -> icons?.mappings?.get(alias) }
        ?: icons?.allIcons?.getOrNull(index)?.asset
        ?: icons?.mappings?.values?.elementAtOrNull(index)
    val size = sizeDp.dp
    if (chosen == null) {
        ThemePreviewIcon(runtime, key, size)
    } else {
        Box(Modifier.size(size).clip(RoundedCornerShape((sizeDp * 0.22f).dp))) {
            ExternalAssetImage(chosen, draftAssets, sizeDp * 4, Modifier.fillMaxSize(),
                ContentScale.Crop) {
                ThemePreviewIcon(runtime, key, size)
            }
        }
    }
}

@Composable
internal fun ExternalAssetImage(
    ref: ThemeAssetRef,
    draftAssets: Map<String, ByteArray>?,
    targetPx: Int,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, ref, draftAssets, targetPx) {
        value = withContext(Dispatchers.IO) {
            if (draftAssets == null && targetPx <= 256) {
                IconResolver.get(context).loadAsset(ref, targetPx)
            } else {
                val bytes = when {
                    ref is ThemeAssetRef.LocalFile && draftAssets != null ->
                        draftAssets[ref.relativePath]
                    else -> ExternalThemeRepository.assetBytes(ref)
                }
                bytes?.let { decodePreviewBitmap(it, targetPx) }
            }
        }
    }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), null, modifier = modifier, contentScale = scale)
    } else {
        fallback()
    }
}

private fun decodePreviewBitmap(bytes: ByteArray, targetPx: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > targetPx * 2 ||
        bounds.outHeight / sample > targetPx * 2
    ) sample *= 2
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()
