package com.example.ui.themeengine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppVisualIdentity
import com.example.ui.components.getAppIdentity

data class ThemeSelection(
    val themePresetId: String = ThemeCatalog.DEFAULT_ID,
    val paletteOverrideId: String? = null,
    val wallpaperOverrideId: String? = null,
    val iconStyleOverrideId: String? = null,
    val wallpaperSourceId: String? = null,
    val iconSourceOverrideId: String? = null,
    val manualIconOverrides: Map<String, String> = emptyMap()
)

data class AiluaThemePreset(
    val id: String,
    val name: String,
    val nameEn: String,
    val paletteId: String,
    val wallpaperId: String,
    val iconStyleId: String,
    val widgetStyleId: String,
    val dockStyleId: String,
    val typographyId: String,
    val statusBarStyleId: String,
    val motionStyleId: String
)

data class PaletteSpec(
    val id: String,
    val name: String,
    val accent: Color,
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val border: Color,
    val highlight: Color
)

data class PalettePair(val light: PaletteSpec, val dark: PaletteSpec)

/** App surfaces share the launcher palette; screens do not own a second theme. */
data class SurfaceVisualSpec(
    val screen: Color,
    val raised: Color,
    val inset: Color,
    val overlay: Color,
    val divider: Color,
)

data class ShapeVisualSpec(
    val small: Float = 10f,
    val medium: Float = 16f,
    val large: Float = 22f,
    val pill: Float = 999f,
)

data class LayoutSpec(
    val screenHorizontalPadding: Float = 24f,
    val sectionGap: Float = 28f,
    val itemGap: Float = 12f,
)

data class UiTextSpec(
    val display: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val body: TextStyle,
    val secondary: TextStyle,
    val caption: TextStyle,
)

data class WallpaperSpec(val key: String, val colors: List<Color>)

data class BorderSpec(val color: Color, val widthDp: Float)
data class ShadowSpec(val elevationDp: Float)

enum class IconShapeSpec { SQUIRCLE, CIRCLE, ROUNDED_RECT, SOFT_SQUARE, NONE }
enum class IconContainerStyle { GRADIENT, SOLID, GLASS, PAPER, OUTLINE, GLYPH_ONLY }
enum class IdentityColorMode { CONTAINER, GLYPH, ACCENT, NONE }
enum class GlyphTintMode { WHITE, IDENTITY, ON_SURFACE }

data class IconVisualSpec(
    val shape: IconShapeSpec,
    val containerStyle: IconContainerStyle,
    val containerScale: Float,
    val glyphScale: Float,
    val identityColorMode: IdentityColorMode,
    val glyphTintMode: GlyphTintMode,
    val shadow: ShadowSpec,
    val border: BorderSpec,
    val labelColor: Color
)

enum class WidgetShape { ROUNDED_RECT, SOFT_SQUARE, RECTANGLE }
enum class WidgetBackgroundStyle { CARD, GLASS, PAPER, FLAT, TRANSPARENT }
data class WidgetVisualSpec(
    val shape: WidgetShape,
    val backgroundStyle: WidgetBackgroundStyle,
    val backgroundColor: Color,
    val foregroundColor: Color,
    val cornerRadiusDp: Float,
    val surfaceAlpha: Float,
    val border: BorderSpec,
    val shadow: ShadowSpec,
    val contentPaddingDp: Float,
    val blurRadiusDp: Float = 24f,
    val highlightAlpha: Float = 0.16f,
    val fallbackAlpha: Float = surfaceAlpha,
)

enum class DockContainerMode { CAPSULE, ISLAND, PAPER_STRIP, NONE }
enum class DockBackgroundStyle { SURFACE, GLASS, PAPER, TRANSPARENT }
data class DockVisualSpec(
    val containerMode: DockContainerMode,
    val backgroundStyle: DockBackgroundStyle,
    val backgroundColor: Color,
    val cornerRadiusDp: Float,
    val surfaceAlpha: Float,
    val tintAlpha: Float,
    val border: BorderSpec,
    val shadow: ShadowSpec,
    val horizontalPaddingDp: Float,
    val verticalPaddingDp: Float,
    val iconScale: Float,
    val blurRadiusDp: Float = 28f,
    val highlightAlpha: Float = 0.16f,
    val fallbackAlpha: Float = surfaceAlpha,
)

enum class TypographyFamily { SYSTEM_SANS, SOFT_SANS, SERIF, MONO }
data class TypographySpec(
    val family: TypographyFamily,
    val labelWeight: FontWeight,
    val labelSizeScale: Float,
    val titleWeight: FontWeight,
    val letterSpacingScale: Float
)

enum class StatusForegroundMode { AUTO, LIGHT, DARK }
data class StatusBarVisualSpec(
    val foregroundMode: StatusForegroundMode,
    val foregroundColor: Color,
    val backgroundAlpha: Float,
    val minimal: Boolean
)

/** Shared system surfaces keep each skin's shape, edge and depth treatment. */
data class SystemCardStyle(
    val backgroundColor: Color,
    val foregroundColor: Color,
    val cornerRadiusDp: Float,
    val surfaceAlpha: Float,
    val border: BorderSpec,
    val shadow: ShadowSpec
)

data class LockscreenVisualSpec(
    val clockScale: Float,
    val foregroundColor: Color,
    val scrimAlpha: Float,
    val notificationStyle: SystemCardStyle,
    val shortcutStyle: SystemCardStyle
)

data class ShadeVisualSpec(
    val backgroundAlpha: Float,
    val cardCornerRadiusDp: Float,
    val cardAlpha: Float,
    val spacingDp: Float,
    val cardStyle: SystemCardStyle
)

data class ControlCenterVisualSpec(
    val tileCornerRadiusDp: Float,
    val activeAlpha: Float,
    val inactiveAlpha: Float,
    val panelAlpha: Float,
    val tileStyle: SystemCardStyle
)

data class LiveActivityVisualSpec(
    val compactCornerRadiusDp: Float,
    val expandedCornerRadiusDp: Float,
    val backgroundColor: Color,
    val foregroundColor: Color,
    val border: BorderSpec,
    val shadow: ShadowSpec
)

enum class EditMotion { WIGGLE, FLOAT, SCALE, NONE }
data class MotionSpec(
    val pressScale: Float,
    val dragScale: Float,
    val springDamping: Float,
    val springStiffness: Float,
    val editMotion: EditMotion
)

data class AiluaThemeRuntime(
    val id: String,
    val palette: PaletteSpec,
    val surfaces: SurfaceVisualSpec,
    val shapes: ShapeVisualSpec,
    val layout: LayoutSpec,
    val text: UiTextSpec,
    val wallpaper: WallpaperSpec,
    val icons: IconVisualSpec,
    val widgets: WidgetVisualSpec,
    val dock: DockVisualSpec,
    val typography: TypographySpec,
    val statusBar: StatusBarVisualSpec,
    val lockscreen: LockscreenVisualSpec,
    val shade: ShadeVisualSpec,
    val controlCenter: ControlCenterVisualSpec,
    val liveActivity: LiveActivityVisualSpec,
    val motion: MotionSpec
)

val LocalAiluaTheme = staticCompositionLocalOf<AiluaThemeRuntime> { ThemeCatalog.defaultRuntime }

@Composable
fun AiluaThemeProvider(runtime: AiluaThemeRuntime, content: @Composable () -> Unit) {
    val palette = runtime.palette
    val onAccent = if (palette.accent.luminance() > 0.20f) Color(0xFF191919) else Color.White
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = palette.accent,
            onPrimary = onAccent,
            primaryContainer = runtime.surfaces.inset,
            onPrimaryContainer = palette.onSurface,
            secondary = palette.accent,
            onSecondary = onAccent,
            secondaryContainer = runtime.surfaces.inset,
            onSecondaryContainer = palette.onSurface,
            tertiary = palette.accent,
            onTertiary = onAccent,
            tertiaryContainer = runtime.surfaces.inset,
            onTertiaryContainer = palette.onSurface,
            background = runtime.surfaces.screen,
            onBackground = palette.onSurface,
            surface = runtime.surfaces.raised,
            surfaceDim = runtime.surfaces.screen,
            surfaceBright = runtime.surfaces.raised,
            surfaceContainerLowest = runtime.surfaces.screen,
            surfaceContainerLow = runtime.surfaces.raised,
            surfaceContainer = runtime.surfaces.raised,
            surfaceContainerHigh = runtime.surfaces.overlay,
            surfaceContainerHighest = runtime.surfaces.inset,
            surfaceVariant = runtime.surfaces.inset,
            onSurface = palette.onSurface,
            onSurfaceVariant = palette.onSurfaceMuted,
            outline = palette.border,
            outlineVariant = runtime.surfaces.divider,
            surfaceTint = Color.Transparent,
        ),
        typography = Typography(
            displayLarge = runtime.text.display, displayMedium = runtime.text.display, displaySmall = runtime.text.display,
            headlineLarge = runtime.text.display, headlineMedium = runtime.text.title, headlineSmall = runtime.text.title,
            titleLarge = runtime.text.title, titleMedium = runtime.text.section, titleSmall = runtime.text.body,
            bodyLarge = runtime.text.body, bodyMedium = runtime.text.body, bodySmall = runtime.text.secondary,
            labelLarge = runtime.text.secondary, labelMedium = runtime.text.caption, labelSmall = runtime.text.caption,
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(runtime.shapes.small.dp),
            small = RoundedCornerShape(runtime.shapes.small.dp),
            medium = RoundedCornerShape(runtime.shapes.medium.dp),
            large = RoundedCornerShape(runtime.shapes.large.dp),
            extraLarge = RoundedCornerShape(runtime.shapes.large.dp),
        ),
    ) {
        CompositionLocalProvider(LocalAiluaTheme provides runtime, content = content)
    }
}

/** All external formats can be normalized to this model before theme resolution. */
data class AiluaThemePackage(
    val id: String,
    val preset: AiluaThemePreset
)

interface ThemePackageProvider {
    fun listThemes(): List<AiluaThemePackage>
    fun resolve(id: String): AiluaThemePackage?
}

object BuiltInThemeProvider : ThemePackageProvider {
    override fun listThemes(): List<AiluaThemePackage> =
        ThemeCatalog.presets.map { AiluaThemePackage(it.id, it) }

    override fun resolve(id: String): AiluaThemePackage? =
        ThemeCatalog.presets.firstOrNull { it.id == id }?.let { AiluaThemePackage(it.id, it) }
}

/** A drawable source descriptor. Importers are intentionally deferred. */
sealed interface IconSource {
    data object BuiltIn : IconSource
    data class AndroidPack(val packageName: String) : IconSource
    data class ThemePackage(val packageId: String) : IconSource
}


/**
 * Turns an app key into its identity before theme treatment. External packs can implement
 * the same contract without changing AppIconItem or the resolver.
 */
interface IconIdentityProvider {
    fun resolve(iconKey: String): AppVisualIdentity?
}

object BuiltInIconSource : IconIdentityProvider {
    override fun resolve(iconKey: String): AppVisualIdentity = getAppIdentity(iconKey)
}

class AndroidIconPackSource(val packageName: String) : IconIdentityProvider {
    override fun resolve(iconKey: String): AppVisualIdentity? = null
}

class ThemePackageIconSource(val packageId: String) : IconIdentityProvider {
    override fun resolve(iconKey: String): AppVisualIdentity? = null
}
