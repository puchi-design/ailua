package com.example.data.ai.model

/**
 * Typed provider failures (P3C-1 correction #5).
 *
 * Every failure that leaves the provider is one of these — callers never parse
 * "401" / "timeout" / "cancelled" out of free-form strings. The HTTP/protocol
 * failure path MUST end here; it must never degrade into a canned fake reply.
 */
sealed interface AiProviderError {
    /** Non-2xx HTTP response (401 is mapped to [Unauthorized], not here). */
    data class Http(val code: Int, val message: String?) : AiProviderError

    /** Socket / connection level failure. */
    data class Network(val message: String?) : AiProviderError

    /** HTTP 401 — bad or missing API key. */
    data object Unauthorized : AiProviderError

    /** Read / call timeout. */
    data object Timeout : AiProviderError

    /** Request was cancelled. */
    data object Cancelled : AiProviderError

    /** The response violated the expected protocol (bad SSE, malformed JSON, empty completion…). */
    data class Protocol(val message: String) : AiProviderError
}
