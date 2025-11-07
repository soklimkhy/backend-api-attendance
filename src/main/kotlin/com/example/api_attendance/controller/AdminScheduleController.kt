package com.example.api_attendance.controller

import com.example.api_attendance.model.Schedule
import com.example.api_attendance.repository.UserRepository
import com.example.api_attendance.service.ScheduleService
import com.example.api_attendance.service.FirestoreTestService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

data class ScheduleCreateRequest(
    val dayOfWeek: Int = 1,
    val startTime: String,
    val endTime: String,
    val room: String,
    val type: String = "REGULAR", // REGULAR | MAKEUP | SPECIAL
    val specificDate: String? = null, // YYYY-MM-DD for MAKEUP/SPECIAL
    val notes: String? = null
)

data class ScheduleUpdateRequest(
    val dayOfWeek: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val room: String? = null,
    val type: String? = null,
    val specificDate: String? = null,
    val status: String? = null,
    val notes: String? = null
)

@RestController
@RequestMapping("/api/admin/schedules")
class AdminScheduleController(
    private val scheduleService: ScheduleService,
    private val userRepository: UserRepository,
    private val firestoreTestService: FirestoreTestService
) {
    private val logger = LoggerFactory.getLogger(AdminScheduleController::class.java)

    private fun requireAdmin() {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) throw IllegalArgumentException("Unauthorized")
        val currentUserId = auth.name ?: throw IllegalArgumentException("Unauthorized")
        val currentUser = userRepository.findById(currentUserId) ?: throw IllegalArgumentException("Unauthorized")
        if (currentUser.role != "ADMIN") throw IllegalArgumentException("Forbidden")
    }

    @GetMapping("/course/{courseId}")
    fun listSchedulesForCourse(@PathVariable courseId: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val schedules = scheduleService.getSchedulesForCourse(courseId)
            ResponseEntity.ok(schedules)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error listing schedules for course $courseId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping("/course/{courseId}")
    fun createScheduleForCourse(@PathVariable courseId: String, @RequestBody req: ScheduleCreateRequest): ResponseEntity<Any> {
        return try {
            requireAdmin()
            // build Schedule
            val schedule = Schedule(
                courseId = courseId,
                dayOfWeek = req.dayOfWeek,
                startTime = req.startTime,
                endTime = req.endTime,
                room = req.room,
                type = Schedule.ScheduleType.valueOf(req.type.uppercase()),
                specificDate = req.specificDate,
                notes = req.notes,
                createdBy = SecurityContextHolder.getContext().authentication.name ?: "system"
            )
            val saved = scheduleService.createSchedule(schedule)
            ResponseEntity.ok(saved)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error creating schedule for course $courseId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/{id}")
    fun getSchedule(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val s = scheduleService.getSchedule(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Schedule not found"))
            ResponseEntity.ok(s)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error getting schedule $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PutMapping("/{id}")
    fun updateSchedule(@PathVariable id: String, @RequestBody req: ScheduleUpdateRequest): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val existing = scheduleService.getSchedule(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Schedule not found"))
            val updates = Schedule(
                id = existing.id,
                courseId = existing.courseId,
                dayOfWeek = req.dayOfWeek ?: existing.dayOfWeek,
                startTime = req.startTime ?: existing.startTime,
                endTime = req.endTime ?: existing.endTime,
                room = req.room ?: existing.room,
                type = req.type?.let { Schedule.ScheduleType.valueOf(it.uppercase()) } ?: existing.type,
                specificDate = req.specificDate ?: existing.specificDate,
                status = req.status?.let { Schedule.ScheduleStatus.valueOf(it.uppercase()) } ?: existing.status,
                notes = req.notes ?: existing.notes,
                createdBy = existing.createdBy,
                createdAt = existing.createdAt
            )
            val saved = scheduleService.updateSchedule(id, updates)
            ResponseEntity.ok(saved)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error updating schedule $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSchedule(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            scheduleService.deleteSchedule(id)
            ResponseEntity.ok(mapOf("message" to "Schedule deleted"))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error deleting schedule $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping("/{id}/cancel")
    fun cancelSchedule(@PathVariable id: String, @RequestBody(required = false) body: Map<String, String>?): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val notes = body?.get("notes")
            val saved = scheduleService.cancelSchedule(id, notes)
            ResponseEntity.ok(saved)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error cancelling schedule $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping("/{id}/complete")
    fun completeSchedule(@PathVariable id: String, @RequestBody(required = false) body: Map<String, String>?): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val notes = body?.get("notes")
            val saved = scheduleService.completeSchedule(id, notes)
            ResponseEntity.ok(saved)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error completing schedule $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/course/{courseId}/date/{dateStr}")
    fun getActiveSchedulesForDate(@PathVariable courseId: String, @PathVariable dateStr: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val date = LocalDate.parse(dateStr)
            val schedules = scheduleService.getActiveSchedulesForDate(courseId, date)
            ResponseEntity.ok(schedules)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error getting active schedules for course $courseId on $dateStr", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    // Debug endpoint: returns schedules and Firestore connection status to help troubleshooting
    @GetMapping("/debug/course/{courseId}")
    fun debugSchedulesForCourse(@PathVariable courseId: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val schedules = scheduleService.getSchedulesForCourse(courseId)
            val fsStatus = try {
                firestoreTestService.testConnection()
            } catch (e: Exception) {
                "Failed to check Firestore: ${e.message}"
            }
            ResponseEntity.ok(mapOf("firestoreStatus" to fsStatus, "schedules" to schedules))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error debugging schedules for course $courseId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }
}
