package com.example.ui.themeengine.external.coloros

import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ImportedThemeSource
import com.example.ui.themeengine.external.ThemeAssetRef
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorOsThemeImporterTest {
    @Test
    fun oppoThemeExtractsMetadataPreviewsWallpaperAndNestedIcons() {
        val nestedIcons = zip("com.android.contacts.png" to PNG, "com.android.contacts.MainActivity.png" to PNG)
        val theme = zip(
            "themeInfo.xml" to """
                <OppoSmartPhoneThemeInfo>
                  <Author>Ada</Author><Summary>Soft theme</Summary>
                  <Description>Local test</Description><UUID>theme-123</UUID>
                  <VersionName>1.2</VersionName>
                  <resolutionInfo><resolution>2400x1080</resolution></resolutionInfo>
                  <packageInfo><package name="com.oppo.launcher" version="1"/></packageInfo>
                </OppoSmartPhoneThemeInfo>
            """.trimIndent().toByteArray(),
            "picture/lock.jpg" to JPEG,
            "picture/home.jpg" to JPEG,
            "wallpaper" to zip("home.png" to PNG, "lock.png" to PNG),
            "icons" to nestedIcons
        )

        val preview = ColorOsThemeImporter().parse(ImportedThemeSource("sample.theme", theme))
        assertEquals(ExternalThemeFormat.COLOROS_THEME, preview.theme.format)
        assertEquals("Soft theme", preview.theme.name)
        assertEquals("Ada", preview.theme.author)
        assertEquals("1.2", preview.theme.version)
        assertEquals("2400x1080", preview.theme.metadata["resolutionInfo"])
        assertEquals("com.oppo.launcher@1", preview.theme.metadata["packageInfo"])
        assertEquals(2, preview.theme.previewAssets.size)
        assertEquals(2, preview.theme.wallpapers.size)
        assertTrue((preview.theme.previewAssets.first() as ThemeAssetRef.LocalFile).relativePath.contains("home"))
        assertNotNull(preview.theme.icons?.mappings?.get("com.android.contacts"))
        assertEquals(2, preview.theme.icons?.allIcons?.size)
        assertTrue(preview.theme.icons!!.mappings.values.all {
            it is ThemeAssetRef.LocalFile && preview.assets.containsKey(it.relativePath)
        })
        // Both nested containers are retained as opaque assets.
        assertTrue(preview.assets.keys.any { it.contains("/raw/") && it.contains("wallpaper") })
        assertTrue(preview.assets.keys.any { it.contains("/raw/") && it.contains("icons") })
    }

    @Test
    fun oplusThemeUsesImageMagicEvenWhenFileExtensionIsWrong() {
        val theme = zip(
            "themeInfo.xml" to """
                <oplussmartphonethemeinfo><Author>Lee</Author><VersionCode>7</VersionCode></oplussmartphonethemeinfo>
            """.trimIndent().toByteArray(),
            "wallpaper/default.dat" to PNG,
            "com.oppo.launcher" to zip("ComponentInfo{com.android.settings/.Settings}.bin" to WEBP),
            "picture/home.bin" to JPEG
        )
        val preview = ColorOsThemeImporter().parse(ImportedThemeSource("unusual.theme", theme))
        assertEquals("Lee", preview.theme.author)
        assertEquals("7", preview.theme.version)
        assertEquals(1, preview.theme.wallpapers.size)
        assertEquals(1, preview.theme.previewAssets.size)
        val icon = preview.theme.icons?.mappings?.get("com.android.settings")
        assertNotNull(icon)
        assertTrue((icon as ThemeAssetRef.LocalFile).relativePath.endsWith(".webp"))
        assertEquals(icon, preview.theme.icons?.mappings?.get(
            "com.android.settings/com.android.settings.Settings"))
        assertFalse(preview.theme.icons!!.allIcons.isEmpty())
    }

    @Test
    fun normalizesPackageAndComponentNames() {
        assertEquals("com.android.contacts", normalizeColorOsIconName("icons/com.android.contacts.png")?.packageName)
        assertEquals("com.android.contacts", normalizeColorOsIconName("com.android.contacts.MainActivity.png")?.packageName)
        assertEquals("com.android.contacts.MainActivity",
            normalizeColorOsIconName("com.android.contacts.MainActivity.png")?.activityName)
        assertEquals("com.android.settings.Settings",
            normalizeColorOsIconName("ComponentInfo{com.android.settings/.Settings}.png")?.activityName)
        assertEquals(null, normalizeColorOsIconName("ComponentInfo{com.android.settings/.Settings.png"))
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { archive ->
            entries.forEach { (name, bytes) ->
                archive.putNextEntry(ZipEntry(name))
                archive.write(bytes)
                archive.closeEntry()
            }
        }
        return output.toByteArray()
    }

    companion object {
        private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        private val JPEG = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x00)
        private val WEBP = "RIFF1234WEBP".toByteArray()
    }
}

