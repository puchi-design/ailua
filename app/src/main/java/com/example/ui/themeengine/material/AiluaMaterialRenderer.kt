package com.example.ui.themeengine.material

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.themeengine.BorderSpec
import com.example.ui.themeengine.DockBackgroundStyle
import com.example.ui.themeengine.DockVisualSpec
import com.example.ui.themeengine.ShadowSpec
import com.example.ui.themeengine.WidgetBackgroundStyle
import com.example.ui.themeengine.WidgetShape
import com.example.ui.themeengine.WidgetVisualSpec
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** The only place in AILUA that owns the backdrop backend's state. */
private val LocalBackdrop = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun AiluaBackdropProvider(content: @Composable () -> Unit) {
    val backdrop = remember { HazeState() }
    CompositionLocalProvider(LocalBackdrop provides backdrop, content = content)
}

/** Apply only to the wallpaper layer; foreground text and icons never enter the blur source. */
@Composable
fun Modifier.ailuaBackdropSource(): Modifier {
    val backdrop = LocalBackdrop.current
    return if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hazeSource(backdrop)
    } else this
}

/** A small renderer contract shared by widgets, the dock and their previews. */
data class AiluaSurfaceMaterial(
    val color: Color,
    val surfaceAlpha: Float,
    val glass: Boolean,
    val transparent: Boolean,
    val cornerRadiusDp: Float,
    val border: BorderSpec,
    val shadow: ShadowSpec,
    val blurRadiusDp: Float,
    val highlightAlpha: Float,
    val fallbackAlpha: Float,
)

fun WidgetVisualSpec.surfaceMaterial(): AiluaSurfaceMaterial = AiluaSurfaceMaterial(
    color = backgroundColor,
    surfaceAlpha = surfaceAlpha,
    glass = backgroundStyle == WidgetBackgroundStyle.GLASS,
    transparent = backgroundStyle == WidgetBackgroundStyle.TRANSPARENT,
    cornerRadiusDp = if (shape == WidgetShape.RECTANGLE) 0f else cornerRadiusDp,
    border = border,
    shadow = shadow,
    blurRadiusDp = blurRadiusDp,
    highlightAlpha = highlightAlpha,
    fallbackAlpha = fallbackAlpha,
)

fun DockVisualSpec.surfaceMaterial(): AiluaSurfaceMaterial = AiluaSurfaceMaterial(
    color = backgroundColor,
    surfaceAlpha = surfaceAlpha,
    glass = backgroundStyle == DockBackgroundStyle.GLASS,
    transparent = backgroundStyle == DockBackgroundStyle.TRANSPARENT,
    cornerRadiusDp = cornerRadiusDp,
    border = border,
    shadow = shadow,
    blurRadiusDp = blurRadiusDp,
    highlightAlpha = highlightAlpha,
    fallbackAlpha = fallbackAlpha,
)

/** A malformed imported value cannot turn the surface into a white slab or a NaN brush. */
internal fun materialAlpha(value: Float, fallback: Float = 0f): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else fallback.coerceIn(0f, 1f)

internal fun AiluaSurfaceMaterial.tintAlpha(backdropAvailable: Boolean): Float = when {
    transparent -> 0f
    glass && !backdropAvailable -> materialAlpha(fallbackAlpha, materialAlpha(surfaceAlpha))
    else -> materialAlpha(surfaceAlpha)
}

/**
 * Alpha belongs to the material behind the content. Never use Modifier.alpha on this surface:
 * applying it to the containing node would also fade every icon, badge and line of text.
 */
@Composable
fun Modifier.ailuaMaterialSurface(material: AiluaSurfaceMaterial): Modifier {
    val backdrop = LocalBackdrop.current
    val radius = material.cornerRadiusDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val shape = RoundedCornerShape(radius.dp)
    val blurRadius = material.blurRadiusDp.takeIf { it.isFinite() }?.coerceIn(0f, 64f) ?: 0f
    val blurAvailable = material.glass && !material.transparent && backdrop != null &&
        blurRadius > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val tint = material.color.copy(alpha = material.tintAlpha(blurAvailable))
    val elevation = material.shadow.elevationDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    var result = this
    // Native elevation on a translucent RenderNode can darken its unpainted padding on API 29.
    // That produces a thick grey ring and rectangular holes around child content. Glass keeps
    // only its authored tint and fine edge; opaque cards retain their normal elevation.
    if (!material.transparent && !material.glass && elevation > 0f) {
        result = result.shadow(elevation.dp, shape, clip = false)
    }
    result = result.clip(shape)

    result = if (blurAvailable) {
        result.hazeEffect(
            state = requireNotNull(backdrop),
            style = HazeStyle(
                backgroundColor = material.color.copy(alpha = 1f),
                tint = HazeTint(tint),
                blurRadius = blurRadius.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(material.color.copy(alpha = material.tintAlpha(false))),
            ),
        ) { blurEnabled = true }
    } else if (material.glass && !material.transparent) result else result.background(tint)

    if (material.glass && !material.transparent) {
        val highlight = materialAlpha(material.highlightAlpha)
        // The authored edge tint also colours the sheen; a zero-width edge draws no ring.
        // Legacy glass with a transparent edge retains its neutral white highlight.
        val sheenColor = if (material.border.color.alpha > 0f) {
            lerp(Color.White, material.border.color.copy(alpha = 1f), 0.4f)
        } else Color.White
        result = result.drawWithCache {
            val sheen = Brush.verticalGradient(
                0f to sheenColor.copy(alpha = highlight),
                0.45f to sheenColor.copy(alpha = highlight * 0.12f),
                1f to Color.Transparent,
            )
            // Draw the entire backing in one outer pass, before any child padding or content.
            // No translucent background or highlight is attached to a child's inner rectangle.
            onDrawBehind {
                if (!blurAvailable) drawRect(tint)
                if (highlight > 0f) drawRect(sheen)
            }
        }
    }
    val borderWidth = material.border.widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    return if (borderWidth > 0f) result.border(borderWidth.dp, material.border.color, shape) else result
}
