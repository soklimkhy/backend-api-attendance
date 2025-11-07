package com.example.api_attendance.service

import com.example.api_attendance.dto.AuthResponseDTO
import com.example.api_attendance.dto.UserResponseDTO
import com.example.api_attendance.exception.InvalidCredentialsException
import com.example.api_attendance.exception.UserNotFoundException
import com.example.api_attendance.exception.UsernameExistsException
import com.example.api_attendance.model.Session
import com.example.api_attendance.model.Token
import com.example.api_attendance.model.User
import com.example.api_attendance.repository.SessionRepository
import com.example.api_attendance.repository.TokenRepository
import com.example.api_attendance.repository.UserRepository
import com.example.api_attendance.security.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService? = null,
    private val tokenRepository: TokenRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val request: HttpServletRequest? = null,
    private val twoFactorAuthService: com.example.api_attendance.service.TwoFactorAuthService? = null
) {
    private val encoder = BCryptPasswordEncoder()
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun register(username: String, password: String): UserResponseDTO {
        logger.info("Registration attempt for username={}", username)
        val existing = userRepository.findByUsername(username)
        if (existing != null) {
            logger.warn("Registration failed - username already exists: {}", username)
            throw UsernameExistsException(username)
        }

        // validate raw password strength before encoding (encoded value will not reflect length)
        require(password.isNotBlank()) { "Password is required" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        val newUser = User(
            username = username,
            password = encoder.encode(password),
            fullName = username // Set fullName same as username during registration
        )

        // Validate model before saving (does not enforce uniqueness beyond the check above)
        newUser.validate()

        val saved = userRepository.save(newUser)
        logger.info("User registered successfully: id={}, username={}", saved.id, saved.username)
        return saved.toResponseDTO()
    }

    fun login(username: String, password: String, otp: Int? = null): AuthResponseDTO {
        logger.info("Login attempt for username={}", username)
        val user = userRepository.findByUsername(username)
            ?: run {
                logger.warn("Login failed - user not found: {}", username)
                throw UserNotFoundException(username)
            }

        if (!encoder.matches(password, user.password)) {
            logger.warn("Login failed - invalid credentials for username={}", username)
            throw InvalidCredentialsException()
        }

        // If user has 2FA enabled, require OTP
        if (user.twoFactorEnabled) {
            if (otp == null) {
                // Indicate to caller that MFA is required
                return AuthResponseDTO(user = user.toResponseDTO(), mfaRequired = true)
            }
            // Verify the provided OTP using TwoFactor service
            if (twoFactorAuthService == null) {
                throw IllegalStateException("2FA configured but service unavailable")
            }
            val ok = twoFactorAuthService.verifyCode(user.id, otp)
            if (!ok) {
                logger.warn("Login failed - invalid 2FA code for username={}", username)
                throw InvalidCredentialsException()
            }
        }

        var accessToken: String? = null
        var refreshToken: String? = null

        // If token/session infrastructure available, create them; otherwise skip (useful for unit tests)
        if (jwtService != null && tokenRepository != null && sessionRepository != null && request != null) {
            // Revoke any existing tokens and sessions
            tokenRepository.revokeAllUserTokens(user.id)
            sessionRepository.invalidateAllUserSessions(user.id)

            // Generate new tokens
            accessToken = jwtService.generateAccessToken(user.id)
            refreshToken = jwtService.generateRefreshToken(user.id)

            // Save token record
            val token = Token(
                userId = user.id,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiredAt = Instant.now().plusSeconds(7 * 24 * 60 * 60) // Token expires in 7 days
            )
            tokenRepository.save(token)

            // Create session record
            val session = Session(
                userId = user.id,
                device = request.getHeader("User-Agent") ?: "unknown",
                ipAddress = request.remoteAddr,
                lastLoginAt = Instant.now(),
                active = true
            )
            sessionRepository.save(session)
        }

        logger.info("Login successful for username={}", username)
        return AuthResponseDTO(
            user = user.toResponseDTO(),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun refreshToken(refreshToken: String): String {
        if (tokenRepository == null || jwtService == null) {
            throw IllegalStateException("Token refresh not available")
        }

        val token = tokenRepository.findByRefreshToken(refreshToken)
            ?: throw InvalidCredentialsException()

        if (token.revoked || token.expiredAt?.isBefore(Instant.now()) == true) {
            throw InvalidCredentialsException()
        }

        // Generate new tokens
        val newAccessToken = jwtService.generateAccessToken(token.userId)
        val newRefreshToken = jwtService.generateRefreshToken(token.userId)

        // Revoke old token
        token.revoked = true
        tokenRepository.save(token)

        // Save new token
        val newToken = Token(
            userId = token.userId,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiredAt = Instant.now().plusSeconds(7 * 24 * 60 * 60) // Token expires in 7 days
        )
        tokenRepository.save(newToken)

        return newAccessToken
    }

    /**
     * Logout user by revoking all tokens and invalidating sessions for the given userId.
     * Returns true if operation performed, false if token/session infrastructure is not available.
     */
    fun logout(userId: String?): Boolean {
        if (userId == null) return false
        if (tokenRepository == null || sessionRepository == null) {
            logger.warn("Logout requested but token/session repositories are not configured")
            return false
        }

        tokenRepository.revokeAllUserTokens(userId)
        sessionRepository.invalidateAllUserSessions(userId)
        logger.info("User logged out: {}", userId)
        return true
    }
}
