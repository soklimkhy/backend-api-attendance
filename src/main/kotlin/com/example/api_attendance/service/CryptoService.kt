package com.example.api_attendance.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

@Service
class CryptoService(
    @Value("\${twofactor.encryption-key:}") private val keyConfig: String
) {
    private val logger = LoggerFactory.getLogger(CryptoService::class.java)
    private var enabled: Boolean = false
    private var key: SecretKey? = null

    init {
        if (keyConfig.isNullOrBlank()) {
            logger.warn("Two-factor encryption key not set: secrets will be stored in plaintext. Set 'twofactor.encryption-key' to enable encryption.")
        } else {
            enabled = true
            // Allow either raw string or base64; try base64 decode first
            val decoded = try {
                Base64.getDecoder().decode(keyConfig)
            } catch (e: IllegalArgumentException) {
                keyConfig.toByteArray()
            }
            // Ensure 32-byte key for AES-256; pad or trim as needed
            val keyBytes = ByteArray(32)
            val len = Integer.min(decoded.size, 32)
            System.arraycopy(decoded, 0, keyBytes, 0, len)
            key = SecretKeySpec(keyBytes, "AES")
        }
    }

    fun encrypt(plain: String): String {
        if (!enabled || key == null) return plain
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            // return base64(iv + cipher)
            val out = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(cipherText, 0, out, iv.size, cipherText.size)
            return Base64.getEncoder().encodeToString(out)
        } catch (e: Exception) {
            logger.error("Error encrypting data", e)
            throw e
        }
    }

    fun decrypt(cipherTextB64: String): String {
        if (!enabled || key == null) return cipherTextB64
        try {
            val all = Base64.getDecoder().decode(cipherTextB64)
            val iv = all.copyOfRange(0, 12)
            val cipherText = all.copyOfRange(12, all.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plain = cipher.doFinal(cipherText)
            return String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Error decrypting data", e)
            throw e
        }
    }
}
