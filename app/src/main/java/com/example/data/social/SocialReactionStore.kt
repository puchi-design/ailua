package com.example.data.social

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MomentPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local user reactions, separate from the character/world event ledger. */
class SocialReactionStore(private val preferences: SharedPreferences) {
    private val mutableMomentOverrides = MutableStateFlow(readMomentOverrides())
    val momentOverrides = mutableMomentOverrides.asStateFlow()

    private val mutableDiaryLikes = MutableStateFlow(readSet(DIARY_LIKES))
    val diaryLikes = mutableDiaryLikes.asStateFlow()

    /** Returns the effective state. The projected post's initial state remains the baseline. */
    @Synchronized
    fun toggleMomentLike(postId: String, initialLiked: Boolean): Boolean {
        // Read the latest saved set so another screen instance cannot lose a reaction.
        val saved = readMomentOverrides()
        val current = saved[postId] ?: initialLiked
        val next = !current
        val updated = saved.toMutableMap().apply {
            if (next == initialLiked) remove(postId) else put(postId, next)
        }
        val liked = updated.filterValues { it }.keys
        val unliked = updated.filterValues { !it }.keys
        if (preferences.edit().putStringSet(MOMENT_LIKED, liked).putStringSet(MOMENT_UNLIKED, unliked).commit()) {
            mutableMomentOverrides.value = updated
            return next
        }
        return current
    }

    fun isDiaryLiked(characterId: String, entryId: String, likedKeys: Set<String> = mutableDiaryLikes.value): Boolean =
        diaryKey(characterId, entryId) in likedKeys

    @Synchronized
    fun toggleDiaryLike(characterId: String, entryId: String): Boolean {
        val key = diaryKey(characterId, entryId)
        val updated = readSet(DIARY_LIKES).toMutableSet()
        val liked = if (key in updated) {
            updated.remove(key)
            false
        } else {
            updated.add(key)
            true
        }
        if (preferences.edit().putStringSet(DIARY_LIKES, updated).commit()) {
            mutableDiaryLikes.value = updated
            return liked
        }
        return !liked
    }

    private fun readMomentOverrides(): Map<String, Boolean> = buildMap {
        readSet(MOMENT_LIKED).forEach { put(it, true) }
        readSet(MOMENT_UNLIKED).forEach { put(it, false) }
    }

    private fun readSet(key: String): Set<String> = preferences.getStringSet(key, emptySet())?.toSet().orEmpty()

    private fun diaryKey(characterId: String, entryId: String): String = "${characterId.length}:$characterId$entryId"

    companion object {
        private const val FILE = "ailua_social_reactions"
        private const val MOMENT_LIKED = "moment_liked"
        private const val MOMENT_UNLIKED = "moment_unliked"
        private const val DIARY_LIKES = "diary_likes"

        fun create(context: Context): SocialReactionStore = SocialReactionStore(
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        )
    }
}

/** Adjust only the user's one vote; authored counts and other people's reactions stay intact. */
fun MomentPost.withUserLike(override: Boolean?): MomentPost {
    val liked = override ?: isLiked
    val delta = liked.toInt() - isLiked.toInt()
    return copy(isLiked = liked, likesCount = (likesCount + delta).coerceAtLeast(0))
}

private fun Boolean.toInt(): Int = if (this) 1 else 0
