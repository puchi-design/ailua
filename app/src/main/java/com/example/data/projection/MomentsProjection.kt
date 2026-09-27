package com.example.data.projection

import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.MomentPost
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically
import com.example.data.registry.CharacterRegistry

/**
 * MomentsProjection
 *
 * Pure projection turning LifeEvent ledger facts into Moments feed posts.
 * No Compose, no Context, no storage.
 *
 * Rules:
 * - only MOMENT and PHOTO facts can become posts
 * - seed fixture posts keep priority for their ids (richer authored content)
 * - runtime posts use generic field mapping — no per-event-id special cases
 * - author identity comes from the caller's lookup (CharacterRegistry in the app)
 */
fun projectMoments(
    seedPosts: List<MomentPost>,
    runtimeEvents: List<LifeEvent>,
    seedEventIds: Set<String> = emptySet(),
    authorNameOf: (String) -> String = { CharacterRegistry.getCharacter(it).name }
): List<MomentPost> {
    val seedPostIds = seedPosts.mapTo(HashSet()) { it.id }
    val runtimePosts = runtimeEvents
        .filter { (it.type == LifeEventType.MOMENT || it.type == LifeEventType.PHOTO) && !it.isUserActivity() && it.id !in seedEventIds }
        .sortedChronologically()
        .reversed()
        .map { ev ->
            MomentPost(
                id = "moment_${ev.id}",
                authorId = ev.characterId,
                authorName = authorNameOf(ev.characterId),
                timestamp = ev.time,
                moodTag = ev.metadata["moodTag"] ?: "",
                locationContext = ev.location ?: "",
                content = ev.description,
                imageType = ev.imageReference ?: "",
                likesCount = 0,
                isLiked = false,
                comments = emptyList()
            )
        }
        .filterNot { it.id in seedPostIds }

    return runtimePosts + seedPosts
}
