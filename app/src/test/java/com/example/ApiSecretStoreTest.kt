package com.example

import com.example.data.ai.security.ApiSecretStore
import com.example.data.ai.security.KeyValueStore
import com.example.data.ai.security.SecretCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** In-memory [KeyValueStore] double — lets tests inspect exactly what gets persisted. */
class InMemoryKeyValueStore : KeyValueStore {
    val entries = mutableMapOf<String, String>()

    override fun getString(key: String): String? = entries[key]

    override fun putString(key: String, value: String) {
        entries[key] = value
    }

    override fun remove(key: String) {
        entries.remove(key)
    }
}

/** Real AES/GCM on a plain JVM key — exercises [SecretCipher] roundtrip semantics without AndroidKeyStore. */
class AesGcmTestCipher : SecretCipher {
    private val key = SecretKeySpec(ByteArray(16) { (it + 1).toByte() }, "AES")

    override fun encrypt(plain: String): String {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext)
    }

    override fun decrypt(payload: String): String {
        val separator = payload.indexOf(':')
        require(separator > 0) { "malformed payload" }
        val iv = Base64.getDecoder().decode(payload.substring(0, separator))
        val ciphertext = Base64.getDecoder().decode(payload.substring(separator + 1))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private const val IV_BYTES = 12
    }
}

/**
 * ApiSecretStoreTest — P3C-1 secret boundary (spec §11 #6 + correction #2).
 *
 * Gate: encrypt/decrypt roundtrip, plaintext never persisted, unknown ref ->
 * null (never another key), masked label, corrupt ciphertext -> null.
 */
class ApiSecretStoreTest {

    private val store = InMemoryKeyValueStore()
    private val cipher = AesGcmTestCipher()
    private val secrets = ApiSecretStore(store, cipher)

    private val apiKey = "sk-plain-0123456789abcd"

    @Test
    fun encryptDecryptRoundtrip() {
        val ref = secrets.putSecret(apiKey)

        assertEquals(apiKey, secrets.getSecret(ref))
    }

    @Test
    fun plaintextIsNeverPersisted() {
        val ref = secrets.putSecret(apiKey)

        assertTrue(store.entries.values.none { it.contains(apiKey) })
        assertTrue(store.entries.keys.none { it.contains(apiKey) })
        val payload = store.entries.getValue("secret:$ref")
        assertNotEquals(apiKey, payload)
        assertTrue(payload.contains(":"))
    }

    @Test
    fun storedHintIsNeverTheFullKey() {
        val ref = secrets.putSecret(apiKey)

        val hint = store.entries.getValue("hint:$ref")
        assertNotEquals(apiKey, hint)
        assertTrue(apiKey.endsWith(hint))
    }

    @Test
    fun unknownRefReturnsNullNeverAnotherKey() {
        val ref = secrets.putSecret(apiKey)

        assertNull(secrets.getSecret("00000000-0000-0000-0000-000000000000"))
        assertEquals(apiKey, secrets.getSecret(ref))
    }

    @Test
    fun twoRefsNeverCross() {
        val keyA = "sk-aaaaaaaaaaaaaaaa"
        val keyB = "sk-bbbbbbbbbbbbbbbb"
        val refA = secrets.putSecret(keyA)
        val refB = secrets.putSecret(keyB)

        assertEquals(keyA, secrets.getSecret(refA))
        assertEquals(keyB, secrets.getSecret(refB))
    }

    @Test
    fun deletedRefReturnsNull() {
        val ref = secrets.putSecret(apiKey)
        secrets.deleteSecret(ref)

        assertNull(secrets.getSecret(ref))
        assertNull(secrets.maskedLabel(ref))
        assertFalse(store.entries.containsKey("secret:$ref"))
        assertFalse(store.entries.containsKey("hint:$ref"))
    }

    @Test
    fun maskedLabelShowsLastFour() {
        val ref = secrets.putSecret("sk-test-abcd")

        assertEquals(ApiSecretStore.MASK + "abcd", secrets.maskedLabel(ref))
    }

    @Test
    fun shortKeyMaskDoesNotLeakIt() {
        val ref = secrets.putSecret("abc")

        assertEquals(ApiSecretStore.MASK, secrets.maskedLabel(ref))
        assertFalse(secrets.maskedLabel(ref)!!.contains("abc"))
    }

    @Test
    fun corruptCiphertextReturnsNull() {
        val ref = secrets.putSecret(apiKey)
        store.entries["secret:$ref"] = "not-valid-ciphertext"

        assertNull(secrets.getSecret(ref))
    }

    @Test
    fun blankKeyIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            secrets.putSecret("   ")
        }
    }

    @Test
    fun prefsFileNameMatchesBackupExclusionContract() {
        assertEquals("ailua_ai_secrets", ApiSecretStore.PREFS_FILE)
    }
}
