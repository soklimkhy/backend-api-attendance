package com.example.api_attendance.controller

import com.example.api_attendance.dto.UserResponseDTO
import com.example.api_attendance.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    data class AuthRequest(val username: String, val password: String, val otp: Int? = null)
    data class RefreshTokenRequest(val refreshToken: String)

    data class UserResponse(
        val id: String,
        val fullName: String,
        val role: String,
        val createdAt: Long
    ) {
        companion object {
            fun fromDTO(user: UserResponseDTO) = UserResponse(
                id = user.id,
                fullName = user.fullName,
                role = user.role,
                createdAt = user.createdAt
            )
        }
    }

    @PostMapping("/register")
    fun register(@RequestBody req: AuthRequest): ResponseEntity<Any> {
        return try {
            val userDto = authService.register(req.username, req.password)
            ResponseEntity.ok(mapOf(
                "message" to "User registered",
                "user" to UserResponse.fromDTO(userDto)
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody req: AuthRequest): ResponseEntity<Any> {
        return try {
            val result = authService.login(req.username, req.password, req.otp)
            val response = ResponseEntity.ok()
            
            // Add tokens to headers if available
            result.accessToken?.let { token ->
                response.header("Authorization", "Bearer $token")
            }
            result.refreshToken?.let { token ->
                response.header("Refresh-Token", token)
            }

            // If MFA required, return 202 with indicator
            if (result.mfaRequired) {
                return ResponseEntity.status(202).body(mapOf("message" to "MFA required", "user" to UserResponse.fromDTO(result.user)))
            }

            // Return user info in body
            response.body(mapOf(
                "message" to "Login successful",
                "user" to UserResponse.fromDTO(result.user),
                "accessToken" to result.accessToken,
                "refreshToken" to result.refreshToken
            ))
        } catch (e: Exception) {
            ResponseEntity.status(401).body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/refresh-token")
    fun refreshToken(@RequestBody req: RefreshTokenRequest): ResponseEntity<Any> {
        return try {
            val token = authService.refreshToken(req.refreshToken)
            ResponseEntity.ok(mapOf(
                "token" to token,
                "message" to "Token refreshed successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.status(401).body(mapOf("error" to e.message))
        }
    }
}
