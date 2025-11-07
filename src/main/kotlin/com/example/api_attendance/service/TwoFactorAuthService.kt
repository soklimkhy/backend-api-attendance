package com.example.api_attendance.service

import com.example.api_attendance.repository.UserRepository
import com.warrenstrange.googleauth.GoogleAuthenticator
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class TwoFactorAuthService(
    private val userRepository: UserRepository,
    private val cryptoService: com.example.api_attendance.service.CryptoService
) {
    private val logger = LoggerFactory.getLogger(TwoFactorAuthService::class.java)
    private val authenticator = GoogleAuthenticator()

    /**
     * Generate a new secret key and QR code URL for 2FA setup
     */
    fun generateSecretKey(userId: String): Map<String, String> {
        val currentUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        logger.info("Generating 2FA secret for user: {}", userId)

        // Determine account name to show in authenticator app (email preferred)
        val accountName = when {
            !currentUser.email.isNullOrBlank() -> currentUser.email
            !currentUser.username.isNullOrBlank() -> currentUser.username
            else -> currentUser.id
        }

        // Generate new secret key
        val credentials = authenticator.createCredentials()

        // Generate QR code URL (guard against library errors)
        val qrCodeUrl = try {
            GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                "AttendanceAPI", // issuer
                accountName, // account name
                credentials     // credentials object
            )
        } catch (e: Exception) {
            logger.error("Failed to generate QR code URL for user: {}", userId, e)
            throw IllegalArgumentException("Failed to generate 2FA QR code")
        }

    // Store secret key temporarily (but don't enable 2FA yet)
    currentUser.twoFactorSecret = cryptoService.encrypt(credentials.key)
        userRepository.save(currentUser)

        logger.info("2FA secret generated for user: {}", userId)
        
        return mapOf(
            "secretKey" to credentials.key,
            "qrCodeUrl" to qrCodeUrl
        )
    }

    /**
     * Verify the provided code and enable 2FA if correct
     */
    fun verifyAndEnable2FA(userId: String, code: Int): Boolean {
        val currentUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        val secretEnc = currentUser.twoFactorSecret
            ?: throw IllegalStateException("No 2FA secret found. Please generate one first.")
        val secret = cryptoService.decrypt(secretEnc)

        try {
            logger.info("Verifying 2FA code for user: {}", userId)
            // Debug info to help diagnose mismatches: log partial secret and server time.
            // NOTE: keep this at DEBUG level in production or remove after debugging.
            val masked = if (secret.length > 8) secret.substring(0,4) + "..." + secret.substring(secret.length-4) else secret
            logger.debug("Decrypted 2FA secret (masked) for user {}: {}", userId, masked)
            val nowMillis = System.currentTimeMillis()
            logger.debug("Server time (ms) for user {}: {} (epochSeconds={})", userId, nowMillis, nowMillis / 1000)

            val isCodeValid = authenticator.authorize(secret, code)
            
            if (isCodeValid) {
                currentUser.twoFactorEnabled = true
                userRepository.save(currentUser)
                logger.info("2FA enabled for user: {}", userId)
                return true
            }
            
            logger.warn("Invalid 2FA code provided for user: {}", userId)
            return false
        } catch (e: Exception) {
            logger.error("Error validating 2FA code for user: {}", userId, e)
            throw IllegalStateException("Error validating code: ${e.message}")
        }
    }

    /**
     * Verify a code for an already-enabled 2FA
     */
    fun verifyCode(userId: String, code: Int): Boolean {
        val currentUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        if (!currentUser.twoFactorEnabled) {
            logger.warn("2FA verification attempted but not enabled for user: {}", userId)
            throw IllegalStateException("2FA is not enabled for this user")
        }

        val secretEnc = currentUser.twoFactorSecret
            ?: throw IllegalStateException("No 2FA secret found")
        val secret = cryptoService.decrypt(secretEnc)

        try {
            logger.info("Verifying 2FA code for user: {}", userId)
            val masked = if (secret.length > 8) secret.substring(0,4) + "..." + secret.substring(secret.length-4) else secret
            logger.debug("Decrypted 2FA secret (masked) for user {}: {}", userId, masked)
            val nowMillis = System.currentTimeMillis()
            logger.debug("Server time (ms) for user {}: {} (epochSeconds={})", userId, nowMillis, nowMillis / 1000)
            return authenticator.authorize(secret, code)
        } catch (e: Exception) {
            logger.error("Error validating 2FA code for user: {}", userId, e)
            throw IllegalStateException("Error validating code: ${e.message}")
        }
    }

    /**
     * Disable 2FA for a user
     */
    fun disable2FA(userId: String, code: Int): Boolean {
        val currentUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        if (!currentUser.twoFactorEnabled) {
            logger.warn("2FA disable attempted but not enabled for user: {}", userId)
            throw IllegalStateException("2FA is not enabled for this user")
        }

        // Verify the code first
        if (!verifyCode(userId, code)) {
            logger.warn("Invalid code provided for 2FA disable by user: {}", userId)
            return false
        }

        // Disable 2FA
        currentUser.twoFactorEnabled = false
        currentUser.twoFactorSecret = null
        userRepository.save(currentUser)
        
        logger.info("2FA disabled for user: {}", userId)
        return true
    }
}