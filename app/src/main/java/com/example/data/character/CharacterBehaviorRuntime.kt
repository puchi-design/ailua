package com.example.data.character

import com.example.data.codec.AiluaCharacterExtensionCodec
import com.example.data.model.AiluaCharacterExtension
import com.example.data.model.CharacterCardData
import com.example.data.model.LifeEventType
import com.example.data.model.ProactiveSettings

/** Interprets only the versioned AILUA namespace; unrelated vendor JSON is never prompt text. */
object CharacterBehaviorRuntime {
    fun prompt(data: CharacterCardData): String? {
        val e = AiluaCharacterExtensionCodec.readOrNull(data) ?: return null
        return buildString {
            appendLine("角色行为约定（倾向，不是已发生的关系事实）：")
            with(e.identity) {
                line("性别", gender); age?.let { line("年龄", "$it") }
                line("职业", occupation); heightCm?.let { line("身高", "${it}cm") }; line("生日", birthday)
            }
            with(e.relationship) {
                line("路线", routeType); line("初始关系", initialRelation)
                line("表达喜欢", affectionStyle); line("依恋方式", attachmentStyle)
                line("身体距离", physicalDistance)
                if (routeType == "romance") {
                    line("嫉妒倾向", level(jealousy)); line("占有欲倾向", level(possessiveness))
                    line("表白所需熟悉程度", level(confessionThreshold))
                    appendLine("关系进展依据真实对话与双方回应；不要把倾向或阈值当作已经恋爱、身体接触或用户同意的事实。")
                }
            }
            with(e.behavior) {
                line("核心欲望", coreDesire); list("缺点", flaws); list("误区", blindSpots)
                list("边界", boundaries); list("软肋", vulnerabilities)
                list("照顾的行为", carePatterns); list("暧昧的行为", flirtPatterns)
                list("嫉妒的行为", jealousyPatterns); list("冲突的行为", conflictPatterns)
            }
            with(e.speech) {
                line("句子节奏", sentenceLength); line("表情频率", emojiFrequency)
                list("称呼", petNames); list("口头习惯", verbalTics); list("避免口癖", forbiddenPhrases); list("语气", tone)
            }
            append(worldGuidance(e))
            appendLine("保持这个角色的缺点和表达差异，不把所有角色都写成同一种完美安慰者。记忆只引用已有事实。")
        }.trim()
    }

    fun worldGuidance(data: CharacterCardData): String? =
        AiluaCharacterExtensionCodec.readOrNull(data)?.let(::worldGuidance)

    private fun worldGuidance(e: AiluaCharacterExtension): String = buildString {
        with(e.initiative) {
            line("主动消息频率", messageFrequency); line("来电频率", callFrequency)
            line("照片频率", photoFrequency); line("动态频率", momentFrequency); list("偏好触发情境", preferredTriggers)
        }
        with(e.life) {
            line("居所", home); line("工作地点", workplace); line("睡眠时段", sleepWindow)
            list("爱好", hobbies); list("社交圈角色ID", socialCircle)
        }
    }

    /** Character preference can only reduce the user's opt-in/contact limit, never raise it. */
    fun proactiveSettings(settings: ProactiveSettings, e: AiluaCharacterExtension?): ProactiveSettings {
        return when (e?.initiative?.messageFrequency?.lowercase()) {
            "none", "never", "off" -> settings.copy(enabled = false)
            "low" -> settings.copy(intervalHours = settings.intervalHours.coerceAtLeast(18), dailyLimit = minOf(settings.dailyLimit, 1))
            "medium" -> settings.copy(intervalHours = settings.intervalHours.coerceAtLeast(8), dailyLimit = minOf(settings.dailyLimit, 2))
            else -> settings
        }
    }

    fun isSleeping(e: AiluaCharacterExtension?, minuteOfDay: Int): Boolean {
        val match = Regex("^(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})$")
            .matchEntire(e?.life?.sleepWindow.orEmpty()) ?: return false
        val values = match.groupValues.drop(1).map { it.toInt() }
        if (values[0] !in 0..23 || values[2] !in 0..23 || values[1] !in 0..59 || values[3] !in 0..59) return false
        val start = values[0] * 60 + values[1]; val end = values[2] * 60 + values[3]
        if (start == end) return false
        return if (start < end) minuteOfDay in start until end else minuteOfDay >= start || minuteOfDay < end
    }

    /** Used by the existing planner's candidate pass, without changing the event ledger or call engine. */
    fun dailyActionLimit(e: AiluaCharacterExtension?, kind: String): Int {
        val frequency = when (kind) {
            "call" -> e?.initiative?.callFrequency
            "photo" -> e?.initiative?.photoFrequency
            "moment" -> e?.initiative?.momentFrequency
            else -> null
        }
        return when (frequency?.lowercase()) {
            "none", "never", "off" -> 0
            "low" -> 1
            "medium" -> if (kind == "photo") 3 else 2
            "high" -> if (kind == "photo") 6 else 4
            else -> Int.MAX_VALUE // Ordinary V2 cards retain the existing planner's limits.
        }
    }

    fun fallbackKinds(e: AiluaCharacterExtension?): List<LifeEventType> = if (e == null) {
        listOf(LifeEventType.THOUGHT, LifeEventType.MEAL, LifeEventType.PHOTO, LifeEventType.TRAVEL,
            LifeEventType.SOCIAL, LifeEventType.MEMORY, LifeEventType.SLEEP)
    } else buildList {
        addAll(listOf(LifeEventType.THOUGHT, LifeEventType.MEAL, LifeEventType.TRAVEL, LifeEventType.SOCIAL, LifeEventType.MEMORY))
        when (e?.initiative?.photoFrequency?.lowercase()) {
            "none", "never", "off" -> Unit
            "high" -> addAll(listOf(LifeEventType.PHOTO, LifeEventType.PHOTO, LifeEventType.PHOTO))
            else -> add(LifeEventType.PHOTO)
        }
        if (e?.initiative?.momentFrequency?.lowercase() in listOf("medium", "high")) add(LifeEventType.MOMENT)
    }

    private fun level(value: Double): String = when {
        value < .34 -> "低，轻微且有分寸"
        value < .67 -> "中，偶尔显露"
        else -> "高，需要明确情境才表达"
    }
    private fun StringBuilder.line(label: String, value: String) {
        if (value.isNotBlank()) appendLine("$label：${value.trim().take(500)}")
    }
    private fun StringBuilder.list(label: String, values: List<String>) =
        line(label, values.filter { it.isNotBlank() }.take(8).joinToString("；") { it.take(160) })
}
