package com.example

import com.example.data.ai.provider.OpenAiCompatibleProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ProviderUrlNormalizationTest — P3C-1 base URL contract.
 *
 * Wire URL must always be exactly `{normalizedBaseUrl}/chat/completions`,
 * no matter how the user typed the base URL.
 */
class ProviderUrlNormalizationTest {

    private fun normalize(raw: String) = OpenAiCompatibleProvider.normalizeBaseUrl(raw)

    @Test
    fun trailingSlashStripped() {
        assertEquals("https://host/v1", normalize("https://host/v1/"))
    }

    @Test
    fun chatCompletionsSuffixStripped() {
        assertEquals("https://host/v1", normalize("https://host/v1/chat/completions"))
    }

    @Test
    fun rootSlashStripped() {
        assertEquals("https://host", normalize("https://host/"))
    }

    @Test
    fun multipleTrailingSlashesStripped() {
        assertEquals("https://host/v1", normalize("https://host/v1///"))
    }

    @Test
    fun chatCompletionsSuffixWithTrailingSlashStripped() {
        assertEquals("https://host/v1", normalize("https://host/v1/chat/completions/"))
    }

    @Test
    fun cleanUrlUnchanged() {
        assertEquals("https://host/v1", normalize("https://host/v1"))
    }

    @Test
    fun surroundingWhitespaceTrimmed() {
        assertEquals("https://host/v1", normalize("  https://host/v1/  "))
    }

    @Test
    fun normalizationIsIdempotent() {
        val once = normalize("https://host/v1///")
        assertEquals(once, normalize(once))
    }

    @Test
    fun blankInputStaysBlank() {
        assertEquals("", normalize("   "))
    }

    @Test
    fun chatCompletionsUrlUsesSingleCanonicalPath() {
        assertEquals(
            "https://host/v1/chat/completions",
            OpenAiCompatibleProvider.chatCompletionsUrl("https://host/v1/"),
        )
        assertEquals(
            "https://host/v1/chat/completions",
            OpenAiCompatibleProvider.chatCompletionsUrl("https://host/v1/chat/completions"),
        )
    }
}
