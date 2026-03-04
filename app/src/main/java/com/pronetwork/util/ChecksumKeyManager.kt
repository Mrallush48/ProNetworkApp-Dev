package com.pronetwork.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the HMAC-SHA256 secret key for financial data integrity checksums.
 *
 * Security architecture:
 * - HMAC key: 256-bit randomly generated via SecureRandom
 * - Storage: AES-256-GCM encrypted, stored in SharedPreferences
 * - Master key: Android Keystore-backed (hardware-secured on supported devices)
 * - Thread-safe: double-checked locking with volatile cache
 * - Returns defensive copies to prevent external mutation
 */
@Singleton
class ChecksumKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "pronetwork_checksum_master"
        private const val PREFS_NAME = "pronetwork_checksum"
        private const val PREF_ENCRYPTED_KEY = "encrypted_hmac_key"
        private const val PREF_IV = "hmac_key_iv"
        private const val HMAC_KEY_SIZE_BYTES = 32
        private const val GCM_TAG_LENGTH = 128
    }

    @Volatile
    private var cachedKey: ByteArray? = null

    /**
     * Returns the HMAC secret key, creating one if it doesn't exist.
     * The key is cached in memory after first access for performance.
     * Always returns a defensive copy.
     */
    fun getSecretKey(): ByteArray {
        cachedKey?.let { return it.copyOf() }

        synchronized(this) {
            cachedKey?.let { return it.copyOf() }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val encryptedB64 = prefs.getString(PREF_ENCRYPTED_KEY, null)
            val ivB64 = prefs.getString(PREF_IV, null)

            val key = if (encryptedB64 != null && ivB64 != null) {
                decryptHmacKey(
                    Base64.decode(encryptedB64, Base64.NO_WRAP),
                    Base64.decode(ivB64, Base64.NO_WRAP)
                )
            } else {
                generateAndStoreKey(prefs)
            }

            cachedKey = key
            return key.copyOf()
        }
    }

    /**
     * Retrieves or creates the AES-256-GCM master key in Android Keystore.
     * This key encrypts/decrypts the HMAC key at rest.
     */
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    /**
     * Generates a new random HMAC key, encrypts it with the Keystore master key,
     * and persists the encrypted form in SharedPreferences.
     */
    private fun generateAndStoreKey(prefs: SharedPreferences): ByteArray {
        val hmacKey = ByteArray(HMAC_KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }

        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encrypted = cipher.doFinal(hmacKey)
        val iv = cipher.iv

        prefs.edit()
            .putString(PREF_ENCRYPTED_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return hmacKey
    }

    /**
     * Decrypts the stored HMAC key using the Keystore master key.
     */
    private fun decryptHmacKey(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }
}
