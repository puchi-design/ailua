package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.data.desktop.WorkspaceGraph
import com.example.data.desktop.WidgetPlacement
import com.example.data.model.CallState
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.themeengine.ThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real device visual check: production theme picker, original selection restored, no fixtures. */
@RunWith(AndroidJUnit4::class)
class P5VThemeRescueSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-UI-Theme"
    ).apply { mkdirs() }

    @Test
    fun threeThemesOnTheSameExistingWorkspace() {
        assumeTrue("Requires an already onboarded device", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Keep existing incoming calls intact", CallStateEngine.currentCall.value?.state != CallState.INCOMING)
        compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
        if (!visible("virtual_home_screen") && visible("virtual_phone_home_indicator")) {
            compose.onNodeWithTag("virtual_phone_home_indicator").performClick()
        }
        compose.waitUntil(10_000) { visible("virtual_home_screen") }
        val original = ThemeStore.selection
        val appearance = instrumentation.targetContext.getSharedPreferences("ailua_settings", 0)
        val originalDark = appearance.getBoolean("dark_theme", false)
        var toggledDark = false
        try {
            // Samples show each theme's own assets; restore user mixing choices in finally.
            compose.runOnIdle { ThemeStore.update(original.copy(
                paletteOverrideId = null, wallpaperOverrideId = null, wallpaperSourceId = null,
                iconStyleOverrideId = null, iconSourceOverrideId = null, manualIconOverrides = emptyMap(),
            )) }
            // Retain the user's page model. Find the existing widget page only through swipes.
            repeat(4) {
                if (!visible("living_character_widget")) {
                    compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeLeft() }
                    compose.waitForIdle()
                }
            }
            if (!visible("living_character_widget")) {
                repeat(4) {
                    if (!visible("living_character_widget")) {
                        compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeRight() }
                        compose.waitForIdle()
                    }
                }
            }
            for ((id, file) in listOf(
                "default" to "01-default-home.png",
                "soft_home" to "02-soft-home.png",
                "midnight_glass" to "09-midnight-glass.png",
                "glass" to "10-glass-border-fix.png",
            )) {
                // Long press without a move enters editing; it cannot commit a layout change.
                compose.onNodeWithTag("virtual_home_screen").performTouchInput {
                    longClick(Offset(width * 0.90f, height * 0.45f))
                }
                compose.onNodeWithTag("home_edit_theme").performClick()
                compose.onNodeWithTag("theme_option_$id").performScrollTo().performClick()
                compose.waitForIdle()
                assertEquals(id, ThemeStore.selection.themePresetId)
                if (id == "default") screenshot("05-theme-center.png")
                androidx.test.espresso.Espresso.pressBack()
                compose.onNodeWithTag("home_edit_done").performClick()
                compose.onNodeWithTag("virtual_home_screen").assertIsDisplayed()
                screenshot(file)
                if (id == "default") verifyExistingFolderAndWidget()
            }
            if (!originalDark) {
                compose.runOnIdle { VirtualSystemUiSession.controller.openControlCenter() }
                compose.onNodeWithTag("control_dark").performClick()
                toggledDark = true
                compose.onNodeWithTag("control_close").performClick()
            }
            // Use the same production picker after dark mode instead of bypassing its transitions.
            compose.onNodeWithTag("virtual_home_screen").performTouchInput {
                longClick(Offset(width * 0.90f, height * 0.45f))
            }
            compose.onNodeWithTag("home_edit_theme").performClick()
            compose.onNodeWithTag("theme_option_default").performScrollTo().performClick()
            androidx.test.espresso.Espresso.pressBack()
            compose.onNodeWithTag("home_edit_done").performClick()
            screenshot("11-default-dark.png")
            repeat(4) {
                if (!visible("life_bento_page")) {
                    compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeLeft() }
                    compose.waitForIdle()
                }
            }
            if (visible("life_bento_page")) screenshot("12-life-bento.png")
        } finally {
            if (toggledDark) {
                compose.runOnIdle { VirtualSystemUiSession.controller.openControlCenter() }
                compose.onNodeWithTag("control_dark").performClick()
                compose.onNodeWithTag("control_close").performClick()
            }
            compose.runOnIdle { ThemeStore.update(original) }
        }
    }

    /** Exercise controls affected by backing-layer changes; restore size through the same UI. */
    private fun verifyExistingFolderAndWidget() {
        val before = WorkspaceGraph.repository.snapshot()
        val folder = before.folders.firstOrNull { visible("workspace_folder_${it.id}") }
        if (folder != null) {
            compose.onNodeWithTag("workspace_folder_${folder.id}").performClick()
            compose.onNodeWithTag("folder_overlay").assertIsDisplayed()
            screenshot("13-folder-open.png")
            androidx.test.espresso.Espresso.pressBack()
        }
        val widget = before.items.firstOrNull {
            it.sourceId == "character_living" && visible("workspace_widget_${it.id}")
        } ?: return
        // A full-width 4x2 instance at its original origin can shrink without moving neighbours.
        if (widget.spanX != 4 || widget.spanY != 2) return
        compose.onNodeWithTag("virtual_home_screen").performTouchInput {
            longClick(Offset(width * 0.90f, height * 0.45f))
        }
        compose.onNodeWithTag("workspace_widget_${widget.id}").performClick()
        val sizes = WidgetPlacement.supportedSizes.getValue(widget.sourceId)
        var currentIndex = sizes.indexOf(widget.spanX to widget.spanY)
        try {
            repeat(sizes.size) {
                currentIndex = (currentIndex + 1) % sizes.size
                val expected = sizes[currentIndex]
                compose.onNodeWithTag("widget_resize").assertIsDisplayed().performClick()
                compose.waitUntil(5_000) {
                    WorkspaceGraph.repository.snapshot().items.single { it.id == widget.id }
                        .let { it.spanX == expected.first && it.spanY == expected.second }
                }
                if (expected == (4 to 1)) screenshot("14-widget-compact.png")
                if (expected == (4 to 2)) screenshot("15-widget-resize-restored.png")
            }
        } finally {
            // If an assertion failed, attempt the remaining production resize steps only.
            repeat(sizes.size) {
                val actual = WorkspaceGraph.repository.snapshot().items.single { it.id == widget.id }
                if ((actual.spanX != widget.spanX || actual.spanY != widget.spanY) && visible("widget_resize")) {
                    compose.onNodeWithTag("widget_resize").performClick()
                    compose.waitForIdle()
                }
            }
            if (visible("home_edit_done")) compose.onNodeWithTag("home_edit_done").performClick()
        }
        val after = WorkspaceGraph.repository.snapshot()
        assertEquals(before.pages, after.pages)
        assertEquals(before.items, after.items)
        assertEquals(before.folders, after.folders)
    }

    private fun visible(tag: String) = compose.onAllNodesWithTag(tag)
        .fetchSemanticsNodes().indices.any { compose.onAllNodesWithTag(tag)[it].isDisplayed() }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(1_000) // Bounded asynchronous wallpaper decode and sheet transitions.
        // IO can finish while the test frame clock is idle. Flush that recomposition before capture.
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try { File(outputDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
}
