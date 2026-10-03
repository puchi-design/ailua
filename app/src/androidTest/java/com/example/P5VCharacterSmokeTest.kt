package com.example

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.context.CharacterContext
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.OfficialCharacters
import com.example.data.registry.CharacterRegistry
import com.example.ui.launcher.LauncherAppCatalog
import com.example.ui.systemui.VirtualSystemUiSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Run explicitly on an already onboarded device. All screens use MainActivity's real
 * navigation. No AI request, call, user-card write, Welcome reset or data clear.
 * Official source cards are exported only to the QA artifacts directory for review.
 * The optional runner argument expectedCharacterId can assert an upgrade's known old
 * selection. Otherwise the persisted ID is captured before the Activity rule launches.
 * Only the current companion is temporarily changed, using the real Profile button,
 * and restored in finally. The edited Creator template remains an unsaved UI draft.
 */
@RunWith(AndroidJUnit4::class)
class P5VCharacterSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val expectedSelectionBeforeLaunch = InstrumentationRegistry.getArguments().getString("expectedCharacterId")
        ?: instrumentation.targetContext.getSharedPreferences("ailua_character_context", Context.MODE_PRIVATE)
            .getString("selected_id", null)

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-CHAR"
    ).apply { mkdirs() }

    @Test
    fun creatorModesOfficialProfilesAndPreservedSelection() {
        assumeTrue("Complete Welcome before this opt-in smoke", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Keep any existing call untouched", CallStateEngine.currentCall.value == null)
        val originalSelectedId = CharacterContext.currentId()
        val originalCustomCards = AiluaLocalStore.customCards.value.toList()
        try {
            // Use authored sources, never a user's same-ID override from the registry.
            OfficialCharacters.cards.forEach { card ->
                File(outputDir, "official-${card.data.id}.json")
                    .writeText(CharacterCardJsonCodec.encode(card), Charsets.UTF_8)
            }
            expectedSelectionBeforeLaunch?.takeIf { it.isNotBlank() }?.let { expected ->
                assertEquals("Startup must preserve the previously selected companion", expected, originalSelectedId)
            }
            record("selection before", originalSelectedId)
            compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
            returnHomeIfAvailable()
            waitForTag("virtual_home_screen")

            // Settings is opened from the real drawer; its Creator entry defaults to simple mode.
            openLibrary()
            compose.onNodeWithTag("settings_entry").performClick()
            waitForTag("settings_screen")
            compose.onNodeWithText("角色工坊", substring = false).performScrollTo().performClick()
            waitForTag("character_creator_screen")
            compose.onNodeWithText("创建角色", substring = false).assertIsDisplayed()
            compose.onNodeWithTag("creator_photo_btn").assertIsDisplayed()
            assertSimpleModeHidesTechnicalFields()
            screenshot("01-creator-simple.png")

            compose.onNodeWithTag("creator_advanced_mode").performClick()
            compose.onNodeWithTag("creator_saf_import_btn").assertIsDisplayed()
            compose.onNodeWithText("System Prompt", substring = false).assertExists()
            compose.onNodeWithText("Post History Instructions", substring = false).assertExists()
            screenshot("02-creator-advanced.png")

            // Load a whole existing template without saving it, then cross both UI modes.
            val yan = CharacterRegistry.getCharacter("yan")
            val yanCardBefore = checkNotNull(CharacterRegistry.getCard("yan"))
            compose.onNodeWithTag("creator_template_import_btn").performClick()
            compose.onNodeWithText(yanCardBefore.data.name, substring = false).performScrollTo().performClick()
            waitForTag("character_creator_screen")
            val editedName = "${yan.name} · 未保存预览"
            compose.onNode(hasSetTextAction() and hasText("Name", substring = false))
                .performScrollTo().performTextReplacement(editedName)
            closeSoftKeyboard()
            creatorScrollToTop()
            // SAF can recreate the host Activity while the user chooses a file.
            // Keep a real unsaved template and mode across that same lifecycle edge.
            compose.activityRule.scenario.recreate()
            waitForTag("character_creator_screen")
            compose.onNodeWithTag("creator_saf_import_btn").assertIsDisplayed()
            compose.onNodeWithText(editedName, substring = false).assertExists()
            compose.onNodeWithTag("creator_simple_mode").performClick()
            assertSimpleModeHidesTechnicalFields()
            compose.onNodeWithText(editedName, substring = false).assertExists()
            creatorScrollToTop()
            compose.onNodeWithTag("creator_advanced_mode").performClick()
            // Raw JSON preview is local and does not launch SAF or write a character card.
            compose.onNodeWithTag("creator_advanced_actions")
                .performScrollToNode(hasTestTag("creator_preview_code_btn"))
            compose.onNodeWithTag("creator_preview_code_btn").performClick()
            compose.onNodeWithText("Character Card V2 JSON", substring = false).assertIsDisplayed()
            compose.onNodeWithText("\"name\": \"$editedName\"", substring = true).assertExists()
            screenshot("03-creator-json-preview.png")
            compose.onNodeWithText("完成", substring = false).performClick()
            assertEquals("Editing a template must remain an unsaved draft", yanCardBefore, CharacterRegistry.getCard("yan"))
            assertEquals("Creator must not change the current companion", originalSelectedId, CharacterContext.currentId())
            goHome()

            listOf("yan", "yeo", "noa").forEachIndexed { index, id ->
                openContactProfile(id)
                val profile = CharacterRegistry.getCharacter(id)
                compose.onNodeWithText(profile.name, substring = false).assertIsDisplayed()
                assertEquals("Opening a profile must not select it", originalSelectedId, CharacterContext.currentId())
                screenshot("0${index + 4}-profile-$id.png")
                goHome()
            }

            openContactProfile("yan")
            if (CharacterContext.currentId() != "yan") {
                compose.onNodeWithTag("profile_select_character").performScrollTo().performClick()
                compose.waitUntil(10_000) { CharacterContext.currentId() == "yan" }
            }
            goHome()
            assertEquals("Home must consume the companion selected in Profile", "yan", CharacterContext.currentId())
            if (hasTag("living_character_widget")) {
                compose.onNode(hasText(yan.name, substring = false) and hasAnyAncestor(hasTestTag("living_character_widget")))
                    .assertIsDisplayed()
            }
            screenshot("07-home-yan.png")
            assertEquals("Smoke must not modify the user's character cards", originalCustomCards, AiluaLocalStore.customCards.value)
            record("result", "PASS: simple/advanced, unsaved template after Activity recreation, JSON preview, three real profile routes, Profile -> Home")
        } catch (error: Throwable) {
            runCatching { screenshot("failure-character.png") }
            record("result", "FAIL: ${error.javaClass.simpleName}: ${error.message}")
            throw error
        } finally {
            // Restore preferences even if a preceding Compose idle assertion failed.
            instrumentation.runOnMainSync { CharacterContext.select(originalSelectedId) }
            runCatching { closeSoftKeyboard() }
            runCatching { returnHomeIfAvailable() }.onFailure { record("return home", "${it.message}") }
            assertEquals("Restore the original companion after smoke", originalSelectedId, CharacterContext.currentId())
            record("selection restored", CharacterContext.currentId())
        }
    }

    private fun assertSimpleModeHidesTechnicalFields() {
        listOf("System Prompt", "Post History Instructions", "Extensions (JSON)", "Character Card V2").forEach {
            compose.onNodeWithText(it, substring = false).assertDoesNotExist()
        }
        compose.onNodeWithTag("creator_saf_import_btn").assertDoesNotExist()
        compose.onNodeWithTag("creator_saf_export_btn").assertDoesNotExist()
    }

    private fun creatorScrollToTop() {
        compose.onAllNodes(hasScrollAction() and hasAnyAncestor(hasTestTag("character_creator_screen")))[0]
            .performScrollToIndex(0)
    }

    private fun openContactProfile(id: String) {
        openLibrary()
        val contacts = LauncherAppCatalog.drawerApps().single { it.id == "contacts" }
        compose.onNodeWithTag("app_library_search").performTextReplacement(contacts.name)
        closeSoftKeyboard()
        val icon = hasTestTag("app_icon_${contacts.iconKey}") and hasAnyAncestor(hasTestTag("app_library_item_contacts"))
        compose.waitUntil(10_000) { compose.onAllNodes(icon).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(icon).performClick()
        waitForTag("contacts_screen")
        compose.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("contacts_screen")))
            .performScrollToNode(hasTestTag("contact_item_$id"))
        compose.onNodeWithTag("contact_item_$id").performClick()
        waitForTag("character_profile_screen")
    }

    private fun openLibrary() {
        waitForTag("virtual_home_screen")
        val apps = compose.onAllNodesWithTag("app_icon_apps")
        assertTrue("Use the existing Apps entry without adding desktop items", apps.fetchSemanticsNodes().isNotEmpty())
        apps[0].performClick()
        waitForTag("app_library_screen")
    }

    private fun goHome() {
        compose.onNodeWithTag("virtual_phone_home_indicator").performClick()
        waitForTag("virtual_home_screen")
    }

    private fun returnHomeIfAvailable() {
        if (!hasTag("virtual_home_screen") && hasTag("virtual_phone_home_indicator")) goHome()
    }

    private fun hasTag(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun waitForTag(tag: String) {
        compose.waitUntil(10_000) { hasTag(tag) }
        compose.onAllNodesWithTag(tag)[0].assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(250)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(outputDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }

    private fun record(check: String, value: String) {
        File(outputDir, "character-smoke-events.txt").appendText("$check | $value\n")
    }
}
