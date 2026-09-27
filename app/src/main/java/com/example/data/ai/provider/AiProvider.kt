package com.example.data.ai.provider

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiStreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * P3C-1 provider contract.
 *
 * Request-scoped cancellation only (correction #1): there is deliberately NO
 * global `cancel()`. Cancellation follows the collector's coroutine:
 *
 *   generation Job.cancel() -> Flow cancellation -> HttpStreamCall.cancel()
 *
 * so concurrent sessions (two characters, a connection test, a background task)
 * can never kill each other's requests.
 */
interface AiProvider {
    fun streamChat(request: AiChatRequest): Flow<AiStreamEvent>
}
