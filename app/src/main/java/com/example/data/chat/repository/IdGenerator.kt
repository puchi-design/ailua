package com.example.data.chat.repository

/**
 * IdGenerator — stable id source for chat entities (P3C-3 §8).
 *
 * Ids must never be derived from the clock (no `System.currentTimeMillis().toString()`),
 * otherwise two writes in the same millisecond collide. Tests inject a
 * deterministic implementation.
 *
 * The interface itself is commonMain-ready. The current JVM/Android
 * implementation (`UuidIdGenerator` in `com.example.data.chat.local.platform`)
 * uses `java.util.UUID` and is NOT commonMain-ready — that one class must be
 * swapped at the KMP migration (P3C-3.1 §5).
 */
interface IdGenerator {
    fun newId(): String
}
