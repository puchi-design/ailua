package com.example.ui.themecenter

import com.example.ui.themeengine.external.ImportedThemeSource
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.external.UnifiedThemeImporter
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.WallpaperCatalog
import com.example.ui.themeengine.external.ExternalThemeFormat
import androidx.compose.ui.graphics.luminance
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiluaThemeArchiveWriterTest {
    @Test fun exportedMixRoundTripsThroughExistingImporterWithoutLosingKeysOrBytes() {
        val wallpaper = "RIFF1234WEBPsample".toByteArray()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        var closed = false
        val output = object : ByteArrayOutputStream() { override fun close() { closed = true } }
        AiluaThemeArchiveWriter.write(output, "sakura-mix", "我的搭配", "sakura_diary", "sakura",
            wallpaper, mapOf("chat" to png, "check_phone" to png))
        assertFalse("SAF caller owns its stream", closed)
        val imported = UnifiedThemeImporter.inspect(ImportedThemeSource("mix.ailuatheme", output.toByteArray()))
        assertEquals("我的搭配", imported.theme.name)
        assertEquals("sakura_diary", imported.theme.basePresetId)
        assertEquals("sakura", imported.theme.preferredPaletteId)
        val ref = imported.theme.wallpapers.single() as ThemeAssetRef.LocalFile
        assertTrue(ref.relativePath.endsWith("current.webp"))
        assertArrayEquals(wallpaper, imported.assets[ref.relativePath])
        assertEquals(setOf("chat", "check_phone"), imported.theme.icons!!.mappings.keys)
        imported.theme.icons!!.mappings.values.forEach { icon ->
            assertArrayEquals(png, imported.assets[(icon as ThemeAssetRef.LocalFile).relativePath])
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathsInIconKeysBeforeWritingArchive() {
        val output = ByteArrayOutputStream()
        AiluaThemeArchiveWriter.write(output, "mix", "Mix", "milk", "cream", byteArrayOf(1),
            mapOf("../../private" to byteArrayOf(1)))
    }

    @Test fun exportedSparseAndVectorOnlyMixesRestoreTheirMaterialAndTypography() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val phase = DayPhase.DUSK
        val weather = WeatherState.RAIN
        for ((preset, icons) in listOf(
            "sakura_diary" to mapOf("chat" to png),
            "y2k_love" to mapOf("gallery" to png),
            "glass" to emptyMap(),
            "milk" to emptyMap(),
            "diary" to emptyMap(),
            "mono" to emptyMap(),
        )) {
            val original = ThemeResolver.resolve(ThemeSelection(preset), false, phase, weather)
            val output = ByteArrayOutputStream()
            AiluaThemeArchiveWriter.write(output, "$preset-mix", "Current", preset,
                original.palette.id, png, icons)
            val imported = UnifiedThemeImporter.inspect(ImportedThemeSource("mix.ailuatheme", output.toByteArray()))
            val selection = ThemeSelection("midnight_glass", iconStyleOverrideId = "midnight_icons",
                paletteOverrideId = "starry").withImportedTheme(imported.theme)
            val restored = ThemeResolver.resolve(selection, false, phase, weather)
            assertEquals(preset, restored.id)
            assertEquals(original.widgets, restored.widgets)
            assertEquals(original.dock, restored.dock)
            assertEquals(original.text, restored.text)
            assertEquals(original.typography, restored.typography)
            assertEquals(original.icons, restored.icons)
            assertEquals(icons.keys, imported.theme.icons?.mappings?.keys ?: emptySet<String>())
            assertEquals(if (icons.isEmpty()) null else "theme:${imported.theme.id}", selection.iconSourceOverrideId)
        }
    }

    @Test fun exportedIconStyleMixRestoresVectorOrBitmapFallbackWithoutChangingShell() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        for (originalSelection in listOf(
            ThemeSelection("default", iconStyleOverrideId = "mono"),
            ThemeSelection("glass", iconStyleOverrideId = "sakura_icons"),
        )) {
            val original = ThemeResolver.resolve(originalSelection, false, DayPhase.DUSK, WeatherState.RAIN)
            val hasBitmapIcons = originalSelection.themePresetId == "glass"
            val output = ByteArrayOutputStream()
            AiluaThemeArchiveWriter.write(output, "mix", "Current mix", original.id, original.palette.id,
                png, if (hasBitmapIcons) mapOf("chat" to png) else emptyMap(),
                iconStyle = ThemeResolver.resolveIconStyleId(originalSelection))
            val imported = UnifiedThemeImporter.inspect(ImportedThemeSource("mix.ailuatheme", output.toByteArray()))
            assertEquals(originalSelection.iconStyleOverrideId, imported.theme.metadata["iconStyle"])
            val applied = ThemeSelection("y2k_love", iconStyleOverrideId = "y2k_icons").withImportedTheme(imported.theme)
            val restored = ThemeResolver.resolve(applied, false, DayPhase.DUSK, WeatherState.RAIN)
            assertEquals(originalSelection.themePresetId, applied.themePresetId)
            assertEquals(originalSelection.iconStyleOverrideId, applied.iconStyleOverrideId)
            assertEquals(original.widgets, restored.widgets)
            assertEquals(original.dock, restored.dock)
            assertEquals(original.text, restored.text)
            assertEquals(original.icons, restored.icons)
            assertEquals(if (hasBitmapIcons) "theme:${imported.theme.id}" else null, applied.iconSourceOverrideId)
        }
    }

    @Test fun exportedWallpaperMixRestoresLightDarkForegroundWhileKeepingImportedPixels() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val cases = WallpaperCatalog.options.filter { it.id.startsWith("oil_") }.map {
            ThemeSelection("midnight_glass", wallpaperOverrideId = it.id)
        } + listOf(
            ThemeSelection("soft_home", wallpaperOverrideId = "midnight_glass"),
            ThemeSelection("milk"), ThemeSelection("glass"), ThemeSelection("diary"), ThemeSelection("mono"),
        )
        for (selection in cases) {
            val original = ThemeResolver.resolve(selection, false, DayPhase.NOON, WeatherState.RAIN)
            val style = wallpaperStyleForExport(selection, original)
            assertTrue("Each current wallpaper maps to a valid catalog style", style != null)
            val output = ByteArrayOutputStream()
            AiluaThemeArchiveWriter.write(output, "wallpaper-mix", "Mixed wallpaper", original.id,
                original.palette.id, png, emptyMap(),
                iconStyle = ThemeResolver.resolveIconStyleId(selection), wallpaperStyle = style)
            val imported = UnifiedThemeImporter.inspect(ImportedThemeSource("mix.ailuatheme", output.toByteArray()))
            assertEquals(style, imported.theme.metadata["wallpaperStyle"])
            val restoredSelection = ThemeSelection("sakura_diary", wallpaperOverrideId = "world",
                wallpaperSourceId = "old-wallpaper").withImportedTheme(imported.theme)
            assertEquals(selection.themePresetId, restoredSelection.themePresetId)
            assertEquals(style, restoredSelection.wallpaperOverrideId)
            assertEquals("Imported bitmap remains the image source", imported.theme.id, restoredSelection.wallpaperSourceId)
            val wallpaper = imported.theme.wallpapers.single() as ThemeAssetRef.LocalFile
            assertArrayEquals(png, imported.assets[wallpaper.relativePath])
            val restored = ThemeResolver.resolve(restoredSelection, false, DayPhase.NOON, WeatherState.RAIN)
            assertEquals(original.wallpaper.key, restored.wallpaper.key)
            assertEquals(original.widgets, restored.widgets)
            assertEquals(original.dock, restored.dock)
            assertEquals(original.icons.labelColor, restored.icons.labelColor)
            assertEquals(original.statusBar.foregroundColor, restored.statusBar.foregroundColor)
            assertEquals(original.statusBar.foregroundMode, restored.statusBar.foregroundMode)
            assertEquals(original.lockscreen.foregroundColor, restored.lockscreen.foregroundColor)
            if (style?.startsWith("oil_") == true) {
                assertTrue("Bright oil wallpaper uses dark Home foreground", restored.icons.labelColor.luminance() < 0.1f)
            } else if (style == "midnight_glass") {
                assertTrue("Dark wallpaper uses light Home foreground", restored.icons.labelColor.luminance() > 0.8f)
            }
        }
    }

    @Test fun optionalWallpaperStyleKeepsLegacyBehaviorAndRejectsUnknownOrOemStyles() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val current = ThemeSelection("soft_home", wallpaperOverrideId = "oil_woodland_path",
            wallpaperSourceId = "old-wallpaper")
        for (style in listOf(null, "unknown-wallpaper", "../../external-file")) {
            val output = ByteArrayOutputStream()
            AiluaThemeArchiveWriter.write(output, "legacy-mix", "Legacy", "glass", "mist", png,
                emptyMap(), wallpaperStyle = style)
            val imported = UnifiedThemeImporter.inspect(ImportedThemeSource("mix.ailuatheme", output.toByteArray()))
            val restored = current.withImportedTheme(imported.theme)
            assertEquals("glass", restored.themePresetId)
            assertNull(restored.wallpaperOverrideId)
            assertEquals(imported.theme.id, restored.wallpaperSourceId)
            if (style == null) assertTrue(imported.theme.metadata.isEmpty())
            for (format in listOf(ExternalThemeFormat.MIUI_MTZ, ExternalThemeFormat.COLOROS_THEME)) {
                val oem = imported.theme.copy(format = format, metadata = mapOf("wallpaperStyle" to "midnight_glass"))
                val applied = current.withImportedTheme(oem)
                assertEquals(current.themePresetId, applied.themePresetId)
                assertEquals(current.wallpaperOverrideId, applied.wallpaperOverrideId)
                assertEquals(oem.id, applied.wallpaperSourceId)
            }
        }
    }
}
