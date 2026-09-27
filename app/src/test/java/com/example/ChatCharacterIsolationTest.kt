package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.chat.model.ChatTurnRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatCharacterIsolationTest — P3C-4: each character keeps its own canonical
 * session and its own prompt history end-to-end through the runtime.
 */
class ChatCharacterIsolationTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun charactersGetSeparateSessionsAndHistories() = runBlocking {
        fixture.use(FakeAiProvider.scripted("回复"))
        fixture.runtime.send("mira", "mira的秘密")
        fixture.runtime.send("yuna", "yuna的秘密")

        val miraSession = fixture.repository.getOrCreatePrivateSession("mira")
        val yunaSession = fixture.repository.getOrCreatePrivateSession("yuna")
        assertNotEquals(miraSession.id, yunaSession.id)

        val miraTurns = fixture.repository.getResolvedTurns(miraSession.id)
        val yunaTurns = fixture.repository.getResolvedTurns(yunaSession.id)
        assertEquals(2, miraTurns.size)
        assertEquals(2, yunaTurns.size)

        val miraText = miraTurns.joinToString { it.activeVariant?.content ?: "" }
        assertTrue(miraText.contains("mira的秘密"))
        assertTrue(!miraText.contains("yuna的秘密"))

        val yunaText = yunaTurns.joinToString { it.activeVariant?.content ?: "" }
        assertTrue(yunaText.contains("yuna的秘密"))
        assertTrue(!yunaText.contains("mira的秘密"))
    }

    @Test
    fun canonicalSessionIsStableAcrossCalls() = runBlocking {
        fixture.use(FakeAiProvider.scripted("回复"))
        fixture.runtime.send("mira", "hi")

        val first = fixture.repository.getOrCreatePrivateSession("mira")
        val second = fixture.repository.getOrCreatePrivateSession("mira")
        assertEquals(first.id, second.id)

        val turns = fixture.repository.getResolvedTurns(second.id)
        assertEquals(2, turns.size)
        assertTrue(turns.all { it.sessionId == first.id })
        assertTrue(turns.all { it.role == ChatTurnRole.USER || it.role == ChatTurnRole.ASSISTANT })
    }

    @Test
    fun promptHistoryIsPerCharacterCard() = runBlocking {
        fixture.use(FakeAiProvider.scripted("回复"))
        fixture.runtime.send("mira", "mira消息")
        fixture.runtime.send("yuna", "yuna消息")

        val miraRequests = fixture.turns("mira").size
        val yunaRequests = fixture.turns("yuna").size
        assertEquals(2, miraRequests)
        assertEquals(2, yunaRequests)
    }
}
