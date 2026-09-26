package com.example.data.engine

/**
 * Result data class when advancing virtual time in AILUA.
 */
data class TimeAdvanceResult(
    val newMinutes: Int,
    val newDateLabel: String,
    val daysAdvanced: Int
)

/**
 * WorldTimeAdvancer
 *
 * Deterministic helper for virtual time calculation and interval action evaluation.
 * Handles same-day progression and midnight transitions with date advancement.
 */
object WorldTimeAdvancer {

    /**
     * Advances current minutes and date label by [deltaMinutes].
     *
     * Example:
     * 23:50 (1430) + 30 min -> 00:20 (20), date: "9月25日" -> "9月26日", daysAdvanced = 1.
     */
    fun advance(
        currentMinutes: Int,
        currentDateLabel: String,
        deltaMinutes: Int
    ): TimeAdvanceResult {
        if (deltaMinutes <= 0) {
            return TimeAdvanceResult(currentMinutes, currentDateLabel, 0)
        }
        val totalMinutes = currentMinutes + deltaMinutes
        val daysAdvanced = totalMinutes / 1440
        val newMinutes = totalMinutes % 1440
        val newDate = if (daysAdvanced > 0) {
            advanceDateLabel(currentDateLabel, daysAdvanced)
        } else {
            currentDateLabel
        }
        return TimeAdvanceResult(
            newMinutes = newMinutes,
            newDateLabel = newDate,
            daysAdvanced = daysAdvanced
        )
    }

    /**
     * Advances Chinese date label format e.g. "9月25日" by [daysToAdd].
     */
    fun advanceDateLabel(dateLabel: String, daysToAdd: Int): String {
        val regex = Regex("""(\d+)月(\d+)日""")
        val match = regex.find(dateLabel)
        if (match != null) {
            var month = match.groupValues[1].toInt()
            var day = match.groupValues[2].toInt()
            day += daysToAdd
            while (true) {
                val maxDays = getDaysInMonth(month)
                if (day > maxDays) {
                    day -= maxDays
                    month += 1
                    if (month > 12) month = 1
                } else {
                    break
                }
            }
            return "${month}月${day}日"
        }
        return if (daysToAdd > 0) "$dateLabel (+${daysToAdd}天)" else dateLabel
    }

    private fun getDaysInMonth(month: Int): Int {
        return when (month) {
            2 -> 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    /**
     * Evaluates whether a scheduled action at [triggerMinutes] falls within
     * the interval traversed from [startMinutes] with [deltaMinutes].
     *
     * In same-day progress (daysAdvanced == 0):
     *   action triggers if triggerMinutes in (startMinutes..endMinutes]
     *
     * In midnight crossing (daysAdvanced >= 1):
     *   action triggers if:
     *   - occurred after startMinutes on day 1 (triggerMinutes > startMinutes), OR
     *   - intermediate days traversed (daysAdvanced > 1), OR
     *   - occurred up to endMinutes on final day (triggerMinutes <= endMinutes)
     */
    fun isActionTriggeredInInterval(
        triggerMinutes: Int,
        startMinutes: Int,
        deltaMinutes: Int
    ): Boolean {
        if (deltaMinutes <= 0) return false
        val totalMinutes = startMinutes + deltaMinutes
        val daysAdvanced = totalMinutes / 1440
        val endMinutes = totalMinutes % 1440

        return if (daysAdvanced == 0) {
            triggerMinutes in (startMinutes + 1)..endMinutes
        } else {
            if (daysAdvanced > 1) {
                true
            } else {
                triggerMinutes > startMinutes || triggerMinutes <= endMinutes
            }
        }
    }
}
