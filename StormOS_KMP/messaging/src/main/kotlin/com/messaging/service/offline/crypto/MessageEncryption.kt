package com.messaging.service.offline.crypto

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessageEncryption {
    // A shared secret across all instances of your app
    private val sharedSecret = "storm_os_mesh_shared_secret_2024"
    private val secretKey: SecretKeySpec

    init {
        // Derive a deterministic 256-bit (32-byte) key
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(sharedSecret.toByteArray(Charsets.UTF_8))
        secretKey = SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 12)
        val ciphertext = data.copyOfRange(12, data.size)
        val spec = GCMParameterSpec(128, iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }

    fun encryptToBase64(text: String): String =
        Base64.encodeToString(encrypt(text.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    fun decryptFromBase64(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return String(decrypt(bytes), Charsets.UTF_8)
    }
}