package com.example.api_attendance.controller

import com.example.api_attendance.service.TwoFactorAuthService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class TwoFactorVerifyRequest(
    val code: Int
)

@RestController
@RequestMapping("/api/user/2fa")
class TwoFactorController(
    private val twoFactorAuthService: TwoFactorAuthService
) {
    private val logger = LoggerFactory.getLogger(TwoFactorController::class.java)

    @PostMapping("/setup")
    fun setup2FA(): ResponseEntity<Any> {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val userId = auth.name ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        try {
            val data = twoFactorAuthService.generateSecretKey(userId)
            return ResponseEntity.ok(data)
        } catch (e: IllegalArgumentException) {
            logger.warn("2FA setup failed for user=$userId: {}", e.message)
            return ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
        } catch (e: Exception) {
            logger.error("Unexpected error during 2FA setup for user=$userId", e)
            return ResponseEntity.status(500).body(mapOf("error" to "Internal server error", "message" to (e.message ?: "")))
        }
    }

    @PostMapping("/verify")
    fun verify2FA(@RequestBody req: TwoFactorVerifyRequest): ResponseEntity<Any> {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val userId = auth.name ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        return try {
            val ok = twoFactorAuthService.verifyAndEnable2FA(userId, req.code)
            if (ok) ResponseEntity.ok(mapOf("message" to "2FA enabled successfully"))
            else ResponseEntity.badRequest().body(mapOf("error" to "Invalid verification code"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(404).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error enabling 2FA for user=$userId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping("/disable")
    fun disable2FA(@RequestBody req: TwoFactorVerifyRequest): ResponseEntity<Any> {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val userId = auth.name ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        return try {
            val ok = twoFactorAuthService.disable2FA(userId, req.code)
            if (ok) ResponseEntity.ok(mapOf("message" to "2FA disabled successfully"))
            else ResponseEntity.badRequest().body(mapOf("error" to "Invalid verification code"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(404).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error disabling 2FA for user=$userId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }
}
