package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.chat.local.ChatDatabase
import com.example.data.chat.local.ChatDriverFactory
import com.example.data.chat.local.SqlDelightChatRepository
import com.example.data.chat.local.platform.SystemEpochClock
import com.example.data.chat.local.platform.UuidIdGenerator
import com.example.data.chat.model.VariantStatus
import com.example.data.chat.repository.ChatRepository
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import com.example.data.memory.repository.MemoryGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Two opt-in instrument invocations, separated by `adb shell am force-stop` and app launch. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29, maxSdkVersion = 29)
class V61ColdRestartDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val prefs get() = context.getSharedPreferences("v61_cold_restart_qa", 0)
    private fun repository(): ChatRepository =
        SqlDelightChatRepository(MemoryGraph.database, UuidIdGenerator(), SystemEpochClock())

    @Test
    fun prepareOpenedPacketAcceptedTransferAndQuote() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("qaColdPhase") == "prepare")
        MemoryGraph.init(context)
        assertNull("Previous QA fixture must be cleaned first", prefs.getString("session", null))
        val characterId = "qa_v61_${UUID.randomUUID().toString().replace('-', '_')}"
        val chat = repository()
        val session = chat.getOrCreatePrivateSession(characterId)
        assertTrue(chat.getResolvedTurns(session.id).isEmpty())
        val original = chat.appendAssistantTurn(session.id, "［QA］早点睡。")
        val quote = chat.appendUserTurn(session.id, "［QA］你也早点睡。", original.id, "［QA］早点睡。")
        val packet = chat.appendAssistantTurn(session.id, "", VariantStatus.COMPLETE)
        chat.updateVariant(checkNotNull(packet.activeVariantId), "", VariantStatus.COMPLETE,
            richPayloads = listOf(RichMessagePayload(RichMessageType.RED_PACKET, "晚饭钱", 30.0, "¥",
                RichMessageStatus.PENDING)))
        assertTrue(chat.updateRichStatus(packet.activeVariantId, 0, RichMessageStatus.OPENED))
        val transfer = chat.appendAssistantTurn(session.id, "", VariantStatus.COMPLETE)
        chat.updateVariant(checkNotNull(transfer.activeVariantId), "", VariantStatus.COMPLETE,
            richPayloads = listOf(RichMessagePayload(RichMessageType.TRANSFER, "今天辛苦了", 18.0, "¥",
                RichMessageStatus.PENDING)))
        assertTrue(chat.updateRichStatus(transfer.activeVariantId, 0, RichMessageStatus.ACCEPTED))
        assertTrue(prefs.edit()
            .putString("session", session.id)
            .putString("original", original.id)
            .putString("quote", quote.id)
            .putString("packet", packet.id)
            .putString("transfer", transfer.id)
            .commit())
    }

    @Test
    fun verifyAfterAppProcessRestart() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("qaColdPhase") == "verify")
        val sessionId = checkNotNull(prefs.getString("session", null))
        val originalId = checkNotNull(prefs.getString("original", null))
        val quoteId = checkNotNull(prefs.getString("quote", null))
        val packetId = checkNotNull(prefs.getString("packet", null))
        val transferId = checkNotNull(prefs.getString("transfer", null))
        MemoryGraph.init(context)
        val chat = repository()
        try {
            val driver = ChatDriverFactory(context.applicationContext).createDriver()
            try {
                val reopened: ChatRepository = SqlDelightChatRepository(
                    ChatDatabase(driver), UuidIdGenerator(), SystemEpochClock())
                val turns = reopened.getResolvedTurns(sessionId).associateBy { it.id }
                assertEquals(RichMessageStatus.OPENED,
                    turns.getValue(packetId).activeVariant?.richPayloads?.single()?.status)
                assertEquals(RichMessageStatus.ACCEPTED,
                    turns.getValue(transferId).activeVariant?.richPayloads?.single()?.status)
                assertEquals(originalId, turns.getValue(quoteId).activeVariant?.quoteMessageId)
                assertEquals("［QA］早点睡。", turns.getValue(quoteId).activeVariant?.quotePreview)
            } finally { driver.close() }
        } finally {
            chat.clearSession(sessionId)
            prefs.edit().clear().commit()
            assertNull(chat.getSession(sessionId))
        }
    }
}
