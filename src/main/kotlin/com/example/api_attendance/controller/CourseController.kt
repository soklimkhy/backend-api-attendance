package com.example.api_attendance.controller

import com.example.api_attendance.model.*
import com.example.api_attendance.repository.UserRepository
import com.example.api_attendance.service.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseService: CourseService,
    private val userRepository: UserRepository,
    private val scheduleService: ScheduleService,
    private val attendanceService: AttendanceService
) {
    private val logger = LoggerFactory.getLogger(CourseController::class.java)

    data class CourseRequest(
        val code: String,
        val name: String,
        val description: String,
        val teacherId: String,
        val academicYear: String,
        val semester: String
    )

    data class UpdateCourseRequest(
        val code: String? = null,
        val name: String? = null,
        val description: String? = null,
        val teacherId: String? = null,
        val academicYear: String? = null,
        val semester: String? = null,
        val active: Boolean? = null
    )

    private fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        val roles = authentication.authorities.map { it.authority }
        logger.info("Authenticated user roles: {}", roles)

        return userRepository.findById(authentication.name)
    }

    private fun verifyViewAccess(): ResponseEntity<Any>? {
        val user = getCurrentUser() ?: return ResponseEntity.status(401)
            .body(mapOf("error" to "Unauthorized"))

        // Allow both admins and teachers to view courses
        if (!user.hasAuthority("COURSE_VIEW") && !user.hasAuthority("ATTENDANCE_VIEW")) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Access denied - Requires admin or teacher privileges"))
        }

        return null
    }

    private fun verifyAdminWrite(): ResponseEntity<Any>? {
        val user = getCurrentUser() ?: return ResponseEntity.status(401)
            .body(mapOf("error" to "Unauthorized"))

        if (!user.hasAuthority("COURSE_CREATE") && !user.hasAuthority("COURSE_EDIT")) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Access denied - Requires admin privileges"))
        }

        return null
    }

    @GetMapping
    fun listCourses(
        @RequestParam(required = false) academicYear: String?,
        @RequestParam(required = false) semester: String?,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean
    ): ResponseEntity<Any> {
        // Verify view access
        verifyViewAccess()?.let { return it }

        val user = getCurrentUser()!!
        
        val courses = if (user.hasAuthority("COURSE_VIEW")) {
            // Admin sees all courses
            if (activeOnly) {
                courseService.listActive(academicYear, semester)
            } else {
                courseService.listActive(academicYear, semester)
            }
        } else {
            // Teacher sees only their courses
            courseService.getCoursesByTeacher(user.id)
        }

        return ResponseEntity.ok(mapOf(
            "courses" to courses,
            "total" to courses.size
        ))
    }

    @GetMapping("/{id}")
    fun getCourse(@PathVariable id: String): ResponseEntity<Any> {
        // Verify view access
        verifyViewAccess()?.let { return it }

        val user = getCurrentUser()!!
        val course = courseService.getCourse(id)
            ?: return ResponseEntity.notFound().build()

        // Teachers can only view their own courses
        if (!user.hasAuthority("COURSE_VIEW") && course.teacherId != user.id) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Access denied - Not the course instructor"))
        }

        return ResponseEntity.ok(mapOf("course" to course))
    }

    @PostMapping
    fun createCourse(@RequestBody req: CourseRequest): ResponseEntity<Any> {
        // Verify admin write access
        verifyAdminWrite()?.let { return it }

        // Verify instructor exists and is a teacher
        val teacher = userRepository.findById(req.teacherId)
            ?: return ResponseEntity.badRequest()
                .body(mapOf("error" to "Teacher not found"))

        if (teacher.role != "TEACHER") {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "User must be a teacher"))
        }

        // Create course
        val course = Course(
            code = req.code,
            name = req.name,
            description = req.description,
            teacherId = req.teacherId,
            academicYear = req.academicYear,
            semester = req.semester
        )

        return try {
            val saved = courseService.createCourse(course)
            logger.info("Course created: {}", saved.id)
            ResponseEntity.ok(mapOf("course" to saved))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/{id}")
    fun updateCourse(@PathVariable id: String, @RequestBody req: UpdateCourseRequest): ResponseEntity<Any> {
        // Verify admin write access
        verifyAdminWrite()?.let { return it }

        // If teacher ID is provided, verify teacher exists and is a teacher
        if (req.teacherId != null) {
            val teacher = userRepository.findById(req.teacherId)
                ?: return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Teacher not found"))

            if (teacher.role != "TEACHER") {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "User must be a teacher"))
            }
        }

        // Create update object with only provided fields
        val updates = Course(
            id = id,
            code = req.code ?: "",
            name = req.name ?: "",
            description = req.description ?: "",
            teacherId = req.teacherId ?: "",
            academicYear = req.academicYear ?: "",
            semester = req.semester ?: "",
            active = req.active ?: true
        )

        return try {
            val updated = courseService.updateCourse(id, updates)
                ?: return ResponseEntity.notFound().build()

            logger.info("Course updated: {}", updated.id)
            ResponseEntity.ok(mapOf("course" to updated))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCourse(@PathVariable id: String): ResponseEntity<Any> {
        // Verify admin write access and delete permission
        val user = getCurrentUser() ?: return ResponseEntity.status(401)
            .body(mapOf("error" to "Unauthorized"))

        if (!user.hasAuthority("COURSE_DELETE")) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Access denied - Requires admin delete privileges"))
        }

        // Verify course exists first
        if (courseService.getCourse(id) == null) {
            return ResponseEntity.notFound().build()
        }

        courseService.deleteCourse(id)
        logger.info("Course deleted: {}", id)
        return ResponseEntity.ok(mapOf("message" to "Course deleted successfully"))
    }

    @PostMapping("/{id}/students")
    fun addStudent(
        @PathVariable id: String,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<Any> {
        // Verify admin write access
        verifyAdminWrite()?.let { return it }

        val studentId = request["studentId"]
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Student ID is required"))

        // Verify student exists and has STUDENT role
        val student = userRepository.findById(studentId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Student not found"))

        if (student.role != "STUDENT") {
            return ResponseEntity.badRequest().body(mapOf("error" to "User must be a student"))
        }

        val updated = courseService.addStudentToCourse(id, studentId)
            ?: return ResponseEntity.notFound().build()

        logger.info("Student {} added to course {}", studentId, id)
        return ResponseEntity.ok(mapOf("course" to updated))
    }

    @DeleteMapping("/{id}/students/{studentId}")
    fun removeStudent(
        @PathVariable id: String,
        @PathVariable studentId: String
    ): ResponseEntity<Any> {
        // Verify admin write access
        verifyAdminWrite()?.let { return it }

        val updated = courseService.removeStudentFromCourse(id, studentId)
            ?: return ResponseEntity.notFound().build()

        logger.info("Student {} removed from course {}", studentId, id)
        return ResponseEntity.ok(mapOf("course" to updated))
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/courses")
    fun getCourses(): ResponseEntity<List<Course>> {
        val courses = courseService.getAllCourses()
        return ResponseEntity.ok(courses)
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/schedules")
    fun getSchedules(): ResponseEntity<List<Schedule>> {
        val schedules = scheduleService.getAllSchedules()
        return ResponseEntity.ok(schedules)
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/attendance/schedule/{scheduleId}")
    fun getAttendanceBySchedule(@PathVariable scheduleId: String): ResponseEntity<List<Attendance>> {
        val teacherId = getCurrentUser()?.id ?: return ResponseEntity.status(401).build()
        val attendanceRecords = attendanceService.getAttendanceBySchedule(scheduleId, teacherId)
        return ResponseEntity.ok(attendanceRecords)
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/attendance/teacher")
    fun getAttendanceForTeacher(): ResponseEntity<List<Attendance>> {
        val teacherId = getCurrentUser()?.id ?: return ResponseEntity.status(401).build()
        val attendanceRecords = attendanceService.getAttendanceForTeacher(teacherId)
        return ResponseEntity.ok(attendanceRecords)
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/attendance/course/{courseId}")
    fun getAttendanceByCourse(@PathVariable courseId: String): ResponseEntity<List<Attendance>> {
        val teacherId = getCurrentUser()?.id ?: return ResponseEntity.status(401).build()
        val attendanceRecords = attendanceService.getAttendanceByCourse(courseId, teacherId)
        return ResponseEntity.ok(attendanceRecords)
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/attendance/schedule/{scheduleId}")
    fun updateAttendanceBySchedule(@PathVariable scheduleId: String, @RequestBody attendanceUpdates: List<Attendance>): ResponseEntity<Void> {
        attendanceService.updateAttendanceBySchedule(scheduleId, attendanceUpdates)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/attendance/course/{courseId}")
    fun updateAttendanceByCourse(@PathVariable courseId: String, @RequestBody attendanceUpdates: List<Attendance>): ResponseEntity<Void> {
        attendanceService.updateAttendanceByCourse(courseId, attendanceUpdates)
        return ResponseEntity.noContent().build()
    }
}