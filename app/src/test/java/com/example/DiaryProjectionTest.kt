package com.example

import com.example.data.mock.MockData
import com.example.data.model.DiaryEntry
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.projection.projectDiary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DiaryProjectionTest
 *
 * P3B-2 Diary projection contract:
 * - only DIARY facts of the requested character become entries
 * - seed world LifeEvents never double-render as diary pages
 * - seed diary entries keep priority; runtime entries are newest-first on top
 * - weather/mood come only from explicit metadata (no fabrication)
 * - empty is a legal state for characters without diary facts
 */
class DiaryProjectionTest {

    private fun event(
        id: String,
        characterId: String = "mira",
        type: LifeEventType = LifeEventType.DIARY,
        time: String = "21:00",
        worldDateLabel: String = "9月25日",
        worldMinutesOfDay: Int = 21 * 60,
        title: String = "title_$id",
        description: String = "desc_$id",
        imageReference: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = type,
        title = title,
        description = description,
        imageReference = imageReference,
        worldDateLabel = worldDateLabel,
        worldMinutesOfDay = worldMinutesOfDay,
        metadata = metadata
    )

    private val authorOf: (String) -> String = { "作者_$it" }

    @Test
    fun diaryEventProjectsToEntry() {
        val runtime = listOf(
            event(
                "d1",
                title = "窗边的新一页",
                description = "今天写下了一些心事",
                imageReference = "rain_window",
                metadata = mapOf("weather" to "小雨", "mood" to "平静")
            )
        )

        val entries = projectDiary("mira", runtime, seedEntries = emptyList(), seedEventIds = emptySet(), authorNameOf = authorOf)

        val entry = entries.single()
        assertEquals("d1", entry.id)
        assertEquals("mira", entry.characterId)
        assertEquals("作者_mira", entry.authorName)
        assertEquals("9月25日", entry.date)
        assertEquals("小雨", entry.weather)
        assertEquals("平静", entry.mood)
        assertEquals("窗边的新一页", entry.title)
        assertEquals("今天写下了一些心事", entry.content)
        assertEquals("rain_window", entry.imageReference)
    }

    @Test
    fun nonDiaryTypesDoNotProject() {
        val entries = projectDiary(
            "mira",
            listOf(
                event("t1", type = LifeEventType.THOUGHT),
                event("m1", type = LifeEventType.MOMENT),
                event("d1", type = LifeEventType.DIARY)
            ),
            seedEntries = emptyList(),
            seedEventIds = emptySet(),
            authorNameOf = authorOf
        )
        assertEquals(listOf("d1"), entries.map { it.id })
    }

    @Test
    fun otherCharactersDiaryDoesNotProject() {
        val entries = projectDiary(
            "mira",
            listOf(event("y1", characterId = "yuna")),
            seedEntries = emptyList(),
            seedEventIds = emptySet(),
            authorNameOf = authorOf
        )
        assertTrue(entries.isEmpty())
    }

    @Test
    fun seedWorldDiaryEventDoesNotDoubleRender() {
        val pulse5 = event("six_mira_diary", description = "seed世界事实")
        val entries = projectDiary(
            "mira",
            listOf(pulse5),
            seedEntries = MockData.diaryEntries,
            seedEventIds = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
            authorNameOf = authorOf
        )
        // six_mira_diary is a seed world fact: never rendered as its own diary page —
        // its authored prose already exists as six_diary_mira_1 ("留两块给自己")
        assertTrue(entries.none { it.id == "six_mira_diary" })
        assertTrue(entries.any { it.id == "six_diary_mira_1" })
        // All of this character's seed entries remain exactly once; other authors
        // must stay out even as the official cast grows.
        MockData.diaryEntries.filter { it.characterId == "mira" }.forEach { seed ->
            assertEquals(1, entries.count { it.id == seed.id })
        }
        assertTrue(entries.all { it.characterId == "mira" })
    }

    @Test
    fun runtimeEntriesPrependedNewestFirst() {
        val older = event("d_old", worldMinutesOfDay = 8 * 60, time = "08:00")
        val newer = event("d_new", worldMinutesOfDay = 22 * 60, time = "22:00")
        val seed = listOf(
            DiaryEntry(
                id = "seed_1",
                characterId = "mira",
                authorName = "小弥",
                date = "9月24日",
                weather = "晴",
                mood = "轻快",
                title = "seed标题",
                content = "seed内容",
                excerpt = "seed摘录"
            )
        )

        val entries = projectDiary("mira", listOf(older, newer), seed, emptySet(), authorOf)

        assertEquals(listOf("d_new", "d_old", "seed_1"), entries.map { it.id })
    }

    @Test
    fun characterWithoutDiaryFactsGetsEmptyList() {
        val entries = projectDiary(
            "unknown_char",
            listOf(event("d1", characterId = "mira")),
            seedEntries = MockData.diaryEntries,
            seedEventIds = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
            authorNameOf = authorOf
        )
        assertTrue(entries.isEmpty())
    }

    @Test
    fun noMetadataMeansNoWeatherOrMoodFabrication() {
        val entries = projectDiary(
            "mira",
            listOf(event("d1")),
            seedEntries = emptyList(),
            seedEventIds = emptySet(),
            authorNameOf = authorOf
        )
        val entry = entries.single()
        assertEquals("", entry.weather)
        assertEquals("", entry.mood)
        assertEquals(null, entry.imageReference)
    }
}
