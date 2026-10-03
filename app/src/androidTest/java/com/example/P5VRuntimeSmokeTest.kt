package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.ai.prompt.PromptAssembler
import com.example.data.ai.prompt.PromptAssemblyInput
import com.example.data.relationship.romance.*
import com.example.data.context.CharacterContext
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
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
 * navigation. No AI request, user-card write, Welcome reset or data clear.
 * Exercises one short in-app call and an isolated persistence probe. Existing
 * relationship records and companion selection are restored in finally.
 */
@RunWith(AndroidJUnit4::class)
class P5VRuntimeSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-R"
    ).apply { mkdirs() }

    @Test
    fun threeRuntimeProfilesRealChatRoutesAndRelationshipPersistence() {
        assumeTrue("Use the already onboarded QA phone", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Keep any existing call untouched", CallStateEngine.currentCall.value == null)
        val originalId = CharacterContext.currentId()
        val originalCards = AiluaLocalStore.customCards.value.toList()
        val originalRecords = RomanceRepository.records.value.toList()
        val probeId = "qa_runtime_${System.nanoTime()}"
        try {
            compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
            returnHomeIfAvailable()
            val rendered = mutableListOf<String>()
            listOf("yan", "yeo", "noa").forEachIndexed { index, id ->
                val card = checkNotNull(CharacterRegistry.getCard(id))
                val runtime = CharacterRuntimeResolver.resolve(id)
                assertTrue(runtime.relationship.romanceEnabled)
                assertEquals("male", runtime.identity.gender)
                assertTrue("Eight authored dialogue scenarios", card.data.exampleMessages.split("<START>").size >= 9)
                val prompt = PromptAssembler.assemble(PromptAssemblyInput(
                    character = card.data,
                    relationshipInstructions = RomanceRepository.promptInstructions(id),
                )).messages.joinToString("\n\n") { it.content }
                assertTrue(prompt.contains("关系"))
                rendered += prompt
                File(outputDir, "runtime-prompt-$id.txt").writeText(prompt, Charsets.UTF_8)
                record(id, "message=${runtime.initiative.messageProbability}, call=${runtime.initiative.callProbability}, photo=${runtime.initiative.photoProbability}, progression=${runtime.relationship.progressionStyle}")
                openContactProfile(id)
                compose.onNodeWithText("发消息", substring = false).performScrollTo().performClick()
                waitForTag("chat_screen")
                compose.onNodeWithText(CharacterRegistry.getCharacter(id).name, substring = false).assertExists()
                screenshot("0${index + 1}-chat-$id.png")
                assertEquals(originalId, CharacterContext.currentId())
                goHome()
            }
            assertEquals(3, rendered.toSet().size)
            assertEquals("Opening chat cannot earn relationship points", originalRecords, RomanceRepository.records.value)
            openContactProfile("yan")
            if (CharacterContext.currentId() != "yan") compose.onNodeWithTag("profile_select_character").performScrollTo().performClick()
            goHome()
            compose.onAllNodesWithTag("app_icon_living")[0].performClick()
            waitForTag("living_screen")
            screenshot("04-living-yan.png")
            compose.onNodeWithText("打电话", substring = false).performScrollTo().performClick()
            waitForTag("call_screen")
            assertEquals("yan", CallStateEngine.currentCall.value?.characterId)
            assertTrue(CallStateEngine.currentCall.value?.userInitiated == true)
            screenshot("05-call-yan.png")
            compose.onNodeWithTag("call_end_btn").performClick()
            compose.waitUntil(5_000) { CallStateEngine.currentCall.value == null }
            assertEquals("A short call cannot earn relationship points", originalRecords, RomanceRepository.records.value)
            goHome()
            // Store a uniquely named QA record, reload through production persistence,
            // then remove only this record; no existing companion's state is changed.
            val now = System.currentTimeMillis()
            val profile = CharacterRuntimeResolver.resolve("yan").copy(characterId = probeId)
            val expected = RomanceReducer.apply(RomanceRecord(probeId),
                RomanceEvent("qa-memory", RomanceEventType.IMPORTANT_MEMORY, now), profile)
            compose.runOnIdle {
                AiluaLocalStore.saveRomanceStates(RomanceRepository.records.value + expected)
                RomanceRepository.restore()
            }
            assertEquals(expected, RomanceRepository.record(probeId))
            compose.activityRule.scenario.recreate()
            waitForTag("virtual_home_screen")
            assertEquals(expected, RomanceRepository.record(probeId))
            assertEquals(originalCards, AiluaLocalStore.customCards.value)
            record("result", "PASS: distinct runtime instructions, three real Chat routes, Living, user-initiated short call, no UI relationship gains, persisted evidence after recreation")
        } catch (error: Throwable) {
            runCatching { screenshot("failure-runtime.png") }
            record("result", "FAIL: ${error.message}")
            throw error
        } finally {
            instrumentation.runOnMainSync {
                AiluaLocalStore.saveRomanceStates(RomanceRepository.records.value.filterNot { it.characterId == probeId })
                RomanceRepository.restore()
                CharacterContext.select(originalId)
            }
            runCatching { closeSoftKeyboard() }
            runCatching { returnHomeIfAvailable() }
            record("selection restored", CharacterContext.currentId())
            assertEquals(originalRecords, RomanceRepository.records.value)
        }
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
        File(outputDir, "runtime-smoke-events.txt").appendText("$check | $value\n")
    }
}
