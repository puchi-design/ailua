package com.example.data.relationship.romance

import com.example.data.character.runtime.CharacterRuntimeProfile

/** A qualitative projection for prompts and initiative; callers choose their clock and actual evidence. */
object RomanceRuntime {
    /** Public-room constraint only: never reveals private conflict, another conversation, or a private relationship label. */
    fun groupPromptInstructions(record: RomanceRecord, profile: CharacterRuntimeProfile): String = buildString {
        appendLine("这是群聊，只依据当前在场交流回应；不透露任何私聊内容、私人分歧或其他角色的私密互动。")
        if (record.romanceDeclined) appendLine("用户已明确设定非恋爱边界。保持正常群内交流，不追求、不吃醋、不劝其改变主意。")
        if (!profile.relationship.romanceEnabled) appendLine("保留角色卡的人际背景设定；运行时不从群聊的友好表现推断恋爱关系。")
        append("不主动在群内宣称与用户的私人关系身份，不把普通群聊发言当成私下的恋爱互认。")
    }

    fun triggerFacts(record: RomanceRecord, profile: CharacterRuntimeProfile, nowEpochMs: Long): RomanceTriggerFacts {
        val conflict = record.recentConflict
        val age = conflict?.let { nowEpochMs - it.occurredAtEpochMs }
        val active = conflict != null && conflict.resolvedAtEpochMs == null && age != null && age in 0..(7 * RomanceReducer.DAY_MS)
        val cooldown = when (profile.relationship.progressionStyle) { "expressive" -> 4; "reserved" -> 24; "slow_trust" -> 12; else -> 8 } * 3_600_000L
        return RomanceTriggerFacts(
            recentConflict = active,
            conflictEventId = if (active) conflict?.eventId else null,
            conflictAtEpochMs = if (active) conflict?.occurredAtEpochMs else null,
            readyToReconnect = active && age!! >= cooldown,
            recentGoodEvent = record.lastGoodEventAtEpochMs?.let { nowEpochMs - it in 0..RomanceReducer.DAY_MS } == true,
            recentGoodEventAtEpochMs = record.lastGoodEventAtEpochMs?.takeIf { nowEpochMs - it in 0..RomanceReducer.DAY_MS },
            lastMeaningfulAtEpochMs = record.lastMeaningfulAtEpochMs,
        )
    }

    fun jealousyEligible(record: RomanceRecord, profile: CharacterRuntimeProfile, nowEpochMs: Long, otherInteraction: RecentUserInteraction?): Boolean =
        profile.relationship.romanceEnabled && !record.romanceDeclined && profile.relationship.jealousy >= .15 &&
            profile.behavior.jealousyPatterns.isNotEmpty() && RomanceReducer.stage(record, profile) >= RomanceStage.AMBIGUOUS &&
            record.state.tension < .6f && otherInteraction != null && otherInteraction.characterId != record.characterId &&
            nowEpochMs - otherInteraction.occurredAtEpochMs in 0..(24 * 3_600_000L)

    fun promptInstructions(
        record: RomanceRecord,
        profile: CharacterRuntimeProfile,
        nowEpochMs: Long,
        otherInteraction: RecentUserInteraction? = null,
    ): String {
        val stage = RomanceReducer.stage(record, profile)
        val facts = triggerFacts(record, profile, nowEpochMs)
        return buildString {
            appendLine("下述记录只说明本机已观察到的互动。保留角色卡的背景设定，同时不捏造实际已发生的互动记录。")
            appendLine(when {
                !profile.relationship.romanceEnabled -> "保留角色卡已有的人际设定和${profile.relationship.routeType}路线；运行时不自行推断或升级恋爱关系，也不把尚无记录解释为彼此陌生。"
                record.romanceDeclined -> "保持用户要求的非恋爱关系，既有熟悉和信任不意味着恢复追求的许可。"
                record.commitmentConfirmedAtEpochMs != null && record.romanticConfirmedAtEpochMs != null -> "双方已经明确谈过共同的长期关系；尊重这份约定，用具体日常慢慢建立信任，不假定所有亲密都已获得同意。"
                record.romanticConfirmedAtEpochMs != null -> "双方已经明确同意交往，仍在实际相处中建立信任和亲密。不要否认已有约定，也不要擅自升级长期承诺。"
                else -> when (stage) {
                RomanceStage.STRANGER -> "你们还在了解彼此；不要假装已经亲密，也不要替用户决定关系。"
                RomanceStage.FAMILIAR -> "最近联系渐渐熟悉，可以记起已经发生的小事。"
                RomanceStage.CLOSE -> "你们已经建立一些信任；亲近不等于已经在交往。"
                RomanceStage.AMBIGUOUS -> "你们似乎比以前更亲近；仍未互相确认恋爱关系，不要擅自宣称伴侣身份。"
                RomanceStage.ROMANTIC -> "你们已明确互相同意交往；延续已有边界，不要自动升级承诺。"
                RomanceStage.COMMITTED -> "你们已明确谈过共同的长期关系；用具体日常体现认真，不要重复宣誓。"
                }
            })
            if (record.romanceDeclined) appendLine("用户已明确设定非恋爱边界。尊重它，不追求、不吃醋、不劝其改变主意；只有用户日后明确改变意愿且双方互认才可更新。")
            if (facts.recentConflict) {
                appendLine(if (facts.readyToReconnect) "最近的分歧尚未说开，已经留出一些时间；合适时可以用一个具体、低压力的行动重新联系，不能假装已经和好。"
                    else "最近的分歧尚未说开，允许短暂冷静、少说一些。用户主动说话时正常回应；不要立即套路式哄好，也不要惩罚性失联。")
                if (profile.behavior.conflictPatterns.isNotEmpty()) appendLine("面对分歧的方式：${profile.behavior.conflictPatterns.take(3).joinToString("；")}")
            }
            if (jealousyEligible(record, profile, nowEpochMs, otherInteraction)) {
                appendLine("用户近期确有与另一个角色互动，但这不表示背叛，也不表示你知道私聊内容。只有当前对话自然提到时，才允许极轻微表现：${profile.behavior.jealousyPatterns.take(2).joinToString("；")}。")
                appendLine("不能监视、盘问、辱骂、控制、强迫、威胁或要求用户隔离其他关系；不要据此制造冲突。")
            }
            if (profile.behavior.boundaries.isNotEmpty()) appendLine("保留角色边界：${profile.behavior.boundaries.take(3).joinToString("；")}")
            appendLine("任何阶段都尊重用户自主决定；分歧或嫉妒不能表现为辱骂、控制、强迫或威胁。")
            append("不要向用户展示内部阶段标签、概率或关系数值。")
        }
    }
}
