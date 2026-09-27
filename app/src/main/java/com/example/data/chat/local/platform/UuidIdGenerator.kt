package com.example.data.chat.local.platform

import com.example.data.chat.repository.IdGenerator

/**
 * JVM/Android id source for chat entities (P3C-3.1 §5).
 *
 * `java.util.UUID` is NOT commonMain-ready — only the `IdGenerator` interface
 * lives in the KMP-safe `repository` package; swap this class at the KMP
 * migration.
 */
class UuidIdGenerator : IdGenerator {
    override fun newId(): String = java.util.UUID.randomUUID().toString()
}
