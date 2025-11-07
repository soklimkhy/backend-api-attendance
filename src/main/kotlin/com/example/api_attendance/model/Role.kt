package com.example.api_attendance.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role(val authorities: List<String>) {
    STUDENT(
        listOf(
            "ATTENDANCE_VIEW",
            "PROFILE_VIEW",
            "PROFILE_EDIT",
            "TWO_FACTOR_MANAGE"
        )
    ),
    TEACHER(
        listOf(
            "COURSE_VIEW",
            "SCHEDULE_VIEW",
            "ATTENDANCE_VIEW",
            "ATTENDANCE_MANAGE"
        )
    ),
    ADMIN(
        listOf(
            "ATTENDANCE_VIEW",
            "ATTENDANCE_MANAGE",
            "PROFILE_VIEW",
            "PROFILE_EDIT",
            "STUDENT_VIEW",
            "STUDENT_MANAGE",
            "TEACHER_VIEW",
            "TEACHER_MANAGE",
            "ROLE_MANAGE",
            "COURSE_VIEW",
            "COURSE_CREATE",
            "COURSE_EDIT",
            "COURSE_DELETE"
        )
    );

    companion object {
        fun getDefaultAuthorities(role: String): List<String> {
            return try {
                valueOf(role.uppercase()).authorities
            } catch (e: IllegalArgumentException) {
                STUDENT.authorities
            }
        }

        fun fromString(value: String?): Role {
            return try {
                value?.let { valueOf(it.uppercase()) } ?: STUDENT
            } catch (e: IllegalArgumentException) {
                STUDENT
            }
        }
    }
}