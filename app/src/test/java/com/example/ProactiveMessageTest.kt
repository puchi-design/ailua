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
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.runtime.InitiativeTrigger
import com.example.data.character.initiative.InitiativeEvidence
import com.example.data.character.initiative.ProactiveContentType
import com.example.data.model.WorldClock
import com.example.data.model.WeatherState
import com.example.data.repository.MailboxRepository
import com.example.data.projection.projectGalleryAssets
import com.example.data.projection.projectMoments
import com.example.data.model.LetterDeliveryState
import com.example.data.relationship.romance.RomanceRepository
import com.example.data.local.AiluaLocalStore
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
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
                if (it.category == NotificationCategory.MESSAGE) assertTrue(characterTurns().isNotEmpty())
                notifications += it
            },
            activeCharacterId = { characterId },
            // Existing pipeline/rule tests isolate sampling from transport and durable writes.
            worldClock = { WorldClock(minutesOfDay = 12 * 60, weather = WeatherState.RAIN) },
            resolveRuntime = { id -> CharacterRuntimeResolver.resolve(id).let { profile -> profile.copy(initiative = profile.initiative.copy(
                messageFrequency = "high", messageProbability = 1.0, callProbability = 0.0,
                photoProbability = 0.0, momentProbability = 0.0, letterProbability = 0.0,
                triggerWeights = mapOf(InitiativeTrigger.RAIN to 1.0))) } },
            sample = { 0.0 },
            evidenceOverride = { _, _, _ -> listOf(InitiativeEvidence(InitiativeTrigger.RAIN, "fixture-rain", "下雨")) },
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

        val expectedId = "proactive_${1_700_000_000_000L}_mira"
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

        val event = WorldStateRepository.events.value.single { it.id == "proactive_${f.chat.clock.now}_yan" }
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

    @Test fun mediaDispatchCommitsActualProjectionAndNavigableNotification() = runBlocking {
        listOf(ProactiveContentType.PHOTO, ProactiveContentType.MOMENT, ProactiveContentType.LETTER).forEachIndexed { index, type ->
            val f = Fixture("yan")
            f.chat.clock.now += 5_000_000 + index * 10_000L
            val body = "修复台上留下了一页纸。$type"
            val fake = FakeAiProvider.scripted(body)
            f.use(fake)
            assertTrue(f.engine.fireIfDue(force = true, forceContentType = type))
            assertEquals(1, fake.requests.size)
            assertEquals(1, f.savedStates)
            assertTrue(f.characterTurns().isEmpty())
            val event = WorldStateRepository.events.value.single { it.id == "proactive_${f.chat.clock.now}_yan" }
            assertEquals(type.name, event.metadata["proactive_content_type"])
            when (type) {
                ProactiveContentType.PHOTO -> {
                    assertEquals(body, projectGalleryAssets(emptyList(), listOf(event)).single().caption)
                    assertEquals("gallery", f.notifications.single().route)
                }
                ProactiveContentType.MOMENT -> {
                    assertEquals(body, projectMoments(emptyList(), listOf(event)).single().content)
                    assertEquals("moments", f.notifications.single().route)
                }
                else -> {
                    val letter = MailboxRepository.letters.value.single { it.id == event.id }
                    assertEquals(body, letter.body)
                    assertEquals(LetterDeliveryState.DELIVERED, letter.deliveryState)
                    assertEquals("mailbox", f.notifications.single().route)
                    // Rehydration does not create a duplicate inbox item.
                    MailboxRepository.syncRuntimeLetters(WorldStateRepository.events.value)
                    assertEquals(1, MailboxRepository.letters.value.count { it.id == event.id })
                }
            }
        }
    }

    @Test fun callInvitationIsAChatInvitationWithoutAnIncomingCallFact() = runBlocking {
        val f = Fixture("yeo")
        f.chat.clock.now += 7_000_000
        f.use(FakeAiProvider.scripted("现在方便说两句吗？没空就晚点。"))
        assertTrue(f.engine.fireIfDue(force = true, forceContentType = ProactiveContentType.CALL_INVITE))
        val event = WorldStateRepository.events.value.single { it.id == "proactive_${f.chat.clock.now}_yeo" }
        assertEquals("CALL_INVITE", event.metadata["proactive_content_type"])
        assertFalse(event.id.startsWith("pulse_call_plan_"))
        assertEquals("chat/yeo", f.notifications.single().route)
        assertEquals(1, f.characterTurns().size)
    }

    @Test fun yeoBurstUsesOneProviderRequestAndOneQuota() = runBlocking {
        val f = Fixture("yeo")
        f.chat.clock.now += 8_000_000
        val provider = FakeAiProvider.scripted("刚看到那条小路。\n就是你说的地方。\n下次一起去？")
        f.use(provider)
        assertTrue(f.engine.fireIfDue(force = true))
        assertEquals(3, f.characterTurns().size)
        assertEquals(1, f.state.sentCount)
        assertEquals(1, provider.requests.size)
        assertEquals(1, f.notifications.size)
    }

    @Test fun failedProviderDoesNotRetryOnEveryHeartbeatInTheSameWindow() = runBlocking {
        val f = Fixture()
        val provider = FakeAiProvider.unauthorized()
        f.use(provider)
        assertFalse(f.engine.fireIfDue())
        assertFalse(f.engine.fireIfDue())
        assertEquals(1, provider.requests.size)
        assertEquals(0, f.state.sentCount)
    }

    @Test fun publicMomentRequestDoesNotReceivePrivateChatOrMemory() = runBlocking {
        val f = Fixture("yan")
        f.chat.clock.now += 9_000_000
        val secret = "private-only-door-code-472991"
        f.chat.repository.appendUserTurn(f.chat.repository.getOrCreatePrivateSession("yan").id, secret)
        f.memoryRepository.saveMemory("yan", secret, "chat")
        val provider = FakeAiProvider.scripted("今天把旧书的封面补好了。")
        f.use(provider)
        assertTrue(f.engine.fireIfDue(force = true, forceContentType = ProactiveContentType.MOMENT))
        val prompt = provider.requests.single().messages.joinToString("\n") { it.content }
        assertFalse(prompt.contains(secret))
        assertFalse(prompt.contains("【最近对话】"))
        assertFalse(prompt.contains("【你的记忆】"))
    }

    @Test fun changedBoundaryOrConflictDuringProviderCallDiscardsOldGeneratedContact() = runBlocking {
        val original = AiluaLocalStore.savedRomanceStates.value
        try {
            listOf("boundary", "conflict").forEachIndexed { index, change ->
                AiluaLocalStore.saveRomanceStates(original)
                RomanceRepository.restore()
                val f = Fixture("yan")
                f.chat.clock.now += 20_000_000 + index * 100_000L
                val delegate = FakeAiProvider.scripted("仍按旧关系生成的消息")
                f.use(object : AiProvider {
                    override fun streamChat(request: com.example.data.ai.model.AiChatRequest) = flow {
                        if (change == "boundary") {
                            RomanceRepository.observeUserBoundary("yan", "race-boundary-${f.chat.clock.now}", "我们只做朋友。", f.chat.clock.now)
                        } else {
                            RomanceRepository.observeExchange("yan", "race-conflict-${f.chat.clock.now}", "我需要一点时间冷静。", "我明白你需要时间。", f.chat.clock.now)
                        }
                        emitAll(delegate.streamChat(request))
                    }
                })
                assertFalse(change, f.engine.fireIfDue(force = true))
                assertEquals(1, delegate.requests.size)
                assertTrue(f.characterTurns().isEmpty())
                assertTrue(f.notifications.isEmpty())
                assertEquals(0, f.savedStates)
                assertFalse(WorldStateRepository.events.value.any { it.id == "proactive_${f.chat.clock.now}_yan" })
            }
        } finally {
            AiluaLocalStore.saveRomanceStates(original)
            RomanceRepository.restore()
        }
    }
}
