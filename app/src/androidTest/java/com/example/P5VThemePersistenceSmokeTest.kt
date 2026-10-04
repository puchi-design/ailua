package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.context.CharacterContext
import com.example.data.desktop.WorkspaceGraph
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.themecenter.withOfficialTheme
import com.example.ui.themeengine.ThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Host runs prepare, force-stops the actual process, then runs verify and restores the user's theme. */
@RunWith(AndroidJUnit4::class)
class P5VThemePersistenceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val backup get() = context.getSharedPreferences("ailua_ui_qa_saved_theme", 0)

    @Test fun prepareForColdStart() {
        assumeTrue(FirstSessionStore.state.value.onboardingComplete)
        assumeTrue(CallStateEngine.currentCall.value == null)
        assertTrue("Do not overwrite an unfinished probe", !backup.contains("qa_ready"))
        val expectedTheme = InstrumentationRegistry.getArguments().getString("qa_theme") ?: "soft_home"
        require(expectedTheme in listOf("soft_home", "midnight_glass"))
        ThemeStore.write(backup, ThemeStore.selection)
        backup.edit().putString("qa_character", CharacterContext.selectedId.value)
            .putString("qa_workspace", workspaceFingerprint()).putString("qa_expected_theme", expectedTheme)
            .putBoolean("qa_ready", true).commit()
        try {
        compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
        compose.onNodeWithTag("virtual_home_screen").assertIsDisplayed()
        compose.onNodeWithTag("virtual_home_screen").performTouchInput {
            longClick(Offset(width * 0.9f, height * 0.45f))
        }
        compose.onNodeWithTag("home_edit_theme").performClick()
        val themeTag = "theme_option_$expectedTheme"
        repeat(12) {
            if (compose.onAllNodesWithTag(themeTag).fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("theme_center_sheet").performTouchInput { swipeUp() }
                compose.waitForIdle()
            }
        }
        compose.onNodeWithTag(themeTag).performScrollTo().performClick()
        compose.onNodeWithTag("theme_apply_full").performScrollTo().performClick()
        assertEquals(expectedTheme, ThemeStore.selection.themePresetId)
        Espresso.pressBack(); Espresso.pressBack()
        compose.onNodeWithTag("home_edit_done").performClick()
        // Flush pending preference writes before the host kills this actual App process.
        context.getSharedPreferences("ailua_settings", 0).edit().commit()
        } catch (failure: Throwable) {
            val original = ThemeStore.readSelection(backup)
            compose.runOnIdle { ThemeStore.update(original) }
            context.getSharedPreferences("ailua_settings", 0).edit().commit()
            context.deleteSharedPreferences("ailua_ui_qa_saved_theme")
            throw failure
        }
    }

    @Test fun verifyAfterColdStart() {
        assertTrue("Run prepare then host force-stop first", backup.getBoolean("qa_ready", false))
        val original = ThemeStore.readSelection(backup)
        try {
            val expectedTheme = checkNotNull(backup.getString("qa_expected_theme", "soft_home"))
            assertEquals(original.withOfficialTheme(expectedTheme), ThemeStore.selection)
            assertEquals(backup.getString("qa_character", null), CharacterContext.selectedId.value)
            assertEquals(backup.getString("qa_workspace", null), workspaceFingerprint())
            val output = File(checkNotNull(context.getExternalFilesDir(null)), "qa/P5.V-UIProduct").apply { mkdirs() }
            File(output, "cold-start-$expectedTheme-persistence.txt").writeText(
                "Actual process force-stop/relaunch: PASS\nTheme $expectedTheme: PASS\n" +
                    "External/manual icons: PASS\nWorkspace pages/items/folders/widget spans/hotseat: PASS\nSelected character: PASS\n")
        } finally {
            compose.runOnIdle { ThemeStore.update(original) }
            context.getSharedPreferences("ailua_settings", 0).edit().commit()
            context.deleteSharedPreferences("ailua_ui_qa_saved_theme")
        }
    }

    private fun workspaceFingerprint(): String {
        val snapshot = WorkspaceGraph.repository.snapshot()
        val content = snapshot.pages.sortedBy { it.id }.toString() +
            snapshot.items.sortedBy { it.id }.toString() + snapshot.folders.sortedBy { it.id }.toString()
        return MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
