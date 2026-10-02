package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.runtime.ProviderResolver
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.engine.CallStateEngine
import com.example.data.engine.ProactiveMessageEngine
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph
import com.example.data.model.CallState
import com.example.data.model.LetterDeliveryState
import com.example.data.repository.MailboxRepository
import com.example.data.systemui.control.ControlCenterStore
import com.example.data.systemui.notification.NotificationCategory
import com.example.data.systemui.notification.VirtualNotification
import com.example.data.systemui.notification.VirtualNotificationGraph
import com.example.ui.systemui.VirtualSystemUiSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.UUID

/**
 * Opt-in connected-device smoke for P5.5. Complete Welcome before running this class.
 * Only the provider output is a labelled local fixture. The production engine writes
 * the assistant turn, world event, success state and notification through its usual path.
 * The fixture reads no provider credentials; no notification rows are inserted by the test, and
 * old messages, letters and notification history are never cleared for setup.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class P55DeviceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val testName = TestName()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val controller get() = VirtualSystemUiSession.controller
    private val queries get() = MemoryGraph.database.virtualNotificationQueries
    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.5"
    ).apply { mkdirs() }

    @Before
    fun readyOnHome() {
        assumeTrue("Complete Welcome using the app before device smoke", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("An existing call must finish before device smoke", CallStateEngine.currentCall.value == null)
        compose.runOnIdle { controller.unlock() }
        waitForTag("virtual_home_screen")
    }

    @Test
    fun smoke01_notificationPipelineFocusDismissAndPersistence() {
        val previousFocus = ControlCenterStore.state.value.focusMode
        val protectedIds = activeNotifications().map { it.id }.toSet()
        val createdIds = mutableSetOf<String>()
        try {
            setFocusThroughControl(false)
            val first = fireFixture("heads-up")
            createdIds += first.id
            waitForTag("heads_up_notification")
            screenshot("heads-up.png")
            compose.onNodeWithTag("notification_${first.id}").performClick()
            waitForTag("chat_screen")
            waitUntil { queries.selectNotificationBySourceKey(first.sourceKey).executeAsOne().seen == 1L }
            assertFalse(queries.selectNotificationBySourceKey(first.sourceKey).executeAsOne().dismissed == 1L)
            screenshot("notification-open-chat.png")
            compose.onNodeWithTag("chat_back_btn").performClick()
            waitForTag("virtual_home_screen")
            record("message heads-up -> Chat -> markSeen", "PASS")

            setFocusThroughControl(true)
            val focused = fireFixture("focus-mode")
            createdIds += focused.id
            SystemClock.sleep(400)
            compose.onNodeWithTag("heads_up_notification").assertDoesNotExist()
            assertTrue(activeNotifications().any { it.id == focused.id })
            record("focus mode keeps notification without heads-up", "PASS")

            compose.runOnIdle { controller.openNotifications() }
            waitForTag("notification_shade")
            waitForTag("notification_${focused.id}")
            screenshot("notification-shade.png")
            compose.onNodeWithTag("notification_${focused.id}").performTouchInput { swipeLeft() }
            waitUntil { queries.selectNotificationBySourceKey(focused.sourceKey).executeAsOne().dismissed == 1L }
            record("swipe dismiss QA notification", "PASS")

            // Clear All is global. Exercise it only when it cannot touch pre-existing history.
            val beforeClear = activeNotifications()
            if (protectedIds.isEmpty() && beforeClear.all { it.id in createdIds }) {
                compose.onNodeWithTag("clear_all_notifications").performClick()
                waitUntil { activeNotifications().isEmpty() }
                assertEquals(1L, queries.selectNotificationBySourceKey(first.sourceKey).executeAsOne().dismissed)
                record("clear all", "PASS: only this run's QA notifications were active")
            } else {
                record("clear all", "SKIP: preserved notifications that existed before this test")
            }
            compose.onNodeWithContentDescription("收起通知中心").performClick()
            waitForTag("virtual_home_screen")

            // Leave a real persisted message notification for the external force-stop/restart check.
            val retained = fireFixture("retain-for-restart")
            createdIds += retained.id
            assertTrue(activeNotifications().any { it.id == retained.id && !it.dismissed })
            File(outputDir, "retained-notification.txt").writeText(
                "id=${retained.id}\nsourceKey=${retained.sourceKey}\nroute=${retained.route}\n" +
                    "provider=local instrumentation fixture; production persistence path\n"
            )
            record("retained notification", "PASS: ${retained.id}")
        } finally {
            setFocusThroughControl(previousFocus)
        }
    }

    @Test
    fun smoke02_lockedIncomingCallLiveActivityAndEnd() {
        compose.runOnIdle {
            controller.lock()
            CallStateEngine.triggerIncomingCall(
                characterId = "mira",
                callerName = "小弥",
                reason = "P5.5 QA · 来电优先级验证",
                timeLabel = WorldHeartbeatEngine.worldClock.value.timeFormatted,
            )
        }
        waitForTag("incoming_call_screen")
        assertTrue(controller.state.value.isLocked)
        compose.onNodeWithTag("virtual_lock_screen").assertDoesNotExist()
        screenshot("incoming-call-while-locked.png")
        compose.onNodeWithTag("call_answer_btn").performClick()
        waitUntil { CallStateEngine.currentCall.value?.state == CallState.CONNECTED }
        waitForTag("live_activity_chip")
        screenshot("live-call.png")
        record("locked incoming -> existing UI answer -> live chip", "PASS")

        compose.runOnIdle { controller.lock() }
        waitForTag("virtual_lock_screen")
        waitForTag("live_activity_summary")
        screenshot("live-call-lockscreen.png")
        compose.onAllNodesWithTag("live_activity_summary")[0].performClick()
        waitForTag("live_activity_expanded")
        screenshot("live-call-expanded.png")
        compose.onNodeWithTag("live_end_call").performClick()
        waitUntil { CallStateEngine.currentCall.value == null }
        compose.onNodeWithTag("live_activity_chip").assertDoesNotExist()
        compose.onNodeWithTag("live_activity_summary").assertDoesNotExist()
        compose.onNodeWithTag("live_activity_expanded").assertDoesNotExist()
        waitForTag("virtual_lock_screen")
        record("lockscreen ongoing -> expanded END -> chip gone", "PASS")
    }

    @Test
    fun smoke03_worldAdvanceDeliversExistingScheduledMail() {
        val pending = MailboxRepository.letters.value.filter { it.deliveryState == LetterDeliveryState.SCHEDULED }
        if (pending.isEmpty()) {
            record("world mail delivery", "SKIP: all existing letters already delivered; history preserved")
        }
        assumeTrue("No undelivered built-in letters remain; do not reset delivery history", pending.isNotEmpty())
        val now = WorldHeartbeatEngine.worldClock.value.minutesOfDay
        val letter = pending.minBy { (it.deliverAtVirtualTimeMinutes - now + 1440) % 1440 }
        val delta = ((letter.deliverAtVirtualTimeMinutes - now + 1440) % 1440).coerceAtLeast(1)
        compose.runOnIdle { WorldHeartbeatEngine.advanceTime(delta) }
        waitUntil { queries.selectNotificationBySourceKey("mail:${letter.id}").executeAsOneOrNull() != null }
        val notification = queries.selectNotificationBySourceKey("mail:${letter.id}").executeAsOne()
        assertEquals(NotificationCategory.MAIL.name, notification.category)
        assertEquals("mailbox", notification.route)
        assertEquals(letter.subject, notification.body)

        // Advancing the real world can also legitimately cross its existing 22:45 call.
        if (CallStateEngine.currentCall.value?.state == CallState.INCOMING) {
            waitForTag("incoming_call_screen")
            compose.onNodeWithTag("call_decline_btn").performClick()
            waitUntil { CallStateEngine.currentCall.value == null }
        }
        compose.runOnIdle { controller.openNotifications() }
        waitForTag("notification_${notification.id}")
        screenshot("mail-delivered.png")
        record("world mail delivery", "PASS: mail:${letter.id}; advanced $delta virtual minutes")
        compose.onNodeWithContentDescription("收起通知中心").performClick()
    }

    private fun fireFixture(label: String): VirtualNotification {
        val text = "[P5.5 QA local fixture] $label · ${UUID.randomUUID()}"
        val provider = object : AiProvider {
            override fun streamChat(request: AiChatRequest) = flowOf(AiStreamEvent.Completed(text))
        }
        val chat = SqlDelightChatRepository(MemoryGraph.database, UuidIdGenerator(), SystemEpochClock())
        val engine = ProactiveMessageEngine(
            chatRepository = chat,
            memoryRepository = MemoryGraph.repository,
            providerResolver = ProviderResolver { ResolvedProvider("qa-local-fixture", "offline-fixture", provider) },
            clock = SystemEpochClock(),
            loadSettings = { AiluaLocalStore.getProactiveSettings() },
            loadState = { AiluaLocalStore.getProactiveState() },
            saveState = { AiluaLocalStore.saveProactiveState(it) },
        )
        assertTrue(runBlocking { engine.fireIfDue(force = true) })
        val notification = activeNotifications().single { it.body == text }
        val session = chat.getOrCreatePrivateSession(checkNotNull(notification.characterId))
        val turn = chat.getResolvedTurns(session.id).single { it.activeVariant?.content == text }
        assertEquals("message:${session.id}:${turn.id}", notification.sourceKey)
        assertEquals(NotificationCategory.MESSAGE, notification.category)
        assertTrue(WorldStateRepository.events.value.any { it.description == text && it.sourceRefId == "proactive_message" })
        assertTrue(AiluaLocalStore.getProactiveState().sentCount > 0)
        assertNotNull(queries.selectNotificationBySourceKey(notification.sourceKey).executeAsOneOrNull())
        return notification
    }

    private fun setFocusThroughControl(enabled: Boolean) {
        compose.runOnIdle {
            controller.unlock()
            controller.openControlCenter()
        }
        waitForTag("control_center")
        if (ControlCenterStore.state.value.focusMode != enabled) {
            compose.onNodeWithTag("control_focus").performClick()
            waitUntil { ControlCenterStore.state.value.focusMode == enabled }
        }
        screenshot(if (enabled) "control-center-focus.png" else "control-center.png")
        compose.onNodeWithTag("control_close").performClick()
        compose.waitForIdle()
    }

    private fun activeNotifications() = runBlocking { VirtualNotificationGraph.repository.observeActive().first() }

    private fun waitForTag(tag: String) {
        waitUntil { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag(tag)[0].assertIsDisplayed()
    }

    private fun waitUntil(condition: () -> Boolean) = compose.waitUntil(timeoutMillis = 10_000, condition = condition)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Device screenshot unavailable" }
        try {
            File(outputDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }

    private fun record(check: String, result: String) {
        File(outputDir, "device-smoke-events.txt").appendText("${testName.methodName} | $check | $result\n")
    }
}
