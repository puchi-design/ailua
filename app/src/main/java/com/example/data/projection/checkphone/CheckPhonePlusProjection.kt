package com.example.data.projection.checkphone

import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.character.runtime.RuntimeSource
import com.example.data.engine.ScheduledActionType
import com.example.data.mock.MockData
import com.example.data.model.CallSession
import com.example.data.model.CallState
import com.example.data.model.CheckPhoneData
import com.example.data.model.GalleryAsset
import com.example.data.model.GalleryAssetType
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WorldPlan
import com.example.data.model.WorldClock
import com.example.data.projection.eligiblePhoneEvents
import com.example.data.projection.EMPTY_CHECK_PHONE_DATA
import com.example.data.projection.projectCheckPhone
import com.example.data.projection.projectGalleryAssets
import com.example.data.relationship.model.RelationshipState
import com.example.data.relationship.romance.RomanceRecord

/**
 * Read-only extension of [projectCheckPhone]. All dynamic traces derive from persisted world,
 * gallery, call and relationship records. A missing fact stays empty; opening a phone never writes
 * a search, a draft or a usage record and never reads the user's MemoRepository.
 */
fun projectCheckPhonePlus(
    characterId: String,
    events: List<LifeEvent>,
    profile: CharacterRuntimeProfile,
    galleryAssets: List<GalleryAsset> = emptyList(),
    callHistory: List<CallSession> = emptyList(),
    worldPlan: WorldPlan? = null,
    relationships: List<RelationshipState> = emptyList(),
    romanceRecord: RomanceRecord? = null,
    seedData: CheckPhoneData? = null,
    seedEventIds: Set<String> = MockData.unifiedLifeEvents.mapTo(HashSet()) { it.id },
    firedActionIds: Set<String> = emptySet(),
    clock: WorldClock? = null,
): CheckPhonePlusSnapshot {
    // A user-edited card retaining an official ID is a new persona for authored phone history.
    // Runtime facts remain attached to its stable ID; the old official seed must not leak in.
    val effectiveSeed = seedData ?: if (profile.source == RuntimeSource.OFFICIAL) null else EMPTY_CHECK_PHONE_DATA
    val base = projectCheckPhone(characterId, events, seedEventIds, effectiveSeed)
    val runtime = eligiblePhoneEvents(characterId, events, seedEventIds)
    val searches = projectCharacterSearchHistory(characterId, events, base, seedEventIds, profile, clock)
    val drafts = projectCharacterDrafts(characterId, events, worldPlan, base, seedEventIds)

    val runtimeNotes = runtime.mapNotNull { event ->
        event.metadata["note"]?.trim()?.takeIf(String::isNotEmpty)?.let { text ->
            CharacterPhoneNote("note:${event.id}", text, event.time, false, PhoneTraceSource.RUNTIME_EVENT)
        }
    }
    val plannedNotes = worldPlan?.actions.orEmpty().filter {
        it.characterId == characterId && it.type == ScheduledActionType.LIFE_EVENT &&
            it.id !in firedActionIds && it.metadata["note"]?.isNotBlank() == true &&
            it.lifeEventType !in setOf(LifeEventType.SLEEP, LifeEventType.MESSAGE) &&
            "user" !in it.relatedCharacterIds
    }.take(4).map { action ->
        CharacterPhoneNote("plan-note:${action.id}", action.metadata.getValue("note"),
            "%02d:%02d".format(action.triggerMinutes / 60, action.triggerMinutes % 60),
            true, PhoneTraceSource.WORLD_PLAN)
    }
    val authoredNotes = base.notes.drop(runtimeNotes.size).mapIndexed { index, text ->
        CharacterPhoneNote("authored-note:$characterId:$index", text, null, false, PhoneTraceSource.AUTHORED)
    }

    val allPhotos = projectGalleryAssets(galleryAssets.filter {
        it.characterId == characterId && it.type != GalleryAssetType.USER_IMPORTED
    }, events)
        .filter { it.characterId == characterId && it.type != GalleryAssetType.USER_IMPORTED }
        .distinctBy { it.lifeEventId ?: it.id }
    val calls = callHistory.filter { it.characterId == characterId &&
        it.state in setOf(CallState.ENDED, CallState.MISSED, CallState.DECLINED) }
        .sortedWith(compareByDescending<CallSession> { it.endedAt }.thenByDescending { it.scheduledAtMinutes })
    return CheckPhonePlusSnapshot(
        characterId = characterId,
        searches = searches,
        drafts = drafts,
        photos = allPhotos.take(20),
        calls = calls.take(20),
        notes = (runtimeNotes + plannedNotes + authoredNotes).distinctBy { it.text }.take(20),
        music = base.recentlyPlayed.distinctBy { it.title to it.artist }.take(20),
        usage = projectCharacterPhoneUsage(characterId, events, seedEventIds),
        browsing = base.browsingHistory.distinct().take(20),
        saved = base.savedItems.distinct().take(20),
        privacy = projectPhonePrivacy(characterId, profile, relationships, romanceRecord),
    )
}
