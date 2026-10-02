package com.example.ui.themeengine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.AiluaTypography
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.components.wallpaperPalette

/** Keeps the existing day and weather gradient algorithm as the world wallpaper source. */
object WorldReactiveWallpaperResolver {
    fun resolve(dayPhase: DayPhase, weather: WeatherState, darkMode: Boolean): WallpaperSpec {
        val result = wallpaperPalette(dayPhase, weather, darkMode)
        return WallpaperSpec("world/" + result.key, result.colors)
    }
}

object ThemeResolver {
    fun resolve(
        selection: ThemeSelection,
        darkMode: Boolean,
        dayPhase: DayPhase,
        weather: WeatherState
    ): AiluaThemeRuntime {
        val preset = ThemeCatalog.byId(selection.themePresetId)
        val world = WorldReactiveWallpaperResolver.resolve(dayPhase, weather, darkMode)
        val paletteId = selection.paletteOverrideId
            ?.takeIf { option -> PaletteCatalog.palettes.any { it.id == option } }
            ?: preset.paletteId
        val paletteBase = PaletteCatalog.resolve(paletteId, darkMode, world.colors)
        val palette = if (preset.id == "glass") {
            paletteBase.copy(
                // Glass uses light foregrounds in both appearance modes. Its system
                // scrims must therefore stay dark even with a light/world palette.
                backgroundPrimary = Color(0xFF172735),
                backgroundSecondary = Color(0xFF233B50),
                onSurface = Color(0xFFF7FAFF),
                onSurfaceMuted = Color(0xFFD5E1EE),
                surface = Color(0xFF26394C),
                surfaceVariant = Color(0xFF39536A),
                border = Color(0xFFBFD1E0),
                highlight = Color.White
            )
        } else paletteBase
        val wallpaperId = selection.wallpaperOverrideId
            ?.takeIf { option -> WallpaperCatalog.options.any { it.id == option } }
            ?: if (paletteId == "world" && selection.paletteOverrideId != null) "world" else preset.wallpaperId
        val wallpaper = resolveWallpaper(wallpaperId, selection.wallpaperOverrideId == null, palette, world, darkMode)
        val iconStyleId = selection.iconStyleOverrideId
            ?.takeIf { option -> IconStyleCatalog.options.any { it.id == option } }
            ?: preset.iconStyleId

        return AiluaThemeRuntime(
            id = preset.id,
            palette = palette,
            surfaces = SurfaceVisualSpec(
                screen = palette.backgroundPrimary,
                raised = palette.surface,
                inset = palette.surfaceVariant,
                overlay = palette.surface,
                divider = palette.border.copy(alpha = 0.45f),
            ),
            shapes = ShapeVisualSpec(),
            layout = LayoutSpec(),
            text = uiTextSpec(preset.typographyId),
            wallpaper = wallpaper,
            icons = iconSpec(iconStyleId, palette),
            widgets = widgetSpec(preset.widgetStyleId, palette),
            dock = dockSpec(preset.dockStyleId, palette),
            typography = typographySpec(preset.typographyId),
            statusBar = statusSpec(preset.statusBarStyleId, palette, darkMode),
            lockscreen = lockscreenSpec(preset.id, palette),
            shade = shadeSpec(preset.id, palette),
            controlCenter = controlCenterSpec(preset.id, palette),
            liveActivity = liveActivitySpec(preset.id, palette),
            motion = motionSpec(preset.motionStyleId)
        )
    }

    private fun uiTextSpec(id: String): UiTextSpec {
        val headingFamily = when (id) {
            "diary" -> FontFamily.Serif
            "mono" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
        return UiTextSpec(
            display = AiluaTypography.headlineLarge.copy(fontFamily = headingFamily),
            title = AiluaTypography.titleLarge.copy(fontFamily = headingFamily),
            section = AiluaTypography.titleMedium.copy(fontFamily = headingFamily),
            body = AiluaTypography.bodyLarge,
            secondary = AiluaTypography.bodySmall,
            caption = AiluaTypography.labelSmall,
        )
    }

    private fun resolveWallpaper(
        id: String,
        usePaletteTint: Boolean,
        palette: PaletteSpec,
        world: WallpaperSpec,
        darkMode: Boolean
    ): WallpaperSpec {
        if (id == "world") return world
        val option = PaletteCatalog.palettes.firstOrNull { it.id == id }
        if (option != null) {
            val colors = if (usePaletteTint && palette.id != "world") {
                val chosen = PaletteCatalog.byId(palette.id)
                if (darkMode) chosen.darkColors else chosen.lightColors
            } else {
                if (darkMode) option.darkColors else option.lightColors
            }
            return WallpaperSpec("gradient/$id/${palette.id}/$darkMode", colors)
        }
        return when (id) {
            "glass" -> {
                val tint = if (darkMode) 0.13f else 0.18f
                val bases = if (darkMode) {
                    listOf(Color(0xFF101B2A), Color(0xFF1D2B3E), Color(0xFF2C3A50))
                } else {
                    listOf(Color(0xFF56718C), Color(0xFF819BB2), Color(0xFFBACBD7))
                }
                WallpaperSpec("glass/${palette.id}/$darkMode", bases.map { mix(it, palette.accent, tint) })
            }
            "diary" -> {
                val bases = if (darkMode) {
                    listOf(Color(0xFF211C18), Color(0xFF29231D), Color(0xFF302820))
                } else {
                    listOf(Color(0xFFF9F3E7), Color(0xFFF5EDDC), Color(0xFFEEE2CF))
                }
                WallpaperSpec("diary/${palette.id}/$darkMode", bases.map { mix(it, palette.accent, 0.06f) })
            }
            "mono" -> {
                val bases = if (darkMode) {
                    listOf(Color(0xFF121315), Color(0xFF17181A), Color(0xFF1B1C1E))
                } else {
                    listOf(Color(0xFFF8F8F6), Color(0xFFF5F5F2), Color(0xFFF1F1EE))
                }
                WallpaperSpec("mono/$darkMode", bases)
            }
            else -> WallpaperSpec("fallback/${palette.id}/$darkMode", listOf(
                palette.backgroundPrimary, palette.backgroundSecondary, palette.backgroundSecondary
            ))
        }
    }

    private fun iconSpec(id: String, p: PaletteSpec): IconVisualSpec = when (id) {
        "glass" -> IconVisualSpec(
            IconShapeSpec.CIRCLE, IconContainerStyle.GLASS, 0.95f, 0.48f,
            IdentityColorMode.GLYPH, GlyphTintMode.IDENTITY,
            ShadowSpec(8f), BorderSpec(p.highlight.copy(alpha = 0.7f), 1f), p.onSurface
        )
        "diary" -> IconVisualSpec(
            IconShapeSpec.ROUNDED_RECT, IconContainerStyle.PAPER, 0.94f, 0.44f,
            IdentityColorMode.ACCENT, GlyphTintMode.ON_SURFACE,
            ShadowSpec(3f), BorderSpec(p.border, 1f), p.onSurface
        )
        "mono" -> IconVisualSpec(
            IconShapeSpec.NONE, IconContainerStyle.GLYPH_ONLY, 0.9f, 0.60f,
            IdentityColorMode.NONE, GlyphTintMode.ON_SURFACE,
            ShadowSpec(0f), BorderSpec(Color.Transparent, 0f), p.onSurface
        )
        else -> IconVisualSpec(
            IconShapeSpec.SQUIRCLE, IconContainerStyle.GRADIENT, 1f, 0.48f,
            IdentityColorMode.CONTAINER, GlyphTintMode.WHITE,
            ShadowSpec(7f), BorderSpec(p.highlight.copy(alpha = 0.6f), 0.7f), p.onSurface
        )
    }

    private fun widgetSpec(id: String, p: PaletteSpec): WidgetVisualSpec = when (id) {
        "glass" -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.GLASS, p.surface,
            p.onSurface, 24f, 0.32f, BorderSpec(p.highlight.copy(alpha = 0.7f), 1f),
            ShadowSpec(12f), 14f
        )
        "diary" -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.PAPER, p.surface,
            p.onSurface, 10f, 0.95f, BorderSpec(p.border, 1f), ShadowSpec(3f), 14f
        )
        "mono" -> WidgetVisualSpec(
            WidgetShape.RECTANGLE, WidgetBackgroundStyle.FLAT, p.surface,
            p.onSurface, 2f, 0.18f, BorderSpec(p.onSurfaceMuted.copy(alpha = 0.4f), 0.7f),
            ShadowSpec(0f), 12f
        )
        else -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.CARD, p.surface,
            p.onSurface, 20f, 0.88f, BorderSpec(p.border.copy(alpha = 0.55f), 0.7f),
            ShadowSpec(7f), 12f
        )
    }

    private fun dockSpec(id: String, p: PaletteSpec): DockVisualSpec = when (id) {
        "glass" -> DockVisualSpec(
            DockContainerMode.ISLAND, DockBackgroundStyle.GLASS, p.surface,
            30f, 0.28f, 0.08f, BorderSpec(p.highlight.copy(alpha = 0.72f), 1f),
            ShadowSpec(16f), 18f, 11f, 0.95f
        )
        "diary" -> DockVisualSpec(
            DockContainerMode.PAPER_STRIP, DockBackgroundStyle.PAPER, p.surface,
            6f, 0.95f, 0.04f, BorderSpec(p.border, 1f),
            ShadowSpec(3f), 16f, 10f, 0.94f
        )
        "mono" -> DockVisualSpec(
            DockContainerMode.NONE, DockBackgroundStyle.TRANSPARENT, Color.Transparent,
            0f, 0f, 0f, BorderSpec(Color.Transparent, 0f),
            ShadowSpec(0f), 8f, 8f, 0.92f
        )
        else -> DockVisualSpec(
            DockContainerMode.CAPSULE, DockBackgroundStyle.SURFACE, p.surface,
            26f, 0.82f, 0.08f, BorderSpec(p.highlight.copy(alpha = 0.8f), 0.7f),
            ShadowSpec(10f), 16f, 10f, 1f
        )
    }

    private fun typographySpec(id: String): TypographySpec = when (id) {
        "glass" -> TypographySpec(TypographyFamily.SYSTEM_SANS, FontWeight.Light, 0.96f, FontWeight.Normal, 1.03f)
        "diary" -> TypographySpec(TypographyFamily.SERIF, FontWeight.Medium, 1.03f, FontWeight.SemiBold, 1f)
        "mono" -> TypographySpec(TypographyFamily.MONO, FontWeight.Normal, 0.93f, FontWeight.Medium, 1.08f)
        else -> TypographySpec(TypographyFamily.SOFT_SANS, FontWeight.Medium, 1f, FontWeight.SemiBold, 1f)
    }

    private fun statusSpec(id: String, p: PaletteSpec, darkMode: Boolean): StatusBarVisualSpec = when (id) {
        "glass" -> StatusBarVisualSpec(StatusForegroundMode.LIGHT, Color.White, 0f, false)
        "diary" -> StatusBarVisualSpec(
            if (darkMode) StatusForegroundMode.LIGHT else StatusForegroundMode.DARK,
            p.onSurface, 0.18f, false
        )
        "mono" -> StatusBarVisualSpec(StatusForegroundMode.AUTO, p.onSurface, 0f, true)
        else -> StatusBarVisualSpec(StatusForegroundMode.AUTO, p.onSurface, 0.1f, false)
    }

    private fun systemCardSpec(id: String, p: PaletteSpec): SystemCardStyle = when (id) {
        "glass" -> SystemCardStyle(p.surface, p.onSurface, 26f, 0.86f,
            BorderSpec(p.highlight.copy(alpha = 0.65f), 1f), ShadowSpec(12f))
        "diary" -> SystemCardStyle(p.surface, p.onSurface, 9f, 0.97f,
            BorderSpec(p.border, 1f), ShadowSpec(3f))
        "mono" -> SystemCardStyle(p.surface, p.onSurface, 2f, 1f,
            BorderSpec(p.onSurfaceMuted.copy(alpha = 0.5f), 0.7f), ShadowSpec(0f))
        else -> SystemCardStyle(p.surface, p.onSurface, 24f, 0.94f,
            BorderSpec(p.highlight.copy(alpha = 0.8f), 0.7f), ShadowSpec(6f))
    }

    private fun lockscreenSpec(id: String, p: PaletteSpec): LockscreenVisualSpec {
        val card = systemCardSpec(id, p)
        return LockscreenVisualSpec(
            clockScale = when (id) { "glass" -> 1.08f; "diary" -> 0.94f; "mono" -> 0.90f; else -> 1f },
            foregroundColor = p.onSurface,
            scrimAlpha = when (id) { "glass" -> 0.80f; "diary" -> 0.10f; "mono" -> 0.05f; else -> 0.08f },
            notificationStyle = card,
            shortcutStyle = card.copy(cornerRadiusDp = when (id) { "diary" -> 12f; "mono" -> 3f; else -> 28f })
        )
    }

    private fun shadeSpec(id: String, p: PaletteSpec): ShadeVisualSpec {
        val card = systemCardSpec(id, p)
        return ShadeVisualSpec(
            backgroundAlpha = when (id) { "glass" -> 0.76f; "diary" -> 0.98f; "mono" -> 1f; else -> 0.95f },
            cardCornerRadiusDp = card.cornerRadiusDp,
            cardAlpha = card.surfaceAlpha,
            spacingDp = when (id) { "diary" -> 10f; "mono" -> 6f; else -> 12f },
            cardStyle = card
        )
    }

    private fun controlCenterSpec(id: String, p: PaletteSpec): ControlCenterVisualSpec {
        val card = systemCardSpec(id, p)
        return ControlCenterVisualSpec(
            tileCornerRadiusDp = card.cornerRadiusDp,
            activeAlpha = when (id) { "glass" -> 0.80f; "mono" -> 1f; else -> 0.95f },
            inactiveAlpha = card.surfaceAlpha,
            panelAlpha = when (id) { "glass" -> 0.78f; "diary" -> 0.98f; else -> 0.96f },
            tileStyle = card
        )
    }

    private fun liveActivitySpec(id: String, p: PaletteSpec): LiveActivityVisualSpec {
        val card = systemCardSpec(id, p)
        return LiveActivityVisualSpec(
            compactCornerRadiusDp = when (id) { "diary" -> 8f; "mono" -> 3f; else -> 24f },
            expandedCornerRadiusDp = card.cornerRadiusDp,
            backgroundColor = card.backgroundColor.copy(alpha = card.surfaceAlpha),
            foregroundColor = card.foregroundColor,
            border = card.border,
            shadow = card.shadow
        )
    }

    private fun motionSpec(id: String): MotionSpec = when (id) {
        "glass" -> MotionSpec(0.96f, 1.06f, 0.66f, 330f, EditMotion.FLOAT)
        "diary" -> MotionSpec(0.97f, 1.03f, 0.84f, 380f, EditMotion.SCALE)
        "mono" -> MotionSpec(0.98f, 1f, 1f, 700f, EditMotion.NONE)
        else -> MotionSpec(0.95f, 1.05f, 0.78f, 420f, EditMotion.WIGGLE)
    }

    private fun mix(from: Color, to: Color, amount: Float): Color = Color(
        red = from.red + (to.red - from.red) * amount,
        green = from.green + (to.green - from.green) * amount,
        blue = from.blue + (to.blue - from.blue) * amount,
        alpha = from.alpha
    )
}
