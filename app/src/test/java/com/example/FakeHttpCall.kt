package com.example

import com.example.data.ai.provider.HttpStreamCall
import com.example.data.ai.provider.HttpStreamCallFactory
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Scripted [HttpStreamCall] for P3C-1 provider tests — no real socket involved.
 *
 * - [code]: HTTP status returned by [execute]
 * - [lines]: body lines returned by [readLine] until EOF (null)
 * - [executeError]: thrown from [execute] (e.g. SocketTimeoutException / IOException)
 * - [lineErrors]: index -> throwable thrown when that line would be read
 * - [pauseAt]: blocks at that line index until [cancel] is called, then throws
 *   "Canceled" — the shape a collector-cancelled request has in production
 */
class FakeHttpCall(
    private val code: Int = 200,
    private val lines: List<String> = emptyList(),
    private val executeError: Throwable? = null,
    private val lineErrors: Map<Int, Throwable> = emptyMap(),
    val pauseAt: Int? = null,
) : HttpStreamCall {

    @Volatile
    var cancelled = false
        private set

    @Volatile
    var closed = false
        private set

    @Volatile
    var executed = false
        private set

    private var index = 0
    private val pauseLatch = CountDownLatch(1)

    override fun execute(): Int {
        executed = true
        executeError?.let { throw it }
        return code
    }

    override fun readLine(): String? {
        val position = index
        if (pauseAt == position) {
            pauseLatch.await(10, TimeUnit.SECONDS)
            throw IOException("Canceled")
        }
        lineErrors[position]?.let { throw it }
        index = position + 1
        return lines.getOrNull(position)
    }

    override fun cancel() {
        cancelled = true
        pauseLatch.countDown()
    }

    override fun close() {
        closed = true
    }
}

/** Captures the last request so tests can assert URL / headers / body. */
class FakeHttpCallFactory(private val call: FakeHttpCall) : HttpStreamCallFactory {

    var lastUrl: String? = null
        private set

    var lastHeaders: Map<String, String> = emptyMap()
        private set

    var lastBody: String? = null
        private set

    override fun newCall(url: String, headers: Map<String, String>, body: String): HttpStreamCall {
        lastUrl = url
        lastHeaders = headers
        lastBody = body
        return call
    }
}
