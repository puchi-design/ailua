package com.example.data.chat.local.platform

import com.example.data.chat.repository.EpochClock

/**
 * Production JVM/Android clock (P3C-3.1 §5).
 *
 * `System.currentTimeMillis()` is NOT commonMain-ready — only the `EpochClock`
 * interface lives in the KMP-safe `repository` package; swap this class at the
 * KMP migration.
 */
class SystemEpochClock : EpochClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
