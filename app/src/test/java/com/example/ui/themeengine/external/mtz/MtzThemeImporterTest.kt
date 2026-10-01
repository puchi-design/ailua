package com.example.ui.themeengine.external.mtz

import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ImportedThemeSource
import com.example.ui.themeengine.external.ThemeAssetRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MtzThemeImporterTest {
    @Test
    fun nestedIconsAndMetadataBecomeNormalizedThemeAssets() {
        val nested = zip(
            "res/drawable-xxhdpi/com.android.contacts.png" to PNG,
            "res/drawable-xhdpi/com.android.contacts.png" to PNG,
            "res/drawable-xxhdpi/com.google.android.apps.messaging.png" to PNG
        )
        val mtz = zip(
            "description.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <MIUI-Theme>
                    <title>Evening</title><designer>Example Designer</designer>
                    <version>1.2</version><description>Quiet theme</description><uiVersion>16</uiVersion>
                </MIUI-Theme>
            """.trimIndent().toByteArray(),
            "icons" to nested,
            "wallpaper/default_wallpaper.jpg" to JPEG,
            "wallpaper/default_lock_wallpaper.png" to PNG,
            "preview/preview_icons_0.jpg" to JPEG,
            "preview/preview_launcher_0.jpg" to JPEG
        )

        val result = MtzThemeImporter().parse(ImportedThemeSource("evening.mtz", mtz))
        assertEquals(ExternalThemeFormat.MIUI_MTZ, result.theme.format)
        assertEquals("Evening", result.theme.name)
        assertEquals("Example Designer", result.theme.author)
        assertEquals("1.2", result.theme.version)
        assertEquals("16", result.theme.metadata["uiVersion"])
        assertEquals(2, result.theme.wallpapers.size)
        assertEquals(2, result.theme.previewAssets.size)
        assertTrue((result.theme.wallpapers.first() as ThemeAssetRef.LocalFile).relativePath.endsWith("/wallpaper/home.jpg"))
        assertTrue((result.theme.previewAssets.first() as ThemeAssetRef.LocalFile).relativePath.endsWith("/preview/launcher.jpg"))
        assertEquals(2, result.theme.icons?.allIcons?.size)
        assertNotNull(result.theme.icons?.mappings?.get("com.android.contacts"))
        assertNotNull(result.theme.icons?.mappings?.get("com.google.android.apps.messaging"))
        assertEquals(6, result.assets.size)
        assertTrue(result.assets.keys.all { it.startsWith("${result.theme.id}/") })
    }

    @Test
    fun missingDescriptionStillImportsRecognizableMtz() {
        val mtz = zip(
            "icons" to zip("res/drawable-xxhdpi/com.android.settings.webp" to WEBP),
            "preview/preview_icons_0.jpg" to JPEG
        )
        val result = MtzThemeImporter().parse(ImportedThemeSource("old-miui.mtz", mtz))
        assertEquals("old-miui", result.theme.name)
        assertEquals(null, result.theme.author)
        assertNotNull(result.theme.icons?.mappings?.get("com.android.settings"))
        assertFalse(result.theme.previewAssets.isEmpty())
    }

    @Test
    fun malformedNestedIconsDoNotDiscardUsableWallpaper() {
        val mtz = zip(
            "description.xml" to "<theme><title>Recovery</title></theme>".toByteArray(),
            "icons" to byteArrayOf(0x50, 0x4b, 0x03, 0x04, 0x00),
            "wallpaper/default_wallpaper.png" to PNG
        )
        val result = MtzThemeImporter().parse(ImportedThemeSource("recovery.mtz", mtz))
        assertEquals("Recovery", result.theme.name)
        assertEquals(null, result.theme.icons)
        assertEquals(1, result.theme.wallpapers.size)
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                for ((name, bytes) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }

    companion object {
        private val PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9b6QcAAAAASUVORK5CYII="
        )
        private val JPEG = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte())
        private val WEBP = "RIFF\u0004\u0000\u0000\u0000WEBP".toByteArray(Charsets.ISO_8859_1)
    }
}

