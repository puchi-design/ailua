package com.example.ui.living

import com.example.data.model.TimelineEvent

internal data class LivingTimelineGroup(
    val label: String,
    val events: List<TimelineEvent>
)

internal fun parseClockMinutes(time: String): Int? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

internal fun livingTimeLabel(time: String, nowMinutes: Int, isCurrent: Boolean): String {
    if (isCurrent) return "现在"
    val minutes = parseClockMinutes(time) ?: return "今天"
    val diff = nowMinutes - minutes
    if (diff in 0..60) return "刚刚"
    return when {
        minutes < 5 * 60 -> "深夜"
        minutes < 12 * 60 -> "上午"
        minutes < 18 * 60 -> "下午"
        else -> "晚上"
    }
}

internal fun groupLivingTimeline(
    events: List<TimelineEvent>,
    nowMinutes: Int
): List<LivingTimelineGroup> {
    val sorted = events.sortedWith(
        compareByDescending<TimelineEvent> { it.isCurrent }
            .thenByDescending { parseClockMinutes(it.time) ?: -1 }
    )
    val groups = mutableListOf<LivingTimelineGroup>()
    sorted.forEach { event ->
        val label = livingTimeLabel(event.time, nowMinutes, event.isCurrent)
        val last = groups.lastOrNull()
        if (last != null && last.label == label) {
            groups[groups.lastIndex] = last.copy(events = last.events + event)
        } else {
            groups += LivingTimelineGroup(label, listOf(event))
        }
    }
    return groups
}
