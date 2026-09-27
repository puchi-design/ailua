package com.example

import com.example.data.ai.model.AiUsage
import com.example.data.ai.provider.SseData
import com.example.data.ai.provider.SseDataDecoder
import com.example.data.ai.provider.SseEvent
import com.example.data.ai.provider.SseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SseParserTest — P3C-1 correction #3: pure Kotlin SSE state machine.
 *
 * Covers every framing case in the spec gate: partial chunks, several events in
 * one read, LF/CRLF (including a CR split across chunks), [DONE], blank data,
 * malformed JSON, EOF without [DONE] — none of which may crash the app.
 */
class SseParserTest {

    private fun parse(chunks: List<String>): List<SseEvent> {
        val parser = SseParser()
        val events = mutableListOf<SseEvent>()
        for (chunk in chunks) events += parser.feed(chunk)
        events += parser.flush()
        return events
    }

    @Test
    fun partialChunksReassembleIntoOneEvent() {
        val events = parse(
            listOf(
                "data: {\"cho",
                "ices\":[{\"del",
                "ta\":{\"content\":\"你",
                "好\"}}]}\n\n",
            ),
        )
        assertEquals(1, events.size)
        assertEquals("""{"choices":[{"delta":{"content":"你好"}}]}""", events[0].data)
    }

    @Test
    fun twoEventsInOneRead() {
        val events = parse(listOf("data: A\n\ndata: B\n\n"))
        assertEquals(listOf("A", "B"), events.map { it.data })
    }

    @Test
    fun crlfSeparators() {
        assertEquals(listOf("X"), parse(listOf("data: X\r\n\r\n")).map { it.data })
    }

    @Test
    fun crSplitAcrossChunks() {
        assertEquals(listOf("X"), parse(listOf("data: X\r", "\n", "\r", "\n")).map { it.data })
    }

    @Test
    fun blankDataFieldYieldsBlankPayload() {
        assertEquals(listOf(""), parse(listOf("data:\n\n")).map { it.data })
    }

    @Test
    fun pureBlankEventEmitsNothing() {
        assertEquals(emptyList<SseEvent>(), parse(listOf("\n\n")))
    }

    @Test
    fun commentLinesIgnored() {
        assertEquals(emptyList<SseEvent>(), parse(listOf(": ping\n", ": ping\n\n")))
    }

    @Test
    fun commentBeforeDataStillDeliversData() {
        assertEquals(listOf("hi"), parse(listOf(": ping\ndata: hi\n\n")).map { it.data })
    }

    @Test
    fun eofWithoutDoneFlushesPendingEvent() {
        val parser = SseParser()
        val streamed = parser.feed("""data: {"choices":[{"delta":{"content":"hi"}}]}""" + "\n")
        assertEquals(0, streamed.size)
        assertEquals(1, parser.flush().size)
    }

    @Test
    fun fieldWithoutColonIsEmptyValue() {
        assertEquals(listOf(""), parse(listOf("data\n\n")).map { it.data })
    }

    @Test
    fun onlyOneLeadingSpaceIsStripped() {
        assertEquals(listOf(" hi"), parse(listOf("data:  hi\n\n")).map { it.data })
    }

    @Test
    fun multipleDataLinesJoinWithNewline() {
        assertEquals(listOf("a\nb"), parse(listOf("data: a\ndata: b\n\n")).map { it.data })
    }

    @Test
    fun feedIsStatelessAcrossArbitraryChunkBoundaries() {
        val canonical = listOf("data: A\n\ndata: B\n\n")
        val awkward = listOf("d", "at", "a: A\n", "\nda", "ta: B", "\n\n")
        assertEquals(parse(canonical), parse(awkward))
    }

    // ── SseDataDecoder ────────────────────────────────────────────────

    @Test
    fun doneMarkerDecoded() {
        assertEquals(SseData.Done, SseDataDecoder.decode(" [DONE] "))
    }

    @Test
    fun blankPayloadDecoded() {
        assertEquals(SseData.Blank, SseDataDecoder.decode("   "))
    }

    @Test
    fun malformedJsonIsMalformedAndDoesNotCrash() {
        assertTrue(SseDataDecoder.decode("{not json") is SseData.Malformed)
        assertTrue(SseDataDecoder.decode("[1,2") is SseData.Malformed)
    }

    @Test
    fun nonObjectPayloadIsMalformed() {
        assertTrue(SseDataDecoder.decode("12345") is SseData.Malformed)
    }

    @Test
    fun errorObjectIsMalformed() {
        val result = SseDataDecoder.decode("""{"error":{"message":"overloaded"}}""")
        assertTrue(result is SseData.Malformed)
        assertTrue((result as SseData.Malformed).reason.contains("provider error"))
    }

    @Test
    fun errorNullFieldIsNotAnError() {
        val result = SseDataDecoder.decode("""{"error":null,"choices":[]}""")
        assertTrue(result is SseData.Chunk)
    }

    @Test
    fun deltaContentExtracted() {
        val result = SseDataDecoder.decode("""{"choices":[{"delta":{"content":"你好"}}]}""")
        assertEquals("你好", (result as SseData.Chunk).content)
        assertNull(result.usage)
    }

    @Test
    fun messageContentExtractedForNonStreamResponse() {
        val result = SseDataDecoder.decode("""{"choices":[{"message":{"content":"单发"}}]}""")
        assertEquals("单发", (result as SseData.Chunk).content)
    }

    @Test
    fun missingContentIsNullNotAnError() {
        val result = SseDataDecoder.decode("""{"choices":[{"delta":{}}]}""")
        assertNull((result as SseData.Chunk).content)
    }

    @Test
    fun usageExtracted() {
        val result = SseDataDecoder.decode(
            """{"choices":[{"delta":{}}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}""",
        )
        assertEquals(AiUsage(3, 2, 5), (result as SseData.Chunk).usage)
    }

    @Test
    fun unsupportedContentShapeIsMalformed() {
        val result = SseDataDecoder.decode("""{"choices":[{"delta":{"content":[{"type":"text"}]}}]}""")
        assertTrue(result is SseData.Malformed)
    }
}
