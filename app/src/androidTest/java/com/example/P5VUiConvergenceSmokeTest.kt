package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.firstsession.FirstSessionStore
import com.example.data.model.CallAction
import com.example.data.model.CallState
import com.example.data.projection.projectDiary
import com.example.data.repository.MailboxRepository
import com.example.ui.launcher.LauncherAppCatalog
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.themeengine.ThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * Opt-in UI6 smoke for an already onboarded real device. Uses production navigation
 * and existing content only: no provider requests, fixture messages, notification
 * insertions, layout changes, imports, history deletion, or reset. Opening the real
 * notification shade may mark existing notifications seen through its normal policy.
 * One explicitly labelled virtual QA call is created only when no call exists; only
 * that call is ended. Its normal call-history entry is intentionally retained.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class P5VUiConvergenceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val controller get() = VirtualSystemUiSession.controller
    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-UI6"
    ).apply { mkdirs() }

    @Test
    fun smoke01_existingAppsAndFourThemePresentation() {
        readyOnHome()
        try {
            preset("milk")
            capture("virtual_home_screen", "01-home.png")

            openApp("chat", "conversation_list_screen")
            screenshot("05-messages.png")
            waitForTag("conv_item_conv_mira")
            compose.onNodeWithTag("conv_item_conv_mira").performClick()
            waitForTag("chat_screen")
            preset("glass")
            screenshot("02-chat.png")
            compose.onNodeWithContentDescription("选项菜单").performClick()
            compose.onNodeWithText("查看资料").performClick()
            waitForTag("character_profile_screen")
            preset("milk")
            screenshot("04-profile.png")
            compose.onNodeWithText("生活", substring = false).performClick()
            capture("living_screen", "03-living.png")
            goHome()

            openApp("moments", "moments_screen")
            screenshot("06-moments.png")
            goHome()
            openApp("gallery", "gallery_screen")
            screenshot("07-gallery.png")
            goHome()

            preset("diary")
            openApp("diary", "diary_screen")
            screenshot("08-diary.png")
            // Read an existing diary without liking, writing, or changing its content.
            val firstDiary = projectDiary(
                characterId = com.example.data.context.CharacterContext.selectedId.value,
                runtimeEvents = WorldStateRepository.events.value
            ).firstOrNull()
            if (firstDiary != null) {
                compose.onNodeWithText(firstDiary.title, substring = false).performClick()
                compose.onNodeWithText("《${firstDiary.title}》").assertIsDisplayed()
                screenshot("08-diary-reader.png")
                compose.onNodeWithTag("diary_back_btn").performClick()
                waitForTag("diary_screen")
            }
            goHome()
            openApp("mailbox", "mailbox_screen")
            compose.onNodeWithText("全部", substring = false).performClick()
            screenshot("09-mailbox.png")
            MailboxRepository.letters.value.firstOrNull()?.let { letter ->
                waitForTag("letter_card_${letter.id}")
                compose.onNodeWithTag("letter_card_${letter.id}").performClick()
                capture("letter_reader_dialog", "09-mailbox-reader.png")
                compose.onNodeWithContentDescription("关闭").performClick()
                waitForTag("mailbox_screen")
            }
            goHome()

            preset("milk")
            openApp("check_phone", "check_phone_screen")
            screenshot("10-checkphone.png")
            goHome()
            openApp("theater", "theater_screen")
            screenshot("11-theater.png")
            goHome()
            preset("mono")
            openLibrary()
            compose.onNodeWithTag("settings_entry").performClick()
            capture("settings_screen", "17-settings.png")
            goHome()
            record("apps", "PASS: real launcher/search/routes, existing readers, Milk/Glass/Diary/Mono")
        } catch (error: Throwable) {
            runCatching { screenshot("failure-apps.png") }
            record("apps", "FAIL: ${error.javaClass.simpleName}: ${error.message}")
            throw error
        } finally {
            preset("milk")
            returnHomeIfAvailable()
        }
    }

    @Test
    fun smoke02_systemSurfacesAndOwnVirtualCall() {
        readyOnHome()
        var createdCallId: String? = null
        try {
            preset("mono")
            pullStatusBar(rightSide = true)
            capture("control_center", "14-control-center.png")
            compose.onNodeWithTag("control_lock").performClick()
            capture("virtual_lock_screen", "12-lockscreen.png")
            compose.onNodeWithTag("lock_unlock").performClick()
            waitForTag("virtual_home_screen")

            pullStatusBar(rightSide = false)
            capture("notification_shade", "13-notifications.png")
            // Keep the real empty state if there are no notifications. Never clear or dismiss history.
            compose.onNodeWithContentDescription("收起通知中心").performClick()
            waitForTag("virtual_home_screen")
            record("system surfaces", "PASS: status gestures, control lock, UI unlock, real notification content")

            preset("milk")
            if (CallStateEngine.currentCall.value != null) {
                record("virtual call", "SKIP: existing call retained")
                return
            }
            compose.runOnIdle {
                if (CallStateEngine.currentCall.value == null) {
                    createdCallId = CallStateEngine.triggerIncomingCall(
                        characterId = "mira",
                        callerName = "小弥",
                        reason = "P5.V-UI6 QA · 界面冒烟验证",
                        timeLabel = WorldHeartbeatEngine.worldClock.value.timeFormatted
                    ).id
                }
            }
            if (createdCallId == null) {
                record("virtual call", "SKIP: another call started; preserved")
                return
            }
            capture("incoming_call_screen", "18-incoming-call.png")
            compose.onNodeWithTag("call_answer_btn").performClick()
            compose.waitUntil(10_000) { CallStateEngine.currentCall.value?.state == CallState.CONNECTED }
            assertEquals(createdCallId, CallStateEngine.currentCall.value?.id)
            capture("call_screen", "15-call.png")
            compose.onNodeWithTag("call_end_btn").performClick()
            compose.waitUntil(10_000) { CallStateEngine.currentCall.value?.id != createdCallId }
            capture("call_history_screen", "16-call-history.png")
            record("virtual call", "PASS: $createdCallId incoming -> UI answer -> connected -> UI end")
            returnHomeIfAvailable()
        } catch (error: Throwable) {
            runCatching { screenshot("failure-system.png") }
            record("system surfaces/call", "FAIL: ${error.javaClass.simpleName}: ${error.message}")
            throw error
        } finally {
            // Cleanup only this test's call if a UI assertion failed before its End button.
            if (createdCallId != null && CallStateEngine.currentCall.value?.id == createdCallId) {
                compose.runOnIdle { CallStateEngine.handleAction(CallAction.END) }
            }
            preset("milk")
            compose.runOnIdle { controller.unlock() }
            returnHomeIfAvailable()
        }
    }

    private fun readyOnHome() {
        assumeTrue("Complete Welcome before this opt-in smoke", FirstSessionStore.state.value.onboardingComplete)
        if (CallStateEngine.currentCall.value?.state == CallState.INCOMING) {
            record("setup", "SKIP: existing incoming call retained")
            assumeTrue("An existing incoming call owns the UI; do not disturb it", false)
        }
        compose.runOnIdle { controller.unlock() }
        if (!hasTag("virtual_home_screen")) returnHomeIfAvailable()
        waitForTag("virtual_home_screen")
    }

    private fun preset(id: String) {
        compose.runOnIdle { ThemeStore.update(ThemeStore.selection.copy(themePresetId = id)) }
        compose.waitForIdle()
        assertEquals(id, ThemeStore.selection.themePresetId)
    }

    private fun openLibrary() {
        waitForTag("virtual_home_screen")
        val apps = compose.onAllNodesWithTag("app_icon_apps")
        assertTrue("Existing Home must expose its Apps entry; the test never adds desktop items", apps.fetchSemanticsNodes().isNotEmpty())
        apps[0].performClick()
        waitForTag("app_library_screen")
    }

    private fun openApp(id: String, targetTag: String) {
        openLibrary()
        val app = LauncherAppCatalog.drawerApps().single { it.id == id }
        compose.onNodeWithTag("app_library_search").performTextReplacement(app.name)
        compose.waitForIdle()
        closeSoftKeyboard()
        val icon = hasTestTag("app_icon_${app.iconKey}") and hasAnyAncestor(hasTestTag("app_library_item_$id"))
        compose.waitUntil(10_000) { compose.onAllNodes(icon).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(icon).performClick()
        waitForTag(targetTag)
    }

    private fun pullStatusBar(rightSide: Boolean) {
        compose.onNodeWithTag("virtual_home_screen").performTouchInput {
            val x = width * if (rightSide) 0.85f else 0.2f
            swipe(start = Offset(x, height * 0.015f), end = Offset(x, height * 0.4f), durationMillis = 450)
        }
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

    private fun capture(tag: String, name: String) {
        waitForTag(tag)
        screenshot(name)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(250) // Allow transition drawing to reach the screenshot surface.
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Device screenshot unavailable" }
        try {
            File(outputDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }

    private fun record(check: String, result: String) {
        File(outputDir, "ui6-smoke-events.txt").appendText("$check | $result\n")
    }
}
