package com.example.api_attendance.controller

import com.example.api_attendance.service.FirestoreTestService
import com.example.api_attendance.service.ScheduleService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalDebugController(
    private val scheduleService: ScheduleService,
    private val firestoreTestService: FirestoreTestService
) {
    private val logger = LoggerFactory.getLogger(InternalDebugController::class.java)

    @GetMapping("/internal/debug/schedules/course/{courseId}")
    fun debugSchedulesForCourse(@PathVariable courseId: String): ResponseEntity<Any> {
        return try {
            val schedules = try { scheduleService.getSchedulesForCourse(courseId) } catch (e: Exception) { listOf<Map<String,Any>>() }
            val fsStatus = try { firestoreTestService.testConnection() } catch (e: Exception) { "Failed to check Firestore: ${e.message}" }
            ResponseEntity.ok(mapOf("firestoreStatus" to fsStatus, "schedules" to schedules))
        } catch (e: Exception) {
            logger.error("Internal debug error for course $courseId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal debug error"))
        }
    }
}
