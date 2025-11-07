package com.example.api_attendance.config

import com.example.api_attendance.model.*
import com.example.api_attendance.repository.*
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DatabaseInitializer(
    private val userRepository: UserRepository,
    private val encoder: BCryptPasswordEncoder,
    private val courseRepository: CourseRepository,
    private val scheduleRepository: ScheduleRepository,
    private val attendanceRepository: AttendanceRepository
) {
    private val logger = LoggerFactory.getLogger(DatabaseInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun initializeDatabase() {
        // Check if admin exists
        if (userRepository.findByUsername("superadmin") == null) {
            // Create initial admin user
            val adminUser = User(
                username = "superadmin",
                password = encoder.encode("superadmin"), // Change this in production
                email = "admin@example.com",
                fullName = "Super Admin",
                role = Role.ADMIN.name,
                authorities = Role.ADMIN.authorities,
                active = true,
                emailVerified = true
            )

            try {
                userRepository.save(adminUser)
                logger.info("Created initial admin user: {}", adminUser.username)
            } catch (e: Exception) {
                logger.error("Failed to create initial admin user", e)
            }
        }

        // Check if test teacher exists
        if (userRepository.findByUsername("superteacher") == null) {
            // Create initial teacher user
            val teacherUser = User(
                username = "superteacher",
                password = encoder.encode("superteacher"), // Change this in production
                email = "teacher@example.com",
                fullName = "Test Teacher",
                role = Role.TEACHER.name,
                authorities = Role.TEACHER.authorities,
                active = true,
                emailVerified = true
            )

            try {
                userRepository.save(teacherUser)
                logger.info("Created initial teacher user: {}", teacherUser.username)
            } catch (e: Exception) {
                logger.error("Failed to create initial teacher user", e)
            }
        }
    }
}