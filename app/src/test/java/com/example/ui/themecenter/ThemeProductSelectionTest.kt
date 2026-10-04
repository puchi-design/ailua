package com.example.ui.themecenter

import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ExternalThemePackage
import com.example.ui.themeengine.external.ThemeAssetRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeProductSelectionTest {
    @Test
    fun applyingAProductPreservesUserIconSourcesWhileResettingBuiltinMix() {
        val current = ThemeSelection("milk", paletteOverrideId = "starry", wallpaperOverrideId = "world",
            wallpaperSourceId = "imported-wallpaper", iconStyleOverrideId = "mono",
            iconSourceOverrideId = "android:com.example.myicons",
            manualIconOverrides = mapOf("chat" to "my-bubble", "gallery" to "my-photo"))
        for (product in officialThemeProducts) {
            val applied = current.withOfficialTheme(product.id)
            assertEquals(product.id, applied.themePresetId)
            assertEquals(current.iconSourceOverrideId, applied.iconSourceOverrideId)
            assertEquals(current.manualIconOverrides, applied.manualIconOverrides)
            assertNull(applied.paletteOverrideId)
            assertNull(applied.wallpaperOverrideId)
            assertNull(applied.wallpaperSourceId)
            assertNull(applied.iconStyleOverrideId)
        }
    }

    @Test
    fun productPreviewsDoNotInheritExistingExternalAssetsOrMutateSelection() {
        val current = ThemeSelection("glass", wallpaperSourceId = "private-wallpaper",
            iconSourceOverrideId = "theme:private-icons", manualIconOverrides = mapOf("chat" to "private-chat"))
        val preview = officialPreviewSelection("sakura_diary")
        assertEquals(ThemeSelection("sakura_diary"), preview)
        assertEquals("private-wallpaper", current.wallpaperSourceId)
        assertEquals("theme:private-icons", current.iconSourceOverrideId)
        assertEquals(mapOf("chat" to "private-chat"), current.manualIconOverrides)
    }

    @Test
    fun onlyAiluaArchivesCanRestoreTheirDeclaredBuiltinShell() {
        val current = ThemeSelection("midnight_glass", paletteOverrideId = "starry",
            wallpaperOverrideId = "world", iconStyleOverrideId = "y2k_icons",
            wallpaperSourceId = "old-wallpaper", iconSourceOverrideId = "android:old.icons",
            manualIconOverrides = mapOf("chat" to "custom"))
        val archive = ExternalThemePackage("new-archive", ExternalThemeFormat.AILUA, "Archive",
            basePresetId = "sakura_diary", preferredPaletteId = "sakura",
            wallpapers = listOf(ThemeAssetRef.LocalFile("new-archive/wallpaper/current.png")))
        val restored = current.withImportedTheme(archive)
        assertEquals("sakura_diary", restored.themePresetId)
        assertEquals("sakura", restored.paletteOverrideId)
        assertEquals(archive.id, restored.wallpaperSourceId)
        assertNull(restored.wallpaperOverrideId)
        assertNull(restored.iconStyleOverrideId)
        assertNull("No bitmap icon set means use the restored shell", restored.iconSourceOverrideId)
        assertEquals(emptyMap<String, String>(), restored.manualIconOverrides)

        for (format in listOf(ExternalThemeFormat.MIUI_MTZ, ExternalThemeFormat.COLOROS_THEME)) {
            val oemApplied = current.withImportedTheme(archive.copy(format = format))
            assertEquals(current.themePresetId, oemApplied.themePresetId)
            assertEquals(current.wallpaperOverrideId, oemApplied.wallpaperOverrideId)
            assertEquals(current.iconStyleOverrideId, oemApplied.iconStyleOverrideId)
            assertEquals(current.iconSourceOverrideId, oemApplied.iconSourceOverrideId)
            assertEquals(current.manualIconOverrides, oemApplied.manualIconOverrides)
            assertEquals(archive.id, oemApplied.wallpaperSourceId)
        }
    }

    @Test
    fun unknownAiluaShellKeepsCurrentShellAndMissingPresetAssetsUseBuiltinFallback() {
        val current = ThemeSelection("glass", paletteOverrideId = "starry",
            wallpaperSourceId = "old-wallpaper", iconSourceOverrideId = "theme:old-icons",
            manualIconOverrides = mapOf("chat" to "custom"))
        val unknown = ExternalThemePackage("unknown", ExternalThemeFormat.AILUA, "Unknown",
            basePresetId = "framework-overlay")
        val unknownApplied = current.withImportedTheme(unknown)
        assertEquals(current.themePresetId, unknownApplied.themePresetId)
        assertEquals(current.paletteOverrideId, unknownApplied.paletteOverrideId)
        assertEquals(current.wallpaperSourceId, unknownApplied.wallpaperSourceId)
        assertEquals(current.iconSourceOverrideId, unknownApplied.iconSourceOverrideId)
        assertEquals(current.manualIconOverrides, unknownApplied.manualIconOverrides)

        val presetOnly = current.withImportedTheme(unknown.copy(basePresetId = "mono"))
        assertEquals("mono", presetOnly.themePresetId)
        assertNull(presetOnly.paletteOverrideId)
        assertNull(presetOnly.wallpaperSourceId)
        assertNull(presetOnly.iconSourceOverrideId)
    }

    @Test
    fun importedIconStyleIsCatalogValidatedAndNeverInterpretedForOemPackages() {
        val current = ThemeSelection("midnight_glass", iconStyleOverrideId = "soft_home_icons")
        val archive = ExternalThemePackage("mix", ExternalThemeFormat.AILUA, "Mix",
            basePresetId = "default", metadata = mapOf("iconStyle" to "mono"))
        assertEquals("mono", current.withImportedTheme(archive).iconStyleOverrideId)
        assertNull(current.withImportedTheme(archive.copy(metadata = mapOf("iconStyle" to "unknown-style"))).iconStyleOverrideId)
        assertEquals(current.iconStyleOverrideId,
            current.withImportedTheme(archive.copy(format = ExternalThemeFormat.MIUI_MTZ)).iconStyleOverrideId)
    }
}
