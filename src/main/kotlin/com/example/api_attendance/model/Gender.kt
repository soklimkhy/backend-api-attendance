package com.example.api_attendance.model

enum class Gender {
    MALE,
    FEMALE;

    companion object {
        fun fromString(value: String?): Gender? {
            return try {
                value?.let { valueOf(it.uppercase()) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}