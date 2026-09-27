package com.example.data.ai.runtime

import com.example.data.ai.model.ProviderProfile
import com.example.data.ai.provider.AiProvider
import com.example.data.ai.provider.OpenAiCompatibleProvider
import com.example.data.ai.repository.ProviderRepository

/**
 * ResolvedProvider — the outcome of a successful [ProviderResolver.resolve]
 * (P3C-4 §3).
 *
 * [provider] is constructed with the plaintext API key at call time. The key
 * itself must NEVER enter UI state, chat DB rows, prompt-debug output, or
 * Logcat — only this already-constructed object carries it internally.
 */
data class ResolvedProvider(
    val profileId: String,
    val model: String,
    val provider: AiProvider,
)

/**
 * ProviderResolver — a very narrow seam over provider configuration
 * (P3C-4 §3): active profile → resolve API key → construct AiProvider.
 *
 * Returns `null` when no active profile exists or the API key cannot be
 * resolved — callers must surface "请先配置 AI 连接" and write NO user turn.
 */
fun interface ProviderResolver {
    fun resolve(): ResolvedProvider?
}

/**
 * Production resolver backed by [ProviderRepository] (P3C-4 §3).
 * Single place where `resolveApiKey()` plaintext meets the provider constructor.
 */
class ActiveProfileProviderResolver(
    private val providerRepository: ProviderRepository,
) : ProviderResolver {
    override fun resolve(): ResolvedProvider? {
        val profile = providerRepository.activeProfile() ?: return null
        if (profile.baseUrl.isBlank() || profile.model.isBlank()) return null
        val apiKey = providerRepository.resolveApiKey(profile.id)
        if (apiKey.isNullOrBlank()) return null
        return ResolvedProvider(
            profileId = profile.id,
            model = profile.model,
            provider = OpenAiCompatibleProvider(
                baseUrl = profile.baseUrl,
                apiKey = apiKey,
            ),
        )
    }
}
