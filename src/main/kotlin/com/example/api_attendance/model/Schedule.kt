package com.example.api_attendance.model

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    @DocumentId
    var id: String = "",

    @PropertyName("course_id")
    var courseId: String = "",

    @PropertyName("day_of_week")
    var dayOfWeek: Int = 1, // 1 = Monday, 7 = Sunday

    @PropertyName("start_time")
    var startTime: String = "", // HH:mm format

    @PropertyName("end_time")
    var endTime: String = "", // HH:mm format

    @PropertyName("room")
    var room: String = "",

    @PropertyName("type")
    var type: ScheduleType = ScheduleType.REGULAR,

    @PropertyName("specific_date")
    var specificDate: String? = null, // YYYY-MM-DD format, only for MAKEUP type

    @PropertyName("status")
    var status: ScheduleStatus = ScheduleStatus.ACTIVE,

    @PropertyName("notes")
    var notes: String? = null,

    @PropertyName("created_by")
    var createdBy: String = "", // Admin/Teacher who created this schedule

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()
) {
    enum class ScheduleType {
        REGULAR,    // Regular weekly schedule
        MAKEUP,     // Makeup class
        SPECIAL     // Special one-time class
    }

    enum class ScheduleStatus {
        ACTIVE,     // Schedule is active
        CANCELLED,  // Class is cancelled for this schedule
        COMPLETED   // Class has been conducted
    }

    fun validate() {
        require(courseId.isNotBlank()) { "Course ID is required" }
        require(dayOfWeek in 1..7) { "Day of week must be between 1 (Monday) and 7 (Sunday)" }
        require(startTime.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) { "Start time must be in HH:mm format" }
        require(endTime.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) { "End time must be in HH:mm format" }
        require(room.isNotBlank()) { "Room is required" }
        require(createdBy.isNotBlank()) { "Created by is required" }

        // Validate specific date for makeup classes
        if (type == ScheduleType.MAKEUP) {
            require(!specificDate.isNullOrBlank()) { "Specific date is required for makeup classes" }
            require(specificDate!!.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) { "Date must be in YYYY-MM-DD format" }
        }

        // Validate notes length if present
        notes?.let {
            require(it.length <= 500) { "Notes must not exceed 500 characters" }
        }

        // Validate time order
        val startParts = startTime.split(":").map { it.toInt() }
        val endParts = endTime.split(":").map { it.toInt() }
        val startMinutes = startParts[0] * 60 + startParts[1]
        val endMinutes = endParts[0] * 60 + endParts[1]
        require(endMinutes > startMinutes) { "End time must be after start time" }
    }
}