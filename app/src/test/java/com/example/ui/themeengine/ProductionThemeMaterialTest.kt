package com.example.ui.themeengine

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.ui.themeengine.material.surfaceMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionThemeMaterialTest {
    private fun runtime(id: String, selection: ThemeSelection = ThemeSelection(id)) =
        ThemeResolver.resolve(selection, false, DayPhase.DUSK, WeatherState.RAIN)

    @Test
    fun officialThemesHaveRealWallpaperAssetsAndIndependentIconPacks() {
        val themes = listOf("default", "soft_home", "rainy_study", "sakura_diary", "y2k_love", "midnight_glass")
        assertEquals(6, themes.map { ThemeCatalog.byId(it).iconStyleId }.toSet().size)
        for (id in themes) {
            val theme = runtime(id)
            assertEquals("builtin/$id", theme.wallpaper.key)
            assertTrue("$id needs a packaged wallpaper", builtInWallpaperResource(theme.wallpaper.key) != null)
        }
        assertEquals("milk", ThemeCatalog.DEFAULT_ID)
        assertEquals(setOf("milk", "glass", "diary", "mono"),
            ThemeCatalog.presets.take(4).map { it.id }.toSet())
    }

    @Test
    fun rainyCoastHasReadableLowAlphaGlassAndSakuraIsOpaquePaper() {
        val rain = runtime("rainy_study")
        assertEquals(WidgetBackgroundStyle.GLASS, rain.widgets.backgroundStyle)
        assertEquals(DockBackgroundStyle.GLASS, rain.dock.backgroundStyle)
        assertTrue(rain.widgets.surfaceAlpha in 0.14f..0.22f)
        assertTrue(rain.dock.surfaceAlpha in 0.14f..0.22f)
        assertTrue(rain.widgets.fallbackAlpha <= 0.30f)
        assertTrue(rain.dock.fallbackAlpha <= 0.30f)
        assertTrue(rain.widgets.blurRadiusDp in 24f..28f)
        assertTrue(rain.dock.blurRadiusDp in 24f..28f)
        assertTrue(rain.palette.accent.blue > rain.palette.accent.red)
        assertTrue(rain.palette.backgroundPrimary.luminance() > 0.7f)
        assertTrue(rain.widgets.foregroundColor.luminance() < 0.1f)
        assertEquals(0f, rain.widgets.border.widthDp)

        val paper = runtime("sakura_diary")
        assertEquals(WidgetBackgroundStyle.PAPER, paper.widgets.backgroundStyle)
        assertEquals(DockBackgroundStyle.PAPER, paper.dock.backgroundStyle)
        assertEquals(DockContainerMode.PAPER_STRIP, paper.dock.containerMode)
        assertTrue(paper.widgets.surfaceAlpha in 0.94f..0.98f)
        assertTrue(paper.dock.surfaceAlpha in 0.94f..0.98f)
        assertEquals(0f, paper.widgets.blurRadiusDp)
        assertEquals(0f, paper.dock.blurRadiusDp)
        assertTrue(paper.widgets.foregroundColor.luminance() < 0.1f)
        assertNotEquals(runtime("soft_home").widgets.backgroundStyle, paper.widgets.backgroundStyle)
        assertNotEquals(runtime("midnight_glass").palette.accent, rain.palette.accent)
    }

    @Test
    fun y2kUsesSmallPhysicalCornersOpaqueTaskbarAndSystemMonospace() {
        val theme = runtime("y2k_love")
        assertEquals(WidgetBackgroundStyle.FLAT, theme.widgets.backgroundStyle)
        assertEquals(DockBackgroundStyle.SURFACE, theme.dock.backgroundStyle)
        assertEquals(DockContainerMode.PAPER_STRIP, theme.dock.containerMode)
        assertEquals(1f, theme.widgets.surfaceAlpha)
        assertEquals(1f, theme.dock.surfaceAlpha)
        assertTrue(theme.widgets.surfaceMaterial().cornerRadiusDp in 2f..6f)
        assertTrue(theme.dock.surfaceMaterial().cornerRadiusDp in 2f..6f)
        assertTrue(theme.widgets.border.widthDp in 0.1f..1f)
        assertTrue(theme.dock.border.widthDp in 0.1f..1f)
        assertEquals(0f, theme.widgets.shadow.elevationDp)
        assertEquals(0f, theme.dock.shadow.elevationDp)
        assertEquals(0f, theme.widgets.blurRadiusDp)
        assertEquals(0f, theme.dock.blurRadiusDp)
        assertEquals(FontFamily.Monospace, theme.text.display.fontFamily)
        assertEquals(FontFamily.Monospace, theme.text.body.fontFamily)
        assertEquals(FontFamily.Monospace, theme.text.secondary.fontFamily)
        assertEquals(FontFamily.Monospace, theme.text.caption.fontFamily)
    }

    @Test
    fun mixingChangesOnlyChosenPartsAndKeepsWallpaperForegroundReadable() {
        val rain = runtime("rainy_study")
        val mixed = runtime("rainy_study", ThemeSelection("rainy_study",
            paletteOverrideId = "sakura", wallpaperOverrideId = "y2k_love", iconStyleOverrideId = "soft_home_icons"))
        assertNotEquals(rain.palette.accent, mixed.palette.accent)
        assertEquals("sakura", mixed.palette.id)
        assertEquals("builtin/y2k_love", mixed.wallpaper.key)
        assertEquals(rain.widgets, mixed.widgets)
        assertEquals(rain.dock, mixed.dock)
        assertTrue(mixed.icons.labelColor.luminance() < 0.1f)
        assertEquals(mixed.icons.labelColor, mixed.statusBar.foregroundColor)

        val paper = runtime("sakura_diary")
        val nightPaper = runtime("sakura_diary", ThemeSelection("sakura_diary", wallpaperOverrideId = "rainy_study"))
        assertEquals(paper.widgets, nightPaper.widgets)
        assertEquals(paper.dock, nightPaper.dock)
        assertTrue(nightPaper.icons.labelColor.luminance() < 0.1f)
        assertEquals(nightPaper.icons.labelColor, nightPaper.statusBar.foregroundColor)

        for (id in listOf("rainy_study", "sakura_diary", "y2k_love")) {
            val worldMixed = runtime(id, ThemeSelection(id, wallpaperOverrideId = "world"))
            assertTrue(worldMixed.wallpaper.key.startsWith("world/"))
            assertEquals(runtime(id).widgets, worldMixed.widgets)
            assertEquals(runtime(id).dock, worldMixed.dock)
        }
    }
}
