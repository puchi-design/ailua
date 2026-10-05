package com.example

import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.model.AiluaCharacterExtension
import com.example.data.projection.checkphone.PhonePrivacyLevel
import com.example.data.projection.checkphone.PhoneSection
import com.example.data.projection.checkphone.CheckPhonePlusSnapshot
import com.example.data.projection.checkphone.projectPhonePrivacy
import com.example.data.relationship.model.RelationshipStage
import com.example.data.relationship.model.RelationshipState
import com.example.data.relationship.romance.RomanceRecord
import com.example.data.relationship.romance.RomanceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckPhoneVisibilityTest {
    private val friendship = CharacterRuntimeResolver.fromExtension(AiluaCharacterExtension(), "yuna", "许朝颜")
    private fun state(stage: RelationshipStage) = RelationshipState("user", "yuna", stage = stage)
    private fun phone(privacy: PhonePrivacyLevel) = CheckPhonePlusSnapshot(
        characterId = "yuna", searches = emptyList(), drafts = emptyList(),
        photos = emptyList(), calls = emptyList(), notes = emptyList(),
        music = emptyList(), usage = emptyList(), browsing = emptyList(),
        saved = emptyList(), privacy = privacy,
    )

    @Test fun initialPrivacyAllowsOnlySearchAndUsage() {
        val snapshot = phone(projectPhonePrivacy("yuna", friendship, emptyList(), null))
        assertEquals(PhonePrivacyLevel.RECENT, snapshot.privacy)
        assertTrue(snapshot.canSee(PhoneSection.SEARCH))
        assertTrue(snapshot.canSee(PhoneSection.USAGE))
        assertFalse(snapshot.canSee(PhoneSection.PHOTOS))
        assertFalse(snapshot.canSee(PhoneSection.DRAFTS))
    }

    @Test fun persistedRelationshipStageUnlocksMediaThenPrivateNotes() {
        val familiar = phone(projectPhonePrivacy("yuna", friendship,
            listOf(state(RelationshipStage.FAMILIAR)), null))
        val close = phone(projectPhonePrivacy("yuna", friendship,
            listOf(state(RelationshipStage.CLOSE)), null))
        assertTrue(familiar.canSee(PhoneSection.PHOTOS))
        assertFalse(familiar.canSee(PhoneSection.NOTES))
        assertTrue(close.canSee(PhoneSection.NOTES))
        assertTrue(close.canSee(PhoneSection.DRAFTS))
    }

    @Test fun strainedRelationshipClosesPrivateSections() {
        val snapshot = phone(projectPhonePrivacy("yuna", friendship,
            listOf(state(RelationshipStage.STRAINED)), null))
        assertFalse(snapshot.canSee(PhoneSection.DRAFTS))
        assertFalse(snapshot.canSee(PhoneSection.MUSIC))
    }

    @Test fun intimateStageRevealsExistingBrowsingButNeverInventsEntries() {
        val snapshot = phone(projectPhonePrivacy("yuna", friendship,
            listOf(state(RelationshipStage.INTIMATE)), null))
        assertEquals(PhonePrivacyLevel.PRIVATE, snapshot.privacy)
        assertTrue(snapshot.canSee(PhoneSection.BROWSING))
        assertTrue(snapshot.canSee(PhoneSection.SAVED))
        assertTrue(snapshot.browsing.isEmpty())
    }

    @Test fun romanceUsesEvidenceStageRatherThanLegacyBondScore() {
        val romanceProfile = friendship.copy(characterId = "hewenchuan",
            relationship = friendship.relationship.copy(routeType = "romance"))
        val record = RomanceRecord("hewenchuan", state = RomanceState(
            familiarity = .38f, trust = .4f, attraction = .2f, intimacy = .2f))
        val snapshot = phone(projectPhonePrivacy("hewenchuan", romanceProfile, emptyList(), record))
        assertEquals(PhonePrivacyLevel.CLOSE, snapshot.privacy)
        assertTrue(snapshot.canSee(PhoneSection.DRAFTS))
    }
}
