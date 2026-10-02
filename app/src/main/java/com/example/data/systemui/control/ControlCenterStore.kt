package com.example.data.systemui.control

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** These settings affect the virtual phone only; no Android system settings are changed. */
data class ControlCenterState(
    val focusMode: Boolean = false,
    val quietMode: Boolean = false,
    val virtualBrightness: Float = 1f,
) {
    val virtualDimAlpha: Float
        get() = (1f - normalizeBrightness(virtualBrightness)) * 0.5f
}

internal fun normalizeBrightness(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.5f, 1f) else 1f

object ControlCenterStore {
    internal const val PREFS_NAME = "ailua_control_center"
    private var preferences: SharedPreferences? = null
    private val mutableState = MutableStateFlow(ControlCenterState())
    val state: StateFlow<ControlCenterState> = mutableState.asStateFlow()

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences = prefs
        mutableState.value = read(prefs)
    }

    @Synchronized
    fun setFocusMode(enabled: Boolean) = update(state.value.copy(focusMode = enabled))

    @Synchronized
    fun setQuietMode(enabled: Boolean) = update(state.value.copy(quietMode = enabled))

    @Synchronized
    fun setVirtualBrightness(value: Float) = update(state.value.copy(virtualBrightness = value))

    private fun update(value: ControlCenterState) {
        val normalized = value.copy(virtualBrightness = normalizeBrightness(value.virtualBrightness))
        mutableState.value = normalized
        preferences?.edit()
            ?.putBoolean("focus_mode", normalized.focusMode)
            ?.putBoolean("quiet_mode", normalized.quietMode)
            ?.putFloat("virtual_brightness", normalized.virtualBrightness)
            ?.apply()
    }

    internal fun read(prefs: SharedPreferences): ControlCenterState = ControlCenterState(
        focusMode = prefs.getBoolean("focus_mode", false),
        quietMode = prefs.getBoolean("quiet_mode", false),
        virtualBrightness = normalizeBrightness(prefs.getFloat("virtual_brightness", 1f)),
    )
}
