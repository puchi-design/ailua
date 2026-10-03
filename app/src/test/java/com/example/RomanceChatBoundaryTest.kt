package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.SendResult
import com.example.data.local.AiluaLocalStore
import com.example.data.relationship.romance.RomanceRecord
import com.example.data.relationship.romance.RomanceRepository
import com.example.data.relationship.romance.RomanceState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Exercise the real SQL-backed send pipeline; an AI response is never permission to enforce a user's boundary. */
class RomanceChatBoundaryTest {
    @Test fun failedOrCancelledProviderCannotLoseAnAlreadyPersistedUserBoundary() = runBlocking {
        val original = AiluaLocalStore.savedRomanceStates.value
        try {
            for (provider in listOf(FakeAiProvider.unauthorized(), FakeAiProvider.cancelAfter(1))) {
                val fixture = ChatRuntimeFixture()
                try {
                    AiluaLocalStore.saveRomanceStates(listOf(RomanceRecord("mira", state = RomanceState(.9f, .9f, .9f, .9f),
                        romanticConfirmedAtEpochMs = fixture.chat.clock.now - 1_000)))
                    RomanceRepository.restore()
                    fixture.use(provider)
                    val outcome = fixture.runtime.send("mira", "我不想和你恋爱，别再追求我。")
                    assertTrue(outcome is SendResult.Failed || outcome == SendResult.Cancelled)
                    assertEquals(2, fixture.turns("mira").size)
                    assertTrue(RomanceRepository.record("mira").romanceDeclined)
                    assertNull(RomanceRepository.record("mira").romanticConfirmedAtEpochMs)
                    RomanceRepository.restore()
                    assertTrue(RomanceRepository.record("mira").romanceDeclined)
                } finally { fixture.chat.driver.close() }
            }
        } finally { AiluaLocalStore.saveRomanceStates(original); RomanceRepository.restore() }
    }

    @Test fun providerPreflightFailureDoesNotInventAnUnsentBoundaryFact() = runBlocking {
        val original = AiluaLocalStore.savedRomanceStates.value
        val fixture = ChatRuntimeFixture()
        try {
            val previous = RomanceRecord("mira", romanticConfirmedAtEpochMs = fixture.chat.clock.now - 1_000)
            AiluaLocalStore.saveRomanceStates(listOf(previous))
            RomanceRepository.restore()
            assertEquals(SendResult.NotConfigured, fixture.runtime.send("mira", "我们只做朋友。"))
            assertTrue(fixture.turns("mira").isEmpty())
            assertEquals(previous, RomanceRepository.record("mira"))
        } finally {
            fixture.chat.driver.close()
            AiluaLocalStore.saveRomanceStates(original)
            RomanceRepository.restore()
        }
    }
}
