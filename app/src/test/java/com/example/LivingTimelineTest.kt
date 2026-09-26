package com.example

import com.example.data.model.TimelineEvent
import com.example.ui.living.groupLivingTimeline
import com.example.ui.living.livingTimeLabel
import com.example.ui.living.parseClockMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTimelineTest {

    private fun event(
        id: String,
        time: String,
        isCurrent: Boolean = false
    ) = TimelineEvent(
        id = id,
        time = time,
        title = "事件$id",
        description = "描述$id",
        isCurrent = isCurrent
    )

    @Test
    fun `parse clock minutes accepts valid times and rejects broken ones`() {
        assertEquals(7 * 60 + 42, parseClockMinutes("07:42"))
        assertEquals(0, parseClockMinutes("00:00"))
        assertEquals(23 * 60 + 59, parseClockMinutes("23:59"))
        assertNull(parseClockMinutes("24:00"))
        assertNull(parseClockMinutes("9:"))
        assertNull(parseClockMinutes("abc"))
        assertNull(parseClockMinutes("12:70"))
    }

    @Test
    fun `current event is labelled now regardless of clock`() {
        assertEquals("现在", livingTimeLabel("21:40", nowMinutes = 21 * 60 + 30, isCurrent = true))
        assertEquals("现在", livingTimeLabel("07:42", nowMinutes = 8 * 60, isCurrent = true))
    }

    @Test
    fun `recent event within an hour is labelled just now`() {
        assertEquals("刚刚", livingTimeLabel("21:14", nowMinutes = 21 * 60 + 30, isCurrent = false))
        assertEquals("刚刚", livingTimeLabel("20:30", nowMinutes = 21 * 60 + 30, isCurrent = false))
    }

    @Test
    fun `older events fall into readable day buckets`() {
        assertEquals("晚上", livingTimeLabel("19:08", nowMinutes = 21 * 60 + 30, isCurrent = false))
        assertEquals("下午", livingTimeLabel("14:20", nowMinutes = 21 * 60 + 30, isCurrent = false))
        assertEquals("上午", livingTimeLabel("09:18", nowMinutes = 21 * 60 + 30, isCurrent = false))
        assertEquals("深夜", livingTimeLabel("03:10", nowMinutes = 9 * 60, isCurrent = false))
        assertEquals("今天", livingTimeLabel("not-a-time", nowMinutes = 9 * 60, isCurrent = false))
    }

    @Test
    fun `groups keep every event with current first then newest first`() {
        val events = listOf(
            event("a", "07:42"),
            event("b", "12:31"),
            event("c", "19:08"),
            event("d", "21:40", isCurrent = true),
            event("e", "21:14"),
            event("f", "14:20")
        )
        val groups = groupLivingTimeline(events, nowMinutes = 21 * 60 + 30)

        assertEquals(events.size, groups.sumOf { it.events.size })
        val flatIds = groups.flatMap { group -> group.events.map { it.id } }
        assertEquals(setOf("a", "b", "c", "d", "e", "f"), flatIds.toSet())
        assertEquals("现在", groups.first().label)
        assertEquals("d", groups.first().events.first().id)
        assertEquals("刚刚", groups[1].label)
        assertEquals("e", groups[1].events.single().id)
        assertEquals("晚上", groups[2].label)
        assertEquals(listOf("c"), groups[2].events.map { it.id })
        assertEquals(listOf("下午", "上午"), groups.drop(3).map { it.label })
        assertEquals(listOf("f", "b"), groups[3].events.map { it.id })
        assertEquals(listOf("a"), groups[4].events.map { it.id })
    }

    @Test
    fun `group labels follow narrative order without splitting runs`() {
        val events = listOf(
            event("a", "07:42"),
            event("b", "09:18"),
            event("c", "12:31"),
            event("d", "14:20"),
            event("e", "16:35")
        )
        val groups = groupLivingTimeline(events, nowMinutes = 18 * 60)

        assertEquals(listOf("下午", "上午"), groups.map { it.label })
        assertEquals(listOf("e", "d", "c"), groups[0].events.map { it.id })
        assertEquals(listOf("b", "a"), groups[1].events.map { it.id })
        assertTrue(groups.all { it.events.isNotEmpty() })
    }

    @Test
    fun `empty timeline produces no groups`() {
        assertTrue(groupLivingTimeline(emptyList(), nowMinutes = 12 * 60).isEmpty())
    }
}
