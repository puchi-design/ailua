package com.example.ui.home.edit

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** Appearance-only settings. Grid coordinates and widget spans never change. */
data class HomeDisplayPreferences(
    val showLabels: Boolean = true,
    val iconScale: Float = 1f,
    val showPageIndicator: Boolean = true,
)

object HomeDisplayPreferencesStore {
    private const val PREFS_NAME = "ailua_home_display"
    private const val KEY_LABELS = "show_labels"
    private const val KEY_ICON_SCALE = "icon_scale"
    private const val KEY_PAGE_INDICATOR = "show_page_indicator"

    private var preferences: SharedPreferences? = null

    var current by mutableStateOf(HomeDisplayPreferences())
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences = prefs
        current = read(prefs)
    }

    @Synchronized
    fun update(value: HomeDisplayPreferences) {
        val normalized = value.copy(iconScale = nearestIconScale(value.iconScale))
        current = normalized
        preferences?.edit()
            ?.putBoolean(KEY_LABELS, normalized.showLabels)
            ?.putFloat(KEY_ICON_SCALE, normalized.iconScale)
            ?.putBoolean(KEY_PAGE_INDICATOR, normalized.showPageIndicator)
            ?.apply()
    }

    internal fun read(prefs: SharedPreferences): HomeDisplayPreferences = HomeDisplayPreferences(
        showLabels = prefs.getBoolean(KEY_LABELS, true),
        iconScale = nearestIconScale(prefs.getFloat(KEY_ICON_SCALE, 1f)),
        showPageIndicator = prefs.getBoolean(KEY_PAGE_INDICATOR, true),
    )

    private fun nearestIconScale(value: Float): Float =
        listOf(0.9f, 1f, 1.1f).minByOrNull { abs(value - it) } ?: 1f
}
