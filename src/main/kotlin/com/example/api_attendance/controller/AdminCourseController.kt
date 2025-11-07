package com.example.api_attendance.controller

import com.example.api_attendance.model.Course
import com.example.api_attendance.repository.CourseRepository
import com.example.api_attendance.repository.UserRepository
import com.example.api_attendance.service.CourseService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class CourseCreateRequest(
    val code: String,
    val name: String,
    val description: String = "",
    val teacherId: String,
    val academicYear: String,
    val semester: String,
    val studentIds: List<String> = listOf()
)

data class CourseUpdateRequest(
    val code: String? = null,
    val name: String? = null,
    val description: String? = null,
    val teacherId: String? = null,
    val academicYear: String? = null,
    val semester: String? = null,
    val studentIds: List<String>? = null,
    val active: Boolean? = null
)

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
class AdminCourseController(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(AdminCourseController::class.java)

    private fun requireAdmin(): String {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) throw IllegalArgumentException("Unauthorized")
        val currentUserId = auth.name ?: throw IllegalArgumentException("Unauthorized")
        val currentUser = userRepository.findById(currentUserId) ?: throw IllegalArgumentException("Unauthorized")
        if (currentUser.role != "ADMIN") throw IllegalArgumentException("Forbidden")
        return currentUserId
    }

    @GetMapping
    fun listCourses(): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val courses = courseRepository.findActive()
            ResponseEntity.ok(courses)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error listing courses", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/{id}")
    fun getCourse(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            ResponseEntity.ok(course)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error getting course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping
    fun createCourse(@RequestBody req: CourseCreateRequest): ResponseEntity<Any> {
        return try {
            requireAdmin()
            // Validate instructor is a teacher
            val instructor = userRepository.findById(req.teacherId)
            if (instructor == null || instructor.role != "TEACHER") {
                return ResponseEntity.badRequest().body(mapOf("error" to "Instructor must be a valid teacher user id"))
            }
            // Validate students are all student role
            val invalidStudents = req.studentIds.filter {
                val u = userRepository.findById(it)
                u == null || u.role != "STUDENT"
            }
            if (invalidStudents.isNotEmpty()) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid student user ids: $invalidStudents"))
            }
            val course = Course(
                code = req.code,
                name = req.name,
                description = req.description,
                teacherId = req.teacherId,
                academicYear = req.academicYear,
                semester = req.semester,
                studentIds = req.studentIds
            )
            course.validate()
            ResponseEntity.ok(courseRepository.save(course))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error creating course", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PutMapping("/{id}")
    fun updateCourse(@PathVariable id: String, @RequestBody req: CourseUpdateRequest): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            req.code?.let { course.code = it }
            req.name?.let { course.name = it }
            req.description?.let { course.description = it }
            req.teacherId?.let {
                val instructor = userRepository.findById(it)
                if (instructor == null || instructor.role != "TEACHER") {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Instructor must be a valid teacher user id"))
                }
                course.teacherId = it
            }
            req.academicYear?.let { course.academicYear = it }
            req.semester?.let { course.semester = it }
            req.studentIds?.let { ids ->
                val invalidStudents = ids.filter {
                    val u = userRepository.findById(it)
                    u == null || u.role != "STUDENT"
                }
                if (invalidStudents.isNotEmpty()) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid student user ids: $invalidStudents"))
                }
                course.studentIds = ids
            }
            req.active?.let { course.active = it }
            course.updatedAt = System.currentTimeMillis()
            course.validate()
            ResponseEntity.ok(courseRepository.save(course))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error updating course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCourse(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            course.active = false
            course.updatedAt = System.currentTimeMillis()
            courseRepository.save(course)
            ResponseEntity.ok(mapOf("message" to "Course disabled"))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error deleting course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    // --- Student management within a course ---
    data class StudentListRequest(val studentIds: List<String>)

    @GetMapping("/{id}/students")
    fun listCourseStudents(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            val students = course.studentIds.mapNotNull { userRepository.findById(it)?.toResponseDTO() }
            ResponseEntity.ok(mapOf("students" to students, "count" to students.size))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error listing students for course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping("/{id}/students")
    fun addStudentsToCourse(@PathVariable id: String, @RequestBody req: StudentListRequest): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            val invalid = mutableListOf<String>()
            req.studentIds.forEach { sid ->
                val u = userRepository.findById(sid)
                if (u == null || u.role != "STUDENT") invalid.add(sid)
            }
            if (invalid.isNotEmpty()) return ResponseEntity.badRequest().body(mapOf("error" to "Invalid student ids: $invalid"))

            // Add unique
            val current = course.studentIds.toMutableList()
            req.studentIds.forEach { if (!current.contains(it)) current.add(it) }
            course.studentIds = current
            course.updatedAt = System.currentTimeMillis()
            courseRepository.save(course)
            ResponseEntity.ok(mapOf("students" to course.studentIds))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error adding students to course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PutMapping("/{id}/students")
    fun replaceStudentsInCourse(@PathVariable id: String, @RequestBody req: StudentListRequest): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            val invalid = req.studentIds.filter { sid ->
                val u = userRepository.findById(sid)
                u == null || u.role != "STUDENT"
            }
            if (invalid.isNotEmpty()) return ResponseEntity.badRequest().body(mapOf("error" to "Invalid student ids: $invalid"))
            course.studentIds = req.studentIds.distinct()
            course.updatedAt = System.currentTimeMillis()
            courseRepository.save(course)
            ResponseEntity.ok(mapOf("students" to course.studentIds))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error replacing students in course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @DeleteMapping("/{id}/students/{studentId}")
    fun removeStudentFromCourse(@PathVariable id: String, @PathVariable studentId: String): ResponseEntity<Any> {
        return try {
            requireAdmin()
            val course = courseRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "Course not found"))
            if (!course.studentIds.contains(studentId)) return ResponseEntity.status(404).body(mapOf("error" to "Student not found in course"))
            course.studentIds = course.studentIds.filter { it != studentId }
            course.updatedAt = System.currentTimeMillis()
            courseRepository.save(course)
            ResponseEntity.ok(mapOf("message" to "Student removed"))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error removing student $studentId from course $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }
}

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('STUDENT')")
class UserCourseController(
    private val courseService: CourseService
) {
    private val logger = LoggerFactory.getLogger(UserCourseController::class.java)

    @GetMapping("/course")
    fun getActiveCoursesForStudent(): ResponseEntity<List<Map<String, Any>>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val studentId = authentication.name // Extract student ID from the authenticated user's details

        // Fetch active courses for the student
        val activeCourses = courseService.getActiveCoursesForStudent(studentId)

        logger.info("Fetching active courses for studentId: $studentId")
        logger.info("Active courses fetched: ${activeCourses.size}")

        return ResponseEntity.ok(activeCourses.map { course ->
            mapOf(
                "id" to course.id,
                "name" to course.name,
                "description" to course.description
                // Removed invalid references to startDate and endDate
            )
        })
    }
}