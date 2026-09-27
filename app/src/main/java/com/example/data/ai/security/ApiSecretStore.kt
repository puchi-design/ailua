package com.example.data.ai.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Minimal key/value seam so the secret store and provider repository are unit-testable
 * without Android (production wiring uses SharedPreferences via [SharedPreferencesKeyValueStore]).
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/**
 * Encryption seam for API secrets. Production: [KeystoreSecretCipher]
 * (AES/GCM master key inside AndroidKeyStore — the key itself never leaves secure hardware).
 */
interface SecretCipher {
    /** @return encoded ciphertext payload (never contains the plaintext). */
    fun encrypt(plain: String): String

    /** @throws Exception when the payload is corrupt or undecryptable. */
    fun decrypt(payload: String): String
}

/**
 * P3C-1 secret boundary (spec §11 #6 + correction #2).
 *
 * Storage layout — only ciphertext + a last-4 hint are persisted:
 *
 *   ailua_ai_secrets.xml
 *     secret:<uuid>  -> "ivBase64:cipherBase64"  (AES/GCM under AndroidKeyStore)
 *     hint:<uuid>    -> last 4 chars (for masked UI label only)
 *
 * [ProviderProfile] only ever holds the UUID ([apiKeyRef]). Full keys are decrypted
 * on demand inside [getSecret] for provider construction and must never enter a
 * public StateFlow, a log line, or an exception message.
 *
 * The backing prefs file is excluded from Android Auto Backup AND device-to-device
 * transfer via backup_rules.xml + data_extraction_rules.xml (BackupExclusionTest).
 */
class ApiSecretStore(
    private val store: KeyValueStore,
    private val cipher: SecretCipher,
) {

    /** Encrypts [plain] and returns a fresh opaque reference. Replaces nothing — caller owns old refs. */
    fun putSecret(plain: String): String {
        require(plain.isNotBlank()) { "API key must not be blank" }
        val ref = UUID.randomUUID().toString()
        store.putString(secretKey(ref), cipher.encrypt(plain))
        store.putString(hintKey(ref), hintFor(plain))
        return ref
    }

    /** @return the plaintext key for [ref], or null when unknown / corrupt. Never another key. */
    fun getSecret(ref: String): String? {
        val payload = store.getString(secretKey(ref)) ?: return null
        return try {
            cipher.decrypt(payload)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteSecret(ref: String) {
        store.remove(secretKey(ref))
        store.remove(hintKey(ref))
    }

    /**
     * Masked display label, e.g. "••••••••abcd".
     * Built from the persisted hint — the full key is never decrypted for UI state.
     *
     * @return null when the reference is unknown.
     */
    fun maskedLabel(ref: String): String? {
        if (store.getString(secretKey(ref)) == null) return null
        val hint = store.getString(hintKey(ref)) ?: return MASK
        return MASK + hint
    }

    private fun hintFor(plain: String): String = if (plain.length > 4) plain.takeLast(4) else ""

    private fun secretKey(ref: String) = "secret:$ref"

    private fun hintKey(ref: String) = "hint:$ref"

    companion object {
        /** SharedPreferences file name — on disk: shared_prefs/ailua_ai_secrets.xml (backup-excluded). */
        const val PREFS_FILE = "ailua_ai_secrets"

        const val MASK = "••••••••"

        fun create(context: Context): ApiSecretStore {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            return ApiSecretStore(SharedPreferencesKeyValueStore(prefs), KeystoreSecretCipher())
        }
    }
}

/** Production [KeyValueStore] backed by a dedicated SharedPreferences file. */
class SharedPreferencesKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

/**
 * AES-256/GCM master key held in AndroidKeyStore; only the ciphertext + IV are persisted.
 * Ciphertext format: "base64(iv):base64(ciphertext)".
 */
class KeystoreSecretCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : SecretCipher {

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    override fun decrypt(payload: String): String {
        val separator = payload.indexOf(':')
        require(separator > 0) { "malformed secret payload" }
        val iv = Base64.decode(payload.substring(0, separator), Base64.NO_WRAP)
        val ciphertext = Base64.decode(payload.substring(separator + 1), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun obtainKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "ailua_ai_secret_master_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
    }
}
