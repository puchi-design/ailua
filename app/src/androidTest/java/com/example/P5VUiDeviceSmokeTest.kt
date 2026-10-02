package com.example

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.context.CharacterContext
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.data.memory.repository.MemoryGraph
import com.example.ui.systemui.VirtualSystemUiSession
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Opt-in UI-only device smoke. These explicitly labelled local fixtures exercise the
 * production repository and Chat presentation without invoking an AI provider.
 * Existing turns are preserved. Created IDs are recorded for optional later cleanup;
 * the test deliberately leaves its four QA turns available for visual review.
 */
@RunWith(AndroidJUnit4::class)
class P5VUiDeviceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val outputDir get() = File(
        checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "qa/P5.V-UI"
    ).apply { mkdirs() }

    @Test
    fun chatLocalFixturesLongPressAndExistingVariant() {
        assumeTrue("Complete Welcome before this smoke test", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Finish the current call before this smoke test", CallStateEngine.currentCall.value == null)
        compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
        waitForTag("virtual_home_screen")

        val characterId = CharacterContext.selectedId.value
        val chat: ChatRepository = SqlDelightChatRepository(MemoryGraph.database, UuidIdGenerator(), SystemEpochClock())
        val session = chat.getOrCreatePrivateSession(characterId)
        val existingTurns = chat.getResolvedTurns(session.id)
        val userFirst = chat.appendUserTurn(session.id, "［UI QA］今天想慢下来，听一会儿雨。")
        val assistantFirst = chat.appendAssistantTurn(
            session.id, "［UI QA］那就把窗留一道缝。雨声正好，我们慢慢聊。",
            status = VariantStatus.COMPLETE, model = "local-ui-fixture",
        )
        val userSecond = chat.appendUserTurn(session.id, "［UI QA］好，先陪我坐一会儿。")
        val assistantSecond = chat.appendAssistantTurn(
            session.id, ORIGINAL_REPLY, status = VariantStatus.COMPLETE, model = "local-ui-fixture",
        )
        val alternate = chat.appendVariant(
            assistantSecond.id, ALTERNATE_REPLY, status = VariantStatus.COMPLETE, model = "local-ui-fixture",
        )
        // Start on the first reply so the menu itself exercises the real variant callback.
        chat.selectVariant(assistantSecond.id, checkNotNull(assistantSecond.activeVariantId))

        val createdTurns = listOf(userFirst, assistantFirst, userSecond, assistantSecond)
        val manifest = JSONObject()
            .put("fixture", "Local UI QA only; no AI provider request")
            .put("characterId", characterId)
            .put("sessionId", session.id)
            .put("preExistingTurnIds", JSONArray(existingTurns.map { it.id }))
            .put("createdTurnIds", JSONArray(createdTurns.map { it.id }))
            .put("createdVariantIds", JSONArray(createdTurns.flatMap { chat.getVariants(it.id).map { variant -> variant.id } }))
            .put("originalVariantId", assistantSecond.activeVariantId)
            .put("alternateVariantId", alternate.id)
            .put("status", "fixtures-created")
        val manifestFile = File(outputDir, "chat-fixture-created-ids-${assistantSecond.id}.json")
        manifestFile.writeText(manifest.toString(2))

        if (!FirstSessionStore.state.value.receivedFirstReply && hasTag("first_session_guide")) {
            compose.onNodeWithTag("first_session_guide").performClick()
        } else if (hasTag("home_quick_chat")) {
            compose.onNodeWithTag("home_quick_chat").performClick()
        } else {
            // The dock uses the "chat" icon identity for the "messages" app route.
            val dockMessages = hasTestTag("app_icon_chat") and hasAnyAncestor(hasTestTag("virtual_phone_dock"))
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodes(dockMessages).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(dockMessages).performClick()
            waitForTag("conversation_list_screen")
            waitForTag("conv_item_conv_$characterId")
            compose.onNodeWithTag("conv_item_conv_$characterId").performClick()
        }
        waitForTag("chat_screen")
        val replyTag = "chat_message_${assistantSecond.id}"
        waitForReply(replyTag, ORIGINAL_REPLY)
        compose.onNodeWithTag("chat_message_${userSecond.id}").assertIsDisplayed()
        compose.onNodeWithTag("chat_message_${assistantSecond.id}").assertIsDisplayed()
        screenshot("chat-fixture.png")

        compose.onNode(hasText(ORIGINAL_REPLY) and hasAnyAncestor(hasTestTag(replyTag)))
            .performTouchInput { longClick() }
        listOf("复制", "保存记忆", "重新生成", "收藏").forEach { label ->
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        screenshot("chat-message-actions.png")
        // No generation, memory write or destructive menu action is invoked.
        compose.onNodeWithText("下一回复分支", substring = true).performClick()
        waitForReply(replyTag, ALTERNATE_REPLY)
        compose.waitUntil(timeoutMillis = 10_000) {
            chat.getResolvedTurns(session.id).single { it.id == assistantSecond.id }.activeVariantId == alternate.id
        }
        screenshot("chat-variant.png")

        val after = chat.getResolvedTurns(session.id)
        assertEquals(existingTurns, after.filter { turn -> existingTurns.any { it.id == turn.id } })
        manifest.put("status", "PASS")
            .put("checks", JSONArray(listOf("user-and-assistant-bubbles", "long-press-menu", "existing-variant-ui-and-db", "pre-existing-turns-preserved")))
            .put("finalActiveVariantId", alternate.id)
        manifestFile.writeText(manifest.toString(2))
    }

    private fun hasTag(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 10_000) { hasTag(tag) }
        compose.onAllNodesWithTag(tag)[0].assertIsDisplayed()
    }

    private fun waitForReply(tag: String, text: String) {
        val matcher = hasText(text) and hasAnyAncestor(hasTestTag(tag))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(matcher).assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Device screenshot unavailable" }
        try {
            File(outputDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val ORIGINAL_REPLY = "［UI QA］我在。你不用急着说话。"
        const val ALTERNATE_REPLY = "［UI QA］那就安静地坐一会儿。等你想说时，我会听。"
    }
}
