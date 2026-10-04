package com.example

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.runtime.SendResult
import com.example.data.chat.model.ChatSessionType
import com.example.data.chat.model.ChatTurnRole
import com.example.data.chat.model.GroupChatIdentity
import com.example.data.chat.model.GroupMessage
import com.example.data.chat.model.GroupReply
import com.example.data.chat.model.GroupSpeakerPlanner
import com.example.data.chat.model.VariantStatus
import com.example.data.engine.WorldStateRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class GroupChatRuntimeTest {
    private val participants = listOf("mira", "yuna", "noa")

    @Test fun identityAndSpeakerValidation() {
        val key = GroupChatIdentity.key("rain_tea", participants)
        assertTrue(GroupChatIdentity.isGroup(key))
        assertEquals(participants, GroupChatIdentity.participants(key))
        assertEquals(listOf("mira"), GroupSpeakerPlanner.choose("大家好", participants, emptyMap(), 0))
        assertEquals(listOf("mira", "yuna"), GroupSpeakerPlanner.choose("小弥和悠奈呢？", participants,
            mapOf("mira" to "小弥", "yuna" to "悠奈", "noa" to "诺亚"), 0))
        assertEquals(GroupReply("mira", "你好"), GroupMessage.decode(GroupMessage.encode(GroupReply("mira", "你好"), participants), participants))
        assertThrows(IllegalArgumentException::class.java) { GroupMessage.encode(GroupReply("outsider", "你好"), participants) }
        assertNull(GroupMessage.decode("outsider\n你好", participants))
    }

    @Test fun groupSessionAndTurnsSurviveSqliteRestart() {
        val file = Files.createTempFile("ailua-group", ".db").toFile()
        try {
            val first = ChatTestHarness.file(file)
            val session = first.repository.getOrCreateGroupSession("rain_tea", participants)
            assertEquals(ChatSessionType.GROUP, session.type)
            assertEquals(participants, session.participantCharacterIds)
            first.repository.appendUserTurn(session.id, "你好")
            first.repository.appendAssistantTurn(session.id, GroupMessage.encode(GroupReply("mira", "你好"), participants))
            first.driver.close()

            val reopened = ChatTestHarness.file(file, createSchema = false)
            val restored = reopened.repository.getOrCreateGroupSession("rain_tea", participants)
            assertEquals(session.id, restored.id)
            assertEquals(2, reopened.repository.getResolvedTurns(restored.id).size)
            reopened.idGenerator.forcedIds.add("private-after-restart")
            assertEquals(0, reopened.repository.getResolvedTurns(reopened.repository.getOrCreatePrivateSession("mira").id).size)
            reopened.driver.close()
        } finally { file.delete() }
    }

    @Test fun oneAndTwoSpeakerRepliesRegenerateAndMemoryStayIsolated() = runBlocking {
        val fixture = ChatRuntimeFixture()
        val provider = object : AiProvider {
            override fun streamChat(request: AiChatRequest) = flow {
                emit(AiStreamEvent.Completed("收到，我在这里。"))
            }
        }
        fixture.use(provider)
        assertEquals(SendResult.Completed, fixture.runtime.sendGroup("rain_tea", participants, "我叫小雨"))
        val session = fixture.repository.getOrCreateGroupSession("rain_tea", participants)
        val initial = fixture.repository.getResolvedTurns(session.id)
        assertEquals(2, initial.size)
        assertEquals("mira", GroupMessage.decode(initial.last().activeVariant!!.content, participants)?.characterId)
        assertEquals(1, fixture.memoryRepository.getMemories("mira").size)
        assertTrue(fixture.memoryRepository.getMemories("yuna").isEmpty())

        assertEquals(SendResult.Completed, fixture.runtime.sendGroup("rain_tea", participants, "苏晚宁和许朝颜今天怎么样？"))
        val afterTwo = fixture.repository.getResolvedTurns(session.id)
        assertEquals(5, afterTwo.size)
        assertEquals(listOf("mira", "yuna"), afterTwo.takeLast(2).mapNotNull {
            GroupMessage.decode(it.activeVariant?.content.orEmpty(), participants)?.characterId
        })
        val secondTurnEvents = WorldStateRepository.events.value.filter { it.sourceAppId == "group_chat" && it.sourceRefId == afterTwo[2].id }
        assertEquals(setOf("mira", "yuna"), secondTurnEvents.filter { "user" in it.relatedCharacterIds }.map { it.characterId }.toSet())
        assertEquals(listOf("mira"), secondTurnEvents.single { it.id.startsWith("group_exchange_") }.relatedCharacterIds)
        assertFalse(secondTurnEvents.any { it.characterId == "noa" })
        assertEquals(SendResult.Completed, fixture.runtime.regenerateGroup("rain_tea", participants))
        val last = fixture.repository.getResolvedTurns(session.id).last()
        assertEquals(ChatTurnRole.ASSISTANT, last.role)
        assertEquals(2, fixture.repository.getVariants(last.id).size)
        assertEquals(VariantStatus.COMPLETE, last.activeVariant?.status)
        fixture.chat.driver.close()
    }
}
