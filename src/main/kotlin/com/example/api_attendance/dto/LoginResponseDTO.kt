package com.example.api_attendance.dto

data class LoginResponseDTO(
    val message: String,
    val user: UserResponseDTO,
    val token: String
)