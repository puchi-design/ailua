package com.example.data.ai.repository

import android.content.Context
import com.example.data.ai.model.ProviderProfile
import com.example.data.ai.security.ApiSecretStore
import com.example.data.ai.security.KeyValueStore
import com.example.data.ai.security.SharedPreferencesKeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * P3C-1 provider profile repository — and ONLY that (spec: no prompt / chat /
 * memory / world responsibilities).
 *
 * Owns:
 * - profile list + activeProfileId (both exposed as StateFlow — they contain
 *   apiKeyRef handles only, never key material)
 * - save / update / delete
 * - API key resolution via [ApiSecretStore] and the masked `••••••••abcd` label
 *
 * [resolveApiKey] exists for provider construction inside the call layer; its
 * plaintext must never be written into StateFlow, logs, or exception messages.
 */
class ProviderRepository(
    private val store: KeyValueStore,
    private val secrets: ApiSecretStore,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private val _profiles = MutableStateFlow<List<ProviderProfile>>(emptyList())
    val profiles: StateFlow<List<ProviderProfile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val raw = store.getString(KEY_PROFILES)
        if (!raw.isNullOrBlank()) {
            val decoded = try {
                json.decodeFromString(ListSerializer(ProviderProfile.serializer()), raw)
            } catch (_: Exception) {
                emptyList()
            }
            _profiles.value = decoded
        }
        _activeProfileId.value = store.getString(KEY_ACTIVE_ID)
            ?.takeIf { id -> decodedContains(id) }
    }

    private fun decodedContains(id: String): Boolean = _profiles.value.any { it.id == id }

    private fun persistProfiles() {
        store.putString(
            KEY_PROFILES,
            json.encodeToString(ListSerializer(ProviderProfile.serializer()), _profiles.value),
        )
    }

    private fun persistActive() {
        val active = _activeProfileId.value
        if (active == null) store.remove(KEY_ACTIVE_ID) else store.putString(KEY_ACTIVE_ID, active)
    }

    /** Insert or update by [ProviderProfile.id]. Returns the stored profile. */
    fun saveProfile(profile: ProviderProfile): ProviderProfile {
        val current = _profiles.value
        val index = current.indexOfFirst { it.id == profile.id }
        _profiles.value = if (index >= 0) {
            current.toMutableList().apply { this[index] = profile }
        } else {
            current + profile
        }
        persistProfiles()
        return profile
    }

    /** Removes the profile and destroys its encrypted API secret. */
    fun deleteProfile(id: String) {
        val existing = _profiles.value.firstOrNull { it.id == id } ?: return
        existing.apiKeyRef?.let { secrets.deleteSecret(it) }
        _profiles.value = _profiles.value.filterNot { it.id == id }
        persistProfiles()
        if (_activeProfileId.value == id) {
            _activeProfileId.value = null
            persistActive()
        }
    }

    /** Points the active slot at [id]; unknown ids clear the slot instead of guessing. */
    fun setActiveProfile(id: String?) {
        _activeProfileId.value = id?.takeIf { candidate -> decodedContains(candidate) }
        persistActive()
    }

    fun profile(id: String): ProviderProfile? = _profiles.value.firstOrNull { it.id == id }

    fun activeProfile(): ProviderProfile? = _activeProfileId.value?.let { profile(it) }

    /**
     * Encrypts [apiKey] and stores a fresh reference on the profile, retiring the
     * previous secret. Blank keys are rejected without touching existing state.
     *
     * @return false when the profile is unknown or [apiKey] is blank.
     */
    fun setApiKey(profileId: String, apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        val current = profile(profileId) ?: return false
        val previousRef = current.apiKeyRef
        val newRef = secrets.putSecret(apiKey)
        previousRef?.let { secrets.deleteSecret(it) }
        saveProfile(current.copy(apiKeyRef = newRef))
        return true
    }

    /** Removes the key reference and destroys the encrypted secret. */
    fun clearApiKey(profileId: String): Boolean {
        val current = profile(profileId) ?: return false
        current.apiKeyRef?.let { secrets.deleteSecret(it) }
        if (current.apiKeyRef != null) saveProfile(current.copy(apiKeyRef = null))
        return current.apiKeyRef != null
    }

    /** Masked label for UI (`••••••••abcd`); never decrypts the full key. */
    fun maskedKeyLabel(profileId: String): String? {
        val ref = profile(profileId)?.apiKeyRef ?: return null
        return secrets.maskedLabel(ref)
    }

    /**
     * Full key for constructing an [com.example.data.ai.provider.OpenAiCompatibleProvider].
     * Call-side only — must never be written into StateFlow, UI state, or logs.
     */
    fun resolveApiKey(profileId: String): String? {
        val ref = profile(profileId)?.apiKeyRef ?: return null
        return secrets.getSecret(ref)
    }

    companion object {
        /** SharedPreferences file name for profile metadata (contains refs only). */
        const val PREFS_FILE = "ailua_ai_provider_store"
        const val KEY_PROFILES = "provider_profiles_json"
        const val KEY_ACTIVE_ID = "active_provider_profile_id"

        fun create(context: Context): ProviderRepository {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            return ProviderRepository(SharedPreferencesKeyValueStore(prefs), ApiSecretStore.create(context))
        }
    }
}
