package com.example

import com.example.data.ai.provider.FakeAiProvider
import com.example.data.ai.runtime.ProviderResolver
import com.example.data.ai.runtime.ResolvedProvider
import com.example.data.chat.local.SqlDelightMemoryRepository
import com.example.data.character.initiative.ProactiveContentType
import com.example.data.engine.ProactiveMessageEngine
import com.example.data.engine.WorldActionPlanner
import com.example.data.model.DayPhase
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.ProactiveSettings
import com.example.data.model.ProactiveState
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterAiPromptPrivacyTest {
    private val world = WorldClock("9月25日", 12 * 60, DayPhase.AFTERNOON, WeatherState.RAIN)
    private val events = listOf(
        LifeEvent("hidden_source", "mira", "11:00", LifeEventType.LOCATION_CHANGE,
            "SECRET_SOURCE_PHONE_VIEW", "用户看过角色手机", sourceAppId = "check_phone",
            location = "SECRET_LOCATION", worldDateLabel = world.dateLabel, worldMinutesOfDay = 11 * 60),
        LifeEvent("hidden_meta", "mira", "11:10", LifeEventType.THOUGHT,
            "SECRET_METADATA_PHONE_VIEW", "私密操作", sourceAppId = "world",
            metadata = mapOf("prompt_visibility" to "hidden"), worldDateLabel = world.dateLabel, worldMinutesOfDay = 11 * 60 + 10),
        LifeEvent("public_work", "mira", "11:20", LifeEventType.THOUGHT,
            "PUBLIC_WORK_FACT", "正在整理订单", sourceAppId = "world",
            worldDateLabel = world.dateLabel, worldMinutesOfDay = 11 * 60 + 20),
    )

    @Test fun worldPlannerNeverReceivesPrivatePhoneViewFacts() = runBlocking {
        val provider = FakeAiProvider.scripted("{\"actions\":[]}")
        val planner = WorldActionPlanner(ProviderResolver { ResolvedProvider("test", "model", provider) })
        planner.generate(world, events, emptyList())
        val prompt = provider.requests.single().messages.joinToString("\n") { it.content }
        assertFalse(prompt.contains("SECRET_SOURCE_PHONE_VIEW"))
        assertFalse(prompt.contains("SECRET_METADATA_PHONE_VIEW"))
        assertFalse(prompt.contains("SECRET_LOCATION"))
        assertTrue(prompt.contains("PUBLIC_WORK_FACT"))
    }

    @Test fun proactiveMomentPromptNeverReceivesPrivatePhoneViewFacts() = runBlocking {
        val f = ChatTestHarness.inMemory()
        try {
            val provider = FakeAiProvider.scripted("午后整理了订单。")
            val engine = ProactiveMessageEngine(
                chatRepository = f.repository,
                memoryRepository = SqlDelightMemoryRepository(f.database, f.idGenerator, f.clock),
                providerResolver = ProviderResolver { ResolvedProvider("test", "model", provider) },
                clock = f.clock,
                loadSettings = { ProactiveSettings(enabled = true) },
                loadState = { ProactiveState() },
                saveState = {},
                timeZone = TimeZone.getTimeZone("UTC"),
                recentWorldEvents = { events },
                postNotification = {},
                activeCharacterId = { "mira" },
                worldClock = { world },
            )
            assertTrue(engine.fireIfDue(force = true, forceContentType = ProactiveContentType.MOMENT))
            val prompt = provider.requests.single().messages.joinToString("\n") { it.content }
            assertFalse(prompt.contains("SECRET_SOURCE_PHONE_VIEW"))
            assertFalse(prompt.contains("SECRET_METADATA_PHONE_VIEW"))
            assertFalse(prompt.contains("SECRET_LOCATION"))
            assertTrue(prompt.contains("PUBLIC_WORK_FACT"))
        } finally { f.driver.close() }
    }
}
