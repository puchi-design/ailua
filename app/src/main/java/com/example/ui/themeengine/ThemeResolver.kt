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
    /** Shared by runtime projection and bitmap resolution, including candidate previews. */
    fun resolveIconStyleId(selection: ThemeSelection): String =
        selection.iconStyleOverrideId
            ?.takeIf { option -> IconStyleCatalog.options.any { it.id == option } }
            ?: ThemeCatalog.byId(selection.themePresetId).iconStyleId

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
        val paletteBase = PaletteCatalog.resolve(paletteId,
            darkMode || preset.id == "midnight_glass", world.colors)
        val palette = if (preset.id == "glass" || preset.id == "midnight_glass") {
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
        // Wallpaper legibility is independent of an App's light/dark surface palette.
        val homeForeground = when (wallpaper.key) {
            "builtin/default", "builtin/soft_home", "builtin/sakura_diary", "builtin/y2k_love", "builtin/rainy_study" -> Color(0xFF283039)
            "builtin/midnight_glass" -> Color(0xFFF5F8FC)
            else -> if (wallpaper.key.startsWith("builtin/oil_")) Color(0xFF283039) else palette.onSurface
        }
        val iconStyleId = resolveIconStyleId(selection)

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
            shapes = if (preset.id == "y2k_love") ShapeVisualSpec(2f, 4f, 6f) else ShapeVisualSpec(),
            layout = LayoutSpec(),
            text = uiTextSpec(preset.typographyId),
            wallpaper = wallpaper,
            icons = iconSpec(iconStyleId, palette).copy(labelColor = homeForeground),
            widgets = widgetSpec(preset.widgetStyleId, palette),
            dock = dockSpec(preset.dockStyleId, palette),
            typography = typographySpec(preset.typographyId),
            statusBar = statusSpec(preset.statusBarStyleId, palette, darkMode).let { status ->
                // A wallpaper-only choice may cross light/dark themes without changing App surfaces.
                if (wallpaper.key.startsWith("builtin/")) status.copy(
                    foregroundMode = if (homeForeground == Color(0xFF283039)) StatusForegroundMode.DARK else StatusForegroundMode.LIGHT,
                    foregroundColor = homeForeground,
                ) else status
            },
            lockscreen = lockscreenSpec(preset.id, palette).copy(foregroundColor = homeForeground),
            shade = shadeSpec(preset.id, palette),
            controlCenter = controlCenterSpec(preset.id, palette),
            liveActivity = liveActivitySpec(preset.id, palette),
            motion = motionSpec(preset.motionStyleId)
        )
    }

    private fun uiTextSpec(id: String): UiTextSpec {
        val headingFamily = when (id) {
            "diary" -> FontFamily.Serif
            "mono", "y2k" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
        return UiTextSpec(
            display = AiluaTypography.headlineLarge.copy(fontFamily = headingFamily),
            title = AiluaTypography.titleLarge.copy(fontFamily = headingFamily),
            section = AiluaTypography.titleMedium.copy(fontFamily = headingFamily),
            body = AiluaTypography.bodyLarge.let { if (id == "y2k") it.copy(fontFamily = headingFamily) else it },
            secondary = AiluaTypography.bodySmall.let { if (id == "y2k") it.copy(fontFamily = headingFamily) else it },
            caption = AiluaTypography.labelSmall.let { if (id == "y2k") it.copy(fontFamily = headingFamily) else it },
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
        if (builtInWallpaperResource("builtin/$id") != null) {
            val colors = when (id) {
                "default" -> listOf(Color(0xFFDCE8F0), Color(0xFFAFC5D2), Color(0xFFECCBB3))
                "soft_home", "oil_rose_garden", "oil_garden_still_life" -> listOf(Color(0xFFD9D5C5), Color(0xFFEAD1CD), Color(0xFFA9AD91))
                "rainy_study", "oil_lake_wildflowers" -> listOf(Color(0xFFC9D4E0), Color(0xFFA4B5C4), Color(0xFFB8BA9A))
                "oil_pink_bloom" -> listOf(Color(0xFFD9DDE0), Color(0xFFE2CDCC), Color(0xFFAEB8AE))
                "oil_woodland_path" -> listOf(Color(0xFFDCDBCE), Color(0xFFB9C1AB), Color(0xFFD5CCB7))
                "oil_waterlilies" -> listOf(Color(0xFFBFC0D0), Color(0xFFC6B9CE), Color(0xFFA6B3AD))
                "sakura_diary" -> listOf(Color(0xFFFCF5E9), Color(0xFFF1E5DB), Color(0xFFE6BCC6))
                "y2k_love" -> listOf(Color(0xFFF4DCF2), Color(0xFFDCCAF2), Color(0xFFD2E1F5))
                else -> listOf(Color(0xFF071322), Color(0xFF173956), Color(0xFF0B192C))
            }
            return WallpaperSpec("builtin/$id", colors)
        }
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
        "default_icons", "soft_home_icons", "rainy_study_icons", "sakura_icons", "midnight_icons" -> IconVisualSpec(
            IconShapeSpec.SQUIRCLE, IconContainerStyle.GRADIENT, 1f, 0.48f,
            IdentityColorMode.CONTAINER, GlyphTintMode.WHITE,
            ShadowSpec(0f), BorderSpec(Color.Transparent, 0f), p.onSurface
        )
        "y2k_icons" -> IconVisualSpec(
            IconShapeSpec.NONE, IconContainerStyle.GRADIENT, 1f, 0.48f,
            IdentityColorMode.CONTAINER, GlyphTintMode.WHITE,
            ShadowSpec(0f), BorderSpec(Color.Transparent, 0f), p.onSurface
        )
        "identity" -> IconVisualSpec(
            IconShapeSpec.SQUIRCLE, IconContainerStyle.GRADIENT, 1f, 0.48f,
            IdentityColorMode.CONTAINER, GlyphTintMode.WHITE,
            ShadowSpec(2f), BorderSpec(Color.Transparent, 0f), p.onSurface
        )
        "glass" -> IconVisualSpec(
            IconShapeSpec.SOFT_SQUARE, IconContainerStyle.SOLID, 0.95f, 0.48f,
            IdentityColorMode.CONTAINER, GlyphTintMode.WHITE,
            ShadowSpec(2f), BorderSpec(Color.Transparent, 0f), p.onSurface
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
        "rain_glass" -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.GLASS, Color(0xFFEAF3F8),
            Color(0xFF283039), 24f, 0.18f,
            BorderSpec(Color(0xFFEAF5FC), 0f), ShadowSpec(0f), 18f,
            blurRadiusDp = 26f, highlightAlpha = 0.15f, fallbackAlpha = 0.20f,
        )
        "sakura_paper" -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.PAPER,
            mix(Color(0xFFFFFAF1), p.accent, 0.035f), Color(0xFF463739),
            14f, 0.96f, BorderSpec(p.border.copy(alpha = 0.38f), 0.5f), ShadowSpec(1f), 18f,
            blurRadiusDp = 0f, highlightAlpha = 0f,
        )
        "y2k_panel" -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.FLAT,
            mix(Color(0xFFF8F1FF), p.accent, 0.04f), Color(0xFF44334F),
            4f, 1f, BorderSpec(Color(0xFF9C85B5), 0.5f), ShadowSpec(0f), 18f,
            blurRadiusDp = 0f, highlightAlpha = 0f,
        )
        "light_glass", "soft_glass", "dark_glass" -> {
            val dark = id == "dark_glass"
            WidgetVisualSpec(
                WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.GLASS,
                if (dark) Color(0xFF152B42) else Color(0xFFFFFBF4),
                if (dark) Color(0xFFF5F8FC) else Color(0xFF283039),
                24f, if (dark) 0.24f else 0.14f,
                BorderSpec(Color.Transparent, 0f), ShadowSpec(0f), 18f,
                blurRadiusDp = if (dark) 28f else 22f,
                highlightAlpha = if (dark) 0.10f else 0.20f,
                fallbackAlpha = if (dark) 0.28f else 0.20f,
            )
        }
        "glass" -> WidgetVisualSpec(
            WidgetShape.ROUNDED_RECT, WidgetBackgroundStyle.GLASS, p.surface,
            p.onSurface, 24f, 0.24f, BorderSpec(Color.Transparent, 0f),
            ShadowSpec(0f), 14f, blurRadiusDp = 28f, highlightAlpha = 0.10f, fallbackAlpha = 0.28f
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
        "rain_glass" -> DockVisualSpec(
            DockContainerMode.ISLAND, DockBackgroundStyle.GLASS, Color(0xFFEAF3F8),
            30f, 0.18f, 0f, BorderSpec(Color(0xFFEAF5FC), 0f), ShadowSpec(0f),
            12f, 12f, 0.92f, blurRadiusDp = 26f, highlightAlpha = 0.15f, fallbackAlpha = 0.22f,
        )
        "sakura_paper" -> DockVisualSpec(
            DockContainerMode.PAPER_STRIP, DockBackgroundStyle.PAPER,
            mix(Color(0xFFFFF8EF), p.accent, 0.035f), 10f, 0.96f, 0f,
            BorderSpec(p.border.copy(alpha = 0.45f), 0.5f), ShadowSpec(1f), 12f, 12f, 0.92f,
            blurRadiusDp = 0f, highlightAlpha = 0f,
        )
        "y2k_taskbar" -> DockVisualSpec(
            DockContainerMode.PAPER_STRIP, DockBackgroundStyle.SURFACE,
            mix(Color(0xFFECE1F5), p.accent, 0.05f), 4f, 1f, 0f,
            BorderSpec(Color(0xFF9C85B5), 0.5f), ShadowSpec(0f), 12f, 12f, 0.92f,
            blurRadiusDp = 0f, highlightAlpha = 0f,
        )
        "light_glass", "soft_glass", "dark_glass" -> {
            val dark = id == "dark_glass"
            DockVisualSpec(
                DockContainerMode.ISLAND, DockBackgroundStyle.GLASS,
                if (dark) Color(0xFF142A40) else Color(0xFFFFFBF4),
                30f, if (dark) 0.22f else 0.16f, 0f,
                BorderSpec(Color.Transparent, 0f), ShadowSpec(0f), 12f, 12f, 0.92f,
                blurRadiusDp = 28f, highlightAlpha = if (dark) 0.10f else 0.20f,
                fallbackAlpha = if (dark) 0.28f else 0.22f,
            )
        }
        "glass" -> DockVisualSpec(
            DockContainerMode.ISLAND, DockBackgroundStyle.GLASS, p.surface,
            30f, 0.22f, 0f, BorderSpec(Color.Transparent, 0f),
            ShadowSpec(0f), 18f, 11f, 0.95f,
            blurRadiusDp = 28f, highlightAlpha = 0.10f, fallbackAlpha = 0.28f
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
        "mono", "y2k" -> TypographySpec(TypographyFamily.MONO, FontWeight.Normal, 0.93f, FontWeight.Medium, 1.08f)
        else -> TypographySpec(TypographyFamily.SOFT_SANS, FontWeight.Medium, 1f, FontWeight.SemiBold, 1f)
    }

    private fun statusSpec(id: String, p: PaletteSpec, darkMode: Boolean): StatusBarVisualSpec = when (id) {
        "light_home" -> StatusBarVisualSpec(StatusForegroundMode.DARK, Color(0xFF283039), 0f, false)
        "glass" -> StatusBarVisualSpec(StatusForegroundMode.LIGHT, Color.White, 0f, false)
        "diary" -> StatusBarVisualSpec(
            if (darkMode) StatusForegroundMode.LIGHT else StatusForegroundMode.DARK,
            p.onSurface, 0.18f, false
        )
        "mono" -> StatusBarVisualSpec(StatusForegroundMode.AUTO, p.onSurface, 0f, true)
        else -> StatusBarVisualSpec(StatusForegroundMode.AUTO, p.onSurface, 0.1f, false)
    }

    private fun systemCardSpec(id: String, p: PaletteSpec): SystemCardStyle = when (id) {
        "default", "soft_home" -> SystemCardStyle(p.surface, p.onSurface, 24f, 0.94f,
            BorderSpec(Color.Transparent, 0f), ShadowSpec(2f))
        "rainy_study" -> SystemCardStyle(Color(0xFF243744), p.onSurface, 24f, 0.90f,
            BorderSpec(Color.Transparent, 0f), ShadowSpec(0f))
        "midnight_glass" -> SystemCardStyle(p.surface, p.onSurface, 24f, 0.88f,
            BorderSpec(Color.Transparent, 0f), ShadowSpec(0f))
        "sakura_diary" -> SystemCardStyle(p.surface, p.onSurface, 14f, 0.97f,
            BorderSpec(p.border.copy(alpha = 0.45f), 0.5f), ShadowSpec(1f))
        "y2k_love" -> SystemCardStyle(p.surface, p.onSurface, 4f, 1f,
            BorderSpec(p.border, 0.5f), ShadowSpec(0f))
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
            scrimAlpha = when (id) {
                "glass" -> 0.80f
                "rainy_study", "midnight_glass" -> 0.16f
                "diary", "sakura_diary" -> 0.10f
                "mono", "y2k_love" -> 0.05f
                else -> 0.08f
            },
            notificationStyle = card,
            shortcutStyle = card.copy(cornerRadiusDp = when (id) {
                "diary", "sakura_diary" -> 12f
                "mono", "y2k_love" -> 4f
                else -> 28f
            })
        )
    }

    private fun shadeSpec(id: String, p: PaletteSpec): ShadeVisualSpec {
        val card = systemCardSpec(id, p)
        return ShadeVisualSpec(
            backgroundAlpha = when (id) {
                "glass" -> 0.76f
                "rainy_study", "midnight_glass" -> 0.94f
                "diary", "sakura_diary" -> 0.98f
                "mono", "y2k_love" -> 1f
                else -> 0.95f
            },
            cardCornerRadiusDp = card.cornerRadiusDp,
            cardAlpha = card.surfaceAlpha,
            spacingDp = when (id) { "diary", "sakura_diary" -> 10f; "mono", "y2k_love" -> 6f; else -> 12f },
            cardStyle = card
        )
    }

    private fun controlCenterSpec(id: String, p: PaletteSpec): ControlCenterVisualSpec {
        val card = systemCardSpec(id, p)
        return ControlCenterVisualSpec(
            tileCornerRadiusDp = card.cornerRadiusDp,
            activeAlpha = when (id) { "glass" -> 0.80f; "mono", "y2k_love" -> 1f; else -> 0.95f },
            inactiveAlpha = card.surfaceAlpha,
            panelAlpha = when (id) { "glass" -> 0.78f; "diary", "sakura_diary" -> 0.98f; else -> 0.96f },
            tileStyle = card
        )
    }

    private fun liveActivitySpec(id: String, p: PaletteSpec): LiveActivityVisualSpec {
        val card = systemCardSpec(id, p)
        return LiveActivityVisualSpec(
            compactCornerRadiusDp = when (id) {
                "diary", "sakura_diary" -> 8f
                "mono", "y2k_love" -> 4f
                else -> 24f
            },
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
