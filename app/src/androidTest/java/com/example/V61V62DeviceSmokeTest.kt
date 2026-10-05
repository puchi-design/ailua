package com.example

import android.app.KeyguardManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.model.ChatTurn
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.firstsession.FirstSessionStore
import com.example.data.local.AiluaLocalStore
import com.example.data.memory.repository.MemoryGraph
import com.example.data.model.CharacterProfile
import com.example.data.projection.checkphone.PhoneSection
import com.example.data.projection.checkphone.projectCheckPhonePlus
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.relationship.romance.RomanceRepository
import com.example.data.repository.GalleryRepository
import com.example.ui.chat.ChatScreen
import com.example.ui.checkphone.CheckPhoneScreen
import com.example.ui.theme.AiluaTheme
import com.example.ui.themeengine.AiluaThemeProvider
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.ThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Opt-in Android 10 QA. The sole chat fixture uses a unique session removed in finally. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29, maxSdkVersion = 29)
class V61V62DeviceSmokeTest {
    // MIUI rejects instrumentation-driven Activity launches. Start the debug-only host
    // through ADB shell, then attach Compose assertions to its existing UI.
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var host: V61QaHostActivity

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val output get() = File(checkNotNull(context.getExternalFilesDir(null)), "qa/V6.1-V6.2")
        .apply { mkdirs() }

    @Test
    fun richCardsQuoteAndPersistedActionsUseOnlyTemporaryQaSession() {
        ready()
        val qaId = "qa_v61_${UUID.randomUUID().toString().replace('-', '_')}"
        val character = CharacterProfile(
            id = qaId, name = "QA 角色", englishName = "QA", title = "真机验证",
            bio = "仅用于本次测试", currentActivity = "测试中", mood = "平静",
            location = "青石街23号", contextualQuote = "早点睡。", avatarId = "hewenchuan",
        )
        val chat: ChatRepository = SqlDelightChatRepository(MemoryGraph.database, UuidIdGenerator(), SystemEpochClock())
        val session = chat.getOrCreatePrivateSession(qaId)
        assertTrue("Unique QA session must be empty", chat.getResolvedTurns(session.id).isEmpty())
        val openedLocation = AtomicReference<String?>(null)

        try {
            val original = chat.appendAssistantTurn(session.id, "［QA］早点睡。", status = VariantStatus.COMPLETE)
            val quote = chat.appendUserTurn(session.id, "［QA］你自己还不是没睡。",
                quoteMessageId = original.id, quotePreview = "［QA］早点睡。")
            val red = richTurn(chat, session.id, RichMessagePayload(
                RichMessageType.RED_PACKET, label = "晚饭钱", amount = 30.0,
                currency = "¥", status = RichMessageStatus.PENDING))
            val transfer = richTurn(chat, session.id, RichMessagePayload(
                RichMessageType.TRANSFER, label = "今天辛苦了", amount = 18.0,
                currency = "¥", status = RichMessageStatus.PENDING))
            val location = richTurn(chat, session.id, RichMessagePayload(
                RichMessageType.LOCATION, label = "青石街23号", locationName = "青石街23号"))
            val sticker = richTurn(chat, session.id, RichMessagePayload(
                RichMessageType.STICKER, label = "偷看", iconKey = "偷看"))

            showFixture { ChatScreen(character = character,
                onOpenWorldLocation = { openedLocation.set(it) }) }
            waitForTag("chat_screen")

            scrollToMessage(original.id)
            compose.onNode(hasText("［QA］早点睡。") and
                hasAnyAncestor(hasTestTag("chat_message_${original.id}")), useUnmergedTree = true)
                .performTouchInput { longClick() }
            compose.onNodeWithText("引用").performClick()
            waitForTag("chat_quote_composer")
            compose.onNodeWithTag("chat_quote_clear").performClick()

            scrollToMessage(quote.id)
            compose.onNode(hasTestTag("chat_quote_block") and
                hasAnyAncestor(hasTestTag("chat_message_${quote.id}")), useUnmergedTree = true)
                .assertIsDisplayed()

            scrollToMessage(red.id)
            compose.onNodeWithTag("rich_red_packet_open_0", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { richStatus(chat, session.id, red.id) == RichMessageStatus.OPENED }

            scrollToMessage(transfer.id)
            compose.onNodeWithTag("rich_transfer_decline_0", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { richStatus(chat, session.id, transfer.id) == RichMessageStatus.DECLINED }
            compose.waitUntil(10_000) {
                AiluaLocalStore.savedWorldEvents.value.count { it.characterId == qaId && it.id.startsWith("rich_") } >= 2
            }

            scrollToMessage(location.id)
            // The jump-to-latest pill overlaps the card center at this scroll position.
            // Tap the exposed top-left area, as a user would.
            compose.onNodeWithTag("rich_location_0", useUnmergedTree = true)
                .performTouchInput { click(Offset(28f, 24f)) }
            compose.waitUntil(3_000) { openedLocation.get() != null }
            assertEquals("青石街23号", openedLocation.get())
            scrollToMessage(sticker.id)
            compose.onNodeWithTag("rich_sticker_0", useUnmergedTree = true).assertIsDisplayed()
            capture("rich-cards.png")

            // Remount UI state; a new SQL driver below checks persisted data separately.
            compose.runOnUiThread { host.setContent {} }
            compose.waitForIdle()
            showFixture { ChatScreen(character = character,
                onOpenWorldLocation = { openedLocation.set(it) }) }
            waitForTag("chat_screen")
            val freshDriver = ChatDriverFactory(context.applicationContext).createDriver()
            try {
                val reopened: ChatRepository = SqlDelightChatRepository(
                    ChatDatabase(freshDriver), UuidIdGenerator(), SystemEpochClock())
                assertEquals(RichMessageStatus.OPENED, richStatus(reopened, session.id, red.id))
                assertEquals(RichMessageStatus.DECLINED, richStatus(reopened, session.id, transfer.id))
                val savedQuote = reopened.getResolvedTurns(session.id).single { it.id == quote.id }
                    .activeVariant
                assertEquals(original.id, savedQuote?.quoteMessageId)
                assertEquals("［QA］早点睡。", savedQuote?.quotePreview)
            } finally { freshDriver.close() }
            scrollToMessage(red.id)
            assertTrue(compose.onAllNodesWithTag("rich_red_packet_open_0", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty())
            compose.onNodeWithText("已打开", useUnmergedTree = true).assertIsDisplayed()
            capture("rich-reopened.png")
        } finally {
            // Stop observing this temporary conversation before deleting it, otherwise
            // ChatScreen could recreate the QA session during its next recomposition.
            compose.runOnUiThread { host.setContent {} }
            compose.waitForIdle()
            chat.clearSession(session.id)
            // Card actions intentionally produce world/relationship evidence. Remove only this
            // unique QA identity; every pre-existing user/character row is kept untouched.
            AiluaLocalStore.saveWorldEvents(
                AiluaLocalStore.savedWorldEvents.value.filterNot { it.characterId == qaId },
            )
            AiluaLocalStore.saveRelationships(
                AiluaLocalStore.savedRelationships.value.filterNot {
                    it.fromCharacterId == qaId || it.toCharacterId == qaId
                },
            )
            AiluaLocalStore.saveRomanceStates(
                AiluaLocalStore.savedRomanceStates.value.filterNot { it.characterId == qaId },
            )
            RelationshipStateRepository.restore()
            assertTrue(AiluaLocalStore.savedWorldEvents.value.none { it.characterId == qaId })
            assertTrue(AiluaLocalStore.savedRelationships.value.none {
                it.fromCharacterId == qaId || it.toCharacterId == qaId
            })
            assertTrue(AiluaLocalStore.savedRomanceStates.value.none { it.characterId == qaId })
            assertNull("Only the dedicated QA session may be removed", chat.getSession(session.id))
        }
    }

    @Test
    fun checkPhoneHeAndXuShowDistinctRealProjectionsAndCurrentPrivacy() {
        ready()
        assumeTrue("Official personas must not be overridden for this comparison",
            AiluaLocalStore.customCards.value.none { it.data.id == "hewenchuan" || it.data.id == "yuna" })
            val he = phoneSnapshot("hewenchuan")
            val xu = phoneSnapshot("yuna")
            assertTrue("He needs a visible search trace", he.searches.isNotEmpty())
            assertTrue("Xu needs a visible search trace", xu.searches.isNotEmpty())
            assertNotEquals("Two characters must not share one phone history",
                he.searches.map { it.query }, xu.searches.map { it.query })

            showFixture { CheckPhoneScreen(characterId = "hewenchuan", characterName = "贺闻川") }
            waitForTag("check_phone_screen")
            compose.onNodeWithText("贺闻川的手机").assertIsDisplayed()
            capture("checkphone-he.png")
            compose.onNodeWithTag("check_phone_section_search").performClick()
            compose.onNodeWithText(he.searches.first().query).assertIsDisplayed()

            showFixture { CheckPhoneScreen(characterId = "yuna", characterName = "许朝颜") }
            waitForTag("check_phone_screen")
            compose.onNodeWithText("许朝颜的手机").assertIsDisplayed()
            capture("checkphone-xu.png")
            compose.onNodeWithTag("check_phone_section_drafts").performClick()
            if (xu.canSee(PhoneSection.DRAFTS)) {
                if (xu.drafts.isEmpty()) compose.onNodeWithText("还没有未发出的草稿").assertIsDisplayed()
                else compose.onNodeWithText(xu.drafts.first().text).assertIsDisplayed()
            } else {
                compose.onNodeWithText("这部分内容暂时无法查看").assertIsDisplayed()
            }
            capture("checkphone-xu-drafts.png")
    }

    private fun richTurn(chat: ChatRepository, sessionId: String, payload: RichMessagePayload): ChatTurn {
        val turn = chat.appendAssistantTurn(sessionId, "", status = VariantStatus.COMPLETE,
            model = "test-only-local-fixture")
        chat.updateVariant(checkNotNull(turn.activeVariantId), "", VariantStatus.COMPLETE,
            richPayloads = listOf(payload))
        return turn
    }

    private fun richStatus(chat: ChatRepository, sessionId: String, turnId: String): RichMessageStatus? =
        chat.getResolvedTurns(sessionId).single { it.id == turnId }.activeVariant
            ?.richPayloads?.singleOrNull()?.status

    private fun phoneSnapshot(id: String) = projectCheckPhonePlus(
        characterId = id,
        events = WorldStateRepository.events.value,
        profile = CharacterRuntimeResolver.resolve(id),
        galleryAssets = GalleryRepository.assets.value,
        callHistory = CallStateEngine.callHistory.value,
        worldPlan = AiluaLocalStore.savedWorldPlan.value,
        relationships = RelationshipStateRepository.states.value,
        romanceRecord = RomanceRepository.records.value.firstOrNull { it.characterId == id },
        firedActionIds = AiluaLocalStore.getFiredWorldActionIds(),
        clock = WorldHeartbeatEngine.worldClock.value,
    )

    private fun ready() {
        instrumentation.uiAutomation.executeShellCommand(
            "am start -n ${context.packageName}/com.example.V61QaHostActivity",
        ).use { input -> android.os.ParcelFileDescriptor.AutoCloseInputStream(input).use { it.readBytes() } }
        compose.waitUntil(15_000) {
            var found: V61QaHostActivity? = null
            instrumentation.runOnMainSync {
                found = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<V61QaHostActivity>()
                    .firstOrNull()
            }
            if (found != null) host = found!!
            found != null
        }
        assumeTrue("Complete onboarding before device QA", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Finish the current call before device QA", CallStateEngine.currentCall.value == null)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        assumeTrue("Unlock the physical device before device QA", keyguard?.isKeyguardLocked != true)
    }

    private fun showFixture(content: @Composable () -> Unit) {
        val clock = WorldHeartbeatEngine.worldClock.value
        val dark = context.getSharedPreferences("ailua_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("dark_theme", false)
        val runtime = ThemeResolver.resolve(ThemeStore.selection, dark, clock.dayPhase, clock.weather)
        compose.runOnUiThread {
            host.setContent {
                AiluaTheme(darkTheme = dark) { AiluaThemeProvider(runtime) { content() } }
            }
        }
        compose.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun scrollToMessage(turnId: String) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
            hasAnyAncestor(hasTestTag("chat_screen")))
            .performScrollToNode(hasTestTag("chat_message_$turnId"))
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        runCatching {
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally { bitmap.recycle() }
        }
    }

}

private val QA_ID_PATTERN = Regex("^qa_v61_[0-9a-f_]{36}$")
private fun isQaCharacter(id: String): Boolean = QA_ID_PATTERN.matches(id)

/** Runs without MainActivity or a Compose rule so cleanup works if MIUI steals focus. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29, maxSdkVersion = 29)
class V61QaCleanupTest {
    /** Recovery for a runner killed before its finally block. Reads only QA session keys in place. */
    @Test
    fun cleanupOnlyInterruptedQaSessions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AiluaLocalStore.init(context)
        MemoryGraph.init(context)
        val dbPath = context.getDatabasePath(ChatDriverFactory.DEFAULT_DATABASE_NAME)
        val qaSessions = if (!dbPath.isFile) emptyList() else {
            val sqlite = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                sqlite.rawQuery(
                    "SELECT id, character_id FROM chat_session WHERE character_id GLOB 'qa_v61_*'",
                    null,
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            val id = cursor.getString(0)
                            val characterId = cursor.getString(1)
                            if (isQaCharacter(characterId)) add(id to characterId)
                        }
                    }
                }
            } finally { sqlite.close() }
        }
        val qaIds = (qaSessions.map { it.second } +
            AiluaLocalStore.savedWorldEvents.value.map { it.characterId } +
            AiluaLocalStore.savedRomanceStates.value.map { it.characterId } +
            AiluaLocalStore.savedRelationships.value.flatMap { listOf(it.fromCharacterId, it.toCharacterId) })
            .filter(::isQaCharacter).toSet()
        val chat: ChatRepository = SqlDelightChatRepository(MemoryGraph.database, UuidIdGenerator(), SystemEpochClock())
        qaSessions.forEach { (sessionId, _) -> chat.clearSession(sessionId) }
        if (qaIds.isNotEmpty()) {
            AiluaLocalStore.saveWorldEvents(
                AiluaLocalStore.savedWorldEvents.value.filterNot { it.characterId in qaIds },
            )
            AiluaLocalStore.saveRelationships(
                AiluaLocalStore.savedRelationships.value.filterNot {
                    it.fromCharacterId in qaIds || it.toCharacterId in qaIds
                },
            )
            AiluaLocalStore.saveRomanceStates(
                AiluaLocalStore.savedRomanceStates.value.filterNot { it.characterId in qaIds },
            )
            RelationshipStateRepository.restore()
        }
        qaSessions.forEach { (sessionId, _) -> assertNull(chat.getSession(sessionId)) }
        assertTrue(AiluaLocalStore.savedWorldEvents.value.none { isQaCharacter(it.characterId) })
        assertTrue(AiluaLocalStore.savedRelationships.value.none {
            isQaCharacter(it.fromCharacterId) || isQaCharacter(it.toCharacterId)
        })
        assertTrue(AiluaLocalStore.savedRomanceStates.value.none { isQaCharacter(it.characterId) })
        // The two-phase cold-start fixture stores only IDs; clear a stale marker if phase two
        // was interrupted after its dedicated session was removed above.
        context.getSharedPreferences("v61_cold_restart_qa", 0).edit().clear().commit()
        println("V6 QA cleanup: ${qaSessions.size} dedicated sessions, ${qaIds.size} identities")
    }

}
