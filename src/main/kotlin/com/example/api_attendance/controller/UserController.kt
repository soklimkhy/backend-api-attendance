package com.example.api_attendance.controller

import com.example.api_attendance.dto.UserResponseDTO
import com.example.api_attendance.model.User
import com.example.api_attendance.repository.UserRepository
import com.example.api_attendance.service.TwoFactorAuthService
import com.example.api_attendance.service.AuthService
import com.example.api_attendance.service.ScheduleService
import com.example.api_attendance.service.AttendanceService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userRepository: UserRepository,
    private val authService: AuthService,
    private val twoFactorService: TwoFactorAuthService,
    private val scheduleService: ScheduleService, // Inject ScheduleService
    private val attendanceService: AttendanceService // Inject AttendanceService
) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    data class TwoFactorVerifyRequest(
        val code: Int
    )

    data class UpdatePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
        val confirmPassword: String
    )

    data class UpdateProfileRequest(
        val username: String? = null,
        val email: String? = null,
        val fullName: String? = null,
        val photoUrl: String? = null,
        val phoneNumber: String? = null,
        val gender: String? = null,
        val dateOfBirth: String? = null
    )

    @GetMapping("/profile")
    fun getProfile(): ResponseEntity<Any> {
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            logger.info("Authentication: {}", authentication)
            
            val userId = authentication.name
            logger.info("UserId from authentication: {}", userId)
            
            val user = userRepository.findById(userId)
            logger.info("Found user: {}", user)
            
            if (user == null) {
                logger.warn("User not found with ID: {}", userId)
                return ResponseEntity.status(404).body(mapOf("error" to "User not found"))
            }

            return ResponseEntity.ok(mapOf("user" to user.toResponseDTO()))
        } catch (e: Exception) {
            logger.error("Error getting profile", e)
            throw e
        }
    }

    @PutMapping("/profile")
    fun updateProfile(@RequestBody req: UpdateProfileRequest): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = authentication.name
        val user = userRepository.findById(userId)
            ?: return ResponseEntity.status(404).body(mapOf("error" to "User not found"))

        // Update only allowed fields
        req.username?.let {
            // If username is changing, ensure uniqueness
            if (it != user.username) {
                val existing = userRepository.findByUsername(it)
                if (existing != null && existing.id != userId) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Username already taken"))
                }
                user.username = it
            }
        }
        req.email?.let { 
            user.email = it
            user.emailVerified = true  // Set email as verified when updated
        }
        req.fullName?.let { user.fullName = it }
        req.photoUrl?.let { user.photoUrl = it }
        req.phoneNumber?.let { 
            user.phoneNumber = it
            user.phoneVerified = true  // Set phone as verified when updated
        }
        req.gender?.let { user.gender = try { com.example.api_attendance.model.Gender.valueOf(it) } catch (e: Exception) { null } }
        req.dateOfBirth?.let { user.dateOfBirth = it }

        // Validate before save — collect all validation errors and return them together
        val errors = user.collectValidationErrors()
        if (errors.isNotEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("errors" to errors))
        }

        // Save and return
        val saved = userRepository.save(user)
        logger.info("User profile updated: {}", userId)
        return ResponseEntity.ok(mapOf("user" to saved.toResponseDTO()))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = authentication.name
        val ok = authService.logout(userId)
        return if (ok) ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
        else ResponseEntity.status(500).body(mapOf("error" to "Logout unavailable"))
    }

    @PutMapping("/password")
    fun updatePassword(@RequestBody req: UpdatePasswordRequest): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val userId = authentication.name
        val user = userRepository.findById(userId)
            ?: return ResponseEntity.status(404).body(mapOf("error" to "User not found"))

        // Validate current password
        val encoder = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
        if (!encoder.matches(req.currentPassword, user.password)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Current password is incorrect"))
        }

        // Validate new password
        if (req.newPassword.length < 8) {
            return ResponseEntity.badRequest().body(mapOf("error" to "New password must be at least 8 characters"))
        }

        // Validate confirm password matches
        if (req.newPassword != req.confirmPassword) {
            return ResponseEntity.badRequest().body(mapOf("error" to "New password and confirm password do not match"))
        }

        // Update password
        user.password = encoder.encode(req.newPassword)
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "Password updated successfully"))
    }

    @GetMapping("/schedule")
    fun getSchedulesForUser(): ResponseEntity<Any> {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            val userId = authentication.name

            // Fetch schedules for the user
            val schedules = scheduleService.getSchedulesForUser(userId)
            ResponseEntity.ok(schedules)
        } catch (e: Exception) {
            logger.error("Error fetching schedules for user", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/schedule/course/{courseId}")
    fun getSchedulesByCourseId(@PathVariable courseId: String): ResponseEntity<Any> {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            val studentId = authentication.name

            logger.info("Fetching schedules for courseId={} and studentId={}", courseId, studentId) // Updated logging

            val schedules = scheduleService.getSchedulesByCourseId(courseId)
            ResponseEntity.ok(schedules)
        } catch (e: Exception) {
            logger.error("Error fetching schedules for courseId=$courseId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/attendance/course/{courseId}")
    fun getAttendanceByCourseId(@PathVariable courseId: String): ResponseEntity<Any> {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
            val studentId = authentication.name

            logger.info("Fetching attendance for courseId={} and studentId={}", courseId, studentId)

            val attendance = attendanceService.getAttendanceByCourseAndStudent(courseId, studentId) // Corrected method reference
            ResponseEntity.ok(attendance)
        } catch (e: Exception) {
            logger.error("Error fetching attendance for courseId=$courseId and studentId", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }
}
