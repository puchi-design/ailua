package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.systemui.control.ControlCenterStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ControlCenterStoreTest {
    @Test
    fun virtualControlsPersistAndBrightnessCannotDimPastTheAllowedRange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(ControlCenterStore.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        ControlCenterStore.initialize(context)
        ControlCenterStore.setFocusMode(true)
        ControlCenterStore.setQuietMode(true)
        ControlCenterStore.setVirtualBrightness(0.1f)
        val restored = ControlCenterStore.read(prefs)
        assertTrue(restored.focusMode)
        assertTrue(restored.quietMode)
        assertEquals(0.5f, restored.virtualBrightness, 0f)
        assertEquals(0.25f, restored.virtualDimAlpha, 0f)
        ControlCenterStore.setVirtualBrightness(Float.NaN)
        assertEquals(1f, ControlCenterStore.read(prefs).virtualBrightness, 0f)
        assertEquals(0f, ControlCenterStore.state.value.virtualDimAlpha, 0f)
    }
}
