package com.example

import com.example.data.ai.model.AiRole
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.chat.local.SqlDelightMemoryRepository
import com.example.data.engine.ProactiveMessageEngine
import com.example.data.engine.ProactiveRules
import com.example.data.engine.WorldStateRepository
import com.example.data.mock.OfficialCharacters
import com.example.data.model.ProactiveSettings
import com.example.data.model.ProactiveState
import com.example.data.systemui.notification.VirtualNotification
import com.example.data.systemui.notification.NotificationCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * P3D-2 proactive messages: pure timing/daily rules + the engine pipeline
 * (one non-streaming call -> assistant turn + LifeEvent + state marks), with
 * every failure path writing nothing.
 */
class ProactiveMessageTest {

    // === Pure rules ===

    private val enabled = ProactiveSettings(enabled = true)
    private val noon = 12 * 60
    private val today = "2023-11-14"
    private val now = 1_700_000_000_000L // 2023-11-14T22:13:20Z

    @Test
    fun disabledNeverFires() {
        assertFalse(
            ProactiveRules.shouldFire(
                settings = enabled.copy(enabled = false),
                state = ProactiveState(),
                nowEpochMs = now,
                minuteOfDay = noon,
                today = today,
            ),
        )
    }

    @Test
    fun intervalBlocksUntilElapsed() {
        val state = ProactiveState(lastSuccessAtEpochMs = now, sentDate = today, sentCount = 1)
        assertFalse(
            ProactiveRules.shouldFire(enabled, state, now + 5 * 3_600_000L, noon, today),
        )
        assertTrue(
            ProactiveRules.shouldFire(enabled, state, now + 6 * 3_600_000L, noon, today),
        )
    }

    @Test
    fun quietWindowWrapsMidnight() {
        assertTrue(ProactiveRules.isQuiet(23 * 60 + 30, 23 * 60, 8 * 60))
        assertTrue(ProactiveRules.isQuiet(7 * 60 + 59, 23 * 60, 8 * 60))
        assertTrue(ProactiveRules.isQuiet(0, 23 * 60, 8 * 60))
        assertFalse(ProactiveRules.isQuiet(8 * 60, 23 * 60, 8 * 60))
        assertFalse(ProactiveRules.isQuiet(22 * 60, 23 * 60, 8 * 60))
        assertFalse(ProactiveRules.isQuiet(noon, 23 * 60, 8 * 60))
    }

    @Test
    fun dailyLimitBlocksSameDayOnly() {
        val state = ProactiveState(lastSuccessAtEpochMs = now, sentDate = today, sentCount = 3)
        assertFalse(ProactiveRules.shouldFire(enabled, state, now + 24 * 3_600_000L, noon, today))
        assertTrue(
            ProactiveRules.shouldFire(enabled, state, now + 24 * 3_600_000L, noon, "2023-11-15"),
        )
    }

    @Test
    fun withSuccessRollsDayCounter() {
        val rolled = ProactiveRules.withSuccess(
            ProactiveState(lastSuccessAtEpochMs = now, sentDate = "2023-11-13", sentCount = 3),
            now + 1000,
            today,
        )
        assertEquals(today, rolled.sentDate)
        assertEquals(1, rolled.sentCount)
        assertEquals(now + 1000, rolled.lastSuccessAtEpochMs)

        val incremented = ProactiveRules.withSuccess(rolled, now + 2000, today)
        assertEquals(2, incremented.sentCount)
    }

    // === Engine pipeline ===

    private class Fixture(val characterId: String = "mira") {
        val chat = ChatTestHarness.inMemory()
        val memoryRepository = SqlDelightMemoryRepository(chat.database, chat.idGenerator, chat.clock)
        val resolver = FakeProviderResolver(null)
        var settings = ProactiveSettings(enabled = true)
        var state = ProactiveState()
        var savedStates = 0
        val notifications = mutableListOf<VirtualNotification>()

        val engine = ProactiveMessageEngine(
            chatRepository = chat.repository,
            memoryRepository = memoryRepository,
            providerResolver = resolver,
            clock = chat.clock,
            loadSettings = { settings },
            loadState = { state },
            saveState = {
                state = it
                savedStates++
            },
            timeZone = TimeZone.getTimeZone("UTC"),
            recentWorldEvents = { emptyList() },
            postNotification = {
                // This callback must run only after both the turn and success state committed.
                assertTrue(savedStates > 0)
                assertTrue(characterTurns().isNotEmpty())
                notifications += it
            },
            activeCharacterId = { characterId },
        )

        fun use(provider: AiProvider) {
            resolver.resolved = ResolvedProvider(
                profileId = "prof-1",
                model = "test-model",
                provider = provider,
            )
        }

        fun characterTurns() = chat.repository
            .getResolvedTurns(chat.repository.getOrCreatePrivateSession(characterId).id)
    }

    @Test
    fun firesWhenRulesPass() = runBlocking {
        val f = Fixture()
        val fake = FakeAiProvider.scripted("今天雨还没停，我把红茶又热了一遍，等你回来。")
        f.use(fake)

        val fired = f.engine.fireIfDue()

        assertTrue(fired)
        assertEquals(1, fake.requests.size)
        assertFalse(fake.requests.single().stream)
        assertEquals(AiRole.SYSTEM, fake.requests.single().messages.first().role)
        assertTrue(fake.requests.single().messages.size >= 2)

        val turns = f.characterTurns()
        assertEquals(1, turns.size)
        assertEquals(
            "今天雨还没停，我把红茶又热了一遍，等你回来。",
            turns.single().activeVariant?.content,
        )

        val expectedId = "proactive_${1_700_000_000_000L}"
        assertTrue(WorldStateRepository.events.value.any { it.id == expectedId })
        assertEquals(1, f.state.sentCount)
        assertEquals("2023-11-14", f.state.sentDate)
        assertEquals(1_700_000_000_000L, f.state.lastSuccessAtEpochMs)
        assertEquals(NotificationCategory.MESSAGE, f.notifications.single().category)
        assertEquals("chat/mira", f.notifications.single().route)
        assertEquals("message:${turns.single().sessionId}:${turns.single().id}", f.notifications.single().sourceKey)
        assertEquals(turns.single().activeVariant?.content, f.notifications.single().body)
    }

    @Test
    fun injectedYanUsesHisBehaviorPromptAndWritesOnlyHisSessionAndNotification() = runBlocking {
        val f = Fixture(characterId = "yan")
        // Keep the ledger event distinct from the legacy Mira fixtures' fixed timestamp.
        f.chat.clock.now += 123_000L
        val reply = "修好的那册书先替你留着。什么时候方便，你决定。"
        val fake = FakeAiProvider.scripted(reply)
        f.use(fake)

        // This case verifies role routing, independent of the shared virtual clock's sleep phase.
        assertTrue(f.engine.fireIfDue(force = true))

        assertEquals(1, fake.requests.size)
        val request = fake.requests.single()
        assertFalse(request.stream)
        assertEquals(AiRole.SYSTEM, request.messages.first().role)
        val prompt = request.messages.joinToString("\n") { it.content }
        assertTrue(prompt.contains("性别：male"))
        assertTrue(prompt.contains(OfficialCharacters.yanExtension.identity.occupation))
        assertTrue(prompt.contains(OfficialCharacters.yanExtension.behavior.coreDesire))
        assertTrue(prompt.contains(OfficialCharacters.yanExtension.behavior.flaws.first()))
        assertFalse(prompt.contains(OfficialCharacters.yeoExtension.behavior.coreDesire))
        assertFalse(prompt.contains(OfficialCharacters.noaExtension.behavior.coreDesire))

        val turns = f.characterTurns()
        val yanSession = f.chat.repository.getOrCreatePrivateSession("yan")
        assertEquals(1, turns.size)
        assertEquals(yanSession.id, turns.single().sessionId)
        assertEquals(reply, turns.single().activeVariant?.content)
        val miraSession = f.chat.repository.getOrCreatePrivateSession("mira")
        assertTrue(f.chat.repository.getResolvedTurns(miraSession.id).isEmpty())

        val event = WorldStateRepository.events.value.single { it.id == "proactive_${f.chat.clock.now}" }
        assertEquals("yan", event.characterId)
        assertEquals(reply, event.description)
        assertEquals(1, f.state.sentCount)
        assertEquals("2023-11-14", f.state.sentDate)
        assertEquals(f.chat.clock.now, f.state.lastSuccessAtEpochMs)
        val notification = f.notifications.single()
        assertEquals(NotificationCategory.MESSAGE, notification.category)
        assertEquals("chat/yan", notification.route)
        assertEquals("message:${yanSession.id}:${turns.single().id}", notification.sourceKey)
        assertEquals(reply, notification.body)
    }

    @Test
    fun disabledWritesNothing() = runBlocking {
        val f = Fixture()
        f.settings = f.settings.copy(enabled = false)
        val fake = FakeAiProvider.scripted("不该出现")
        f.use(fake)

        assertFalse(f.engine.fireIfDue())

        assertEquals(0, fake.requests.size)
        assertTrue(f.characterTurns().isEmpty())
        assertEquals(0, f.state.sentCount)
    }

    @Test
    fun quietHoursBlockQuietly() = runBlocking {
        val f = Fixture()
        f.chat.clock.now = 1_700_004_600_000L // 2023-11-14T23:30Z
        val fake = FakeAiProvider.scripted("不该出现")
        f.use(fake)

        assertFalse(f.engine.fireIfDue())

        assertEquals(0, fake.requests.size)
        assertTrue(f.characterTurns().isEmpty())
    }

    @Test
    fun intervalBlocksSecondFire() = runBlocking {
        val f = Fixture()
        val fake = FakeAiProvider.scripted("第一次主动消息")
        f.use(fake)
        assertTrue(f.engine.fireIfDue())

        f.chat.clock.now += 3_600_000L // +1h, interval is 6h
        assertFalse(f.engine.fireIfDue())

        assertEquals(1, fake.requests.size)
        assertEquals(1, f.characterTurns().size)
        assertEquals(1, f.state.sentCount)
    }

    @Test
    fun missingProviderWritesNothing() = runBlocking {
        val f = Fixture()

        assertFalse(f.engine.fireIfDue())

        assertTrue(f.characterTurns().isEmpty())
        assertEquals(0, f.state.sentCount)
    }

    @Test
    fun failedCallWritesNothing() = runBlocking {
        val f = Fixture()
        f.use(FakeAiProvider.unauthorized())

        assertFalse(f.engine.fireIfDue())

        assertTrue(f.characterTurns().isEmpty())
        assertEquals(0, f.state.sentCount)
        assertTrue(f.notifications.isEmpty())
    }

    @Test
    fun forceSkipsTimingRulesButStillSends() = runBlocking {
        val f = Fixture()
        f.settings = f.settings.copy(enabled = false, intervalHours = 24)
        f.chat.clock.now = 1_700_004_600_000L // inside quiet hours
        val fake = FakeAiProvider.scripted("开发控制台强制触发")
        f.use(fake)

        assertTrue(f.engine.fireIfDue(force = true))

        assertEquals(1, fake.requests.size)
        assertEquals(1, f.characterTurns().size)
    }
}
