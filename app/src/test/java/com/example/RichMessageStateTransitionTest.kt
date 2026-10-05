package com.example

import com.example.data.chat.model.VariantStatus
import com.example.data.chat.rich.RichMessageParser
import com.example.data.chat.rich.RichMessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichMessageStateTransitionTest {
    @Test fun transferCanBeAcceptedOrDeclinedExactlyOnce() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("hewenchuan")
            val accept = assistant(f, session.id, "[转账:18:午餐]")
            val decline = assistant(f, session.id, "[转账:8:咖啡]")
            assertTrue(f.repository.updateRichStatus(accept, 0, RichMessageStatus.ACCEPTED))
            assertFalse(f.repository.updateRichStatus(accept, 0, RichMessageStatus.ACCEPTED))
            assertFalse(f.repository.updateRichStatus(accept, 0, RichMessageStatus.DECLINED))
            assertTrue(f.repository.updateRichStatus(decline, 0, RichMessageStatus.DECLINED))
            assertEquals(listOf(RichMessageStatus.ACCEPTED, RichMessageStatus.DECLINED),
                f.repository.getResolvedTurns(session.id).map { it.activeVariant!!.richPayloads.single().status })
        } finally { f.driver.close() }
    }

    @Test fun redPacketOpenAndGiftReceiveAreDurableAndTypeChecked() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("mira")
            val packet = assistant(f, session.id, "[红包:20:点心]")
            val gift = assistant(f, session.id, "[礼物:一束花]")
            assertFalse(f.repository.updateRichStatus(packet, 0, RichMessageStatus.ACCEPTED))
            assertTrue(f.repository.updateRichStatus(packet, 0, RichMessageStatus.OPENED))
            assertFalse(f.repository.updateRichStatus(packet, 0, RichMessageStatus.OPENED))
            assertTrue(f.repository.updateRichStatus(gift, 0, RichMessageStatus.RECEIVED))
            assertFalse(f.repository.updateRichStatus("missing", 0, RichMessageStatus.OPENED))
        } finally { f.driver.close() }
    }

    private fun assistant(f: ChatTestHarness, sessionId: String, raw: String): String {
        val turn = f.repository.appendAssistantTurn(sessionId, "", VariantStatus.STREAMING)
        val parsed = RichMessageParser.parse(raw)
        f.repository.updateVariant(turn.activeVariantId!!, parsed.content, VariantStatus.COMPLETE,
            richPayloads = parsed.payloads)
        return turn.activeVariantId
    }
}
