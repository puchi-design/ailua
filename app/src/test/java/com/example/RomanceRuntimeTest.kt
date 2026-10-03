package com.example

import com.example.data.character.runtime.*
import com.example.data.local.AiluaLocalStore
import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.relationship.romance.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RomanceRuntimeTest {
    private val now = 1_800_000_000_000L
    private val day = RomanceReducer.DAY_MS
    private fun profile(style: String = "balanced", route: String = "romance") = CharacterRuntimeProfile(
        "test", "Test", RuntimeSource.IMPORTED, CharacterIdentityRuntime(gender = "arbitrary"),
        RelationshipRuntime(routeType = route, jealousy = .5, progressionStyle = style),
        BehaviorRuntime(jealousyPatterns = listOf("问得少一些"), conflictPatterns = listOf("先整理想法")),
        SpeechRuntime(), InitiativeRuntime(), LifeRuntime(), VisualRuntime(),
    )
    private fun event(id: String, type: RomanceEventType = RomanceEventType.IMPORTANT_MEMORY, at: Long = now, fingerprint: String = id) = RomanceEvent(id, type, at, fingerprint)
    private fun exchange(record: RomanceRecord, id: String, user: String, reply: String, at: Long = now) =
        RomanceInteractionPolicy.observeExchange(record, profile(), id, user, reply, at)

    @Test fun ordinaryMessagesNeverAddRelationshipPoints() {
        var record = RomanceRecord("test")
        repeat(100) { record = exchange(record, "$it", "今天只是给你打个招呼，早安早安", "早，书店刚刚开门。", now + it * 1_000L) }
        assertEquals(RomanceState(), record.state)
        assertEquals(RomanceStage.STRANGER, RomanceReducer.stage(record, profile()))
    }

    @Test fun sustainedConversationNeedsElapsedTimeDistinctUserContributionsAndBothSides() {
        var record = RomanceRecord("test")
        val first = "今天修复的那本书让我想起第一次去旧书店的事情"
        record = exchange(record, "1", first, "那家店是不是开在巷子尽头？", now)
        record = exchange(record, "2", first, "有些地方变了，有些还留着。", now + 5 * 60_000)
        record = exchange(record, "3", "书架上有一张夹了很久的火车票，好像有一段故事", "车票上写的终点是哪里？", now + 10 * 60_000)
        assertEquals(RomanceState(), record.state)
        record = exchange(record, "4", "我想把这些旧物整理好，下次再一起慢慢找线索", "好，我把工作台空出来。", now + 11 * 60_000)
        assertTrue(record.state.familiarity > 0f)
        val rewarded = record.state
        record = exchange(record, "5", "接着讲一些不同的故事，希望这段对话继续很久", "我在听，你接着讲。", now + 12 * 60_000)
        assertEquals(rewarded, record.state)
        assertEquals(record, exchange(record, "5", "改变文字也不能重放同一个用户回合", "是的，不应该重放。", now + 15 * 60_000))
    }

    @Test fun emptyOrFailedReplyCannotEstablishEvidence() {
        val record = RomanceRecord("test")
        assertEquals(record, exchange(record, "1", "我想和你正式交往", ""))
    }

    @Test fun persistedUserBoundaryTakesEffectBeforeAnyReplyAndCannotBeUndoneByFailure() {
        val high = RomanceRecord("test", state = RomanceState(.9f, .9f, .9f, .9f), romanticConfirmedAtEpochMs = now - day)
        val bounded = RomanceInteractionPolicy.observeUserBoundary(high, profile(), "user-turn", "我不想和你恋爱。", now)
        assertTrue(bounded.romanceDeclined)
        assertNull(bounded.romanticConfirmedAtEpochMs)
        assertEquals(bounded, exchange(bounded, "user-turn", "我不想和你恋爱。", ""))
        assertEquals(bounded, RomanceInteractionPolicy.observeUserBoundary(bounded, profile(), "user-turn", "我不想和你恋爱。", now + 1_000))
        assertEquals(high, RomanceInteractionPolicy.observeUserBoundary(high, profile(), "confession", "我想和你正式交往。", now))
    }

    @Test fun semanticEventIdsAndFingerprintsPreventReplayAndMemoryFarming() {
        val first = RomanceReducer.apply(RomanceRecord("test"), event("m1", fingerprint = "same-memory"), profile())
        assertEquals(first, RomanceReducer.apply(first, event("m1"), profile()))
        val duplicate = RomanceReducer.apply(first, event("m2", at = now + day, fingerprint = "same-memory"), profile())
        assertEquals(first.state, duplicate.state)
        var repeated = first
        repeat(20) { repeated = RomanceReducer.apply(repeated, event("more$it"), profile()) }
        assertEquals(first.state.trust * 1.3f, repeated.state.trust, .00001f)
    }

    @Test fun oldOutOfOrderEventDoesNotResetTodaysRewardBudget() {
        var record = RomanceReducer.apply(RomanceRecord("test"), event("today"), profile())
        val old = RomanceReducer.apply(record, event("old", at = now - day), profile())
        assertEquals(record.state, old.state)
        record = RomanceReducer.apply(old, event("another-today"), profile())
        assertEquals(.075f, record.state.trust, .00001f)
    }

    @Test fun lateSemanticEventsCannotReviveResolvedConflictOrOverrideLaterBoundary() {
        val p = profile()
        var record = RomanceReducer.apply(RomanceRecord("test"), event("conflict", RomanceEventType.CONFLICT, now), p)
        record = RomanceReducer.apply(record, event("resolved", RomanceEventType.CONFLICT_RESOLVED, now + day), p)
        val lateConflict = RomanceReducer.apply(record, event("old-conflict", RomanceEventType.CONFLICT, now + 1_000), p)
        assertEquals(record.recentConflict, lateConflict.recentConflict)
        assertEquals(record.state, lateConflict.state)
        record = RomanceReducer.apply(record, event("boundary", RomanceEventType.BOUNDARY_RESET, now + 2 * day), p)
        val lateConsent = RomanceReducer.apply(record, event("old-consent", RomanceEventType.MUTUAL_AFFECTION, now + day), p)
        assertTrue(lateConsent.romanceDeclined)
        assertNull(lateConsent.romanticConfirmedAtEpochMs)
        assertEquals(record.state, lateConsent.state)
        assertTrue("old-consent" in lateConsent.processedEventIds)
    }

    @Test fun familiarAndFriendlyRoutesNeverBecomeRomanceThroughThresholds() {
        val high = RomanceRecord("test", state = RomanceState(.95f, .95f, .95f, .95f), romanticConfirmedAtEpochMs = now, commitmentConfirmedAtEpochMs = now)
        for (route in listOf("friendship", "family", "companion", "custom", "sibling")) {
            assertEquals(RomanceStage.CLOSE, RomanceReducer.stage(high, profile(route = route)))
            assertFalse(RomanceRuntime.jealousyEligible(high, profile(route = route), now, RecentUserInteraction("other", now)))
        }
    }

    @Test fun romanticAndCommittedNeedExplicitMutualEvidenceAndMultipleDimensions() {
        val high = RomanceRecord("test", state = RomanceState(.95f, .95f, .95f, .95f))
        assertEquals(RomanceStage.AMBIGUOUS, RomanceReducer.stage(high, profile()))
        val romantic = exchange(high, "yes", "我想和你正式交往。", "我也愿意和你在一起。")
        assertEquals(RomanceStage.ROMANTIC, RomanceReducer.stage(romantic, profile()))
        val committed = exchange(romantic, "future", "我想和你认真走下去。", "我也想和你认真走下去。")
        assertEquals(RomanceStage.COMMITTED, RomanceReducer.stage(committed, profile()))
        assertEquals(RomanceStage.FAMILIAR, RomanceReducer.stage(committed.copy(state = RomanceState(.2f, .2f, .95f, .95f)), profile()))
    }

    @Test fun relationshipInstructionsRespectExplicitAgreementBeforeDimensionsCrossThresholds() {
        val low = exchange(RomanceRecord("test"), "mutual", "我想和你正式交往。", "我也愿意和你在一起。")
        assertEquals(RomanceStage.STRANGER, RomanceReducer.stage(low, profile()))
        val instructions = RomanceRuntime.promptInstructions(low, profile(), now)
        assertTrue(instructions.contains("已经明确同意交往"))
        assertFalse(instructions.contains("仍未互相确认"))
        assertFalse(instructions.contains("不要假装已经亲密"))
    }

    @Test fun fallbackAndNonRomanticRoutesDoNotEraseCardDefinedFamilyOrFriendship() {
        for (route in listOf("companion", "family", "friendship", "custom")) {
            val instructions = RomanceRuntime.promptInstructions(RomanceRecord("test"), profile(route = route), now)
            assertTrue(instructions.contains("保留角色卡已有的人际设定"))
            assertFalse(instructions.contains("你们还在了解彼此"))
        }
    }

    @Test fun groupProjectionPreservesBoundaryWithoutDisclosingPrivateRelationshipEvidence() {
        val record = RomanceRecord("test", romanceDeclined = true, recentConflict = RomanceConflict("secret-private-conflict", now),
            romanticConfirmedAtEpochMs = now - day, state = RomanceState(.9f, .9f, .9f, .9f))
        val prompt = RomanceRuntime.groupPromptInstructions(record, profile())
        assertTrue(prompt.contains("非恋爱边界"))
        assertFalse(prompt.contains("secret-private-conflict"))
        assertFalse(prompt.contains("问得少一些"))
        assertFalse(prompt.contains("已经明确同意交往"))
        assertFalse(prompt.contains("0.9"))
    }

    @Test fun oneSidedConfessionQuoteQuestionHypothesisNegationCannotConfirmRomance() {
        val plain = RomanceRecord("test")
        val accepted = "我也愿意和你在一起。"
        val excluded = listOf("如果我想和你正式交往。", "我不想和你正式交往。", "我想和你正式交往？", "他说：我想和你正式交往。", "“我想和你正式交往。”", "测试：我想和你正式交往。")
        excluded.forEachIndexed { index, text -> assertNull(exchange(plain, "$index", text, accepted).romanticConfirmedAtEpochMs) }
        assertNull(exchange(plain, "one-sided", "晚上想吃点什么", accepted).romanticConfirmedAtEpochMs)
        assertNull(exchange(plain, "refused", "我想和你正式交往。", "我不愿意和你在一起。").romanticConfirmedAtEpochMs)
    }

    @Test fun explicitUserBoundaryDoesNotRequireAssistantPermission() {
        val high = RomanceRecord("test", state = RomanceState(.9f, .9f, .9f, .9f), romanticConfirmedAtEpochMs = now)
        val stopped = exchange(high, "boundary", "我们只做朋友。", "但我还想试试。")
        assertTrue(stopped.romanceDeclined)
        assertNull(stopped.romanticConfirmedAtEpochMs)
        assertEquals(0f, stopped.state.attraction)
        assertEquals(RomanceStage.CLOSE, RomanceReducer.stage(stopped, profile()))
        assertTrue(RomanceRuntime.promptInstructions(stopped, profile(), now).contains("不追求、不吃醋"))
        val later = exchange(stopped, "new-mutual", "我想和你正式交往。", "我也愿意和你在一起。", now + day)
        assertFalse(later.romanceDeclined)
    }

    @Test fun naturalNegativeBoundariesAndEnglishContractionsAreNotDiscardedAsNegatedAffection() {
        val high = RomanceRecord("test", state = RomanceState(.9f, .9f, .9f, .9f), romanticConfirmedAtEpochMs = now)
        listOf("我不想和你恋爱。", "别再追求我，我最近没有心情。", "其实，我现在不想和你交往了。", "let's stay friends.", "I don't want to date you.", "别再追求我，好吗？")
            .forEachIndexed { index, text -> assertTrue(text, exchange(high, "boundary$index", text, "但我还想试试。").romanceDeclined) }
        listOf("如果我说我不想和你恋爱，你会怎么做。", "我不是不想和你恋爱。", "“别再追求我。”", "他说：别再追求我。", "我不想和你恋爱吗？", "'let us stay friends.'")
            .forEachIndexed { index, text -> assertFalse(text, exchange(high, "excluded$index", text, "好的。").romanceDeclined) }
    }

    @Test fun naturalConflictAndSpecificGoodNewsAreGroundedInBothSpeakers() {
        val initial = RomanceRecord("test")
        val conflict = exchange(initial, "conflict", "你刚才那句话让我难过，我想一个人待会儿。", "我知道你现在很难过，先留点时间。")
        assertNotNull(conflict.recentConflict)
        val good = exchange(initial, "good", "我终于通过了驾驶考试！", "恭喜你！", now)
        assertEquals(now, good.lastGoodEventAtEpochMs)
        assertEquals(now, RomanceRuntime.triggerFacts(good, profile(), now).recentGoodEventAtEpochMs)
        assertNull(exchange(initial, "quote", "小说里她说我终于通过了驾驶考试！", "恭喜你！").lastGoodEventAtEpochMs)
    }

    @Test fun threeProgressionStylesRespondDifferentlyToTheSameRealExperiences() {
        fun advance(style: String): RomanceRecord {
            var record = RomanceRecord("test")
            repeat(6) { index ->
                record = RomanceReducer.apply(record, event("m$index", at = now + index * day), profile(style))
                record = RomanceReducer.apply(record, event("c$index", RomanceEventType.USER_CALL, now + index * day), profile(style))
            }
            return record
        }
        val yan = advance("slow_trust"); val yeo = advance("expressive"); val noa = advance("reserved")
        assertTrue(yan.state.trust > yeo.state.trust)
        assertTrue(yeo.state.attraction > yan.state.attraction)
        assertTrue(yeo.state.intimacy > yan.state.intimacy)
        assertTrue(noa.state.familiarity < yan.state.familiarity)
        val trusted = RomanceRecord("test", state = RomanceState(trust = .7f))
        val lowTrust = RomanceReducer.apply(RomanceRecord("test"), event("letter", RomanceEventType.LETTER), profile("reserved"))
        val highTrust = RomanceReducer.apply(trusted, event("letter", RomanceEventType.LETTER), profile("reserved"))
        assertTrue(highTrust.state.intimacy > lowTrust.state.intimacy * 3)
    }

    @Test fun conflictRequiresActualAcknowledgedExchangeAndResolutionHasEvidence() {
        val record = RomanceRecord("test")
        assertNull(exchange(record, "one-sided", "我需要一点时间冷静。", "书店打烊了。").recentConflict)
        val conflict = exchange(record, "conflict", "我需要一点时间冷静。", "我明白你需要时间。")
        assertNotNull(conflict.recentConflict)
        assertTrue(conflict.state.tension > 0f)
        assertFalse(RomanceRuntime.triggerFacts(conflict, profile("reserved"), now + 12 * 3_600_000L).readyToReconnect)
        assertTrue(RomanceRuntime.triggerFacts(conflict, profile("expressive"), now + 5 * 3_600_000L).readyToReconnect)
        assertTrue(RomanceRuntime.triggerFacts(conflict, profile("reserved"), now + day).readyToReconnect)
        val resolved = exchange(conflict, "resolved", "这件事我们说开了。", "我们说开了。", now + day)
        assertNotNull(resolved.recentConflict?.resolvedAtEpochMs)
        assertTrue(resolved.state.tension < conflict.state.tension)
        assertFalse(RomanceRuntime.triggerFacts(resolved, profile(), now + day).recentConflict)
    }

    @Test fun jealousyNeedsRomanticStageRecentOtherInteractionAndDefinedTendency() {
        val high = RomanceRecord("test", state = RomanceState(.6f, .7f, .5f, .5f))
        val other = RecentUserInteraction("other", now)
        assertTrue(RomanceRuntime.jealousyEligible(high, profile(), now, other))
        assertFalse(RomanceRuntime.jealousyEligible(RomanceRecord("test"), profile(), now, other))
        assertFalse(RomanceRuntime.jealousyEligible(high, profile(), now, null))
        assertFalse(RomanceRuntime.jealousyEligible(high, profile(), now + 2 * day, other))
        assertFalse(RomanceRuntime.jealousyEligible(high, profile(), now, RecentUserInteraction("test", now)))
        assertFalse(RomanceRuntime.jealousyEligible(high, profile().copy(relationship = RelationshipRuntime(routeType = "romance")), now, other))
        val prompt = RomanceRuntime.promptInstructions(high, profile(), now, other)
        assertTrue(prompt.contains("问得少一些"))
        assertTrue(prompt.contains("不能监视、盘问、辱骂、控制、强迫、威胁"))
        assertFalse(prompt.contains("0.6"))
        assertFalse(prompt.contains("AMBIGUOUS"))
    }

    @Test fun anniversaryCannotBeInventedByNewCardOrSingleStatement() {
        val newbie = exchange(RomanceRecord("test"), "anniversary", "今天是我们的交往纪念日。", "我记得这个日子。")
        assertEquals(RomanceState(), newbie.state)
        val established = RomanceRecord("test", romanticConfirmedAtEpochMs = now - 30 * day)
        val anniversary = exchange(established, "anniversary", "今天是我们的交往纪念日。", "我记得这个日子。")
        assertTrue(anniversary.state.intimacy > 0f)
    }

    @Test fun dimensionsStayFiniteAndWithinUnitRange() {
        val invalid = RomanceRecord("test", state = RomanceState(Float.NaN, 3f, -1f, Float.POSITIVE_INFINITY, -4f))
        val state = RomanceReducer.apply(invalid, event("sanitize"), profile()).state
        listOf(state.familiarity, state.trust, state.attraction, state.intimacy, state.tension).forEach { assertTrue(it.isFinite() && it in 0f..1f) }
    }

    @Test fun evidenceSurvivesSerializationAndRepositoryRestoreWithoutLegacyBondMigration() {
        val old = AiluaLocalStore.savedRomanceStates.value
        try {
            val conflict = exchange(RomanceRecord("persist"), "conflict", "我需要一点时间冷静。", "我明白你需要时间。")
            val encoded = Json.encodeToString(RomanceRecord.serializer(), conflict)
            val restored = Json.decodeFromString(RomanceRecord.serializer(), encoded)
            assertEquals(conflict, restored)
            AiluaLocalStore.saveRomanceStates(listOf(restored))
            RomanceRepository.restore()
            assertEquals(restored, RomanceRepository.record("persist"))
            assertEquals(RomanceState(), RomanceRepository.record("unseen").state)
        } finally { AiluaLocalStore.saveRomanceStates(old); RomanceRepository.restore() }
    }

    @Test fun realMemoryCallLetterAndStoryHooksDedupeAndExcludeSeedOrTinyCalls() {
        val old = AiluaLocalStore.savedRomanceStates.value
        val character = "romance_hook_${System.nanoTime()}"
        try {
            val seed = LifeEvent(id = "relationship_memory_seed", characterId = character, time = "12:00", type = LifeEventType.MEMORY,
                title = "记忆", description = "一条重要且足够长的真实共同记忆", sourceAppId = "memory", metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER))
            RomanceRepository.observeLifeEvent(seed)
            assertEquals(RomanceState(), RomanceRepository.record(character).state)
            RomanceRepository.observeLifeEvent(seed.copy(metadata = seed.metadata + ("interaction_epoch_ms" to now.toString())))
            val remembered = RomanceRepository.record(character)
            assertTrue(remembered.state.trust > 0f)
            RomanceRepository.recordUserCall(character, "missed", 0, true, now)
            RomanceRepository.recordUserCall(character, "incoming", 100, false, now)
            assertEquals(remembered, RomanceRepository.record(character))
            RomanceRepository.recordUserCall(character, "real", 80, true, now)
            RomanceRepository.recordLetterRead(character, "letter", now)
            RomanceRepository.recordSharedStory(character, "story", now)
            val completed = RomanceRepository.record(character)
            assertTrue(completed.state.intimacy > remembered.state.intimacy)
            RomanceRepository.recordUserCall(character, "real", 80, true, now + day)
            RomanceRepository.recordLetterRead(character, "letter", now + day)
            RomanceRepository.recordSharedStory(character, "story", now + day)
            assertEquals(completed, RomanceRepository.record(character))
        } finally { AiluaLocalStore.saveRomanceStates(old); RomanceRepository.restore() }
    }
}
