package com.example.data.model

/**
 * LifeEventOrder
 *
 * Unified chronological ordering for LifeEvent records based on virtual world time.
 * - worldDateLabel format: "9月25日" (rank = month * 100 + day, blank/unknown = -1)
 * - worldMinutesOfDay preferred; falls back to parsing the "HH:MM" display time
 * - deterministic tiebreak by id so ordering is stable across reloads
 */

fun parseWorldDateRank(dateLabel: String): Int {
    if (dateLabel.isBlank()) return -1
    val monthEnd = dateLabel.indexOf('月')
    if (monthEnd <= 0) return -1
    val dayStart = monthEnd + 1
    val dayEnd = dateLabel.indexOf('日', dayStart)
    val month = dateLabel.substring(0, monthEnd).toIntOrNull() ?: return -1
    val day = if (dayEnd > dayStart) {
        dateLabel.substring(dayStart, dayEnd).toIntOrNull() ?: return -1
    } else {
        dateLabel.substring(dayStart).toIntOrNull() ?: return -1
    }
    if (month !in 1..12 || day !in 1..31) return -1
    return month * 100 + day
}

fun parseClockTimeToMinutes(time: String): Int? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

fun LifeEvent.virtualDateRank(): Int = parseWorldDateRank(worldDateLabel)

fun LifeEvent.virtualMinuteRank(): Int {
    if (worldMinutesOfDay in 0..1439) return worldMinutesOfDay
    return parseClockTimeToMinutes(time) ?: -1
}

val LifeEventChronological: Comparator<LifeEvent> =
    compareBy({ it.virtualDateRank() }, { it.virtualMinuteRank() }, { it.time }, { it.id })

fun List<LifeEvent>.sortedChronologically(): List<LifeEvent> = sortedWith(LifeEventChronological)
