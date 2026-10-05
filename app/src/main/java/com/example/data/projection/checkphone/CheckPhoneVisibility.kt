package com.example.data.projection.checkphone

import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.relationship.model.RelationshipStage
import com.example.data.relationship.model.RelationshipState
import com.example.data.relationship.romance.RomanceRecord
import com.example.data.relationship.romance.RomanceReducer
import com.example.data.relationship.romance.RomanceStage

/** Uses existing qualitative stages. No phone-specific affinity threshold or new relationship state. */
fun projectPhonePrivacy(
    characterId: String,
    profile: CharacterRuntimeProfile,
    relationships: List<RelationshipState>,
    romanceRecord: RomanceRecord?,
): PhonePrivacyLevel {
    val relation = relationships.firstOrNull {
        setOf(it.fromCharacterId, it.toCharacterId) == setOf("user", characterId)
    }
    if (profile.relationship.romanceEnabled && romanceRecord?.characterId == characterId) {
        return when (RomanceReducer.stage(romanceRecord, profile)) {
            RomanceStage.STRANGER -> PhonePrivacyLevel.RECENT
            RomanceStage.FAMILIAR -> PhonePrivacyLevel.FAMILIAR
            RomanceStage.CLOSE, RomanceStage.AMBIGUOUS -> PhonePrivacyLevel.CLOSE
            RomanceStage.ROMANTIC, RomanceStage.COMMITTED -> PhonePrivacyLevel.PRIVATE
        }
    }
    return when (relation?.stage) {
        RelationshipStage.FAMILIAR -> PhonePrivacyLevel.FAMILIAR
        RelationshipStage.CLOSE -> PhonePrivacyLevel.CLOSE
        RelationshipStage.INTIMATE -> PhonePrivacyLevel.PRIVATE
        RelationshipStage.STRANGER, RelationshipStage.ACQUAINTANCE,
        RelationshipStage.STRAINED, null -> PhonePrivacyLevel.RECENT
    }
}
