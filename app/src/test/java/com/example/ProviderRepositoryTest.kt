package com.example

import com.example.data.ai.model.ProviderProfile
import com.example.data.ai.repository.ProviderRepository
import com.example.data.ai.security.ApiSecretStore
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ProviderRepositoryTest — P3C-1 repository gate.
 *
 * Profiles + activeProfileId + save/update/delete + key ref resolution +
 * masked label. The API key itself must never appear in profile serialization,
 * and an unknown ref must never resolve to a different profile's key.
 */
class ProviderRepositoryTest {

    private val store = InMemoryKeyValueStore()
    private val secrets = ApiSecretStore(store, AesGcmTestCipher())
    private val repository = ProviderRepository(store, secrets)

    private val keyOne = "sk-one-111122223333"
    private val keyTwo = "sk-two-444455556666"

    private fun profile(
        id: String = "openai",
        name: String = "OpenAI Compatible",
        baseUrl: String = "https://api.openai.com/v1",
        model: String = "gpt-4o-mini",
    ) = ProviderProfile(id = id, name = name, baseUrl = baseUrl, model = model)

    @Test
    fun saveProfilePersistsAcrossInstances() {
        repository.saveProfile(profile())

        val reloaded = ProviderRepository(store, secrets)
        assertEquals(listOf("openai"), reloaded.profiles.value.map { it.id })
    }

    @Test
    fun saveProfileUpsertsById() {
        repository.saveProfile(profile())
        repository.saveProfile(profile(name = "Renamed", model = "gpt-4o"))

        assertEquals(1, repository.profiles.value.size)
        assertEquals("Renamed", repository.profile("openai")!!.name)
        assertEquals("gpt-4o", repository.profile("openai")!!.model)
    }

    @Test
    fun profileSerializationNeverContainsApiKey() {
        repository.saveProfile(profile())
        repository.setApiKey("openai", keyOne)

        val encoded = Json.encodeToString(ProviderProfile.serializer(), repository.profile("openai")!!)
        assertFalse(encoded.contains(keyOne))
        assertTrue(encoded.contains(repository.profile("openai")!!.apiKeyRef!!))
    }

    @Test
    fun setApiKeyStoresRefAndResolvesPlaintext() {
        repository.saveProfile(profile())

        assertTrue(repository.setApiKey("openai", keyOne))

        val stored = repository.profile("openai")!!
        assertTrue(stored.apiKeyRef != null)
        assertEquals(keyOne, repository.resolveApiKey("openai"))
        assertEquals(ApiSecretStore.MASK + "3333", repository.maskedKeyLabel("openai"))
    }

    @Test
    fun setApiKeyTwiceRetiresOldSecret() {
        repository.saveProfile(profile())
        repository.setApiKey("openai", keyOne)
        val oldRef = repository.profile("openai")!!.apiKeyRef!!

        repository.setApiKey("openai", keyTwo)

        val newRef = repository.profile("openai")!!.apiKeyRef!!
        assertNotEquals(oldRef, newRef)
        assertNull(secrets.getSecret(oldRef))
        assertEquals(keyTwo, repository.resolveApiKey("openai"))
    }

    @Test
    fun setApiKeyRejectsUnknownProfile() {
        assertFalse(repository.setApiKey("ghost", keyOne))
        assertNull(repository.profile("ghost"))
    }

    @Test
    fun blankKeyIsRejectedWithoutMutatingState() {
        repository.saveProfile(profile())
        repository.setApiKey("openai", keyOne)

        assertFalse(repository.setApiKey("openai", "   "))

        assertEquals(keyOne, repository.resolveApiKey("openai"))
    }

    @Test
    fun clearApiKeyDestroysSecret() {
        repository.saveProfile(profile())
        repository.setApiKey("openai", keyOne)
        val ref = repository.profile("openai")!!.apiKeyRef!!

        assertTrue(repository.clearApiKey("openai"))

        assertNull(repository.profile("openai")!!.apiKeyRef)
        assertNull(secrets.getSecret(ref))
        assertNull(repository.maskedKeyLabel("openai"))
        assertNull(repository.resolveApiKey("openai"))
        assertFalse(repository.clearApiKey("openai"))
    }

    @Test
    fun deleteProfileDestroysSecret() {
        repository.saveProfile(profile())
        repository.setApiKey("openai", keyOne)
        val ref = repository.profile("openai")!!.apiKeyRef!!
        repository.setActiveProfile("openai")

        repository.deleteProfile("openai")

        assertTrue(repository.profiles.value.isEmpty())
        assertNull(secrets.getSecret(ref))
        assertNull(repository.activeProfile())
    }

    @Test
    fun activeProfilePersistsAcrossInstances() {
        repository.saveProfile(profile())
        repository.setActiveProfile("openai")

        val reloaded = ProviderRepository(store, secrets)
        assertEquals("openai", reloaded.activeProfileId.value)
        assertEquals("openai", reloaded.activeProfile()!!.id)
    }

    @Test
    fun unknownActiveProfileNeverGuesses() {
        repository.saveProfile(profile())
        repository.setActiveProfile("openai")
        repository.setActiveProfile("ghost")

        assertNull(repository.activeProfile())

        store.putString(ProviderRepository.KEY_ACTIVE_ID, "ghost2")
        val reloaded = ProviderRepository(store, secrets)
        assertNull(reloaded.activeProfile())
    }

    @Test
    fun resolveNeverReturnsAnotherProfilesKey() {
        repository.saveProfile(profile(id = "one"))
        repository.saveProfile(profile(id = "two"))
        repository.setApiKey("one", keyOne)
        repository.setApiKey("two", keyTwo)

        assertEquals(keyOne, repository.resolveApiKey("one"))
        assertEquals(keyTwo, repository.resolveApiKey("two"))
        assertNull(repository.resolveApiKey("ghost"))
    }

    @Test
    fun corruptProfilesJsonResetsToEmpty() {
        store.putString(ProviderRepository.KEY_PROFILES, "{not json")

        val reloaded = ProviderRepository(store, secrets)
        assertTrue(reloaded.profiles.value.isEmpty())
    }
}
