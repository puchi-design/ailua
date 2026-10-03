package com.example.data.firstsession

import android.content.Context
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.WorldClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FirstSessionState(
    val onboardingComplete: Boolean = false,
    val sentFirstMessage: Boolean = false,
    val receivedFirstReply: Boolean = false,
    val viewedLiving: Boolean = false,
    val viewedMoments: Boolean = false,
    val viewedCheckPhone: Boolean = false,
    val continuationCreated: Boolean = false,
) {
    val journeyComplete: Boolean get() = receivedFirstReply && viewedLiving && continuationCreated
}

object FirstSessionPolicy {
    /** The first short fact must be voluntarily and explicitly stated by the user. */
    fun firstMemory(text: String): String? {
        val trimmed = text.trim().take(120)
        val name = Regex("^(?:我叫|你可以叫我|叫我)([\\p{L}\\p{N}]{2,12})[。！!，, ]*$").matchEntire(trimmed)
        if (name != null) return "用户希望被称为${name.groupValues[1]}"
        return null
    }

    fun continuation(characterId: String, characterName: String, location: String, clock: WorldClock): LifeEvent {
        val activity = when (characterId) {
            "yan" -> "修复书桌上的旧书"
            "yeo" -> "整理今天拍下的照片"
            "yuna" -> "整理新甜点的食谱"
            "noa" -> "翻开桌边的旧书"
            else -> "整理窗边的手记"
        }
        return LifeEvent(
            id = "first_session_continuation_$characterId", characterId = characterId,
            time = clock.timeFormatted, type = LifeEventType.THOUGHT,
            title = "$characterName$activity", description = "$characterName$activity，继续自己的生活。",
            location = location, worldDateLabel = clock.dateLabel,
            worldMinutesOfDay = clock.minutesOfDay, sourceAppId = "first_session",
        )
    }
}

/** Tiny local journey flags; the world fact still uses the existing LifeEvent ledger. */
object FirstSessionStore {
    private var prefs: android.content.SharedPreferences? = null
    private val mutable = MutableStateFlow(FirstSessionState())
    val state = mutable.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE)
        val p = prefs ?: return
        mutable.value = FirstSessionState(
            p.getBoolean("onboarding", false), p.getBoolean("sent", false),
            p.getBoolean("reply", false), p.getBoolean("living", false),
            p.getBoolean("moments", false), p.getBoolean("check_phone", false),
            p.getBoolean("continuation", false),
        )
    }

    private fun update(key: String, change: (FirstSessionState) -> FirstSessionState) {
        val next = change(mutable.value)
        prefs?.edit()?.putBoolean(key, when (key) {
            "onboarding" -> next.onboardingComplete
            "sent" -> next.sentFirstMessage
            "reply" -> next.receivedFirstReply
            "living" -> next.viewedLiving
            "moments" -> next.viewedMoments
            "check_phone" -> next.viewedCheckPhone
            else -> next.continuationCreated
        })?.apply()
        mutable.value = next
    }

    fun completeOnboarding() = update("onboarding") { it.copy(onboardingComplete = true) }
    fun markSent() = update("sent") { it.copy(sentFirstMessage = true) }
    fun markLiving() = update("living") { it.copy(viewedLiving = true) }
    fun markMoments() = update("moments") { it.copy(viewedMoments = true) }
    fun markCheckPhone() = update("check_phone") { it.copy(viewedCheckPhone = true) }

    fun markFirstReply(characterId: String, characterName: String, location: String) {
        if (mutable.value.receivedFirstReply) return
        val clock = WorldHeartbeatEngine.worldClock.value
        WorldStateRepository.appendLifeEvent(FirstSessionPolicy.continuation(characterId, characterName, location, clock))
        update("reply") { it.copy(receivedFirstReply = true) }
        update("continuation") { it.copy(continuationCreated = true) }
    }
}
