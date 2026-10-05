package com.example

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.checkphone.PhoneTraceSource
import com.example.data.projection.checkphone.projectCharacterSearchHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryProjectionTest {
    private fun fact(id: String, characterId: String, query: String) = LifeEvent(
        id = id, characterId = characterId, time = "20:13", type = LifeEventType.THOUGHT,
        title = "找资料", description = "看了资料", worldDateLabel = "10月5日",
        worldMinutesOfDay = 20 * 60 + 13, metadata = mapOf("search_query" to query)
    )

    @Test fun latestWorldFactPrecedesAuthoredSeed() {
        val seed = EMPTY_CHECK_PHONE_DATA.copy(searchHistory = listOf("展览预约"))
        val combined = seed.copy(searchHistory = listOf("摄影展几点闭馆") + seed.searchHistory)
        val searches = projectCharacterSearchHistory("yuna",
            listOf(fact("new", "yuna", "摄影展几点闭馆")), combined)
        assertEquals(listOf("摄影展几点闭馆", "展览预约"), searches.map { it.query })
        assertEquals(PhoneTraceSource.RUNTIME_EVENT, searches.first().source)
        assertEquals(PhoneTraceSource.AUTHORED, searches.last().source)
    }

    @Test fun characterIsolationAndMissingFactsRemainEmpty() {
        val searches = projectCharacterSearchHistory("yuna",
            listOf(fact("other", "mira", "她的搜索")), EMPTY_CHECK_PHONE_DATA)
        assertTrue(searches.isEmpty())
    }

    @Test fun blankMetadataDoesNotProduceAHistoryEntry() {
        val searches = projectCharacterSearchHistory("yuna",
            listOf(fact("blank", "yuna", "  ")), EMPTY_CHECK_PHONE_DATA)
        assertTrue(searches.isEmpty())
    }

    @Test fun ruleProjectionChangesWithActualLocationAndWeather() {
        val profile = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(),
            "hewenchuan", "贺闻川").let { it.copy(
                identity = it.identity.copy(occupation = "建筑与空间设计师"),
                life = it.life.copy(workplace = "木间空间工作室"),
            ) }
        val photo = LifeEvent("photo-fact", "hewenchuan", "16:10", LifeEventType.PHOTO,
            "木样", "拍摄木样", location = "木间空间工作室", worldDateLabel = "10月5日")
        val rain = projectCharacterSearchHistory("hewenchuan", listOf(photo), EMPTY_CHECK_PHONE_DATA,
            profile = profile, clock = WorldClock(minutesOfDay = 16 * 60, weather = WeatherState.RAIN))
        assertEquals(listOf("木间空间工作室 附近照片冲印", "雨天木材防潮怎么处理"),
            rain.map { it.query })
        assertTrue(rain.all { it.source == PhoneTraceSource.RULE_PROJECTION })
        val clear = projectCharacterSearchHistory("hewenchuan", listOf(photo), EMPTY_CHECK_PHONE_DATA,
            profile = profile, clock = WorldClock(minutesOfDay = 16 * 60, weather = WeatherState.CLEAR))
        assertEquals(listOf("木间空间工作室 附近照片冲印"), clear.map { it.query })
    }
}
