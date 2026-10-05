package com.example

import com.example.data.chat.rich.RichInteractionEvidence
import com.example.data.chat.rich.RichMessagePayload
import com.example.data.chat.rich.RichMessageStatus
import com.example.data.chat.rich.RichMessageType
import com.example.data.chat.model.VariantStatus
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LifeEventType
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.relationship.romance.RomanceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichMessageEvidenceTest {
    @Test fun staleOrIncompleteAssistantVariantCannotAcceptCard() {
        val f = ChatTestHarness.inMemory()
        try {
            val session = f.repository.getOrCreatePrivateSession("hewenchuan")
            val turn = f.repository.appendAssistantTurn(session.id, "", VariantStatus.STREAMING)
            val packet = RichMessagePayload(RichMessageType.RED_PACKET, "晚饭钱", 30.0, "¥", RichMessageStatus.PENDING)
            f.repository.updateVariant(turn.activeVariantId!!, "", VariantStatus.STREAMING, richPayloads = listOf(packet))
            assertFalse(f.repository.updateRichStatus(turn.activeVariantId, 0, RichMessageStatus.OPENED))
            f.repository.updateVariant(turn.activeVariantId, "", VariantStatus.COMPLETE, richPayloads = listOf(packet))
            val replacement = f.repository.appendVariant(turn.id, "再想想", VariantStatus.COMPLETE)
            assertFalse(f.repository.updateRichStatus(turn.activeVariantId, 0, RichMessageStatus.OPENED))
            assertFalse(f.repository.updateRichStatus(replacement.id, 0, RichMessageStatus.OPENED))
        } finally { f.driver.close() }
    }

    @Test fun acceptedVirtualTransferCreatesOneWorldFactAndOneLightRomanceEvidence() {
        val suffix = System.nanoTime().toString()
        val variant = "variant_$suffix"
        val payload = RichMessagePayload(RichMessageType.TRANSFER, "午饭钱", 18.0, "¥", RichMessageStatus.PENDING)
        val evidenceId = "rich_${variant}_0_accepted"
        repeat(2) {
            RichInteractionEvidence.record("hewenchuan", "turn_$suffix", variant, 0, payload, RichMessageStatus.ACCEPTED)
        }
        assertEquals(1, WorldStateRepository.eventsForSource("chat").count { it.id == evidenceId })
        assertTrue("rich:$evidenceId" in RomanceRepository.record("hewenchuan").processedEventIds)
    }

    @Test fun decliningVirtualTransferDoesNotAdvanceRelationship() {
        val characterId = "decline_test_${System.nanoTime()}"
        val variant = "variant_$characterId"
        val payload = RichMessagePayload(RichMessageType.TRANSFER, "午饭钱", 18.0, "¥", RichMessageStatus.PENDING)
        val before = RelationshipStateRepository.states.value
        repeat(2) {
            RichInteractionEvidence.record(characterId, "turn_$characterId", variant, 0, payload,
                RichMessageStatus.DECLINED)
        }
        val eventId = "rich_${variant}_0_declined"
        val events = WorldStateRepository.eventsForSource("chat").filter { it.id == eventId }
        assertEquals(1, events.size)
        assertEquals(LifeEventType.SOCIAL, events.single().type)
        assertEquals(before, RelationshipStateRepository.states.value)
    }
}
