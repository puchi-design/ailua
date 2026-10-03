package com.example.ui.themeengine.material

import androidx.compose.ui.graphics.Color
import com.example.ui.themeengine.BorderSpec
import com.example.ui.themeengine.ShadowSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class AiluaMaterialRendererTest {
    private fun material(glass: Boolean = true, transparent: Boolean = false) = AiluaSurfaceMaterial(
        color = Color.White,
        surfaceAlpha = 0.14f,
        glass = glass,
        transparent = transparent,
        cornerRadiusDp = 30f,
        border = BorderSpec(Color.Transparent, 0f),
        shadow = ShadowSpec(0f),
        blurRadiusDp = 26f,
        highlightAlpha = 0.18f,
        fallbackAlpha = 0.20f,
    )

    @Test
    fun glassKeepsItsAuthoredLowAlphaOnBlurCapableDevices() {
        assertEquals(0.14f, material().tintAlpha(backdropAvailable = true), 0f)
    }

    @Test
    fun oldAndroidUsesTheFallbackTintWithoutAnOpaqueMinimum() {
        assertEquals(0.20f, material().tintAlpha(backdropAvailable = false), 0f)
    }

    @Test
    fun solidMaterialDoesNotUseGlassFallbackAlpha() {
        val card = material(glass = false).copy(surfaceAlpha = 1f)
        assertEquals(1f, card.tintAlpha(backdropAvailable = false), 0f)
    }

    @Test
    fun transparentMaterialStaysTransparentOnBothPaths() {
        val clear = material(transparent = true)
        assertEquals(0f, clear.tintAlpha(backdropAvailable = false), 0f)
        assertEquals(0f, clear.tintAlpha(backdropAvailable = true), 0f)
    }

    @Test
    fun malformedImportedAlphaHasABoundedFiniteFallback() {
        assertEquals(0.14f, material().copy(fallbackAlpha = Float.NaN).tintAlpha(false), 0f)
        assertEquals(1f, materialAlpha(7f), 0f)
        assertEquals(0f, materialAlpha(-1f), 0f)
        assertEquals(0f, materialAlpha(Float.POSITIVE_INFINITY), 0f)
    }
}
