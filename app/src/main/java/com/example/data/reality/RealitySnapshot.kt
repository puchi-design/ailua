package com.example.data.reality

/** Nullable values mean unavailable or not authorized, never zero. */
data class RealitySnapshot(
    val capturedAt: Long,
    val batteryPercent: Int? = null,
    val charging: Boolean? = null,
    val screenInteractive: Boolean? = null,
    val todayScreenTimeMinutes: Int? = null,
    val recentAppCategory: String? = null,
    val todaySteps: Long? = null,
    val lastSleepMinutes: Int? = null,
    val usagePermissionGranted: Boolean = false,
    val healthAvailable: Boolean = false,
    val healthStepsGranted: Boolean = false,
    val healthSleepGranted: Boolean = false,
)

data class RealitySettings(
    val enabled: Boolean = true,
    val usageEnabled: Boolean = false,
    val healthEnabled: Boolean = false,
)

/** Human-readable, short and deliberately selective. No package names or raw records enter AI prompts. */
object RealityContextPolicy {
    fun context(snapshot: RealitySnapshot?, settings: RealitySettings): String? {
        if (!settings.enabled || snapshot == null) return null
        val lines = buildList {
            if (snapshot.batteryPercent != null && snapshot.batteryPercent < 20 && snapshot.charging == false)
                add("手机电量较低（${snapshot.batteryPercent}%），尚未充电")
            if (settings.usageEnabled && snapshot.todayScreenTimeMinutes != null && snapshot.todayScreenTimeMinutes >= 240)
                add("今天使用手机约 ${snapshot.todayScreenTimeMinutes / 60} 小时")
            if (settings.healthEnabled && snapshot.todaySteps != null && snapshot.todaySteps >= 6000)
                add("今天走了约 ${(snapshot.todaySteps / 1000) * 1000} 步")
        }
        return if (lines.isEmpty()) null else "现实环境（低敏摘要，仅在自然相关时提及，不要重复提醒）：\n" + lines.joinToString("\n") { "- $it" }
    }
}
