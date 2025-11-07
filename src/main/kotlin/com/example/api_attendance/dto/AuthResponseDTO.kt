package com.example.api_attendance.dto

data class AuthResponseDTO(
    val user: UserResponseDTO,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val mfaRequired: Boolean = false
)