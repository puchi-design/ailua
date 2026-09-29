package com.example.data.ai.provider

import com.example.data.ai.model.AiChatRequest
import com.example.data.ai.model.AiProviderError
import com.example.data.ai.model.AiStreamEvent
import com.example.data.ai.model.AiUsage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * One in-flight streaming HTTP call: status line first, then line-by-line body reads.
 * Blocking — must be [cancel]led to unblock a reader, which is exactly what flow
 * cancellation does (correction #1).
 */
interface HttpStreamCall {
    /** Executes the request and returns the HTTP status code. */
    fun execute(): Int

    /** Next body line without its terminator, or null at EOF. Blocks. */
    fun readLine(): String?

    fun cancel()

    fun close()
}

fun interface HttpStreamCallFactory {
    fun newCall(url: String, headers: Map<String, String>, body: String): HttpStreamCall
}

/**
 * OpenAI-compatible `/chat/completions` streaming provider (P3C-1).
 *
 * Boundaries (spec §11):
 * - provider NEVER assembles prompts — it only ships the [AiChatRequest] it is given
 * - typed [AiProviderError] only; HTTP/protocol failure emits Failed and stops.
 *   There is NO canned-reply fallback anywhere in this file.
 * - no HttpLoggingInterceptor is attached: Authorization never reaches a log
 *   (correction #4). [redact] additionally scrubs the key out of any provider
 *   message we propagate, so it can never surface in an exception or log rendering.
 *
 * Base URL contract (correction / Warm Tavern MIT behavior, clean-room applied):
 * trailing slashes and an accidental `/chat/completions` suffix are stripped so the
 * wire URL is always `{normalizedBaseUrl}/chat/completions`.
 */
class OpenAiCompatibleProvider(
    private val baseUrl: String,
    private val apiKey: String?,
    private val callFactory: HttpStreamCallFactory = OkHttpStreamCallFactory(),
) : AiProvider {

    private val normalizedBaseUrl = normalizeBaseUrl(baseUrl)

    override fun streamChat(request: AiChatRequest): Flow<AiStreamEvent> = flow {
        if (!normalizedBaseUrl.startsWith("http://") && !normalizedBaseUrl.startsWith("https://")) {
            emit(AiStreamEvent.Failed(AiProviderError.Protocol(redact("invalid base url: $normalizedBaseUrl"))))
            return@flow
        }

        coroutineScope {
            val call = try {
                callFactory.newCall(
                    "$normalizedBaseUrl$CHAT_COMPLETIONS_PATH",
                    requestHeaders(),
                    buildRequestBody(request),
                )
            } catch (t: Throwable) {
                emitFailure(t)
                return@coroutineScope
            }

            // Request-scoped cancellation (correction #1): the instant this coroutine
            // is cancelled the watcher aborts the call, which unblocks a pending
            // readLine()/execute(). No global cancel() exists anywhere.
            val cancellationWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    call.cancel()
                }
            }

            try {
                val code = try {
                    call.execute()
                } catch (t: Throwable) {
                    emitFailure(t)
                    return@coroutineScope
                }

                if (code == UNAUTHORIZED_CODE) {
                    emit(AiStreamEvent.Failed(AiProviderError.Unauthorized))
                    return@coroutineScope
                }
                if (code !in 200..299) {
                    val message = readErrorBody(call)
                    emit(AiStreamEvent.Failed(AiProviderError.Http(code, redact(message))))
                    return@coroutineScope
                }

                emit(AiStreamEvent.Started)
                val accumulator = StreamAccumulator()

                if (!request.stream) {
                    val full = try {
                        readAllText(call)
                    } catch (t: Throwable) {
                        emitFailure(t)
                        return@coroutineScope
                    }
                    applyDecoded(SseDataDecoder.decode(full), accumulator)
                    finishStream(accumulator)
                    return@coroutineScope
                }

                val parser = SseParser()
                try {
                    while (true) {
                        val line = call.readLine() ?: break
                        for (event in parser.feed(line + "\n")) {
                            applyDecoded(SseDataDecoder.decode(event.data), accumulator)
                        }
                    }
                    for (event in parser.flush()) {
                        applyDecoded(SseDataDecoder.decode(event.data), accumulator)
                    }
                } catch (t: Throwable) {
                    emitFailure(t)
                    return@coroutineScope
                }
                finishStream(accumulator)
            } finally {
                cancellationWatcher.cancel()
                call.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Classifies a transport failure into a terminal event.
     * A cancelled collector wins over every error mapping — cancellation is never
     * reported as Failed, and no fabricated reply is ever produced.
     */
    private suspend fun FlowCollector<AiStreamEvent>.emitFailure(t: Throwable) {
        coroutineContext.ensureActive()
        if (isExternalCancelMessage(t)) {
            emit(AiStreamEvent.Cancelled)
            return
        }
        emit(AiStreamEvent.Failed(classify(t)))
    }

    private suspend fun FlowCollector<AiStreamEvent>.applyDecoded(
        decoded: SseData,
        accumulator: StreamAccumulator,
    ) {
        when (decoded) {
            is SseData.Malformed -> {
                if (accumulator.protocolError == null) accumulator.protocolError = decoded.reason
            }
            is SseData.Chunk -> {
                decoded.usage?.let { accumulator.usage = it }
                val text = decoded.content
                if (!text.isNullOrEmpty()) {
                    accumulator.text.append(text)
                    emit(AiStreamEvent.Delta(text))
                }
            }
            SseData.Blank, SseData.Done -> Unit
        }
    }

    /** Terminal rule: protocol error > empty completion > usage + Completed. */
    private suspend fun FlowCollector<AiStreamEvent>.finishStream(accumulator: StreamAccumulator) {
        val protocolError = accumulator.protocolError
        if (protocolError != null) {
            emit(AiStreamEvent.Failed(AiProviderError.Protocol(redact(protocolError))))
            return
        }
        if (accumulator.text.isEmpty()) {
            emit(AiStreamEvent.Failed(AiProviderError.Protocol("empty completion")))
            return
        }
        accumulator.usage?.let { emit(AiStreamEvent.Usage(it)) }
        emit(AiStreamEvent.Completed(accumulator.text.toString()))
    }

    private fun classify(t: Throwable): AiProviderError = when {
        t is InterruptedIOException -> AiProviderError.Timeout
        t is IllegalArgumentException ->
            AiProviderError.Protocol(redact(t.message ?: "invalid request"))
        t is IOException -> AiProviderError.Network(redactOrNull(t.message))
        else -> AiProviderError.Network(redactOrNull(t.message))
    }

    private fun isExternalCancelMessage(t: Throwable): Boolean =
        t is IOException && (
            t.message?.contains("canceled", ignoreCase = true) == true ||
                t.message?.contains("cancelled", ignoreCase = true) == true
            )

    private fun requestHeaders(): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        headers["Content-Type"] = "application/json"
        headers["Accept"] = "text/event-stream"
        if (!apiKey.isNullOrBlank()) headers["Authorization"] = "Bearer $apiKey"
        return headers
    }

    private fun buildRequestBody(request: AiChatRequest): String {
        val messages = JsonArray(
            request.messages.map { message ->
                buildJsonObject {
                    put("role", message.role.name.lowercase())
                    put("content", message.content)
                }
            },
        )
        return buildJsonObject {
            put("model", request.model)
            put("messages", messages)
            put("stream", request.stream)
            request.temperature?.let { put("temperature", it) }
              request.maxTokens?.let { put("max_tokens", it) }
              if (request.jsonResponse) put("response_format", buildJsonObject { put("type", "json_object") })
        }.toString()
    }

    private fun readErrorBody(call: HttpStreamCall, cap: Int = ERROR_BODY_CAP): String {
        val builder = StringBuilder()
        try {
            while (builder.length < cap) {
                val line = call.readLine() ?: break
                if (builder.isNotEmpty()) builder.append(' ')
                builder.append(line)
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
        }
        return builder.toString().take(cap)
    }

    private fun readAllText(call: HttpStreamCall): String {
        val builder = StringBuilder()
        while (true) {
            val line = call.readLine() ?: break
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(line)
        }
        return builder.toString()
    }

    /** Replaces any occurrence of the API key in provider-facing text with a mask. */
    private fun redact(text: String): String {
        val key = apiKey
        return if (key.isNullOrBlank()) text else text.replace(key, MASK)
    }

    private fun redactOrNull(text: String?): String? = text?.let { redact(it) }

    private class StreamAccumulator {
        val text = StringBuilder()
        var protocolError: String? = null
        var usage: AiUsage? = null
    }

    companion object {
        const val CHAT_COMPLETIONS_PATH = "/chat/completions"
        const val MASK = "••••"
        private const val UNAUTHORIZED_CODE = 401
        private const val ERROR_BODY_CAP = 512

        /**
         * `https://host/v1/` -> `https://host/v1`
         * `https://host/v1/chat/completions` -> `https://host/v1`
         * `https://host/` -> `https://host`
         * `https://host/v1///` -> `https://host/v1`
         */
        fun normalizeBaseUrl(raw: String): String {
            var normalized = raw.trim().trimEnd('/')
            if (normalized.endsWith(CHAT_COMPLETIONS_PATH)) {
                normalized = normalized.removeSuffix(CHAT_COMPLETIONS_PATH).trimEnd('/')
            }
            return normalized
        }

        fun chatCompletionsUrl(raw: String): String = normalizeBaseUrl(raw) + CHAT_COMPLETIONS_PATH
    }
}

/**
 * Production [HttpStreamCallFactory]: OkHttp, blocking line reads.
 *
 * Deliberately NO HttpLoggingInterceptor on this path (correction #4): request
 * headers — Authorization above all — are never logged in any build type.
 */
class OkHttpStreamCallFactory(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) : HttpStreamCallFactory {

    override fun newCall(url: String, headers: Map<String, String>, body: String): HttpStreamCall {
        val builder = Request.Builder().url(url)
        headers.forEach { (name, value) -> builder.header(name, value) }
        builder.post(body.toRequestBody(JSON_MEDIA_TYPE))
        return OkHttpStreamCall(client.newCall(builder.build()))
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val WRITE_TIMEOUT_SECONDS = 30L
    }
}

private class OkHttpStreamCall(private val call: Call) : HttpStreamCall {
    private var response: Response? = null
    private var source: BufferedSource? = null

    override fun execute(): Int {
        val executed = call.execute()
        response = executed
        source = executed.body?.source()
        return executed.code
    }

    override fun readLine(): String? = source?.readUtf8Line()

    override fun cancel() {
        call.cancel()
    }

    override fun close() {
        try {
            response?.close()
        } catch (_: Exception) {
            // closing is best-effort
        }
        response = null
        source = null
    }
}
