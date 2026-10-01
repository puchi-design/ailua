package com.example.ui.themeengine.external

import com.example.ui.themeengine.external.ailua.AiluaThemeImporter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiluaThemeImporterTest {
    @Test fun genericNameUsesContentsAndImportsSchemaOneAssets() {
        val source = ImportedThemeSource("download.bin", zip(
            "manifest.json" to """{"schema":1,"id":"Sakura.Diary","name":"Sakura Diary","author":"Ada"}""".toByteArray(),
            "theme.json" to """{"basePreset":"diary","palette":"sakura","wallpaper":"wallpaper/second.webp","icons":{"path":"icons/"}}""".toByteArray(),
            "wallpaper/first.png" to PNG,
            "wallpaper/second.webp" to WEBP,
            "preview/home.jpg" to JPEG,
            "icons/com.android.contacts.png" to PNG,
            "icons/ignored.txt" to byteArrayOf(1)
        ))
        val preview = UnifiedThemeImporter.inspect(source)
        assertEquals(ExternalThemeFormat.AILUA, preview.theme.format)
        assertEquals("Sakura Diary", preview.theme.name)
        assertEquals("Ada", preview.theme.author)
        assertEquals("diary", preview.theme.basePresetId)
        assertEquals("sakura", preview.theme.preferredPaletteId)
        assertTrue(preview.theme.id.startsWith("sakura-diary-"))
        assertEquals(2, preview.theme.wallpapers.size)
        assertTrue((preview.theme.wallpapers.first() as ThemeAssetRef.LocalFile).relativePath.endsWith("second.webp"))
        assertEquals(1, preview.theme.previewAssets.size)
        val icon = preview.theme.icons?.mappings?.get("com.android.contacts")
        assertNotNull(icon)
        assertArrayEquals(PNG, preview.assets[(icon as ThemeAssetRef.LocalFile).relativePath])
        assertEquals(4, preview.assets.size)
    }

    @Test fun legacyPresetAndMissingWallpaperReferenceRemainImportable() {
        val source = ImportedThemeSource("legacy.ailuatheme", zip(
            "manifest.json" to """{"schema":1,"name":"Legacy"}""".toByteArray(),
            "theme.json" to """{"preset":{"skin":"milk","palette":"sakura","wallpaper":"wallpaper/missing.jpg"}}""".toByteArray(),
            "wallpaper/home.png" to PNG
        ))
        val preview = AiluaThemeImporter().parse(source)
        assertEquals("milk", preview.theme.basePresetId)
        assertEquals("sakura", preview.theme.preferredPaletteId)
        assertEquals(1, preview.theme.wallpapers.size)
    }

    @Test fun unsupportedSchemaAndMissingThemeJsonAreRejected() {
        val unsupported = zip("manifest.json" to """{"schema":2}""".toByteArray(), "theme.json" to "{}".toByteArray())
        val incomplete = zip("manifest.json" to """{"schema":1}""".toByteArray())
        assertRejects { UnifiedThemeImporter.inspect(ImportedThemeSource("future.ailuatheme", unsupported)) }
        assertRejects { AiluaThemeImporter().parse(ImportedThemeSource("missing.ailuatheme", incomplete)) }
        assertRejects { UnifiedThemeImporter.inspect(ImportedThemeSource("broken.ailuatheme", byteArrayOf(0x50, 0x4b, 0x03, 0x04))) }
    }

    @Test fun detectionUsesStructureBeforeExtension() {
        val miui = zip("description.xml" to "<theme/>".toByteArray())
        val colorOs = zip("themeInfo.xml" to "<OppoSmartPhoneThemeInfo/>".toByteArray())
        assertEquals(ExternalThemeFormat.MIUI_MTZ, detectThemeFormat("wrong.theme", miui, SafeThemeArchive(miui).listEntries()))
        assertEquals(ExternalThemeFormat.COLOROS_THEME, detectThemeFormat("wrong.mtz", colorOs, SafeThemeArchive(colorOs).listEntries()))
        assertEquals(null, detectThemeFormat("x.ailuatheme", byteArrayOf(1, 2, 3)))
    }

    private fun assertRejects(action: () -> Unit) {
        var rejected = false
        try { action() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { archive ->
            entries.forEach { (name, bytes) ->
                archive.putNextEntry(ZipEntry(name))
                archive.write(bytes)
                archive.closeEntry()
            }
        }
        output.toByteArray()
    }

    companion object {
        private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        private val WEBP = "RIFF1234WEBP".toByteArray()
        private val JPEG = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xff.toByte())
    }
}