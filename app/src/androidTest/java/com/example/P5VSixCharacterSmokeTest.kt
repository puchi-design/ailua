package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.context.CharacterContext
import com.example.data.desktop.WorkspaceGraph
import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.OfficialCharacters
import com.example.data.model.CallSession
import com.example.data.registry.CharacterRegistry
import com.example.ui.call.IncomingCallScreen
import com.example.ui.launcher.LauncherAppCatalog
import com.example.ui.onboarding.WelcomeScreen
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.theme.AiluaTheme
import com.example.ui.themeengine.AiluaThemeProvider
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.ThemeStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Opt-in six-person visual QA on an already onboarded phone, without sending an
 * AI message, clearing data, seeding chat history or triggering a real call.
 * Contacts/Profile/Home/Chat/Moments/Group use MainActivity's actual navigation.
 * Welcome and IncomingCall are explicitly labelled isolated presentation fixtures:
 * no finish/import/call action is invoked and no call is inserted in the ledger.
 * The host runner compares the private before/after database, including all old
 * chat rows. Existing app navigation may create previously absent empty sessions;
 * these are reported as additions rather than deleted to manufacture equality.
 */
@RunWith(AndroidJUnit4::class)
class P5VSixCharacterSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val output get() = File(checkNotNull(context.getExternalFilesDir(null)), "qa/P5.V-SixCharacters")
        .apply { mkdirs() }
    private val controller get() = VirtualSystemUiSession.controller
    private val runId get() = InstrumentationRegistry.getArguments().getString("qa_run_id") ?: "manual"
    private val recoveryFile get() = File(output, "qa-restore.json")
    private val expected = listOf(
        Expected("hewenchuan", "hewenchuan", "贺闻川", "romance"),
        Expected("zhoujianye", "zhoujianye", "周见野", "romance"),
        Expected("peixubai", "peixubai", "裴叙白", "romance"),
        Expected("mira", "suwanning", "苏晚宁", "friendship"),
        Expected("yuna", "xuchaoyan", "许朝颜", "friendship"),
        Expected("noa", "songzhiwei", "宋知微", "friendship"),
    )

    @Test fun avatarsRosterRealScreensAndPreservedUserState() {
        assumeTrue("This smoke requires an already onboarded device", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Do not disturb a current user call", CallStateEngine.currentCall.value == null)
        assumeTrue("Retain user overrides rather than replace them to obtain official screenshots",
            AiluaLocalStore.customCards.value.none { it.data.id in expected.map(Expected::id) })
        val originalId = CharacterContext.currentId()
        val originalTheme = ThemeStore.selection
        val originalWorkspace = WorkspaceGraph.repository.snapshot()
        val originalCards = AiluaLocalStore.customCards.value.toList()
        val originalCallHistory = AiluaLocalStore.savedCallHistory.value.map { it.copy() }
        val protected = captureProtectedPreferences()
        val originalWelcome = context.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE).all.toMap()
        check(!recoveryFile.exists()) { "An interrupted QA backup exists; recover that recorded run before another smoke" }
        assertTrue("First-session state is boolean-only", originalWelcome.values.all { it is Boolean })
        recoveryFile.writeText(JSONObject().put("qaRunId", runId).put("selectedId", originalId)
            .put("firstSession", JSONObject(originalWelcome)).toString(2))
        val captures = JSONArray()
        var fixtureContentInstalled = false
        var mainHostRestored = true
        var passed = false
        try {
            record("selection-before", originalId)
            InstrumentationRegistry.getArguments().getString("expectedCharacterId")?.let {
                assertEquals("Upgrade must preserve the known persisted selection", it, originalId)
            }
            verifyOfficialRoster()
            decodeAllTwelveAndSaveNativeCircleBoards()
            compose.runOnIdle { controller.unlock() }
            goHome()

            expected.forEachIndexed { index, character ->
                val prefix = (index + 1).toString().padStart(2, '0')
                openApp("contacts", "contacts_screen")
                scrollIn("contacts_screen", "contact_item_${character.id}")
                waitForAvatar(character.id, "main")
                assertTextIn(character.name, "contact_item_${character.id}")
                capture("$prefix-contacts-${character.id}.png", "MainActivity:Contacts", character.id, captures)
                compose.onNodeWithTag("contact_item_${character.id}").performClick()
                waitForTag("character_profile_screen")
                waitForAvatar(character.id, "main")
                compose.onNodeWithText(character.name, substring = false).assertIsDisplayed()
                assertEquals("Opening Profile must not silently select a character", CharacterContext.currentId(),
                    if (index == 0) originalId else expected[index - 1].id)
                capture("$prefix-profile-${character.id}.png", "MainActivity:Profile", character.id, captures)
                if (CharacterContext.currentId() != character.id) {
                    compose.onNodeWithTag("profile_select_character").performScrollTo().performClick()
                    compose.waitUntil(10_000) { CharacterContext.currentId() == character.id }
                }
                goHome()
                findCharacterWidgetPage()
                waitForAvatar(character.id, "alt")
                assertTextIn(character.name, "living_character_widget")
                capture("$prefix-home-${character.id}.png", "MainActivity:Home", character.id, captures)

                openApp("chat", "conversation_list_screen")
                scrollIn("conversation_list_screen", "conv_item_conv_${character.id}")
                waitForAvatar(character.id, "main")
                assertTextIn(character.name, "conv_item_conv_${character.id}")
                capture("$prefix-conversations-${character.id}.png", "MainActivity:ConversationList", character.id, captures)
                compose.onNodeWithTag("conv_item_conv_${character.id}").performClick()
                waitForTag("chat_screen")
                waitForAvatar(character.id, "main")
                compose.onNodeWithText(character.name, substring = false).assertIsDisplayed()
                // No input, Send, regenerate, provider sheet or message action is touched.
                capture("$prefix-chat-${character.id}.png", "MainActivity:ChatTopBar", character.id, captures)
                goHome()
            }

            openApp("moments", "moments_screen")
            compose.waitUntil(15_000) { expected.any { avatarVisible(it.id, "main") } }
            capture("07-moments.png", "MainActivity:MomentsExistingPosts", null, captures)
            goHome()
            openApp("chat", "conversation_list_screen")
            scrollIn("conversation_list_screen", "conv_item_conv_group")
            compose.onNodeWithTag("conv_item_conv_group").performClick()
            waitForTag("group_chat_screen")
            // Empty or populated group history is kept untouched; the real member strip
            // shows the three friends without manufacturing assistant messages.
            listOf("mira", "yuna", "noa").forEach { waitForAvatar(it, "main") }
            capture("08-group-members.png", "MainActivity:GroupChatMembers", null, captures)
            goHome()

            // MainActivity has already set its content. Replace it only for isolated
            // presentation probes, then recreate the real host in finally.
            val welcomeBeforeFixture = context.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE).all.toMap()
            fixtureContentInstalled = true
            showFixture { WelcomeScreen { _, _ -> error("QA must never finish/import Welcome") } }
            waitForTag("welcome_screen")
            compose.onNodeWithText("开始", substring = false).performClick()
            compose.onNodeWithText("连接 AI", substring = false).assertExists()
            if (compose.onAllNodes(hasText("暂时跳过", substring = false)).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("暂时跳过", substring = false).performClick()
            } else {
                compose.onNodeWithText("继续", substring = false).performClick()
            }
            compose.onNodeWithText("今晚，你想先认识谁？", substring = false).assertExists()
            compose.onNodeWithText("心动对象", substring = false).assertExists()
            compose.onNodeWithText("我的朋友", substring = false).assertExists()
            expected.forEach { character ->
                compose.onNodeWithTag("welcome_character_${character.id}").performScrollTo()
                waitForAvatar(character.id, "main")
                assertTextIn(character.name, "welcome_character_${character.id}")
            }
            compose.onNodeWithTag("welcome_character_hewenchuan").performScrollTo()
            capture("09-onboarding-heart-fixture.png", "IsolatedFixture:WelcomeHeartRoster", null, captures)
            compose.onNodeWithTag("welcome_character_noa").performScrollTo()
            capture("10-onboarding-friends-fixture.png", "IsolatedFixture:WelcomeFriendRoster", null, captures)
            assertEquals("Rendering Welcome must not change persisted onboarding", welcomeBeforeFixture,
                context.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE).all)

            expected.forEach { character ->
                val preview = CallSession(id = "qa-visual-${character.id}", characterId = character.id,
                    callerName = character.name, avatarId = character.id)
                showFixture { IncomingCallScreen(previewSession = preview) }
                waitForTag("incoming_call_screen")
                waitForAvatar(character.id, "alt")
                compose.onNodeWithText(character.name, substring = false).assertIsDisplayed()
                assertEquals("A call fixture must never reach the call engine", null, CallStateEngine.currentCall.value)
                capture("11-call-${character.id}-fixture.png", "IsolatedFixture:IncomingCallNoEngineAction", character.id, captures)
            }
            passed = true
            record("result", "PASS: 6 canonical stable IDs/routes, 12 real asset decodes, real navigation, no AI/send/call/Welcome finish")
        } catch (error: Throwable) {
            runCatching { capture("failure-six-character.png", "Failure", null, captures) }
            record("result", "FAIL: ${error.javaClass.simpleName}: ${error.message}")
            throw error
        } finally {
            // Restore only the reversible selection and first-session visitation flags;
            // never clear a database, delete a chat or rewrite any history.
            instrumentation.runOnMainSync { CharacterContext.select(originalId) }
            restorePreferences("ailua_first_session", originalWelcome)
            if (fixtureContentInstalled) {
                runCatching { compose.activityRule.scenario.recreate() }
                    .onFailure { mainHostRestored = false; record("restore-main-host", "FAIL: ${it.javaClass.simpleName}") }
            }
            runCatching { compose.runOnIdle { controller.unlock() }; goHome() }
                .onFailure { mainHostRestored = false; record("restore-home", "FAIL: ${it.javaClass.simpleName}") }
            val checks = JSONObject().apply {
                put("mainHostRestored", mainHostRestored)
                put("selectedId", CharacterContext.currentId() == originalId)
                put("theme", ThemeStore.selection == originalTheme)
                put("workspace", WorkspaceGraph.repository.snapshot() == originalWorkspace)
                put("customCards", AiluaLocalStore.customCards.value == originalCards)
                put("callHistory", AiluaLocalStore.savedCallHistory.value == originalCallHistory)
                put("protectedPreferences", captureProtectedPreferences() == protected)
                put("onboardingPreferences", context.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE).all == originalWelcome)
            }
            File(output, "six-character-result.json").writeText(JSONObject().apply {
                put("instrumentationReachedEnd", passed)
                put("qaRunId", runId)
                put("protectedStateChecks", checks)
                put("captures", captures)
                put("fixtureLimit", "Welcome and Call are isolated presentation; no onboarding finish or actual call-engine test")
                put("databasePolicy", "Host compares all existing rows and separately reports navigation-created empty sessions; no QA cleanup deletes history")
            }.toString(2))
            if (checks.getBoolean("selectedId") && checks.getBoolean("onboardingPreferences")) recoveryFile.delete()
            assertTrue("Every protected state check must pass: $checks", checks.keys().asSequence().all { checks.getBoolean(it) })
        }
    }

    /** Host invokes only after interrupting the same recorded smoke run. */
    @Test fun recoverInterruptedQaSelection() {
        if (!recoveryFile.exists()) {
            File(output, "qa-recovery-result.json").writeText(JSONObject().put("qaRunId", runId).put("backupExisted", false)
                .put("restored", true).toString(2))
            return
        }
        val backup = JSONObject(recoveryFile.readText())
        assertEquals("Never apply a backup from a different QA run", runId, backup.getString("qaRunId"))
        val selected = backup.getString("selectedId")
        val flags = backup.getJSONObject("firstSession")
        val oldFlags = flags.keys().asSequence().associateWith { flags.getBoolean(it) }
        instrumentation.runOnMainSync { CharacterContext.select(selected) }
        restorePreferences("ailua_first_session", oldFlags)
        val restored = CharacterContext.currentId() == selected &&
            context.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE).all == oldFlags
        File(output, "qa-recovery-result.json").writeText(JSONObject().put("qaRunId", runId).put("backupExisted", true)
            .put("restored", restored).toString(2))
        assertTrue("Recover the original QA selection/Welcome flags", restored)
        check(recoveryFile.delete()) { "Could not remove the QA-only recovery backup" }
    }

    private fun verifyOfficialRoster() {
        val cards = OfficialCharacters.cards.associateBy { it.data.id }
        assertTrue("All six identities must exist in authored official cards", cards.keys.containsAll(expected.map(Expected::id)))
        listOf("suwanning", "xuchaoyan", "songzhiwei").forEach { slug ->
            assertFalse("A female public asset slug must not create another persisted identity", slug in cards)
        }
        expected.forEach { character ->
            assertEquals(character.name, checkNotNull(cards[character.id]).data.name)
            assertEquals(character.name, CharacterRegistry.getCharacter(character.id).name)
            assertEquals(character.route, CharacterRuntimeResolver.resolve(character.id).relationship.routeType)
        }
    }

    private fun decodeAllTwelveAndSaveNativeCircleBoards() {
        val rows = JSONArray()
        listOf("main", "alt").forEach { variant ->
            val board = Bitmap.createBitmap(840, 500, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(board).apply { drawColor(Color.rgb(247, 247, 247)) }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            try {
                expected.forEachIndexed { column, character ->
                    val asset = "characters/${character.slug}/avatar_$variant.webp"
                    val decoded = context.assets.open(asset).use { checkNotNull(BitmapFactory.decodeStream(it)) }
                    try {
                        assertEquals("Real asset width $asset", 512, decoded.width)
                        assertEquals("Real asset height $asset", 512, decoded.height)
                        listOf(48, 64, 96).forEachIndexed { row, edge ->
                            val reduced = Bitmap.createScaledBitmap(decoded, edge, edge, true)
                            try {
                                val samples = IntArray(edge * edge)
                                reduced.getPixels(samples, 0, edge, 0, 0, edge, edge)
                                assertTrue("Formal avatar must be fully opaque $asset", samples.all { Color.alpha(it) == 255 })
                                assertTrue("Thumbnail cannot be a blank bitmap $asset", samples.toSet().size > 16)
                                val x = column * 140f + (140 - edge) / 2f
                                val y = row * 156f + 28
                                val saved = canvas.save()
                                canvas.clipPath(Path().apply { addCircle(x + edge / 2f, y + edge / 2f, edge / 2f, Path.Direction.CW) })
                                canvas.drawBitmap(reduced, x, y, paint)
                                canvas.restoreToCount(saved)
                                paint.color = Color.rgb(39, 47, 56); paint.textSize = 16f
                                canvas.drawText("$variant ${edge}px", column * 140f + 20, row * 156f + 20, paint)
                                canvas.drawText(character.name, column * 140f + 34, row * 156f + 143, paint)
                            } finally { if (reduced !== decoded) reduced.recycle() }
                        }
                        rows.put(JSONObject().put("stableId", character.id).put("publicSlug", character.slug)
                            .put("variant", variant).put("asset", asset).put("decodedWidth", decoded.width).put("decodedHeight", decoded.height)
                            .put("nativeCirclePixels", JSONArray(listOf(48, 64, 96))).put("opaqueAndNonBlank", true))
                    } finally { decoded.recycle() }
                }
                File(output, "asset-$variant-circle-native.png").outputStream().use { board.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally { board.recycle() }
        }
        File(output, "asset-device-decode.json").writeText(JSONObject().put("expected", 12).put("decoded", rows).toString(2))
    }

    private fun captureProtectedPreferences(): Map<String, Map<String, Any?>> {
        val names = listOf("ailua_character_context", "ailua_home_display", "ailua_settings", "ailua_control_center", "ailua_virtual_lock")
        val result = names.associateWith { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.filterKeys { key ->
                name != "ailua_settings" || key.startsWith("theme_engine_") || key in setOf("dark_theme", "developer")
            }.toMap()
        }.toMutableMap()
        val protectedOsKeys = setOf(AiluaLocalStore.KEY_CUSTOM_CARDS, AiluaLocalStore.KEY_CALL_HISTORY,
            AiluaLocalStore.KEY_BOOKMARKED_MSGS, AiluaLocalStore.KEY_HOME_APP_ORDER, AiluaLocalStore.KEY_RELATIONSHIPS,
            AiluaLocalStore.KEY_ROMANCE_STATES, AiluaLocalStore.KEY_THEATER_BOOKMARK)
        result["ailua_os_store"] = context.getSharedPreferences("ailua_os_store", Context.MODE_PRIVATE).all
            .filterKeys { it in protectedOsKeys }.toMap()
        return result
    }

    private fun restorePreferences(name: String, original: Map<String, *>) {
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.filterNot { it in original }.forEach { editor.remove(it) }
        original.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                null -> editor.remove(key)
                else -> error("Unsupported preference type for $key")
            }
        }
        check(editor.commit()) { "Failed to restore $name" }
    }

    private fun showFixture(content: @Composable () -> Unit) {
        val clock = WorldHeartbeatEngine.worldClock.value
        val dark = context.getSharedPreferences("ailua_settings", Context.MODE_PRIVATE).getBoolean("dark_theme", false)
        val runtime = ThemeResolver.resolve(ThemeStore.selection, dark, clock.dayPhase, clock.weather)
        compose.runOnUiThread {
            compose.activity.setContent {
                AiluaTheme(darkTheme = dark) {
                    AiluaThemeProvider(runtime) { Box(Modifier.fillMaxSize().background(runtime.surfaces.screen)) { content() } }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun waitForAvatar(id: String, variant: String) {
        val tag = "character_portrait_loaded_${id}_$variant"
        compose.waitUntil(20_000) { visible(tag, unmerged = true) }
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().indices
            .first { compose.onAllNodesWithTag(tag, useUnmergedTree = true)[it].isDisplayed() }
    }

    private fun avatarVisible(id: String, variant: String) = visible("character_portrait_loaded_${id}_$variant", unmerged = true)

    private fun assertTextIn(text: String, ancestorTag: String) = compose.onNode(
        hasText(text, substring = false) and hasAnyAncestor(hasTestTag(ancestorTag)), useUnmergedTree = true
    ).assertIsDisplayed()

    private fun scrollIn(screen: String, target: String) {
        if (screen == "conversation_list_screen") {
            // Its SQL reads are asynchronous; wait for the pinned companion row
            // before attempting to seek any item in the lazily composed list.
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag("conv_item_conv_${CharacterContext.currentId()}").fetchSemanticsNodes().isNotEmpty()
            }
        }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and hasAnyAncestor(hasTestTag(screen)))
            .performScrollToNode(hasTestTag(target))
        compose.onNodeWithTag(target).assertIsDisplayed()
    }

    private fun openApp(id: String, screenTag: String) {
        goHome()
        val apps = compose.onAllNodesWithTag("app_icon_apps")
        val appIndex = apps.fetchSemanticsNodes().indices.first { apps[it].isDisplayed() }
        apps[appIndex].performClick()
        waitForTag("app_library_screen")
        val entry = LauncherAppCatalog.drawerApps().single { it.id == id }
        compose.onNodeWithTag("app_library_search").performTextReplacement(entry.name)
        Espresso.closeSoftKeyboard()
        compose.onNode(hasTestTag("app_icon_${entry.iconKey}") and hasAnyAncestor(hasTestTag("app_library_item_$id"))).performClick()
        waitForTag(screenTag)
    }

    private fun goHome() {
        if (!visible("virtual_home_screen") && visible("virtual_phone_home_indicator")) {
            compose.onNodeWithTag("virtual_phone_home_indicator").performClick()
        }
        waitForTag("virtual_home_screen")
    }

    private fun findCharacterWidgetPage() {
        repeat(4) { if (!visible("living_character_widget")) {
            compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeLeft() }
            compose.waitForIdle()
        } }
        repeat(4) { if (!visible("living_character_widget")) {
            compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeRight() }
            compose.waitForIdle()
        } }
        waitForTag("living_character_widget")
    }

    private fun visible(tag: String, unmerged: Boolean = false): Boolean {
        val nodes = compose.onAllNodesWithTag(tag, useUnmergedTree = unmerged)
        return nodes.fetchSemanticsNodes().indices.any { nodes[it].isDisplayed() }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(15_000) { visible(tag) }
    }

    private fun capture(name: String, kind: String, id: String?, captures: JSONArray) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            captures.put(JSONObject().put("file", name).put("routeKind", kind).put("stableId", id ?: JSONObject.NULL)
                .put("width", bitmap.width).put("height", bitmap.height))
        } finally { bitmap.recycle() }
    }

    private fun record(check: String, value: String) = File(output, "six-character-events.txt").appendText("$check | $value\n")
    private data class Expected(val id: String, val slug: String, val name: String, val route: String)
}
