package com.example.api_attendance.dto

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceUpdateRequest(
    val scheduleId: String,
    val attendanceRecords: List<AttendanceRecord>
)

@Serializable
data class AttendanceRecord(
    val studentId: String,
    val status: String,
    val time: Long,
    val notes: String?
)