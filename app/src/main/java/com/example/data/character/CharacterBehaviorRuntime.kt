package com.example.data.character

import com.example.data.character.runtime.CharacterRuntimeProfile
import com.example.data.character.runtime.CharacterRuntimeResolver
import com.example.data.character.runtime.RuntimeSource
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEventType
import com.example.data.model.ProactiveSettings

/** Formats a normalized runtime profile. JSON interpretation belongs solely to the resolver. */
object CharacterBehaviorRuntime {
    const val MAX_INSTRUCTION_CHARS = 4800

    fun prompt(profile: CharacterRuntimeProfile): String = buildString {
        appendLine("角色行为约定（倾向，不是已发生的关系事实）：")
        appendLine("关系进展只依据真实互动、当前关系状态与双方意愿；不把示例、偏好、阈值当作已恋爱、接触或同意的事实。")
        if (!profile.relationship.romanceEnabled) {
            appendLine("当前未启用恋爱路线；保留角色卡和用户选择，不因性别、标签或普通关心推断恋爱。")
        } else {
            appendLine("吃醋须同时有足够关系基础、近期明确互动和角色倾向；不凭空制造情敌。不辱骂、控制、强迫或威胁用户。")
            appendLine("冲突可需要时间或短暂冷淡，应说明暂停与再谈的意图；不要用失联或惩罚迫使用户妥协。")
        }
        if (profile.source == RuntimeSource.OFFICIAL) {
            appendLine("避免每条都安慰→总结→建议；可以问一句、开玩笑、谈自己的事、留白，不必回答完整。")
            appendLine("别频繁套用‘辛苦了’‘我会一直陪着你’‘你已经做得很好了’‘无论如何我都支持你’；真实语境适合时可以简短使用。")
        }
        with(profile.speech) {
            line("句子节奏", sentenceLength); line("表情频率", emojiFrequency)
            list("称呼", petNames); list("口头习惯", verbalTics); list("避免口癖", forbiddenPhrases); list("语气", tone)
        }
        with(profile.behavior) {
            line("核心欲望", coreDesire); list("缺点", flaws); list("误区", blindSpots)
            list("边界", boundaries); list("软肋", vulnerabilities); list("照顾的行为", carePatterns)
            if (profile.relationship.romanceEnabled) {
                list("暧昧的行为", flirtPatterns); list("嫉妒的行为", jealousyPatterns)
            }
            list("冲突的行为", conflictPatterns)
        }
        with(profile.relationship) {
            line("路线", routeType); line("初始关系", initialRelation)
            line("表达喜欢", affectionStyle); line("依恋方式", attachmentStyle); line("身体距离", physicalDistance)
            if (romanceEnabled) {
                line("推进节奏", when (progressionStyle) {
                    "slow_trust" -> "先累积信任，再缓慢显露吸引；行动多于表白"
                    "expressive" -> "直球分享和试探，亲近较快，仍要等对方回应"
                    "reserved" -> "熟悉得很慢；信任足够后才逐渐破例与靠近"
                    else -> "遵循角色设定和真实互动，不按消息次数自动推进"
                })
                line("嫉妒倾向", level(jealousy)); line("表白所需熟悉程度", level(confessionThreshold))
            }
        }
        with(profile.identity) {
            line("性别", gender); age?.let { line("年龄", "$it") }
            line("职业", occupation); heightCm?.let { line("身高", "${it}cm") }; line("生日", birthday)
        }
        append(worldGuidance(profile))
    }.trim().take(MAX_INSTRUCTION_CHARS)

    /** Kept for callers passing a card at an assembly boundary; business reads the returned profile. */
    fun prompt(data: CharacterCardData): String = prompt(CharacterRuntimeResolver.resolve(data))

    fun worldGuidance(profile: CharacterRuntimeProfile): String = buildString {
        with(profile.initiative) {
            line("主动消息频率", messageFrequency); line("来电频率", callFrequency)
            line("照片频率", photoFrequency); line("动态频率", momentFrequency); line("信件频率", letterFrequency)
            list("偏好触发情境", preferredTriggers)
            line("单次联系", if (maxTextBurst > 1) "最多${maxTextBurst}条短意群；合计一次联系，不能绕过免打扰与限额" else "一条有内容的消息，不连续催回复")
        }
        with(profile.life) {
            line("居所", home); line("工作地点", workplace); line("睡眠时段", sleepWindow)
            list("爱好", hobbies); list("社交圈角色ID", socialCircle)
        }
        appendLine("只引用已提供的生活、记忆和媒体；计划不是已发生的事件，不声称已发送不存在的照片或已经打通电话。")
    }

    fun worldGuidance(data: CharacterCardData): String = worldGuidance(CharacterRuntimeResolver.resolve(data))

    /** Character preference can only reduce the user's opt-in/contact limit, never raise it. */
    fun proactiveSettings(settings: ProactiveSettings, profile: CharacterRuntimeProfile): ProactiveSettings =
        when (profile.initiative.messageFrequency) {
            "none", "never", "off" -> settings.copy(enabled = false)
            "very_low" -> settings.copy(intervalHours = settings.intervalHours.coerceAtLeast(36), dailyLimit = minOf(settings.dailyLimit, 1))
            "low" -> settings.copy(intervalHours = settings.intervalHours.coerceAtLeast(18), dailyLimit = minOf(settings.dailyLimit, 1))
            "low_medium", "medium" -> settings.copy(intervalHours = settings.intervalHours.coerceAtLeast(8), dailyLimit = minOf(settings.dailyLimit, 2))
            else -> settings
        }

    fun isSleeping(profile: CharacterRuntimeProfile, minuteOfDay: Int): Boolean =
        sleeping(profile.life.sleepWindow, minuteOfDay)

    /** Used before scheduling; no change to the event ledger or already-recorded facts. */
    fun dailyActionLimit(profile: CharacterRuntimeProfile, kind: String): Int {
        val frequency = when (kind) {
            "call" -> profile.initiative.callFrequency
            "photo" -> profile.initiative.photoFrequency
            "moment" -> profile.initiative.momentFrequency
            "letter" -> profile.initiative.letterFrequency
            else -> return Int.MAX_VALUE
        }
        return when (frequency) {
            "none", "never", "off" -> 0
            "very_low", "low" -> 1
            "low_medium" -> if (kind == "photo") 2 else 1
            "medium" -> if (kind == "photo") 3 else 2
            "medium_high" -> if (kind == "photo") 4 else 3
            "high" -> if (kind == "photo") 6 else 4
            else -> 1 // Conservative fallback even when a caller bypasses the resolver.
        }
    }

    fun fallbackKinds(profile: CharacterRuntimeProfile): List<LifeEventType> = buildList {
        addAll(listOf(LifeEventType.THOUGHT, LifeEventType.MEAL, LifeEventType.TRAVEL, LifeEventType.SOCIAL, LifeEventType.MEMORY))
        when (profile.initiative.photoFrequency) {
            "none", "never", "off" -> Unit
            "high" -> repeat(3) { add(LifeEventType.PHOTO) }
            "medium_high", "medium", "low_medium" -> repeat(2) { add(LifeEventType.PHOTO) }
            else -> add(LifeEventType.PHOTO)
        }
        if (profile.initiative.momentProbability > 0) add(LifeEventType.MOMENT)
    }

    @Deprecated("Resolve the current character card and pass CharacterRuntimeProfile")
    fun proactiveSettings(settings: ProactiveSettings, e: AiluaCharacterExtension?): ProactiveSettings =
        e?.let { proactiveSettings(settings, CharacterRuntimeResolver.fromExtension(it)) } ?: settings

    @Deprecated("Resolve the current character card and pass CharacterRuntimeProfile")
    fun isSleeping(e: AiluaCharacterExtension?, minuteOfDay: Int): Boolean =
        e?.let { isSleeping(CharacterRuntimeResolver.fromExtension(it), minuteOfDay) } ?: false

    @Deprecated("Resolve the current character card and pass CharacterRuntimeProfile")
    fun dailyActionLimit(e: AiluaCharacterExtension?, kind: String): Int =
        e?.let { dailyActionLimit(CharacterRuntimeResolver.fromExtension(it), kind) } ?: Int.MAX_VALUE

    @Deprecated("Resolve the current character card and pass CharacterRuntimeProfile")
    fun fallbackKinds(e: AiluaCharacterExtension?): List<LifeEventType> =
        e?.let { fallbackKinds(CharacterRuntimeResolver.fromExtension(it)) }
            ?: listOf(LifeEventType.THOUGHT, LifeEventType.MEAL, LifeEventType.PHOTO, LifeEventType.TRAVEL, LifeEventType.SOCIAL, LifeEventType.MEMORY, LifeEventType.SLEEP)

    private fun sleeping(window: String, minuteOfDay: Int): Boolean {
        if (minuteOfDay !in 0..1439) return false
        val match = Regex("^(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})$").matchEntire(window) ?: return false
        val values = match.groupValues.drop(1).map { it.toInt() }
        if (values[0] !in 0..23 || values[2] !in 0..23 || values[1] !in 0..59 || values[3] !in 0..59) return false
        val start = values[0] * 60 + values[1]; val end = values[2] * 60 + values[3]
        if (start == end) return false
        return if (start < end) minuteOfDay in start until end else minuteOfDay >= start || minuteOfDay < end
    }

    private fun level(value: Double): String = when {
        value < .34 -> "低，轻微且有分寸"
        value < .67 -> "中，偶尔显露"
        else -> "高，需要明确情境才表达"
    }
    private fun StringBuilder.line(label: String, value: String) {
        if (value.isNotBlank()) appendLine("$label：${value.trim().take(240)}")
    }
    private fun StringBuilder.list(label: String, values: List<String>) =
        line(label, values.filter { it.isNotBlank() }.take(3).joinToString("；") { it.take(90) })
}
