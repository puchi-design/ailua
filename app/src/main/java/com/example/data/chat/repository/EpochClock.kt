package com.example.data.chat.repository

/**
 * EpochClock — the single time source for chat persistence (P3C-3 §9).
 *
 * Repository and runtime code must read time ONLY through this interface so
 * tests can freeze or advance it deterministically.
 *
 * The interface itself is commonMain-ready. The current JVM/Android
 * implementation (`SystemEpochClock` in `com.example.data.chat.local.platform`)
 * uses `System.currentTimeMillis()` and is NOT commonMain-ready — that one class
 * must be swapped at the KMP migration (P3C-3.1 §5).
 */
interface EpochClock {
    fun nowEpochMs(): Long
}
