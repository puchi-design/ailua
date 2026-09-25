package com.example.data.repository

import com.example.data.engine.WorldStateRepository
import com.example.data.local.AiluaLocalStore
import com.example.data.model.Letter
import com.example.data.model.LetterDeliveryState
import com.example.data.model.LetterType
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MailboxRepository {

    private val initialLetters = listOf(
        Letter(
            id = "letter_mira_1",
            characterId = "mira",
            senderName = "小弥",
            subject = "《给忙了一整天的你》",
            body = "窗外的雨渐渐大起来了。听你今天说话的语气，感觉整个人紧绷着，像是一张被拉紧的弓弦……\n\n桌前给你留了半盒烘烤的金桂花干，如果夜里觉得苦闷，泡温水喝一点点就好。\n\n别对自己太苛刻，早点休息，好梦。",
            createdAtVirtualTime = "21:15",
            deliverAtVirtualTimeMinutes = 21 * 60 + 30, // 21:30
            deliverAtVirtualTimeString = "21:30",
            deliveryState = LetterDeliveryState.DELIVERED,
            letterType = LetterType.LETTER,
            paperColorHex = 0xFFFAF6EE
        ),
        Letter(
            id = "letter_yuna_1",
            characterId = "yuna",
            senderName = "悠奈",
            subject = "《限定布丁兑换券！🍮》",
            body = "当当当当！凭此券可于明天在木兰茶馆向悠奈兑换‘特浓手作焦糖布丁’一口！🍮\n\n不许赖皮，不许不来！要是今天遇到不开心的事，连吃两口也可以勉强批准啦～记住今晚要早点睡，明天见！",
            createdAtVirtualTime = "21:20",
            deliverAtVirtualTimeMinutes = 22 * 60 + 30, // 22:30
            deliverAtVirtualTimeString = "22:30",
            deliveryState = LetterDeliveryState.SCHEDULED,
            letterType = LetterType.NOTE,
            relatedLifeEventId = "pulse_10",
            paperColorHex = 0xFFFFF9ED
        ),
        Letter(
            id = "letter_noa_1",
            characterId = "noa",
            senderName = "诺亚",
            subject = "《夹在旧书里的银杏书签》",
            body = "在月光书阁整理《时间重叠之境》时，翻出了这张制于十年前的银杏叶书签。叶脉依然清晰如昨。\n\n世间大多数纷扰都如过眼云烟，唯有沉静下来的文字能够对抗虚无与遗忘。赠予你，愿能平复片刻焦躁。",
            createdAtVirtualTime = "21:00",
            deliverAtVirtualTimeMinutes = 23 * 60 + 0, // 23:00
            deliverAtVirtualTimeString = "23:00",
            deliveryState = LetterDeliveryState.SCHEDULED,
            letterType = LetterType.POSTCARD,
            relatedLifeEventId = "pulse_11",
            paperColorHex = 0xFFF5F3ED
        )
    )

    private val _letters = MutableStateFlow<List<Letter>>(initialLetters)
    val letters: StateFlow<List<Letter>> = _letters.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        syncWithLocalStore(21 * 60 + 30)
    }

    fun syncWithLocalStore(currentVirtualMinutes: Int) {
        val deliveredIds = AiluaLocalStore.deliveredLetterIds.value
        val openedIds = AiluaLocalStore.openedLetterIds.value

        val updated = _letters.value.map { letter ->
            when {
                openedIds.contains(letter.id) -> letter.copy(deliveryState = LetterDeliveryState.OPENED)
                deliveredIds.contains(letter.id) -> letter.copy(deliveryState = LetterDeliveryState.DELIVERED)
                letter.deliverAtVirtualTimeMinutes <= currentVirtualMinutes -> {
                    // Time is due -> mark delivered
                    AiluaLocalStore.markLetterDelivered(letter.id)
                    letter.copy(deliveryState = LetterDeliveryState.DELIVERED)
                }
                else -> letter.copy(deliveryState = LetterDeliveryState.SCHEDULED)
            }
        }
        _letters.value = updated
        _unreadCount.value = updated.count { it.deliveryState == LetterDeliveryState.DELIVERED }
    }

    /**
     * Evaluates scheduled letters when virtual clock advances.
     */
    fun checkScheduledDeliveries(currentVirtualMinutes: Int): List<Letter> {
        val newlyDelivered = mutableListOf<Letter>()
        val updated = _letters.value.map { letter ->
            if (letter.deliveryState == LetterDeliveryState.SCHEDULED && letter.deliverAtVirtualTimeMinutes <= currentVirtualMinutes) {
                AiluaLocalStore.markLetterDelivered(letter.id)
                val delivered = letter.copy(deliveryState = LetterDeliveryState.DELIVERED)
                newlyDelivered.add(delivered)

                // Push LifeEvent to shared WorldStateRepository
                WorldStateRepository.appendLifeEvent(
                    LifeEvent(
                        id = "pulse_letter_${letter.id}",
                        characterId = letter.characterId,
                        time = letter.deliverAtVirtualTimeString,
                        type = LifeEventType.THOUGHT,
                        title = "${letter.senderName}寄达了新信笺",
                        description = "《${letter.subject}》已轻轻投递至心网信箱，等待你开启品读。",
                        location = if (letter.characterId == "mira") "青石街23号" else if (letter.characterId == "yuna") "街角全家便利店" else "月光书阁"
                    )
                )

                delivered
            } else {
                letter
            }
        }

        if (newlyDelivered.isNotEmpty()) {
            _letters.value = updated
            _unreadCount.value = updated.count { it.deliveryState == LetterDeliveryState.DELIVERED }
        }
        return newlyDelivered
    }

    fun markOpened(letterId: String) {
        AiluaLocalStore.markLetterOpened(letterId)
        val updated = _letters.value.map {
            if (it.id == letterId) it.copy(deliveryState = LetterDeliveryState.OPENED) else it
        }
        _letters.value = updated
        _unreadCount.value = updated.count { it.deliveryState == LetterDeliveryState.DELIVERED }
    }
}
