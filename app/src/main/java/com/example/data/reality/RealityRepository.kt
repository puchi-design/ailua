package com.example.data.reality

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Local, on-demand Android signal adapter. Nothing is stored or uploaded except consent switches. */
object RealityRepository {
    val stepsPermission: String by lazy { HealthPermission.getReadPermission(StepsRecord::class) }
    val sleepPermission: String by lazy { HealthPermission.getReadPermission(SleepSessionRecord::class) }
    private var context: Context? = null
    private val mutex = Mutex()
    private val _settings = MutableStateFlow(RealitySettings())
    val settings = _settings.asStateFlow()
    private val _snapshot = MutableStateFlow<RealitySnapshot?>(null)
    val snapshot = _snapshot.asStateFlow()
    private var usageFetchedAt = 0L
    private var healthFetchedAt = 0L
    private var cachedUsage: Pair<Int, String?>? = null
    private var cachedHealth = HealthRead()

    fun init(appContext: Context) {
        context = appContext.applicationContext
        val prefs = appContext.getSharedPreferences("reality_bridge", Context.MODE_PRIVATE)
        _settings.value = RealitySettings(
            enabled = prefs.getBoolean("enabled", true),
            usageEnabled = prefs.getBoolean("usage_enabled", false),
            healthEnabled = prefs.getBoolean("health_enabled", false),
        )
    }

    fun setSettings(value: RealitySettings) {
        val ctx = context ?: return
        ctx.getSharedPreferences("reality_bridge", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", value.enabled)
            .putBoolean("usage_enabled", value.usageEnabled)
            .putBoolean("health_enabled", value.healthEnabled)
            .apply()
        _settings.value = value
        _snapshot.value = null
        usageFetchedAt = 0L
        healthFetchedAt = 0L
    }

    fun usageGranted(): Boolean {
        val ctx = context ?: return false
        return runCatching {
            val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun healthStatus(): Int = if (Build.VERSION.SDK_INT < 26) HealthConnectClient.SDK_UNAVAILABLE else context?.let { ctx ->
        runCatching { HealthConnectClient.getSdkStatus(ctx) }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)
    } ?: HealthConnectClient.SDK_UNAVAILABLE

    suspend fun refresh(force: Boolean = false): RealitySnapshot? = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext null
        mutex.withLock {
            val old = _snapshot.value
            val now = System.currentTimeMillis()
            if (!force && old != null && now - old.capturedAt < 3 * 60_000L) return@withLock old
            val settingsValue = _settings.value
            val usageGranted = usageGranted()
            val healthAvailable = healthStatus() == HealthConnectClient.SDK_AVAILABLE
            if (!settingsValue.enabled) {
                val empty = RealitySnapshot(now, usagePermissionGranted = usageGranted, healthAvailable = healthAvailable)
                _snapshot.value = empty
                return@withLock empty
            }
            val battery = runCatching { ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
            val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = if (status >= 0) status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL else null
            val interactive = runCatching { (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive }.getOrNull()
            val usage = if (settingsValue.usageEnabled && usageGranted) {
                if (force || now - usageFetchedAt >= 15 * 60_000L) {
                    cachedUsage = readUsage(ctx, now)
                    usageFetchedAt = now
                }
                cachedUsage
            } else null
            val health = if (settingsValue.healthEnabled && healthAvailable) {
                if (force || now - healthFetchedAt >= 45 * 60_000L) {
                    cachedHealth = readHealth(ctx)
                    healthFetchedAt = now
                }
                cachedHealth
            } else HealthRead()
            RealitySnapshot(
                capturedAt = now, batteryPercent = percent, charging = charging,
                screenInteractive = interactive, todayScreenTimeMinutes = usage?.first,
                recentAppCategory = usage?.second, todaySteps = health.steps,
                lastSleepMinutes = health.sleepMinutes, usagePermissionGranted = usageGranted,
                healthAvailable = healthAvailable, healthStepsGranted = health.stepsGranted,
                healthSleepGranted = health.sleepGranted,
            ).also { _snapshot.value = it }
        }
    }

    private fun readUsage(ctx: Context, now: Long): Pair<Int, String?>? = runCatching {
        val usage = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val stats = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now).orEmpty()
            .filter { it.lastTimeUsed >= start && it.packageName != ctx.packageName }
        if (stats.isEmpty()) return@runCatching null
        val total = stats.sumOf { it.totalTimeInForeground }.div(60_000).toInt()
        val top = stats.maxByOrNull { it.lastTimeUsed }?.packageName
        val category = top?.let { packageName ->
            val info = runCatching { ctx.packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
            when (info?.category) {
                ApplicationInfo.CATEGORY_GAME -> "游戏"
                ApplicationInfo.CATEGORY_AUDIO -> "音乐"
                ApplicationInfo.CATEGORY_VIDEO -> "视频"
                ApplicationInfo.CATEGORY_SOCIAL -> "社交"
                ApplicationInfo.CATEGORY_NEWS -> "阅读"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "工作"
                else -> "应用使用"
            }
        }
        total to category
    }.getOrNull()

    private data class HealthRead(val steps: Long? = null, val sleepMinutes: Int? = null, val stepsGranted: Boolean = false, val sleepGranted: Boolean = false)

    private suspend fun readHealth(ctx: Context): HealthRead = try {
        val client = HealthConnectClient.getOrCreate(ctx)
        val granted = client.permissionController.getGrantedPermissions()
        val stepsGranted = stepsPermission in granted
        val sleepGranted = sleepPermission in granted
        val now = Instant.now()
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val steps = if (stepsGranted) runCatching {
            client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(today, now)))[StepsRecord.COUNT_TOTAL]
        }.getOrNull() else null
        val sleep = if (sleepGranted) runCatching {
            client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(now.minus(Duration.ofDays(2)), now), pageSize = 20))
                .records.maxByOrNull { it.endTime }?.let { Duration.between(it.startTime, it.endTime).toMinutes().toInt() }
        }.getOrNull() else null
        HealthRead(steps, sleep, stepsGranted, sleepGranted)
    } catch (_: Exception) { HealthRead() }
}
