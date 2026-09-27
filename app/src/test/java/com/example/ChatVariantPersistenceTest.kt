package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.chat.model.VariantStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatVariantPersistenceTest — P3C-4 §11: real variant switching through
 * [com.example.data.chat.repository.ChatRepository.selectVariant]; the
 * active choice survives fresh reads (i.e. app reopen).
 */
class ChatVariantPersistenceTest {

    private val fixture = ChatRuntimeFixture()

    @Test
    fun variantSwitchPersistsAcrossFreshReads() = runBlocking {
        fixture.use(FakeAiProvider.scripted("第一版"))
        fixture.runtime.send("mira", "hi")
        fixture.use(FakeAiProvider.scripted("第二版"))
        fixture.runtime.regenerate("mira")
        fixture.use(FakeAiProvider.scripted("第三版"))
        fixture.runtime.regenerate("mira")

        val session = fixture.sessionId("mira")
        val turn = fixture.repository.getResolvedTurns(session).last()
        assertEquals(3, turn.variantCount)
        assertEquals("第三版", turn.activeVariant?.content)

        val variants = fixture.repository.getVariants(turn.id)
        assertEquals(3, variants.size)
        assertEquals(listOf(0, 1, 2), variants.map { it.variantIndex })

        fixture.repository.selectVariant(turn.id, variants[0].id)

        // Fresh read = what the app sees after reopen.
        val afterReopen = fixture.repository.getResolvedTurns(session).last()
        assertEquals(variants[0].id, afterReopen.activeVariantId)
        assertEquals("第一版", afterReopen.activeVariant?.content)
        assertEquals(3, afterReopen.variantCount)

        fixture.repository.selectVariant(turn.id, variants[1].id)
        val switched = fixture.repository.getResolvedTurns(session).last()
        assertEquals("第二版", switched.activeVariant?.content)
        assertEquals(3, switched.variantCount)
    }

    @Test
    fun failedVariantSelectionIsVisibleAfterSwitch() = runBlocking {
        fixture.use(FakeAiProvider.scripted("正常"))
        fixture.runtime.send("mira", "hi")

        val session = fixture.sessionId("mira")
        val turn = fixture.repository.getResolvedTurns(session).last()
        val failed = fixture.repository.appendVariant(
            turnId = turn.id,
            content = "失败的部分文本",
            status = VariantStatus.FAILED,
            errorType = "HTTP",
            errorMessage = "HTTP 500: boom",
        )

        fixture.repository.selectVariant(turn.id, failed.id)
        val resolved = fixture.repository.getResolvedTurns(session).last()
        assertEquals(VariantStatus.FAILED, resolved.activeVariant?.status)
        assertEquals("失败的部分文本", resolved.activeVariant?.content)
        assertEquals(2, resolved.variantCount)
        assertTrue(resolved.activeVariant?.errorMessage?.contains("500") == true)
    }
}
