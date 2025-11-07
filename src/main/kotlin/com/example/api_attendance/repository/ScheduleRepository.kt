package com.example.api_attendance.repository

import com.example.api_attendance.model.Schedule
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

@Repository
class ScheduleRepository(private val firestore: Firestore? = null) {
    private val logger = LoggerFactory.getLogger(ScheduleRepository::class.java)
    
    private val inMemory = mutableMapOf<String, Schedule>()
    private val collection = firestore?.collection("schedules")

    fun findById(id: String): Schedule? {
        // test/in-memory mode
        if (collection == null) {
            return inMemory[id]
        }

        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(Schedule::class.java) else null
    }

    fun findByCourseId(courseId: String): List<Schedule> {
        // test/in-memory mode
        if (collection == null) {
            val schedules = inMemory.values.filter { it.courseId == courseId }
            logger.info("In-memory mode: Found ${schedules.size} schedules for courseId=$courseId")
            return schedules
        }

        val snapshot = collection
            .whereEqualTo("courseId", courseId) // Updated field name to match Firestore
            .get()
            .get()
        val schedules = snapshot.documents.mapNotNull { it.toObject(Schedule::class.java) }
        logger.info("Firestore mode: Found ${schedules.size} schedules for courseId=$courseId")
        return schedules
    }

    fun findActiveSchedules(courseId: String, date: LocalDate): List<Schedule> {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)
        val dayOfWeek = date.dayOfWeek.value

        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.filter { schedule ->
                schedule.courseId == courseId &&
                schedule.status == Schedule.ScheduleStatus.ACTIVE &&
                (
                    (schedule.type == Schedule.ScheduleType.REGULAR && schedule.dayOfWeek == dayOfWeek) ||
                    (schedule.type != Schedule.ScheduleType.REGULAR && schedule.specificDate == dateStr)
                )
            }
        }

        // Query for regular schedules on this day of week
        val regularQuery = collection
            .whereEqualTo("course_id", courseId)
            .whereEqualTo("status", Schedule.ScheduleStatus.ACTIVE.name)
            .whereEqualTo("type", Schedule.ScheduleType.REGULAR.name)
            .whereEqualTo("day_of_week", dayOfWeek)

        // Query for special/makeup classes on this specific date
        val specialQuery = collection
            .whereEqualTo("course_id", courseId)
            .whereEqualTo("status", Schedule.ScheduleStatus.ACTIVE.name)
            .whereNotEqualTo("type", Schedule.ScheduleType.REGULAR.name)
            .whereEqualTo("specific_date", dateStr)

        val regularSchedules = regularQuery.get().get()
            .documents.mapNotNull { it.toObject(Schedule::class.java) }
        
        val specialSchedules = specialQuery.get().get()
            .documents.mapNotNull { it.toObject(Schedule::class.java) }

        return regularSchedules + specialSchedules
    }

    fun save(schedule: Schedule): Schedule {
        schedule.validate() // Validate before saving

        // test/in-memory mode
        if (collection == null) {
            if (schedule.id.isBlank()) schedule.id = "schedule_${inMemory.size + 1}"
            schedule.updatedAt = System.currentTimeMillis()
            inMemory[schedule.id] = schedule
            return schedule
        }

        val docRef = if (schedule.id.isBlank()) collection.document() else collection.document(schedule.id)
        if (schedule.id.isBlank()) schedule.id = docRef.id
        schedule.updatedAt = System.currentTimeMillis()
        docRef.set(schedule).get()
        return schedule
    }

    fun delete(id: String) {
        // test/in-memory mode
        if (collection == null) {
            inMemory.remove(id)
            return
        }

        collection.document(id).delete().get()
    }

    fun cancelSchedule(id: String): Schedule? {
        val schedule = findById(id) ?: return null
        schedule.status = Schedule.ScheduleStatus.CANCELLED
        return save(schedule)
    }

    fun completeSchedule(id: String): Schedule? {
        val schedule = findById(id) ?: return null
        schedule.status = Schedule.ScheduleStatus.COMPLETED
        return save(schedule)
    }

    fun findAll(): List<Schedule> {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.toList()
        }

        val snapshot = collection.get().get()
        return snapshot.documents.mapNotNull { it.toObject(Schedule::class.java) }
    }

    fun findSchedulesByUserId(userId: String): List<Schedule> {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.filter { false } // Removed studentIds reference
        }

        val snapshot = collection
            .whereArrayContains("studentIds", userId) // Removed invalid query
            .get()
            .get()

        return snapshot.documents.mapNotNull { it.toObject(Schedule::class.java) }
    }

    fun findSchedulesByCourseId(courseId: String): List<Schedule> {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.filter { it.courseId == courseId }
        }

        logger.info("Querying schedules for courseId={}", courseId) // Corrected field name in logging
        logger.info("Firestore collection: {}", collection.path) // Removed unnecessary safe call
        logger.info("Firestore query parameters: courseId={}", courseId)

        val snapshot = collection
            .whereEqualTo("courseId", courseId) // Corrected field name to match Firestore
            .get()
            .get()

        logger.info("Firestore documents in 'schedules' collection:")
        snapshot.documents.forEach { doc ->
            logger.info("Document ID: {}, Data: {}", doc.id, doc.data)
        }

        logger.info("Found {} schedules in Firestore", snapshot.documents.size)

        return snapshot.documents.mapNotNull { it.toObject(Schedule::class.java) }
    }
}