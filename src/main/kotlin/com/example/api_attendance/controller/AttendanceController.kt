package com.example.api_attendance.controller

import com.example.api_attendance.model.Attendance
import com.example.api_attendance.service.AttendanceService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/attendance")
class AttendanceController(private val attendanceService: AttendanceService) {

    private val logger = LoggerFactory.getLogger(AttendanceController::class.java)

    @GetMapping("/{id}")
    fun getAttendanceById(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            val attendance = attendanceService.getAttendanceById(id)
                ?: return ResponseEntity.status(404).body(mapOf("error" to "Attendance not found"))
            ResponseEntity.ok(attendance)
        } catch (e: Exception) {
            logger.error("Error fetching attendance $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/schedule/{scheduleId}")
    fun getAttendanceByScheduleId(@PathVariable scheduleId: String): ResponseEntity<Any> {
        return try {
            val attendances = attendanceService.getAttendanceByScheduleId(scheduleId)
            ResponseEntity.ok(attendances)
        } catch (e: Exception) {
            logger.error("Error fetching attendance for schedule $scheduleId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping
    fun saveAttendance(@RequestBody attendance: Attendance): ResponseEntity<Any> {
        return try {
            val savedAttendance = attendanceService.saveAttendance(attendance)
            ResponseEntity.ok(savedAttendance)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error saving attendance", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PutMapping("/{id}")
    fun updateAttendance(@PathVariable id: String, @RequestBody updatedAttendance: Attendance): ResponseEntity<Any> {
        return try {
            val attendance = attendanceService.updateAttendance(id, updatedAttendance)
                ?: return ResponseEntity.status(404).body(mapOf("error" to "Attendance not found"))
            ResponseEntity.ok(attendance)
        } catch (e: Exception) {
            logger.error("Error updating attendance $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteAttendance(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            val deleted = attendanceService.deleteAttendance(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.status(404).body(mapOf("error" to "Attendance not found"))
            }
        } catch (e: Exception) {
            logger.error("Error deleting attendance $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }
}