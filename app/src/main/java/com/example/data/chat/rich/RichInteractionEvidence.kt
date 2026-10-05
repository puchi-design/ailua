package com.example.data.chat.rich

import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.relationship.romance.RomanceRepository

/** Called only after ChatRepository accepts a card transition; never touches any payment system. */
object RichInteractionEvidence {
    fun record(
        characterId: String,
        turnId: String,
        variantId: String,
        payloadIndex: Int,
        payload: RichMessagePayload,
        status: RichMessageStatus,
    ) {
        if (characterId.isBlank() || variantId.isBlank() || payloadIndex < 0) return
        val valid = when (payload.type) {
            RichMessageType.RED_PACKET -> status == RichMessageStatus.OPENED
            RichMessageType.TRANSFER -> status == RichMessageStatus.ACCEPTED || status == RichMessageStatus.DECLINED
            RichMessageType.GIFT -> status == RichMessageStatus.RECEIVED
            else -> false
        }
        if (!valid) return
        val clock = WorldHeartbeatEngine.worldClock.value
        val id = "rich_${variantId}_${payloadIndex}_${status.name.lowercase()}"
        val action = when (status) {
            RichMessageStatus.OPENED -> "打开了虚拟红包"
            RichMessageStatus.ACCEPTED -> "收下了虚拟转账"
            RichMessageStatus.DECLINED -> "退回了虚拟转账"
            RichMessageStatus.RECEIVED -> "收下了礼物"
            else -> return
        }
        WorldStateRepository.appendLifeEvent(LifeEvent(
            id = id,
            characterId = characterId,
            time = clock.timeFormatted,
            // Declining is recorded as a neutral private fact. MESSAGE would award
            // relationship progress through the existing RelationshipReducer.
            type = if (status == RichMessageStatus.DECLINED) LifeEventType.SOCIAL else LifeEventType.MESSAGE,
            title = action,
            description = payload.label.orEmpty().take(100),
            visibility = "PRIVATE",
            relatedCharacterIds = listOf("user"),
            worldDateLabel = clock.dateLabel,
            worldMinutesOfDay = clock.minutesOfDay,
            sourceAppId = "chat",
            sourceRefId = turnId,
            metadata = mapOf(
                "actor" to LIFE_EVENT_ACTOR_USER,
                "rich_type" to payload.type.name,
                "rich_status" to status.name,
                "rich_variant_id" to variantId,
            ),
        ))
        if (status != RichMessageStatus.DECLINED) {
            RomanceRepository.recordRichInteraction(characterId, id)
        }
    }
}
