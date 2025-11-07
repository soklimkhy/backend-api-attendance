package com.example.api_attendance.model

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class Attendance(
    @DocumentId
    var id: String = "",

    @PropertyName("schedule_id")
    var scheduleId: String = "",

    @PropertyName("course_id")
    var courseId: String = "",

    @PropertyName("student_id")
    var studentId: String = "",

    @PropertyName("student_full_name")
    var studentFullName: String = "",

    @PropertyName("date")
    var date: String = "", // YYYY-MM-DD format

    @PropertyName("status")
    var status: AttendanceStatus = AttendanceStatus.PRESENT,

    @PropertyName("time")
    var time: Long? = null, // Time to verify student attendance

    @PropertyName("verified_by")
    var verifiedBy: String? = null, // Teacher/Admin who verified the attendance

    @PropertyName("notes")
    var notes: String? = null,

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()
) {
    enum class AttendanceStatus {
        PRESENT,
        ONLINE,
        LATE,
        ABSENT,
        EXCUSED
    }


    fun validate() {
        require(scheduleId.isNotBlank()) { "Schedule ID is required" }
        require(courseId.isNotBlank()) { "Course ID is required" }
        require(studentId.isNotBlank()) { "Student ID is required" }
        require(date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Date must be in YYYY-MM-DD format" }

        // Validate time if present
        time?.let { attendanceTime ->
            val attendanceDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(attendanceTime),
                ZoneId.systemDefault()
            )
            val scheduleDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            require(attendanceDateTime.toLocalDate() == scheduleDate) { "Attendance time must match schedule date" }
        }

        // Validate notes length if present
        notes?.let {
            require(it.length <= 500) { "Notes must not exceed 500 characters" }
        }
    }
}