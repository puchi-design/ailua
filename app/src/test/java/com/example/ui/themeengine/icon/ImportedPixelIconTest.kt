package com.example.ui.themeengine.icon

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ui.themeengine.IconSource
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.external.ExternalIconEntry
import com.example.ui.themeengine.external.ExternalIconSet
import com.example.ui.themeengine.external.ExternalThemeFormat
import com.example.ui.themeengine.external.ExternalThemePackage
import com.example.ui.themeengine.external.ExternalThemeRepository
import com.example.ui.themeengine.external.ThemeAssetRef
import com.example.ui.themeengine.external.ThemeImportPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportedPixelIconTest {
    @Test
    fun exportedY2kUsesNearestForAutomaticAndManualIconsIndependentOfCurrentSkin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ExternalThemeRepository.initialize(context)
        val resolver = IconResolver.get(context)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val png = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        bitmap.recycle()
        val cases = listOf(
            Triple(ExternalThemeFormat.AILUA, "y2k_icons", true),
            Triple(ExternalThemeFormat.MIUI_MTZ, "y2k_icons", false),
            Triple(ExternalThemeFormat.COLOROS_THEME, "y2k_icons", false),
            Triple(ExternalThemeFormat.ANDROID_ICON_PACK, "y2k_icons", false),
            Triple(ExternalThemeFormat.AILUA, "mono", false),
            Triple(ExternalThemeFormat.AILUA, "unknown-style", false),
            Triple(ExternalThemeFormat.AILUA, "", false),
        )
        for ((index, case) in cases.withIndex()) {
            val (format, style, pixelArt) = case
            val id = "pixel-source-test-$index"
            val ref = ThemeAssetRef.LocalFile("$id/icons/chat.png")
            val packageTheme = ExternalThemePackage(id, format, "Test",
                icons = ExternalIconSet("$id/icons", "Test", IconSource.ThemePackage(id),
                    mapOf("chat" to ref), listOf(ExternalIconEntry("chat", null, ref))),
                metadata = mapOf("iconStyle" to style))
            ExternalThemeRepository.install(ThemeImportPreview(packageTheme, mapOf(ref.relativePath to png)))
            try {
                for (preset in listOf("default", "y2k_love", "glass")) {
                    for (manual in listOf(emptyMap(), mapOf("chat" to "chat"))) {
                        val selected = ThemeSelection(preset, iconSourceOverrideId = "theme:$id",
                            manualIconOverrides = manual)
                        val resolved = resolver.resolveIcon("chat", selected, 56)
                        assertNotNull("$format/$style/$preset must resolve the imported asset", resolved)
                        assertEquals(pixelArt, resolved!!.isPixelArt)
                        assertNull("Imported artwork keeps its own silhouette", resolved.maskShape)
                        assertEquals(56, resolved.bitmap.width)
                        assertEquals("Actual bitmap scaling uses the source's filter policy", !pixelArt,
                            Shadows.shadowOf(resolved.bitmap).createdFromFilter)
                    }
                }
            } finally { ExternalThemeRepository.delete(id) }
        }
    }
}
