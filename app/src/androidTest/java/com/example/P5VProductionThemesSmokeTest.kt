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
import androidx.compose.ui.test.swipeUp
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

/** Existing phone, production picker, exact layout comparisons, no AI calls or fixtures. */
@RunWith(AndroidJUnit4::class)
class P5VProductionThemesSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-UIProduct"
    ).apply { mkdirs() }

    @Test
    fun sixMaterialsAnd108IconsOnTheSameExistingWorkspace() {
        assumeTrue("Already onboarded device required", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Keep existing calls intact", CallStateEngine.currentCall.value == null)
        val original = ThemeStore.selection
        val before = WorkspaceGraph.repository.snapshot()
        compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
        if (!visible("virtual_home_screen") && visible("virtual_phone_home_indicator")) {
            compose.onNodeWithTag("virtual_phone_home_indicator").performClick()
        }
        compose.waitUntil(10_000) { visible("virtual_home_screen") }
        verifyBitmaps()
        try {
            // Official samples temporarily remove mixes; all original overrides return in finally.
            compose.runOnIdle { ThemeStore.update(original.copy(
                paletteOverrideId = null, wallpaperOverrideId = null, wallpaperSourceId = null,
                iconStyleOverrideId = null, iconSourceOverrideId = null, manualIconOverrides = emptyMap(),
            )) }
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
            for ((id, file) in listOf(
                "default" to "01-default", "soft_home" to "02-soft",
                "rainy_study" to "03-rainy", "sakura_diary" to "04-sakura",
                "y2k_love" to "05-y2k", "midnight_glass" to "06-midnight",
            )) {
                compose.onNodeWithTag("virtual_home_screen").performTouchInput {
                    longClick(Offset(width * 0.90f, height * 0.45f))
                }
                compose.onNodeWithTag("home_edit_theme").performClick()
                scrollToTheme(id)
                compose.onNodeWithTag("theme_option_$id").performClick()
                // The consumer detail flow and the original picker share the same app entry.
                if (compose.onAllNodesWithTag("theme_apply_full").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag("theme_apply_full").performScrollTo().performClick()
                }
                assertEquals(id, ThemeStore.selection.themePresetId)
                if (compose.onAllNodesWithTag("theme_detail").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag("theme_detail_back").performScrollTo().performClick()
                }
                androidx.test.espresso.Espresso.pressBack()
                compose.onNodeWithTag("home_edit_done").performClick()
                compose.onNodeWithTag("virtual_home_screen").assertIsDisplayed()
                screenshot("$file.png")
                before.folders.firstOrNull { visible("workspace_folder_${it.id}") }?.let { folder ->
                    compose.onNodeWithTag("workspace_folder_${folder.id}").performClick()
                    compose.onNodeWithTag("folder_overlay").assertIsDisplayed()
                    screenshot("$file-folder.png")
                    androidx.test.espresso.Espresso.pressBack()
                }
                if (id == "soft_home" || id == "midnight_glass") observePerformance(id)
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

    private fun scrollToTheme(id: String) {
        val tag = "theme_option_$id"
        // Large real previews can put a card outside the semantics viewport; use actual swipes.
        repeat(12) {
            if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag(tag).performScrollTo()
                return
            }
            compose.onNodeWithTag("theme_center_sheet").performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun verifyBitmaps() {
        val resolver = IconResolver.get(instrumentation.targetContext)
        assertEquals(6, BundledIconCatalog.packs.size)
        assertEquals(18, BundledIconCatalog.iconKeys.size)
        for (pack in BundledIconCatalog.packs) for (key in BundledIconCatalog.iconKeys) {
            val resource = checkNotNull(BundledIconCatalog.resource(pack.iconStyleId, key))
            val bitmap = resolver.loadAsset(resource.asset, 64)
            assertNotNull("Decode ${resource.drawableName}", bitmap)
            assertEquals(64, bitmap!!.width)
            assertEquals(64, bitmap.height)
            val pixels = IntArray(4096)
            bitmap.getPixels(pixels, 0, 64, 0, 0, 64, 64)
            assertTrue("Nonempty ${resource.drawableName}", pixels.toSet().size > 8)
            assertTrue("Opaque ${resource.drawableName}", pixels.all { (it ushr 24) == 255 })
        }
    }

    private fun visible(tag: String) = compose.onAllNodesWithTag(tag)
        .fetchSemanticsNodes().indices.any { compose.onAllNodesWithTag(tag)[it].isDisplayed() }

    private fun observePerformance(id: String) {
        val packageName = instrumentation.targetContext.packageName
        fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText() }
        }
        shell("dumpsys gfxinfo $packageName reset")
        val started = SystemClock.elapsedRealtime()
        repeat(3) {
            compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeLeft() }
            compose.waitForIdle()
            compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeRight() }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("living_character_widget").assertIsDisplayed()
        File(outputDir, "performance-$id.txt").writeText(
            "API29 fallback; six real page gestures completed in ${SystemClock.elapsedRealtime() - started} ms.\n" +
                "Instrumentation observation only, not a standalone performance benchmark or API31 blur validation.\n" +
                shell("dumpsys gfxinfo $packageName"))
    }

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
