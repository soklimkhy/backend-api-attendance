package com.example.api_attendance.service

import com.example.api_attendance.dto.AttendanceRecord
import com.example.api_attendance.dto.AttendanceUpdateRequest
import com.example.api_attendance.model.Attendance
import com.example.api_attendance.model.Course
import com.example.api_attendance.repository.AttendanceRepository
import com.example.api_attendance.repository.CourseRepository
import com.example.api_attendance.repository.ScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val courseRepository: CourseRepository,
    private val scheduleRepository: ScheduleRepository
) {
    private val logger = LoggerFactory.getLogger(AttendanceService::class.java)

    /**
     * Student checks in for a given schedule. If an attendance record exists for schedule+student+date,
     * update it, otherwise create a new record.
     */
    /**
     * Student checks in for a given schedule. If an attendance record exists for schedule+student+date,
     * update it, otherwise create a new record. Validates course enrollment and schedule.
     */
    fun checkIn(scheduleId: String, courseId: String, studentId: String): Attendance {
        // Validate course exists and student is enrolled
        val course = courseRepository.findById(courseId)
            ?: throw IllegalArgumentException("Course not found")
        
        require(course.studentIds.contains(studentId)) {
            "Student is not enrolled in this course"
        }

        // Validate schedule exists and belongs to course
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")
            
        require(schedule.courseId == courseId) {
            "Schedule does not belong to the specified course"
        }

        val date = LocalDate.now()
        val dateStr = date.toString()

        // Ensure `existing` is properly resolved using AttendanceRepository
        val existingAttendance = attendanceRepository.findByScheduleAndStudent(scheduleId, studentId)
        val now = System.currentTimeMillis()

        val attendance = if (existingAttendance != null && existingAttendance.date == dateStr) {
            // update check-in
            existingAttendance.time = now
            existingAttendance.status = Attendance.AttendanceStatus.PRESENT
            existingAttendance
        } else {
            Attendance(
                id = "",
                scheduleId = scheduleId,
                courseId = courseId,
                studentId = studentId,
                date = dateStr,
                status = Attendance.AttendanceStatus.PRESENT,
                time = now
            )
        }

        attendance.validate()
        val saved = attendanceRepository.save(attendance)
        logger.info("Student checked in: studentId={}, scheduleId={}, attendanceId={}", studentId, scheduleId, saved.id)
        return saved
    }

    fun checkOut(attendanceId: String): Attendance? {
        val attendance = attendanceRepository.findById(attendanceId) ?: return null
        val now = System.currentTimeMillis()
        attendance.time = now
        // If check-in was missing, set it to now as well
        if (attendance.time == null) attendance.time = now

        attendance.validate()
        val saved = attendanceRepository.save(attendance)
        logger.info("Student checked out: attendanceId={}", attendanceId)
        return saved
    }

    fun verify(attendanceId: String, verifierId: String): Attendance? {
        val saved = attendanceRepository.verifyAttendance(attendanceId, verifierId)
        if (saved != null) logger.info("Attendance verified: id={}, verifier={}", attendanceId, verifierId)
        return saved
    }

    fun getAttendanceBySchedule(scheduleId: String, teacherId: String): List<Attendance> {
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        val course = courseRepository.findById(schedule.courseId)
            ?: throw IllegalArgumentException("Course not found")

        require(course.teacherId == teacherId) {
            "Teacher does not have access to this schedule"
        }

        return attendanceRepository.findByScheduleId(scheduleId)
    }

    fun getAttendanceByStudentAndDate(studentId: String, date: LocalDate): List<Attendance> = attendanceRepository.findByStudentAndDate(studentId, date)

    fun getAttendanceByCourseAndDate(courseId: String, date: LocalDate): List<Attendance> = attendanceRepository.findByCourseAndDate(courseId, date)

    /**
     * Get or create an attendance record for a schedule and student. 
     * Validates course enrollment and schedule before creating.
     */
    fun getOrCreateAttendanceForScheduleAndStudent(scheduleId: String, courseId: String, studentId: String): Attendance {
        // Validate course exists and student is enrolled
        val course = courseRepository.findById(courseId)
            ?: throw IllegalArgumentException("Course not found")
        
        require(course.studentIds.contains(studentId)) {
            "Student is not enrolled in this course"
        }

        // Validate schedule exists and belongs to course
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")
            
        require(schedule.courseId == courseId) {
            "Schedule does not belong to the specified course"
        }

        return attendanceRepository.findByScheduleAndStudent(scheduleId, studentId)
            ?: Attendance(
                id = "",
                scheduleId = scheduleId,
                courseId = courseId,
                studentId = studentId,
                date = LocalDate.now().toString(),
                status = Attendance.AttendanceStatus.ABSENT
            ).also { attendanceRepository.save(it) }
    }
    
    /**
     * Bulk verify multiple attendance records at once
     */
    fun bulkVerifyAttendance(attendanceIds: List<String>, verifierId: String): List<Attendance> {
        return attendanceIds.mapNotNull { id ->
            attendanceRepository.verifyAttendance(id, verifierId)
                ?.also { logger.info("Attendance verified: id={}, verifier={}", id, verifierId) }
        }
    }

    /**
     * Mark student as late for a schedule
     */
    fun markAsLate(attendanceId: String, notes: String? = null): Attendance? {
        val attendance = attendanceRepository.findById(attendanceId) ?: return null
        attendance.status = Attendance.AttendanceStatus.LATE
        attendance.notes = notes
        // Do not re-run full validation here to avoid failing when only status/notes are changed
        val saved = attendanceRepository.save(attendance)
        logger.info("Student marked as late: attendanceId={}", attendanceId)
        return saved
    }
    
    /**
     * Mark student as excused for a schedule
     */
    fun markAsExcused(attendanceId: String, notes: String? = null): Attendance? {
        val attendance = attendanceRepository.findById(attendanceId) ?: return null
        attendance.status = Attendance.AttendanceStatus.EXCUSED
        attendance.notes = notes
        // Skip full validation for a simple status change
        val saved = attendanceRepository.save(attendance)
        logger.info("Student marked as excused: attendanceId={}", attendanceId)
        return saved
    }

    fun getAttendanceById(id: String): Attendance? {
        return attendanceRepository.findById(id)
    }

    fun getAttendanceByScheduleId(scheduleId: String): List<Attendance> {
        return attendanceRepository.findByScheduleId(scheduleId)
    }

    fun saveAttendance(attendance: Attendance): Attendance {
        attendance.validate()
        return attendanceRepository.save(attendance)
    }

    fun deleteAttendanceById(id: String) {
        attendanceRepository.deleteById(id)
    }

    fun verifyAttendance(id: String, verifierId: String): Attendance? {
        val attendance = attendanceRepository.findById(id) ?: return null
        attendance.verifiedBy = verifierId
        attendance.updatedAt = System.currentTimeMillis()
        return attendanceRepository.save(attendance)
    }

    /**
     * Validate that no attendance record exists for the given schedule and student.
     */
    fun validateUniqueAttendance(scheduleId: String, studentId: String) {
        val existingAttendance = attendanceRepository.findByScheduleAndStudent(scheduleId, studentId)
        require(existingAttendance == null) {
            "Attendance already exists for this schedule and student"
        }
    }

    fun updateAttendance(id: String, updatedAttendance: Attendance): Attendance? {
        val existingAttendance = attendanceRepository.findById(id) ?: return null
        val updated = existingAttendance.copy(
            scheduleId = updatedAttendance.scheduleId,
            courseId = updatedAttendance.courseId,
            studentId = updatedAttendance.studentId,
            date = updatedAttendance.date,
            status = updatedAttendance.status,
            time = updatedAttendance.time,
            verifiedBy = updatedAttendance.verifiedBy,
            notes = updatedAttendance.notes,
            updatedAt = System.currentTimeMillis()
        )
        return attendanceRepository.save(updated)
    }

    fun deleteAttendance(id: String): Boolean {
        val existingAttendance = attendanceRepository.findById(id) ?: return false
        attendanceRepository.deleteById(existingAttendance.id)
        return true
    }

    fun getAttendanceForTeacher(teacherId: String): List<Attendance> {
        val courses = courseRepository.findByTeacherId(teacherId)
        val courseIds = courses.map { it.id }

        val attendanceRecords = mutableListOf<Attendance>()
        for (courseId in courseIds) {
            attendanceRecords.addAll(attendanceRepository.findByCourseAndDate(courseId, LocalDate.now()))
        }
        return attendanceRecords
    }

    fun getAttendanceByCourse(courseId: String, teacherId: String): List<Attendance> {
        // Validate course exists and belongs to the teacher
        val course = courseRepository.findById(courseId)
            ?: throw IllegalArgumentException("Course not found")

        require(course.teacherId == teacherId) {
            "Teacher does not have access to this course"
        }

        // Fetch attendance records for the course
        return attendanceRepository.findByCourseId(courseId)
    }

    fun updateAttendanceBySchedule(scheduleId: String, attendanceUpdates: List<Attendance>) {
        attendanceUpdates.forEach { update ->
            val existing = attendanceRepository.findByScheduleAndStudent(scheduleId, update.studentId)
                ?: throw IllegalArgumentException("Attendance record not found for student: ${update.studentId}")

            existing.status = update.status
            existing.time = update.time
            attendanceRepository.save(existing)
        }
    }

    fun updateAttendanceByCourse(courseId: String, attendanceUpdates: List<Attendance>) {
        attendanceUpdates.forEach { update ->
            val existingRecords = attendanceRepository.findByCourseAndStudent(courseId, update.studentId)

            if (existingRecords.isEmpty()) {
                throw IllegalArgumentException("Attendance record not found for student: ${update.studentId}")
            }

            existingRecords.forEach { existing ->
                existing.apply {
                    status = update.status
                    time = update.time
                }
                attendanceRepository.save(existing)
            }
        }
    }

    fun getCoursesForTeacher(teacherId: String): List<Course> {
        logger.info("Fetching courses for teacherId: $teacherId")
        val courses = courseRepository.findByTeacherId(teacherId)
        logger.info("Courses retrieved: $courses")
        return courses
    }

    fun updateAttendanceForSchedule(attendanceUpdateRequest: AttendanceUpdateRequest, teacherId: String) {
        val schedule = scheduleRepository.findById(attendanceUpdateRequest.scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        val course = courseRepository.findById(schedule.courseId)
            ?: throw IllegalArgumentException("Course not found")

        require(course.teacherId == teacherId) {
            "Teacher does not have access to this schedule"
        }

        attendanceUpdateRequest.attendanceRecords.forEach { update ->
            val existing = attendanceRepository.findByScheduleAndStudent(schedule.id, update.studentId)

            if (existing == null) {
                logger.warn("Attendance record not found for student: ${update.studentId}")
                return@forEach
            }

            val status = try {
                Attendance.AttendanceStatus.valueOf(update.status.uppercase())
            } catch (e: IllegalArgumentException) {
                logger.error("Invalid status value: ${update.status}", e)
                return@forEach
            }

            existing.status = status
            existing.time = update.time
            existing.notes = update.notes
            attendanceRepository.save(existing)
        }
    }

    fun getAttendanceByCourseId(courseId: String): List<Attendance> {
        // Fetch all attendance records for the given course ID
        val attendanceRecords = attendanceRepository.findByCourseId(courseId)

        // Populate studentFullName for each attendance record
        attendanceRecords.forEach { attendance ->
            val student = courseRepository.findStudentById(attendance.studentId)
            attendance.studentFullName = student?.fullName ?: "Unknown"
        }

        return attendanceRecords
    }

    fun getAttendanceByCourseAndStudent(courseId: String, studentId: String): List<Attendance> {
        logger.info("Fetching attendance for courseId: $courseId and studentId: $studentId")
        return attendanceRepository.findByCourseAndStudent(courseId, studentId)
    }
}
