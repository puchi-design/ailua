package com.example

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.MomentPost
import com.example.data.projection.projectMoments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MomentsProjectionTest
 *
 * P3B-2 Moments projection contract:
 * - only MOMENT/PHOTO facts become posts
 * - id identity is stable (moment_<eventId>), no distinctBy(time)
 * - seed fixture posts win for the same id
 * - runtime posts are newest-first and prepended; no fabricated fields
 */
class MomentsProjectionTest {

    private val seedPost = MomentPost(
        id = "moment_pulse_4",
        authorId = "mira",
        authorName = "小弥",
        timestamp = "20:12",
        moodTag = "被在意",
        locationContext = "雨停后的阳台",
        content = "seed内容",
        imageType = "flowers",
        likesCount = 3,
        isLiked = false
    )

    private fun event(
        id: String,
        characterId: String = "mira",
        type: LifeEventType = LifeEventType.MOMENT,
        time: String = "12:00",
        worldMinutesOfDay: Int = 12 * 60,
        description: String = "desc_$id",
        location: String? = null,
        imageReference: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) = LifeEvent(
        id = id,
        characterId = characterId,
        time = time,
        type = type,
        title = "title_$id",
        description = description,
        location = location,
        imageReference = imageReference,
        worldDateLabel = "9月25日",
        worldMinutesOfDay = worldMinutesOfDay,
        metadata = metadata
    )

    @Test
    fun momentEventProjectsToPostWithStableId() {
        val posts = projectMoments(
            seedPosts = emptyList(),
            runtimeEvents = listOf(event("e1", description = "今天很满足", location = "客厅")),
            authorNameOf = { "小弥" }
        )

        val post = posts.single()
        assertEquals("moment_e1", post.id)
        assertEquals("mira", post.authorId)
        assertEquals("小弥", post.authorName)
        assertEquals("今天很满足", post.content)
        assertEquals("客厅", post.locationContext)
        assertEquals("12:00", post.timestamp)
    }

    @Test
    fun nonMomentTypesDoNotProject() {
        val posts = projectMoments(
            seedPosts = emptyList(),
            runtimeEvents = listOf(
                event("t1", type = LifeEventType.THOUGHT),
                event("d1", type = LifeEventType.DIARY),
                event("m1", type = LifeEventType.MOMENT)
            ),
            authorNameOf = { "小弥" }
        )
        assertEquals(listOf("moment_m1"), posts.map { it.id })
    }

    @Test
    fun photoEventsAlsoProject() {
        val posts = projectMoments(
            seedPosts = emptyList(),
            runtimeEvents = listOf(event("p1", type = LifeEventType.PHOTO, imageReference = "rain_window")),
            authorNameOf = { "小弥" }
        )
        assertEquals("rain_window", posts.single().imageType)
    }

    @Test
    fun seedFixtureWinsForSameId() {
        val runtime = listOf(
            event("pulse_4", type = LifeEventType.PHOTO, description = "runtime版本")
        )
        val posts = projectMoments(
            seedPosts = listOf(seedPost),
            runtimeEvents = runtime,
            seedEventIds = emptySet(),
            authorNameOf = { "小弥" }
        )

        val match = posts.filter { it.id == "moment_pulse_4" }
        assertEquals(1, match.size)
        assertEquals("seed内容", match.single().content)
    }

    @Test
    fun seedWorldEventsExcludedById() {
        val runtime = listOf(event("pulse_7", description = "runtime重复"))
        val posts = projectMoments(
            seedPosts = listOf(seedPost),
            runtimeEvents = runtime,
            seedEventIds = setOf("pulse_7"),
            authorNameOf = { "小弥" }
        )
        assertEquals(listOf("moment_pulse_4"), posts.map { it.id })
    }

    @Test
    fun runtimePostsAreNewestFirstAndPrepended() {
        val older = event("e_old", worldMinutesOfDay = 8 * 60, time = "08:00")
        val newer = event("e_new", worldMinutesOfDay = 20 * 60, time = "20:00")

        val posts = projectMoments(
            seedPosts = listOf(seedPost),
            runtimeEvents = listOf(older, newer),
            authorNameOf = { "小弥" }
        )

        assertEquals(listOf("moment_e_new", "moment_e_old", "moment_pulse_4"), posts.map { it.id })
    }

    @Test
    fun noMetadataMeansNoMoodOrLocationFabrication() {
        val posts = projectMoments(
            seedPosts = emptyList(),
            runtimeEvents = listOf(event("e1", location = null)),
            authorNameOf = { "小弥" }
        )
        val post = posts.single()
        assertEquals("", post.moodTag)
        assertEquals("", post.locationContext)
        assertEquals("", post.imageType)
    }

    @Test
    fun allCharactersProjectIntoSharedFeed() {
        val posts = projectMoments(
            seedPosts = emptyList(),
            runtimeEvents = listOf(
                event("y1", characterId = "yuna"),
                event("n1", characterId = "noa")
            ),
            authorNameOf = { if (it == "yuna") "优娜" else "诺娅" }
        )
        assertEquals(2, posts.size)
        assertTrue(posts.any { it.authorName == "优娜" })
        assertTrue(posts.any { it.authorName == "诺娅" })
    }

    @Test
    fun sameMinuteEventsBothSurvive() {
        val posts = projectMoments(
            seedPosts = emptyList(),
            runtimeEvents = listOf(
                event("a", worldMinutesOfDay = 12 * 60, time = "12:00"),
                event("b", worldMinutesOfDay = 12 * 60, time = "12:00")
            ),
            authorNameOf = { "小弥" }
        )
        assertEquals(2, posts.size)
    }
}
