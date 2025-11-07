package com.example.api_attendance.service

import com.example.api_attendance.model.Schedule
import com.example.api_attendance.repository.CourseRepository
import com.example.api_attendance.repository.ScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val courseRepository: CourseRepository
) {
    private val logger = LoggerFactory.getLogger(ScheduleService::class.java)

    /**
     * Create a new schedule
     */
    fun createSchedule(schedule: Schedule): Schedule {
        // Verify course exists
        courseRepository.findById(schedule.courseId)
            ?: throw IllegalArgumentException("Course not found")

        schedule.validate()
        val saved = scheduleRepository.save(schedule)
        logger.info("Created schedule id={}", saved.id)
        return saved
    }

    /**
     * Update an existing schedule
     */
    fun updateSchedule(scheduleId: String, updates: Schedule): Schedule {
        val existing = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        // Verify course exists if changed
        if (updates.courseId != existing.courseId) {
            courseRepository.findById(updates.courseId)
                ?: throw IllegalArgumentException("Course not found")
        }

        // Update fields while preserving creation info
        existing.dayOfWeek = updates.dayOfWeek
        existing.startTime = updates.startTime.takeIf { it.isNotBlank() } ?: existing.startTime
        existing.endTime = updates.endTime.takeIf { it.isNotBlank() } ?: existing.endTime
        existing.room = updates.room.takeIf { it.isNotBlank() } ?: existing.room
        existing.type = updates.type
        existing.specificDate = updates.specificDate ?: existing.specificDate
        existing.status = updates.status
        existing.notes = updates.notes ?: existing.notes

        existing.validate()
        return scheduleRepository.save(existing)
    }

    /**
     * Get a schedule by ID
     */
    fun getSchedule(scheduleId: String): Schedule? {
        return scheduleRepository.findById(scheduleId)
    }

    /**
     * Get all schedules for a course
     */
    fun getSchedulesForCourse(courseId: String): List<Schedule> {
        // Verify course exists
        courseRepository.findById(courseId)
            ?: throw IllegalArgumentException("Course not found")

        return scheduleRepository.findByCourseId(courseId)
    }

    /**
     * Get active schedules for a course on a specific date
     */
    fun getActiveSchedulesForDate(courseId: String, date: LocalDate): List<Schedule> {
        // Verify course exists
        courseRepository.findById(courseId)
            ?: throw IllegalArgumentException("Course not found")

        return scheduleRepository.findActiveSchedules(courseId, date)
    }

    /**
     * Cancel a schedule
     */
    fun cancelSchedule(scheduleId: String, notes: String? = null): Schedule {
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        schedule.status = Schedule.ScheduleStatus.CANCELLED
        schedule.notes = notes ?: "Schedule cancelled"
        val saved = scheduleRepository.save(schedule)
        logger.info("Schedule cancelled: {}", scheduleId)
        return saved
    }

    /**
     * Mark a schedule as completed
     */
    fun completeSchedule(scheduleId: String, notes: String? = null): Schedule {
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        schedule.status = Schedule.ScheduleStatus.COMPLETED
        schedule.notes = notes
        val saved = scheduleRepository.save(schedule)
        logger.info("Schedule completed: {}", scheduleId)
        return saved
    }

    /**
     * Delete a schedule
     */
    fun deleteSchedule(scheduleId: String) {
        // Verify schedule exists
        scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        scheduleRepository.delete(scheduleId)
        logger.info("Deleted schedule id={}", scheduleId)
    }

    /**
     * Create a makeup schedule for a cancelled schedule
     */
    fun createMakeupSchedule(originalScheduleId: String, makeupDate: LocalDate, 
                           startTime: String, endTime: String, room: String, 
                           createdBy: String, notes: String? = null): Schedule {
        val originalSchedule = scheduleRepository.findById(originalScheduleId)
            ?: throw IllegalArgumentException("Original schedule not found")

        if (originalSchedule.status != Schedule.ScheduleStatus.CANCELLED) {
            throw IllegalArgumentException("Can only create makeup schedule for cancelled schedules")
        }

        val makeupSchedule = Schedule(
            courseId = originalSchedule.courseId,
            dayOfWeek = makeupDate.dayOfWeek.value,
            startTime = startTime,
            endTime = endTime,
            room = room,
            type = Schedule.ScheduleType.MAKEUP,
            specificDate = makeupDate.toString(),
            status = Schedule.ScheduleStatus.ACTIVE,
            notes = notes ?: "Makeup class for schedule ${originalScheduleId}",
            createdBy = createdBy
        )

        makeupSchedule.validate()
        val saved = scheduleRepository.save(makeupSchedule)
        logger.info("Makeup schedule created: {} for original schedule: {}", saved.id, originalScheduleId)
        return saved
    }

    /**
     * Get all schedules
     */
    fun getAllSchedules(): List<Schedule> {
        return scheduleRepository.findAll()
    }

    /**
     * Get a schedule for a teacher
     */
    fun getScheduleForTeacher(scheduleId: String, teacherId: String): Schedule {
        val schedule = scheduleRepository.findById(scheduleId)
            ?: throw IllegalArgumentException("Schedule not found")

        val course = courseRepository.findById(schedule.courseId)
            ?: throw IllegalArgumentException("Course not found")

        require(course.teacherId == teacherId) {
            "Teacher does not have access to this schedule"
        }

        return schedule
    }

    /**
     * Get schedules by course ID
     */
    fun getSchedulesByCourseId(courseId: String): List<Schedule> {
        logger.info("Service layer: Fetching schedules for courseId={}", courseId) // Updated logging
        return scheduleRepository.findSchedulesByCourseId(courseId)
    }

    /**
     * Get schedules for a user
     */
    fun getSchedulesForUser(userId: String): List<Schedule> {
        // Fetch schedules for the user
        return scheduleRepository.findSchedulesByUserId(userId)
    }
}
