package com.example.data.ai.repository

import android.content.Context

/**
 * ProviderGraph — process-wide holder for the single [ProviderRepository]
 * instance (P3C-4 §4).
 *
 * Mirrors the project's `AiluaLocalStore.init(context)` pattern: MainActivity
 * calls [init] once, every screen (chat runtime resolver, AI connection
 * sheet) shares the same StateFlows — so a profile saved in the sheet is
 * immediately visible to the runtime without a reload.
 */
object ProviderGraph {

    @Volatile
    private var instance: ProviderRepository? = null

    fun init(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance = ProviderRepository.create(context.applicationContext)
                }
            }
        }
    }

    val repository: ProviderRepository
        get() = instance
            ?: error("ProviderGraph.init(context) must be called from MainActivity.onCreate before use")
}
