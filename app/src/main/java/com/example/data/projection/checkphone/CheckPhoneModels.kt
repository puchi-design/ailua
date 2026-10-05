package com.example.data.projection.checkphone

import com.example.data.model.CallSession
import com.example.data.model.GalleryAsset
import com.example.data.model.MusicTrack

enum class PhoneTraceSource { RUNTIME_EVENT, WORLD_PLAN, RULE_PROJECTION, AUTHORED }
enum class CharacterDraftStatus { UNSENT }
enum class PhoneSection { SEARCH, DRAFTS, PHOTOS, CALLS, NOTES, MUSIC, USAGE, BROWSING, SAVED }
enum class PhonePrivacyLevel { RECENT, FAMILIAR, CLOSE, PRIVATE }

data class PhoneSearchRecord(val id: String, val query: String, val time: String?, val source: PhoneTraceSource)
data class CharacterDraft(
    val id: String,
    val characterId: String,
    val text: String,
    val createdAt: String?,
    val contextEventId: String? = null,
    val status: CharacterDraftStatus = CharacterDraftStatus.UNSENT,
    val source: PhoneTraceSource,
)
data class CharacterPhoneNote(val id: String, val text: String, val time: String?, val planned: Boolean, val source: PhoneTraceSource)
data class CharacterPhoneUsage(val id: String, val appName: String, val time: String, val sourceEventId: String)

data class CheckPhonePlusSnapshot(
    val characterId: String,
    val searches: List<PhoneSearchRecord>,
    val drafts: List<CharacterDraft>,
    val photos: List<GalleryAsset>,
    val calls: List<CallSession>,
    val notes: List<CharacterPhoneNote>,
    val music: List<MusicTrack>,
    val usage: List<CharacterPhoneUsage>,
    val browsing: List<String>,
    val saved: List<String>,
    val privacy: PhonePrivacyLevel,
) {
    fun canSee(section: PhoneSection): Boolean = when (section) {
        PhoneSection.SEARCH, PhoneSection.USAGE -> true
        PhoneSection.PHOTOS, PhoneSection.CALLS, PhoneSection.MUSIC -> privacy >= PhonePrivacyLevel.FAMILIAR
        PhoneSection.DRAFTS, PhoneSection.NOTES -> privacy >= PhonePrivacyLevel.CLOSE
        PhoneSection.BROWSING, PhoneSection.SAVED -> privacy >= PhonePrivacyLevel.PRIVATE
    }
}
