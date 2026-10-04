package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MomentPost
import com.example.data.social.SocialReactionStore
import com.example.data.social.withUserLike
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SocialReactionStoreTest {
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("social_reaction_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun momentTogglePersistsAcrossStoreRecreationAndRestoresAuthoredCount() {
        val post = post("a", count = 12, initialLiked = false)
        val store = SocialReactionStore(preferences)

        assertTrue(store.toggleMomentLike(post.id, post.isLiked))
        assertEquals(13, post.withUserLike(store.momentOverrides.value[post.id]).likesCount)

        val restarted = SocialReactionStore(preferences)
        assertTrue(restarted.momentOverrides.value[post.id] == true)
        assertEquals(13, post.withUserLike(restarted.momentOverrides.value[post.id]).likesCount)

        assertFalse(restarted.toggleMomentLike(post.id, post.isLiked))
        assertFalse(restarted.momentOverrides.value.containsKey(post.id))
        assertEquals(12, post.withUserLike(restarted.momentOverrides.value[post.id]).likesCount)
        assertFalse(SocialReactionStore(preferences).momentOverrides.value.containsKey(post.id))
    }

    @Test
    fun authoredPrelikedMomentCanBeUnlikedAndRestored() {
        val post = post("preliked", count = 34, initialLiked = true)
        val store = SocialReactionStore(preferences)

        assertFalse(store.toggleMomentLike(post.id, post.isLiked))
        val restarted = SocialReactionStore(preferences)
        val unliked = post.withUserLike(restarted.momentOverrides.value[post.id])
        assertFalse(unliked.isLiked)
        assertEquals(33, unliked.likesCount)

        assertTrue(restarted.toggleMomentLike(post.id, post.isLiked))
        assertEquals(34, post.withUserLike(SocialReactionStore(preferences).momentOverrides.value[post.id]).likesCount)
    }

    @Test
    fun diaryLikesArePerEntryAndPerCharacterAfterRestart() {
        val store = SocialReactionStore(preferences)
        assertTrue(store.toggleDiaryLike("mira", "diary_1"))
        assertFalse(store.isDiaryLiked("mira", "diary_2"))
        assertFalse(store.isDiaryLiked("yuna", "diary_1"))

        val restarted = SocialReactionStore(preferences)
        assertTrue(restarted.isDiaryLiked("mira", "diary_1"))
        assertFalse(restarted.isDiaryLiked("mira", "diary_2"))
        assertFalse(restarted.isDiaryLiked("yuna", "diary_1"))

        assertFalse(restarted.toggleDiaryLike("mira", "diary_1"))
        assertFalse(SocialReactionStore(preferences).isDiaryLiked("mira", "diary_1"))
    }

    private fun post(id: String, count: Int, initialLiked: Boolean) = MomentPost(
        id = id, authorId = "mira", authorName = "苏晚宁", timestamp = "12:00", moodTag = "",
        locationContext = "", content = "今天", likesCount = count, isLiked = initialLiked
    )
}
