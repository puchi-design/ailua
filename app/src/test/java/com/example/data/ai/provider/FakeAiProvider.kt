package com.example.data.ai.provider

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiStreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** One step of a [FakeAiProvider] script. */
sealed interface FakeAiStep {
    /** Emits `Delta(text)` and appends it to the accumulated reply. */
    data class Emit(val text: String) : FakeAiStep

    /** Emits `Completed` with everything emitted so far. */
    data object Complete : FakeAiStep

    /** Emits `Failed(error)` and stops — models real HTTP/protocol failures. */
    data class Fail(val error: AiProviderError) : FakeAiStep

    /** Emits the terminal `Cancelled` event and stops. */
    data object Cancel : FakeAiStep

    /** Virtual delay — lets cancellation tests pause mid-stream. */
    data class Pause(val millis: Long) : FakeAiStep
}

/**
 * Deterministic scripted provider for P3C-2..P3C-6 tests — no real API key needed.
 *
 * HARD RULE (spec §11 #4): [FakeAiProvider] is TEST-ONLY. It must never be
 * instantiated by production code, and never used as a fallback when a real
 * provider fails — real failures surface as `Failed(...)` on the real provider.
 */
class FakeAiProvider(private val script: List<FakeAiStep>) : AiProvider {

    private val recorded = mutableListOf<AiChatRequest>()

    /** Requests this fake received — for assertions only, never carries secrets. */
    val requests: List<AiChatRequest>
        get() = recorded.toList()

    override fun streamChat(request: AiChatRequest): Flow<AiStreamEvent> = flow {
        recorded += request
        emit(AiStreamEvent.Started)
        val accumulated = StringBuilder()
        for (step in script) {
            when (step) {
                is FakeAiStep.Emit -> {
                    accumulated.append(step.text)
                    emit(AiStreamEvent.Delta(step.text))
                }
                FakeAiStep.Complete -> {
                    emit(AiStreamEvent.Completed(accumulated.toString()))
                    return@flow
                }
                is FakeAiStep.Fail -> {
                    emit(AiStreamEvent.Failed(step.error))
                    return@flow
                }
                FakeAiStep.Cancel -> {
                    emit(AiStreamEvent.Cancelled)
                    return@flow
                }
                is FakeAiStep.Pause -> delay(step.millis)
            }
        }
        emit(AiStreamEvent.Completed(accumulated.toString()))
    }

    companion object {
        /** `scripted("你", "好", "呀")` -> Started, Delta(你), Delta(好), Delta(呀), Completed("你好呀"). */
        fun scripted(vararg deltas: String): FakeAiProvider = FakeAiProvider(
            deltas.map { FakeAiStep.Emit(it) } + FakeAiStep.Complete,
        )

        fun httpError(code: Int, message: String?): FakeAiProvider =
            FakeAiProvider(listOf(FakeAiStep.Fail(AiProviderError.Http(code, message))))

        fun unauthorized(): FakeAiProvider =
            FakeAiProvider(listOf(FakeAiStep.Fail(AiProviderError.Unauthorized)))

        fun timeout(): FakeAiProvider =
            FakeAiProvider(listOf(FakeAiStep.Fail(AiProviderError.Timeout)))

        fun protocolError(reason: String): FakeAiProvider =
            FakeAiProvider(listOf(FakeAiStep.Fail(AiProviderError.Protocol(reason))))

        /** Started, n deltas, then terminal `Cancelled` (models a call cut mid-stream). */
        fun cancelAfter(chunks: Int): FakeAiProvider = FakeAiProvider(
            (0 until chunks).map { FakeAiStep.Emit("chunk$it") } + FakeAiStep.Cancel,
        )

        /** Model returned nothing: Started, Completed(""). */
        fun emptyCompletion(): FakeAiProvider = FakeAiProvider(listOf(FakeAiStep.Complete))

        /** Stays mid-stream for [millis] before emitting [deltas] — for collector-cancel tests. */
        fun pause(millis: Long, vararg deltas: String): FakeAiProvider = FakeAiProvider(
            listOf(FakeAiStep.Pause(millis)) + deltas.map { FakeAiStep.Emit(it) } + FakeAiStep.Complete,
        )
    }
}
