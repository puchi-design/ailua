package com.example

import com.example.data.ai.runtime.ProviderResolver
import com.example.data.engine.ScheduledActionType
import com.example.data.engine.WorldActionPlanner
import com.example.data.engine.WorldPlanValidator
import com.example.data.engine.WorldPlanRuntime
import com.example.data.engine.UserContactCooldown
import com.example.data.engine.WorldTimeAdvancer
import com.example.data.model.DayPhase
import com.example.data.model.LifeEventType
import com.example.data.model.PlannedWorldAction
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.data.model.WorldPlan
import com.example.data.projection.projectCheckPhone
import com.example.data.projection.projectDiary
import com.example.data.projection.projectMoments
import com.example.data.projection.projectPresence
import com.example.data.registry.CharacterRegistry
import org.junit.Assert.*
import org.junit.Test
import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.runtime.ResolvedProvider
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

class WorldPlanTest {
    private val clock = WorldClock("9月25日", 21 * 60 + 30, DayPhase.EVENING, WeatherState.RAIN)
    private fun action(id: String, time: Int, date: String = clock.dateLabel, type: LifeEventType = LifeEventType.THOUGHT) = PlannedWorldAction(
        id = id, characterId = "mira", triggerWorldDate = date, triggerMinutes = time,
        type = if (type == LifeEventType.MOMENT) ScheduledActionType.MOMENT else ScheduledActionType.LIFE_EVENT,
        lifeEventType = type, title = "活动$id", description = "角色在生活$id", location = "青石街23号",
    )
    private fun plan(vararg actions: PlannedWorldAction) = WorldPlan(clock.dateLabel, clock.minutesOfDay, actions.toList())

    @Test fun rejectsDuplicatePastAndExcessContact() {
        val valid = listOf(action("a", 1310), action("b", 1350), action("c", 1390))
        assertTrue(WorldPlanValidator.validate(plan(*valid.toTypedArray()), clock, emptyList(), emptyList()))
        assertFalse(WorldPlanValidator.validate(plan(valid[0], valid[0], valid[2]), clock, emptyList(), emptyList()))
        assertFalse(WorldPlanValidator.validate(plan(action("old", 1280), valid[1], valid[2]), clock, emptyList(), emptyList()))
        val contacts = valid.take(2).map { it.copy(lifeEventType = LifeEventType.MESSAGE, relatedCharacterIds = listOf("user")) }
        assertFalse(WorldPlanValidator.validate(plan(contacts[0], contacts[1], valid[2]), clock, emptyList(), emptyList()))
    }

    @Test fun crossMidnightAndJsonRoundTrip() {
        val tomorrow = WorldTimeAdvancer.advanceDateLabel(clock.dateLabel, 1)
        val original = plan(action("a", 1310), action("b", 10, tomorrow), action("c", 70, tomorrow))
        assertTrue(WorldPlanValidator.validate(original, clock, emptyList(), emptyList()))
        val encoded = kotlinx.serialization.json.Json.encodeToString(WorldPlan.serializer(), original)
        assertEquals(original, kotlinx.serialization.json.Json.decodeFromString(WorldPlan.serializer(), encoded))
        val planner = WorldActionPlanner(ProviderResolver { null })
        val actionsJson = encoded.substringAfter("\"actions\":").dropLast(1)
        assertEquals(original.actions, planner.parse("{\"actions\":$actionsJson}"))
        assertNull(planner.parse("{broken"))
    }

    @Test fun plannedFactProjectsAcrossApps() {
        val event = com.example.data.model.LifeEvent(
            id = "p4a_projection", characterId = "mira", time = "22:40", type = LifeEventType.MOMENT,
            title = "小弥做甜点", description = "刚做好的栗子布丁", location = "青石街23号",
            worldDateLabel = "9月25日", worldMinutesOfDay = 1360,
            metadata = mapOf("search_query" to "栗子布丁做法", "note" to "买牛奶"),
        )
        assertTrue(projectMoments(emptyList(), listOf(event), emptySet()).any { it.id == "moment_p4a_projection" })
        val diary = event.copy(id = "p4a_diary", type = LifeEventType.DIARY)
        assertTrue(projectDiary("mira", listOf(diary), emptyList(), emptySet()).any { it.id == "p4a_diary" })
        val phone = projectCheckPhone("mira", listOf(event), emptySet())
        assertTrue(phone.searchHistory.contains("栗子布丁做法"))
        assertTrue(phone.notes.contains("买牛奶"))
        assertEquals("小弥做甜点", projectPresence(CharacterRegistry.getCharacter("mira"), listOf(event)).currentActivity)
    }

    @Test fun fallbackIsValidAndFullExistingPlanIsKept() {
        val fallback = WorldPlanRuntime.fallback(clock)
        assertTrue(WorldPlanValidator.validate(fallback, clock, emptyList(), emptyList()))
        assertFalse(WorldPlanRuntime.needsPlan(clock, fallback.actions.take(2), "9月24日"))
        assertTrue(WorldPlanRuntime.needsPlan(clock, fallback.actions.take(1), clock.dateLabel))
    }

    @Test fun unsupportedStructuredResponseRetriesOnlyOnceAsPlainJson() = runBlocking {
        val requests = mutableListOf<AiChatRequest>()
        val provider = object : AiProvider {
            override fun streamChat(request: AiChatRequest) = flow {
                requests += request
                if (request.jsonResponse) emit(AiStreamEvent.Failed(AiProviderError.Http(400, "unsupported response_format")))
                else emit(AiStreamEvent.Completed("{\"actions\":[]}"))
            }
        }
        val planner = WorldActionPlanner(ProviderResolver { ResolvedProvider("test", "model", provider) })
        assertNull(planner.generate(clock, emptyList(), emptyList()))
        assertEquals(listOf(true, false), requests.map { it.jsonResponse })
    }

    @Test fun serverFailureDoesNotRetryProvider() = runBlocking {
        val requests = mutableListOf<AiChatRequest>()
        val provider = object : AiProvider {
            override fun streamChat(request: AiChatRequest) = flow {
                requests += request
                emit(AiStreamEvent.Failed(AiProviderError.Http(503, "unavailable")))
            }
        }
        val planner = WorldActionPlanner(ProviderResolver { ResolvedProvider("test", "model", provider) })
        assertNull(planner.generate(clock, emptyList(), emptyList()))
        assertEquals(1, requests.size)
    }

    @Test fun socialAndDiaryDailyLimitsAndRhythmGuards() {
        assertFalse(WorldPlanValidator.validate(plan(
            action("m1", 1310, type = LifeEventType.MOMENT),
            action("m2", 1350, type = LifeEventType.MOMENT),
            action("m3", 1390, type = LifeEventType.MOMENT),
        ), clock, emptyList(), emptyList()))
        assertFalse(WorldPlanValidator.validate(plan(action("a", 1310), action("b", 1320), action("c", 1390)), clock, emptyList(), emptyList()))
        val tomorrow = WorldTimeAdvancer.advanceDateLabel(clock.dateLabel, 1)
        val call = action("call", 180, tomorrow, LifeEventType.MESSAGE).copy(type = ScheduledActionType.INCOMING_CALL)
        assertFalse(WorldPlanValidator.validate(plan(action("a", 1310), action("b", 1390), call), clock, emptyList(), emptyList()))
    }

    @Test fun recentPlannedContactBlocksProactiveFollowUp() {
        val current = WorldClock(clock.dateLabel, 1350, DayPhase.NIGHT, WeatherState.RAIN)
        val contact = com.example.data.model.LifeEvent("recent_call", "mira", "22:00", LifeEventType.MESSAGE,
            "来电", "聊过天", worldDateLabel = clock.dateLabel, worldMinutesOfDay = 1320, sourceAppId = "heartbeat")
        assertTrue(UserContactCooldown.recentlyContacted(current, listOf(contact)))
        assertFalse(UserContactCooldown.recentlyContacted(current.copy(minutesOfDay = 1411), listOf(contact)))
    }

    @Test fun fallbackSearchesAroundDailyLimits() {
        val saturated = (1..4).map { number ->
            com.example.data.model.LifeEvent("thought_$number", "mira", "21:00", LifeEventType.THOUGHT,
                "旧想法", "旧想法", worldDateLabel = clock.dateLabel, worldMinutesOfDay = 1260,
                sourceAppId = "heartbeat")
        }
        val fallback = WorldPlanRuntime.fallback(clock, emptyList(), saturated)
        assertNotNull(fallback)
        assertTrue(WorldPlanValidator.validate(fallback!!, clock, emptyList(), saturated))
    }
}
