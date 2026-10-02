package com.example.data.systemui.lock

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** This preference controls only AILUA's virtual lockscreen, never Android Keyguard. */
object VirtualLockStore {
    private var preferences: SharedPreferences? = null
    private val mutable = MutableStateFlow(true)
    val lockOnColdStart = mutable.asStateFlow()

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences("ailua_virtual_lock", Context.MODE_PRIVATE)
        mutable.value = preferences!!.getBoolean("lock_on_cold_start", true)
    }

    fun setLockOnColdStart(value: Boolean) {
        preferences?.edit()?.putBoolean("lock_on_cold_start", value)?.apply()
        mutable.value = value
    }
}
