package com.example.ui.themeengine.icon

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ui.themeengine.IconShapeSpec
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.external.ThemeAssetRef
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.DataInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundledIconBitmapTest {
    @Test
    fun allRegisteredAssetsHaveWebpHeadersAndResolveAtRequestedSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = IconResolver.get(context)
        for (pack in BundledIconCatalog.packs) for (key in BundledIconCatalog.iconKeys) {
            val resource = BundledIconCatalog.resource(pack.iconStyleId, key)!!
            val id = context.resources.getIdentifier(resource.drawableName, "drawable", context.packageName)
            assertNotNull(resource.drawableName, id.takeIf { it != 0 })
            // Legacy Robolectric can simulate unsupported formats, so also verify the packaged bytes.
            val header = ByteArray(12)
            DataInputStream(context.resources.openRawResource(id)).use { it.readFully(header) }
            assertArrayEquals("RIFF".toByteArray(Charsets.US_ASCII), header.copyOfRange(0, 4))
            assertArrayEquals("WEBP".toByteArray(Charsets.US_ASCII), header.copyOfRange(8, 12))
            val bitmap = resolver.loadAsset(resource.asset, 64)
            assertNotNull(resource.drawableName, bitmap)
            assertEquals(64, bitmap!!.width)
            assertEquals(64, bitmap.height)
            assertSame(bitmap, resolver.loadAsset(resource.asset, 64))
        }
    }

    @Test
    fun realResolverUsesAliasesAndFallsBackFromMissingExternalPack() {
        val resolver = IconResolver.get(ApplicationProvider.getApplicationContext())
        val selected = ThemeSelection("soft_home", iconSourceOverrideId = "android:missing.icon.pack")
        val expected = resolver.loadAsset(BundledIconCatalog.resource("soft_home_icons", "chat")!!.asset, 56)
        assertNotNull(expected)
        assertSame(expected, resolver.resolveBitmap("messages", selected, 56))
    }

    @Test
    fun pixelSamplingMetadataFollowsTheActualResolvedOfficialAsset() {
        val resolver = IconResolver.get(ApplicationProvider.getApplicationContext())
        val y2k = ThemeSelection("default", iconStyleOverrideId = "y2k_icons")
        assertEquals(true, resolver.resolveIcon("chat", y2k, 56)?.isPixelArt)
        assertEquals(IconShapeSpec.NONE, resolver.resolveIcon("chat", y2k, 56)?.maskShape)
        assertEquals(true, resolver.resolveIcon("chat",
            y2k.copy(iconSourceOverrideId = "android:missing.icon.pack"), 56)?.isPixelArt)
        for (style in listOf("default_icons", "soft_home_icons", "midnight_icons")) {
            val resolved = resolver.resolveIcon("chat", ThemeSelection("default", iconStyleOverrideId = style), 56)
            assertEquals(false, resolved?.isPixelArt)
            assertEquals(IconShapeSpec.SQUIRCLE, resolved?.maskShape)
        }
    }

    @Test
    fun missingPixelAssetUsesDefaultMaskWhileExternalSuccessKeepsItsOwnSilhouette() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val selection = ThemeSelection("default", iconStyleOverrideId = "y2k_icons",
            iconSourceOverrideId = "theme:test-external")
        val fallback = resolveIconWithFallback(
            "chat", selection,
            loadManual = { _, _, _ -> null },
            loadExternal = { _, _ -> null },
            loadBundled = { resource ->
                if (resource.isPixelArt) null else ResolvedIconBitmap.bundled(bitmap, resource)
            },
        )
        assertNotNull(fallback)
        assertEquals(false, fallback!!.isPixelArt)
        assertEquals(IconShapeSpec.SQUIRCLE, fallback.maskShape)

        val originalArtwork = ResolvedIconBitmap(bitmap)
        for (manual in listOf(emptyMap(), mapOf("chat" to "chosen"))) {
            val external = resolveIconWithFallback(
                "chat", selection.copy(manualIconOverrides = manual),
                loadManual = { _, _, _ -> originalArtwork },
                loadExternal = { _, _ -> originalArtwork },
                loadBundled = { error("External artwork must retain priority") },
            )
            assertSame(originalArtwork, external)
            assertNull(external!!.maskShape)
            assertEquals(false, external.isPixelArt)
        }
    }

    @Test
    fun unsupportedAppsLegacyStylesInvalidKeysAndInvalidSizesReturnVectorFallback() {
        val resolver = IconResolver.get(ApplicationProvider.getApplicationContext())
        assertNull(resolver.resolveBitmap("dream", ThemeSelection("default"), 56))
        assertNull(resolver.resolveBitmap("chat", ThemeSelection("milk"), 56))
        assertNull(resolver.resolveBitmap("chat", ThemeSelection("default", iconStyleOverrideId = "mono"), 56))
        assertNull(resolver.resolveBitmap("chat", ThemeSelection("default"), 0))
        assertNull(resolver.resolveBitmap("chat", ThemeSelection("default"), 1025))
        assertNull(resolver.loadAsset(ThemeAssetRef.BuiltIn("ti_default_missing"), 56))
    }
}
