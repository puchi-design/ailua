package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.desktop.WorkspaceGraph
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.themeengine.ThemeStore
import com.example.ui.themeengine.icon.BundledIconCatalog
import com.example.ui.themeengine.icon.IconResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in visual acceptance on an existing phone: no seeding, provider calls, or layout writes. */
@RunWith(AndroidJUnit4::class)
class P5VProductionIconsSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-UIR3"
    ).apply { mkdirs() }

    @Test
    fun fourIconLanguagesOnTheSameFrozenWorkspace() {
        assumeTrue("Requires an already onboarded device", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Do not interrupt an existing call", CallStateEngine.currentCall.value == null)
        val original = ThemeStore.selection
        val before = WorkspaceGraph.repository.snapshot()
        verifyPackagedBitmaps()
        compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
        if (!visible("virtual_home_screen") && visible("virtual_phone_home_indicator")) {
            compose.onNodeWithTag("virtual_phone_home_indicator").performClick()
        }
        compose.waitUntil(10_000) { visible("virtual_home_screen") }
        try {
            compose.runOnIdle { ThemeStore.update(original.copy(
                paletteOverrideId = null, wallpaperOverrideId = null, wallpaperSourceId = null,
                iconStyleOverrideId = null, iconSourceOverrideId = null, manualIconOverrides = emptyMap(),
            )) }
            // Locate the existing widget page only through production swipes.
            repeat(4) {
                if (!visible("living_character_widget")) {
                    compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeLeft() }
                    compose.waitForIdle()
                }
            }
            repeat(4) {
                if (!visible("living_character_widget")) {
                    compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeRight() }
                    compose.waitForIdle()
                }
            }
            compose.onNodeWithTag("living_character_widget").assertIsDisplayed()
            for ((theme, file) in listOf(
                "default" to "01-default-icons.png",
                "soft_home" to "02-soft-icons.png",
                "midnight_glass" to "03-midnight-icons.png",
                "default" to "04-y2k-prototype.png",
            )) {
                openThemePicker()
                compose.onNodeWithTag("theme_option_$theme").performScrollTo().performClick()
                if (file == "04-y2k-prototype.png") {
                    compose.onNodeWithTag("theme_center_tab_icons").performClick()
                    compose.onNodeWithTag("icon_option_y2k_icons").performScrollTo().performClick()
                    assertEquals("y2k_icons", ThemeStore.selection.iconStyleOverrideId)
                    screenshot("05-y2k-icon-picker.png")
                }
                androidx.test.espresso.Espresso.pressBack()
                compose.onNodeWithTag("home_edit_done").performClick()
                assertEquals(theme, ThemeStore.selection.themePresetId)
                compose.onNodeWithTag("virtual_home_screen").assertIsDisplayed()
                screenshot(file)
                val folder = before.folders.firstOrNull { visible("workspace_folder_${it.id}") }
                if (folder != null) {
                    compose.onNodeWithTag("workspace_folder_${folder.id}").performClick()
                    compose.onNodeWithTag("folder_overlay").assertIsDisplayed()
                    screenshot(file.removeSuffix(".png") + "-folder.png")
                    androidx.test.espresso.Espresso.pressBack()
                }
                val after = WorkspaceGraph.repository.snapshot()
                assertEquals(before.pages, after.pages)
                assertEquals(before.items, after.items)
                assertEquals(before.folders, after.folders)
            }
        } finally {
            compose.runOnIdle { ThemeStore.update(original) }
        }
        assertEquals(original, ThemeStore.selection)
    }

    private fun verifyPackagedBitmaps() {
        val resolver = IconResolver.get(instrumentation.targetContext)
        for (pack in BundledIconCatalog.packs) for (key in BundledIconCatalog.iconKeys) {
            val resource = checkNotNull(BundledIconCatalog.resource(pack.iconStyleId, key))
            val bitmap = resolver.loadAsset(resource.asset, 64)
            assertNotNull("Android must decode ${resource.drawableName}", bitmap)
            assertEquals(64, bitmap!!.width)
            assertEquals(64, bitmap.height)
            val pixels = IntArray(64 * 64)
            bitmap.getPixels(pixels, 0, 64, 0, 0, 64, 64)
            assertTrue("Icon is blank: ${resource.drawableName}", pixels.toSet().size > 8)
            assertTrue("Icon must remain opaque: ${resource.drawableName}",
                pixels.all { (it ushr 24) == 255 })
        }
    }

    private fun openThemePicker() {
        compose.onNodeWithTag("virtual_home_screen").performTouchInput {
            longClick(Offset(width * 0.90f, height * 0.45f))
        }
        compose.onNodeWithTag("home_edit_theme").performClick()
    }

    private fun visible(tag: String) = compose.onAllNodesWithTag(tag)
        .fetchSemanticsNodes().indices.any { compose.onAllNodesWithTag(tag)[it].isDisplayed() }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(1_000)
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try { File(outputDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
}
