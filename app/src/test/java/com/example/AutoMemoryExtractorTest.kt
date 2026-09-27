package com.example

import com.example.data.ai.model.AiRole
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.chat.local.SqlDelightMemoryRepository
import com.example.data.chat.model.VariantStatus
import com.example.data.memory.auto.AutoMemoryExtractor
import com.example.data.memory.model.MemoryType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3D-1 auto-memory extraction: threshold, transcript shape, cursor advance
 * rules, content dedupe, and "failures never touch chat" safety.
 */
class AutoMemoryExtractorTest {

    private class Fixture {
        val chat = ChatTestHarness.inMemory()
        val memoryRepository = SqlDelightMemoryRepository(chat.database, chat.idGenerator, chat.clock)
        val resolver = FakeProviderResolver(null)
        val extractor = AutoMemoryExtractor(
            chatRepository = chat.repository,
            memoryRepository = memoryRepository,
            cursorQueries = chat.database.memoryExtractCursorQueries,
            providerResolver = resolver,
            clock = chat.clock,
            characterName = "Mira",
        )

        fun use(provider: AiProvider) {
            resolver.resolved = ResolvedProvider(
                profileId = "prof-1",
                model = "test-model",
                provider = provider,
            )
        }

        /** Alternating user/assistant COMPLETE turns: `消息0`/`回复1`/`消息2`/… */
        fun seedTurns(characterId: String, count: Int): String {
            val session = chat.repository.getOrCreatePrivateSession(characterId).id
            repeat(count) { i ->
                if (i % 2 == 0) {
                    chat.repository.appendUserTurn(session, "消息$i")
                } else {
                    chat.repository.appendAssistantTurn(
                        sessionId = session,
                        content = "回复$i",
                        status = VariantStatus.COMPLETE,
                        providerProfileId = "prof-1",
                        model = "test-model",
                    )
                }
            }
            return session
        }

        fun cursor(characterId: String, sessionId: String) =
            chat.database.memoryExtractCursorQueries
                .selectMemoryExtractCursor("$characterId:$sessionId")
                .executeAsOneOrNull()
    }

    private val validJson =
        """{"memories":[{"content":"用户喜欢雨天","importance":0.8},{"content":"用户在杭州工作","importance":0.6}]}"""

    @Test
    fun belowThresholdDoesNothing() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 11)
        val fake = FakeAiProvider.scripted(validJson)
        f.use(fake)

        f.extractor.run("mira", session)

        assertEquals(0, fake.requests.size)
        assertTrue(f.memoryRepository.getMemories("mira").isEmpty())
        assertNull(f.cursor("mira", session))
    }

    @Test
    fun missingProviderIsNoOp() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 12)

        f.extractor.run("mira", session)

        assertTrue(f.memoryRepository.getMemories("mira").isEmpty())
        assertNull(f.cursor("mira", session))
    }

    @Test
    fun thresholdSavesMemoriesAndAdvancesCursor() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 12)
        val fake = FakeAiProvider.scripted(validJson)
        f.use(fake)

        f.extractor.run("mira", session)

        assertEquals(1, fake.requests.size)
        val request = fake.requests.single()
        assertFalse(request.stream)
        assertEquals("test-model", request.model)
        assertEquals(AiRole.SYSTEM, request.messages.first().role)
        val transcript = request.messages.joinToString("\n") { it.content }
        assertTrue(transcript.contains("用户：消息0"))
        assertTrue(transcript.contains("Mira：回复1"))

        val memories = f.memoryRepository.getMemories("mira")
        assertEquals(2, memories.size)
        memories.forEach {
            assertEquals("chat-auto", it.sourceAppId)
            assertEquals(MemoryType.LONG_TERM, it.type)
            assertTrue(it.sourceRefId!!.startsWith("batch:"))
        }
        assertNotNull(f.cursor("mira", session))

        f.extractor.run("mira", session)
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun modelFailureKeepsCursorForRetry() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 12)
        f.use(FakeAiProvider.unauthorized())

        f.extractor.run("mira", session)
        assertTrue(f.memoryRepository.getMemories("mira").isEmpty())
        assertNull(f.cursor("mira", session))

        val fake = FakeAiProvider.scripted(validJson)
        f.use(fake)
        f.extractor.run("mira", session)

        assertEquals(1, fake.requests.size)
        assertEquals(2, f.memoryRepository.getMemories("mira").size)
        assertNotNull(f.cursor("mira", session))
    }

    @Test
    fun invalidJsonKeepsCursorForRetry() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 12)
        f.use(FakeAiProvider.scripted("这不是JSON"))

        f.extractor.run("mira", session)
        assertTrue(f.memoryRepository.getMemories("mira").isEmpty())
        assertNull(f.cursor("mira", session))

        val fake = FakeAiProvider.scripted(validJson)
        f.use(fake)
        f.extractor.run("mira", session)

        assertEquals(1, fake.requests.size)
        assertEquals(2, f.memoryRepository.getMemories("mira").size)
        assertNotNull(f.cursor("mira", session))
    }

    @Test
    fun duplicateContentIsSkippedButCursorAdvances() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 12)
        f.memoryRepository.saveMemory(
            characterId = "mira",
            content = "用户喜欢雨天。",
            sourceAppId = "chat",
            sourceRefId = "msg-1",
        )
        val fake = FakeAiProvider.scripted(
            """{"memories":[{"content":"用户喜欢雨天","importance":0.9}]}""",
        )
        f.use(fake)

        f.extractor.run("mira", session)

        val memories = f.memoryRepository.getMemories("mira")
        assertEquals(1, memories.size)
        assertEquals("chat", memories.single().sourceAppId)
        assertNotNull(f.cursor("mira", session))
    }

    @Test
    fun emptyMemoryBatchStillAdvancesCursor() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 12)
        val fake = FakeAiProvider.scripted("""{"memories":[]}""")
        f.use(fake)

        f.extractor.run("mira", session)

        assertEquals(1, fake.requests.size)
        assertTrue(f.memoryRepository.getMemories("mira").isEmpty())
        assertNotNull(f.cursor("mira", session))
    }

    @Test
    fun incompleteTurnsDoNotCountTowardThreshold() = runBlocking {
        val f = Fixture()
        val session = f.seedTurns("mira", 11)
        f.chat.repository.appendAssistantTurn(
            sessionId = session,
            content = "被取消",
            status = VariantStatus.CANCELLED,
            providerProfileId = "prof-1",
            model = "test-model",
        )
        val fake = FakeAiProvider.scripted(validJson)
        f.use(fake)

        f.extractor.run("mira", session)

        assertEquals(0, fake.requests.size)
        assertTrue(f.memoryRepository.getMemories("mira").isEmpty())
        assertNull(f.cursor("mira", session))
    }
}
