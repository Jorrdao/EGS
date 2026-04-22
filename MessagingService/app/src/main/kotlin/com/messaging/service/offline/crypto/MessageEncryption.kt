package com.messaging.service.offline.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption/decryption for BLE mesh message payloads.
 *
 * Uses the Android Keystore system so the key material never leaves
 * secure hardware (on supported devices).
 *
 * Each encrypt() call generates a fresh 12-byte IV; the IV is prepended
 * to the ciphertext so decrypt() can extract it.
 *
 * Output format: Base64( IV[12] || TAG[16] || CIPHERTEXT )
 */
@Singleton
class MessageEncryption @Inject constructor() {

    private val keyAlias = "messaging_ble_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    init { ensureKeyExists() }

    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv         = cipher.iv          // 12 bytes
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext              // IV || TAG || CIPHERTEXT
    }

    fun decrypt(data: ByteArray): ByteArray {
        val iv         = data.copyOfRange(0, 12)
        val ciphertext = data.copyOfRange(12, data.size)
        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        return cipher.doFinal(ciphertext)
    }

    fun encryptToBase64(text: String): String =
        Base64.encodeToString(encrypt(text.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    fun decryptFromBase64(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return String(decrypt(bytes), Charsets.UTF_8)
    }

    private fun getKey(): SecretKey =
        (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey

    private fun ensureKeyExists() {
        if (keyStore.containsAlias(keyAlias)) return
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        keyGen.generateKey()
    }
}
