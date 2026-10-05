package com.example.data.projection

import com.example.data.mock.MockData
import com.example.data.mock.OfficialCharacters
import com.example.data.model.CheckPhoneData
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.MusicTrack
import com.example.data.model.PrivatePhoto
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically

/**
 * CheckPhoneProjection
 *
 * Pure projection: the phone-owner's private traces assembled ONLY from facts that
 * actually exist. No invented search history, drafts, or browsing — if a fact did
 * not happen, its section stays empty.
 *
 * Rules:
 * - Mira keeps her authored seed phone data; other characters start from empty
 *   (never fall back to Mira's content)
 * - PHOTO facts project into the private gallery
 * - explicit metadata keys project into their matching sections
 *   (search_query / draft / note / music_* / browsing_query / saved_item / hidden_thought)
 * - seed world LifeEvents are excluded (their traces already live in seed data)
 */
val EMPTY_CHECK_PHONE_DATA = CheckPhoneData(
    searchHistory = emptyList(),
    unsentDrafts = emptyList(),
    notes = emptyList(),
    recentlyPlayed = emptyList(),
    privateGallery = emptyList(),
    browsingHistory = emptyList(),
    savedItems = emptyList(),
    hiddenThoughts = emptyList()
)

fun projectCheckPhone(
    characterId: String,
    runtimeEvents: List<LifeEvent>,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
    seedData: CheckPhoneData? = null
): CheckPhoneData {
    val seed = seedData
        ?: OfficialCharacters.checkPhoneByCharacter[characterId]
        ?: if (characterId == "mira") MockData.checkPhoneData else EMPTY_CHECK_PHONE_DATA

    val runtime = eligiblePhoneEvents(characterId, runtimeEvents, seedEventIds)
    if (runtime.isEmpty()) return seed

    val gallery = runtime
        .filter { it.type == LifeEventType.PHOTO }
        .map { ev ->
            PrivatePhoto(
                title = ev.metadata["photo_title"] ?: ev.title,
                time = ev.time,
                imageType = ev.imageReference ?: "",
                note = ev.description
            )
        }

    val tracks = runtime.mapNotNull { ev ->
        ev.metadata["music_title"]?.takeIf(String::isNotBlank)?.let { title ->
            MusicTrack(
                title = title,
                artist = ev.metadata["music_artist"] ?: "",
                albumCoverType = ev.metadata["music_cover"] ?: "",
                duration = ev.metadata["music_duration"] ?: ""
            )
        }
    }

    return seed.copy(
        privateGallery = gallery + seed.privateGallery,
        searchHistory = runtime.mapNotNull { it.metadata["search_query"]?.takeIf(String::isNotBlank) } + seed.searchHistory,
        unsentDrafts = runtime.mapNotNull { it.metadata["draft"]?.takeIf(String::isNotBlank) } + seed.unsentDrafts,
        notes = runtime.mapNotNull { it.metadata["note"]?.takeIf(String::isNotBlank) } + seed.notes,
        recentlyPlayed = tracks + seed.recentlyPlayed,
        browsingHistory = runtime.mapNotNull { it.metadata["browsing_query"]?.takeIf(String::isNotBlank) } + seed.browsingHistory,
        savedItems = runtime.mapNotNull { it.metadata["saved_item"]?.takeIf(String::isNotBlank) } + seed.savedItems,
        hiddenThoughts = runtime.mapNotNull { it.metadata["hidden_thought"]?.takeIf(String::isNotBlank) } + seed.hiddenThoughts
    )
}

/** Shared eligibility boundary for the original projection and its richer read-only view. */
internal fun eligiblePhoneEvents(
    characterId: String,
    events: List<LifeEvent>,
    seedEventIds: Set<String>,
): List<LifeEvent> = events
    .filter { it.characterId == characterId && !it.isUserActivity() && it.id !in seedEventIds }
    .sortedChronologically()
    .reversed()
