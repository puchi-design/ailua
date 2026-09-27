package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.SendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ChatProviderMissingTest — P3C-4 §5: without a configured provider (or a
 * missing character) send must surface the preflight result and write
 * NOTHING — no session, no user turn, no variant.
 */
class ChatProviderMissingTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun missingProviderReturnsNotConfiguredAndWritesNothing() = runBlocking {
        fixture.removeProvider()

        val result = fixture.runtime.send("mira", "今天好累")

        assertEquals(SendResult.NotConfigured, result)
        assertEquals(0L, fixture.chat.database.chatSessionQueries.countSessions().executeAsOne())
        assertEquals(0L, fixture.chat.database.chatTurnQueries.countTurns().executeAsOne())
        assertEquals(0L, fixture.chat.database.chatVariantQueries.countVariants().executeAsOne())
    }

    @Test
    fun missingProviderBlocksRegenerateToo() = runBlocking {
        fixture.removeProvider()

        val result = fixture.runtime.regenerate("mira")

        assertEquals(SendResult.NotConfigured, result)
        assertEquals(0L, fixture.chat.database.chatTurnQueries.countTurns().executeAsOne())
    }

    @Test
    fun missingCharacterReturnsNoCharacterAndWritesNothing() = runBlocking {
        fixture.use(FakeAiProvider.scripted("嗨"))

        val result = fixture.runtime.send("ghost-character", "hi")

        assertEquals(SendResult.NoCharacter, result)
        assertEquals(0L, fixture.chat.database.chatSessionQueries.countSessions().executeAsOne())
        assertEquals(0L, fixture.chat.database.chatTurnQueries.countTurns().executeAsOne())
    }
}
