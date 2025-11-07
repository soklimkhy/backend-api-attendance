package com.example.api_attendance.dto

import com.example.api_attendance.model.Gender

data class UserResponseDTO(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val gender: Gender? = null,
    val dateOfBirth: String? = null,
    val role: String = "",
    val authorities: List<String> = listOf(),
    val active: Boolean = true,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)