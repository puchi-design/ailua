package com.example.data.chat.repository

/**
 * EpochClock — the single time source for chat persistence (P3C-3 §9).
 *
 * Repository and runtime code must read time ONLY through this interface so
 * tests can freeze or advance it deterministically.
 */
interface EpochClock {
    fun nowEpochMs(): Long
}

/**
 * Production clock. This class is the only place in the chat runtime allowed
 * to call `System.currentTimeMillis()` (P3C-3 §9).
 */
class SystemEpochClock : EpochClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
