package com.example

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
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
}
