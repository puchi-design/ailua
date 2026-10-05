package com.example.data.engine

import com.example.data.model.LIFE_EVENT_ACTOR_USER
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import java.util.concurrent.atomic.AtomicLong

/**
 * UserActivityRecorder — P3D-3 cross-app continuity.
 *
 * Every virtual-app action the user performs is appended to the same
 * LifeEvent ledger the character already writes to, so the next chat prompt
 * naturally carries it (WorldChatPromptContext.lifeEvents ->
 * PromptAssembler "Recent world events").
 *
 * User-originated events are always tagged with [LIFE_EVENT_ACTOR_USER] so
 * character-side projections never mistake them for the character's own life.
 */
object UserActivityRecorder {

    private const val MAX_SNIPPET = 80

    private val counter = AtomicLong(0)

    /** 收藏照片 -> PHOTO fact. */
    fun recordPhotoImport(
        title: String,
        characterId: String = activeCharacterId(),
    ): LifeEvent {
        val clock = WorldHeartbeatEngine.worldClock.value
        return WorldStateRepository.appendLifeEvent(
            LifeEvent(
                id = uniqueId("user_photo"),
                characterId = characterId,
                time = clock.timeFormatted,
                type = LifeEventType.PHOTO,
                title = "用户收藏了一张照片",
                description = snippet(title),
                location = null,
                worldDateLabel = clock.dateLabel,
                worldMinutesOfDay = clock.minutesOfDay,
                sourceAppId = "gallery",
                sourceRefId = null,
                metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER),
            )
        )
    }

    /** 聊天 -> MESSAGE fact tied to the character that was chatted with. */
    fun recordChatMessage(
        characterId: String,
        characterName: String,
        userText: String,
    ): LifeEvent? {
        val text = userText.trim()
        if (text.isEmpty()) return null
        val clock = WorldHeartbeatEngine.worldClock.value
        return WorldStateRepository.appendLifeEvent(
            LifeEvent(
                id = uniqueId("user_msg"),
                characterId = characterId,
                time = clock.timeFormatted,
                type = LifeEventType.MESSAGE,
                title = "用户向$characterName 发来消息",
                description = snippet("用户：$text"),
                location = null,
                worldDateLabel = clock.dateLabel,
                worldMinutesOfDay = clock.minutesOfDay,
                sourceAppId = "chat",
                sourceRefId = null,
                metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER),
            )
        )
    }

    /** 换地点 / 拜访某地 -> LOCATION_CHANGE fact. */
    fun recordPlaceVisit(
        characterId: String,
        placeName: String,
    ): LifeEvent {
        val clock = WorldHeartbeatEngine.worldClock.value
        return WorldStateRepository.appendLifeEvent(
            LifeEvent(
                id = uniqueId("user_visit"),
                characterId = characterId,
                time = clock.timeFormatted,
                type = LifeEventType.LOCATION_CHANGE,
                title = "用户来到了$placeName",
                description = snippet("用户移动到「$placeName」，想在附近待一会儿。"),
                location = placeName,
                worldDateLabel = clock.dateLabel,
                worldMinutesOfDay = clock.minutesOfDay,
                sourceAppId = "world",
                sourceRefId = null,
                metadata = mapOf("actor" to LIFE_EVENT_ACTOR_USER),
            )
        )
    }

    /** Opening a character's virtual phone is private usage evidence, never a prompt fact. */
    fun recordCheckPhoneView(characterId: String): LifeEvent {
        val clock = WorldHeartbeatEngine.worldClock.value
        return WorldStateRepository.appendLifeEvent(
            LifeEvent(
                id = uniqueId("user_check_phone"),
                characterId = characterId,
                time = clock.timeFormatted,
                type = LifeEventType.SOCIAL,
                title = "用户查看了角色手机",
                description = "打开了角色的虚拟手机",
                visibility = "PRIVATE",
                worldDateLabel = clock.dateLabel,
                worldMinutesOfDay = clock.minutesOfDay,
                sourceAppId = "check_phone",
                metadata = mapOf(
                    "actor" to LIFE_EVENT_ACTOR_USER,
                    "activity" to "USER_ACTIVITY",
                    "prompt_visibility" to "hidden",
                ),
            )
        )
    }

    private fun activeCharacterId(): String =
        WorldHeartbeatEngine.heartbeatState.value.activeCharacterId

    private fun uniqueId(prefix: String): String =
        "${prefix}_${System.currentTimeMillis()}_${counter.incrementAndGet()}"

    private fun snippet(text: String): String =
        text.replace('\n', ' ').take(MAX_SNIPPET)
}
