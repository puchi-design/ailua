package com.example.data.ai.model

import kotlinx.serialization.Serializable

/**
 * Persisted OpenAI-compatible provider profile.
 *
 * HARD RULE (spec §11 #6): the API key itself never appears in this model.
 * [apiKeyRef] is an opaque UUID handle into [com.example.data.ai.security.ApiSecretStore];
 * the plaintext key lives only in Keystore-encrypted storage and must never be
 * serialized here, logged, or exposed through a public StateFlow.
 */
@Serializable
data class ProviderProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val apiKeyRef: String? = null,
)
