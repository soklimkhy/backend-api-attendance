package com.example.api_attendance.model

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import com.example.api_attendance.dto.UserResponseDTO
import kotlinx.serialization.Serializable
import java.util.regex.Pattern

@Serializable
data class User(
    @DocumentId
    var id: String = "",

    @PropertyName("username")
    var username: String = "",

    @PropertyName("email")
    var email: String = "",

    @PropertyName("password")
    var password: String = "",

    @PropertyName("full_name")
    var fullName: String = "",

    @PropertyName("photo_url")
    var photoUrl: String? = null,

    @PropertyName("phone_number")
    var phoneNumber: String? = null,

    @PropertyName("gender")
    var gender: Gender? = null,

    @PropertyName("date_of_birth")
    var dateOfBirth: String? = null,

    @PropertyName("role")
    var role: String = Role.STUDENT.name,

    @PropertyName("authorities")
    var authorities: List<String> = Role.STUDENT.authorities,

    @PropertyName("active")
    var active: Boolean = true,

    @PropertyName("email_verified")
    var emailVerified: Boolean = false,

    @PropertyName("phone_verified")
    var phoneVerified: Boolean = false,

    @PropertyName("two_factor_enabled")
    var twoFactorEnabled: Boolean = false,

    @PropertyName("two_factor_secret")
    var twoFactorSecret: String? = null,

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()
) {
    @PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    companion object {
        private val EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
        )

        fun isValidEmail(email: String): Boolean =
            EMAIL_PATTERN.matcher(email).matches()
    }

    fun hasAuthority(authority: String): Boolean = 
        authorities.contains(authority)

    fun hasAnyAuthority(vararg authorities: String): Boolean =
        authorities.any { hasAuthority(it) }

    fun collectValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        // Username validation
        if (username.isBlank()) {
            errors.add("Username is required")
        } else {
            if (username.length < 3) errors.add("Username must be at least 3 characters")
            if (!username.matches(Regex("^[a-zA-Z0-9_.-]+$"))) errors.add("Username can only contain letters, numbers, dots, underscores, and hyphens")
        }

        // Password validation
        if (password.isBlank()) {
            errors.add("Password is required")
        } else {
            if (password.length < 8) errors.add("Password must be at least 8 characters")
        }

        // Email validation
        if (email.isNotBlank() && !isValidEmail(email)) {
            errors.add("Invalid email format")
        }

        // Full name validation
        if (fullName.isNotBlank()) {
            if (fullName.length < 2) errors.add("Full name must be at least 2 characters if provided")
            // Allow underscores so fullName can be similar to username (e.g. postman_test_123)
            if (!fullName.matches(Regex("^[a-zA-Z0-9_\\s.-]+$"))) errors.add("Full name can only contain letters, numbers, spaces, dots, underscores, and hyphens")
        }

        // Photo URL validation
        photoUrl?.let {
            if (it.isNotBlank() && !it.matches(Regex("^https?://.*$"))) {
                errors.add("Photo URL must be a valid HTTP(S) URL")
            }
        }

        // Phone number validation
        phoneNumber?.let {
            if (it.isNotBlank() && !it.matches(Regex("^\\+?[0-9]{7,15}$"))) {
                errors.add("Phone number must be valid if provided")
            }
        }

        // Gender validation
        if (gender != null && !Gender.values().contains(gender)) {
            errors.add("Gender must be valid if provided")
        }

        // Date of birth validation
        dateOfBirth?.let {
            if (it.isNotBlank()) {
                try {
                    val parsed = java.time.LocalDate.parse(it)
                    if (parsed.year <= 1900) errors.add("Date of birth must be valid if provided")
                } catch (ex: Exception) {
                    errors.add("Date of birth must be an ISO date (yyyy-MM-dd) if provided")
                }
            }
        }

        if (role.isBlank()) errors.add("Role is required")

        return errors
    }

    fun validate() {
        val errors = collectValidationErrors()
        if (errors.isNotEmpty()) throw IllegalArgumentException(errors.first())
    }

    fun toResponseDTO(): UserResponseDTO = UserResponseDTO(
        id = id,
        username = username,
        email = email,
        fullName = fullName,
        photoUrl = photoUrl,
        phoneNumber = phoneNumber,
        gender = gender,
        dateOfBirth = dateOfBirth,
        role = role,
        authorities = authorities,
        active = active,
        emailVerified = emailVerified,
        phoneVerified = phoneVerified,
        twoFactorEnabled = twoFactorEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}