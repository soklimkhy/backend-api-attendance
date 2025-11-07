package com.example.api_attendance.model

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class Course(
    @DocumentId
    var id: String = "",

    @PropertyName("code")
    var code: String = "", // e.g., "CS101"

    @PropertyName("name")
    var name: String = "", // e.g., "Introduction to Computer Science"

    @PropertyName("description")
    var description: String = "",

    @PropertyName("teacher_id")
    var teacherId: String = "", // Reference to the teacher responsible for the course

    @PropertyName("academic_year")
    var academicYear: String = "", // e.g., "2025-2026"

    @PropertyName("semester")
    var semester: String = "", // e.g., "Fall", "Spring"

    @PropertyName("students")
    var studentIds: List<String> = listOf(), // List of student User IDs

    @PropertyName("active")
    var active: Boolean = true,

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun validate() {
        require(code.isNotBlank()) { "Course code is required" }
        require(name.isNotBlank()) { "Course name is required" }
        require(teacherId.isNotBlank()) { "Teacher ID is required" }
        require(academicYear.matches(Regex("^\\d{4}-\\d{4}$"))) { "Academic year must be in format YYYY-YYYY" }
        require(semester.isNotBlank()) { "Semester is required" }
    }
}