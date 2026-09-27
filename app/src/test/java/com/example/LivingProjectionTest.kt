package com.example

import com.example.data.model.CharacterProfile
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.TimelineEvent
import com.example.data.projection.projectLiving
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LivingProjectionTest
 *
 * P3B-2 Living projection contract:
 * - runtime facts win over seed data for current state
 * - seed timeline entries are preserved as-is (flags untouched without runtime facts)
 * - timeline merge dedupes by id, never by time
 * - only the requested character's facts project
 */
class LivingProjectionTest {

    private val mira = CharacterProfile(
        id = "mira",
        name = "小弥",
        englishName = "Mira",
        title = "窗边的她",
        bio = "bio",
        currentActivity = "在窗边听雨",
        mood = "澄澈",
        location = "客厅窗边",
        contextualQuote = "quote"
    )

    private fun event(
        id: String,
        characterId: String = "mira",
        time: String = "12:00",
        type: LifeEventType = LifeEventType.THOUGHT,
        worldDateLabel: String = "9月25日",
        worldMinutesOfDay: Int = 12 * 60,
        title: String = "title_$id",
        location: String? = null
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = type,
        title = title,
        description = "desc_$id",
        location = location,
        worldDateLabel = worldDateLabel,
        worldMinutesOfDay = worldMinutesOfDay
    )

    private fun seedEntry(
        id: String,
        time: String = "08:00",
        isCurrent: Boolean = false,
        title: String = "seed_$id"
    ) = TimelineEvent(
        id = id,
        time = time,
        title = title,
        description = "seed_desc_$id",
        isCurrent = isCurrent
    )

    @Test
    fun withoutRuntimeFactsProfileFieldsAndSeedFlagsWin() {
        val seed = listOf(seedEntry("s1", isCurrent = true))
        val result = projectLiving(mira, seed, emptyList())

        assertEquals(mira.currentActivity, result.currentActivity)
        assertEquals(mira.location, result.currentLocation)
        assertEquals(null, result.currentEvent)
        assertTrue(result.timeline.single().isCurrent)
    }

    @Test
    fun runtimeFactWinsOverSeedActivity() {
        val seed = listOf(seedEntry("s1", isCurrent = true))
        val runtime = listOf(event("r1", time = "12:30", worldMinutesOfDay = 12 * 60 + 30, title = "正在泡茶"))

        val result = projectLiving(mira, seed, runtime)

        assertEquals("正在泡茶", result.currentActivity)
        assertEquals("r1", result.currentEvent?.id)
        assertEquals("客厅窗边", result.currentLocation)
        // seed flag cleared, runtime fact marked current
        assertFalse(result.timeline.first { it.id == "s1" }.isCurrent)
        assertTrue(result.timeline.first { it.id == "r1" }.isCurrent)
    }

    @Test
    fun runtimeFactLocationWinsWhenPresent() {
        val runtime = listOf(event("r1", location = "厨房"))
        val result = projectLiving(mira, emptyList(), runtime)
        assertEquals("厨房", result.currentLocation)
    }

    @Test
    fun runtimeFactWithoutLocationFallsBackToProfileLocation() {
        val runtime = listOf(event("r1", location = null))
        val result = projectLiving(mira, emptyList(), runtime)
        assertEquals(mira.location, result.currentLocation)
        assertEquals(mira.location, result.timeline.single().location)
    }

    @Test
    fun mergeDedupesByIdNotByTime() {
        val seed = listOf(seedEntry("s1", time = "12:00"))
        val runtime = listOf(
            event("r1", time = "12:00", worldMinutesOfDay = 12 * 60, title = "同一分钟另一件事")
        )

        val result = projectLiving(mira, seed, runtime)

        assertEquals(2, result.timeline.size)
        assertTrue(result.timeline.any { it.id == "s1" })
        assertTrue(result.timeline.any { it.id == "r1" })
    }

    @Test
    fun duplicateIdKeepsSeedEntry() {
        val seed = listOf(seedEntry("dup", title = "seed版本"))
        val runtime = listOf(event("dup", title = "runtime版本"))

        val result = projectLiving(mira, seed, runtime)

        assertEquals(1, result.timeline.size)
        assertEquals("seed版本", result.timeline.single().title)
        // seed ids never count as runtime facts, so profile state stays authoritative
        assertEquals(mira.currentActivity, result.currentActivity)
    }

    @Test
    fun otherCharactersFactsDoNotProject() {
        val seed = listOf(seedEntry("s1"))
        val runtime = listOf(
            event("r_yuna", characterId = "yuna", title = "Yuna的动静"),
            event("r_noa", characterId = "noa", title = "Noa的动静")
        )

        val result = projectLiving(mira, seed, runtime)

        assertEquals(mira.currentActivity, result.currentActivity)
        assertEquals(listOf("s1"), result.timeline.map { it.id })
    }

    @Test
    fun seedLifeEventsAreExcludedFromCurrentState() {
        // a seed world fact (e.g. pulse_5) must not become the Living current state
        val seed = listOf(seedEntry("s1", isCurrent = true))
        val pulse5 = event("pulse_5", time = "21:00", worldMinutesOfDay = 21 * 60, title = "seed事实")

        val result = projectLiving(
            character = mira,
            seedTimeline = seed,
            runtimeEvents = listOf(pulse5),
            seedEventIds = setOf("pulse_5")
        )

        assertEquals(mira.currentActivity, result.currentActivity)
        assertFalse(result.timeline.first { it.id == "pulse_5" }.isCurrent)
    }

    @Test
    fun latestRuntimeFactWinsChronologically() {
        val runtime = listOf(
            event("r_early", worldMinutesOfDay = 8 * 60, time = "08:00", title = "早"),
            event("r_late", worldMinutesOfDay = 21 * 60, time = "21:00", title = "晚")
        )

        val result = projectLiving(mira, emptyList(), runtime)

        assertEquals("晚", result.currentActivity)
        assertTrue(result.timeline.first { it.id == "r_late" }.isCurrent)
        assertFalse(result.timeline.first { it.id == "r_early" }.isCurrent)
    }
}
