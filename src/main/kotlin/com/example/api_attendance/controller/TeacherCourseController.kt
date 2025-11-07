package com.example.api_attendance.controller

import com.example.api_attendance.model.Attendance
import com.example.api_attendance.model.Schedule
import com.example.api_attendance.service.AttendanceService
import com.example.api_attendance.service.CourseService
import com.example.api_attendance.service.ScheduleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
class TeacherCourseController(
    private val attendanceService: AttendanceService,
    private val courseService: CourseService,
    private val scheduleService: ScheduleService // Inject ScheduleService
) {

    @GetMapping("/courses")
    fun getCoursesForTeacher(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val teacherId = authentication.name // Extract teacher ID from the authenticated user's details

        // Fetch courses for the teacher
        val courses = attendanceService.getCoursesForTeacher(teacherId)

        // Map courses without duplicating student IDs
        val coursesWithStudents = courses.map { course ->
            mapOf(
                "course" to course
            )
        }

        return ResponseEntity.ok(coursesWithStudents)
    }

    @GetMapping("/courses/student/{studentId}")
    fun getStudentDetailsInCourse(@PathVariable("studentId") studentId: String): ResponseEntity<Map<String, Any>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val teacherId = authentication.name // Extract teacher ID from the authenticated user's details

        // Fetch student details using the studentId
        val studentDetails = courseService.getStudentDetailsById(studentId, teacherId)

        // Construct the response
        val response = mapOf(
            "students" to studentDetails,
            "count" to studentDetails.size
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/schedules/course/{courseId}")
    fun getSchedulesForCourse(@PathVariable("courseId") courseId: String): ResponseEntity<List<Schedule>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val teacherId = authentication.name // Extract teacher ID from the authenticated user's details

        // Validate that the course belongs to the teacher
        val course = courseService.getCourseById(courseId)
            ?: return ResponseEntity.notFound().build()

        if (course.teacherId != teacherId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Fetch schedules for the course
        val schedules = scheduleService.getSchedulesByCourseId(courseId)
        return ResponseEntity.ok(schedules)
    }

    @GetMapping("/attendance/schedule/{courseId}")
    fun getAttendanceByCourse(@PathVariable("courseId") courseId: String): ResponseEntity<List<Map<String, Any?>>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val teacherId = authentication.name // Extract teacher ID from the authenticated user's details

        // Validate that the course belongs to the teacher
        val course = courseService.getCourseById(courseId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        if (course.teacherId != teacherId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Fetch attendance records for the course
        val attendanceRecords = attendanceService.getAttendanceByCourseId(courseId)

        // Transform attendance records into the desired JSON structure
        val response = attendanceRecords.map { attendance ->
            mapOf(
                "courseId" to attendance.courseId,
                "createdAt" to attendance.createdAt,
                "date" to attendance.date,
                "notes" to attendance.notes,
                "scheduleId" to attendance.scheduleId,
                "status" to attendance.status.name,
                "studentId" to attendance.studentId,
                "studentFullName" to attendance.studentFullName,
                "time" to attendance.time,
                "updatedAt" to attendance.updatedAt,
                "verifiedBy" to attendance.verifiedBy
            )
        }

        return ResponseEntity.ok(response)
    }
}