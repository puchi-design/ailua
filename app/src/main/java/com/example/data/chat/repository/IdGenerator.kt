package com.example.data.chat.repository

/**
 * IdGenerator — stable id source for chat entities (P3C-3 §8).
 *
 * Ids must never be derived from the clock (no `System.currentTimeMillis().toString()`),
 * otherwise two writes in the same millisecond collide. Tests inject a
 * deterministic implementation.
 */
interface IdGenerator {
    fun newId(): String
}

/**
 * Current implementation: random UUID. `java.util` is intentionally kept — the
 * KMP migration only needs to swap this one class (P3C-3 §14).
 */
class UuidIdGenerator : IdGenerator {
    override fun newId(): String = java.util.UUID.randomUUID().toString()
}
