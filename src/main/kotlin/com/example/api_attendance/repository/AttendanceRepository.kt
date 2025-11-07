package com.example.api_attendance.repository

import com.example.api_attendance.model.Attendance
import com.google.cloud.firestore.Firestore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class AttendanceRepository(private val firestore: Firestore) {

    private val logger = LoggerFactory.getLogger(AttendanceRepository::class.java)

    private val collection = firestore.collection("attendances")

    fun findById(id: String): Attendance? {
        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(Attendance::class.java) else null
    }

    fun findByScheduleId(scheduleId: String): List<Attendance> {
        logger.info("Fetching attendance records for scheduleId: {}", scheduleId)
        val snapshot = collection.whereEqualTo("scheduleId", scheduleId).get().get()
        logger.info("Query returned {} records", snapshot.documents.size)
        if (snapshot.documents.isEmpty()) {
            logger.warn("No attendance records found for scheduleId: {}", scheduleId)
        }
        return snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
    }

    fun findByStudentAndDate(studentId: String, date: LocalDate): List<Attendance> {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)

        val snapshot = collection
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("date", dateStr)
            .get()
            .get()
        return snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
    }

    fun findByCourseAndDate(courseId: String, date: LocalDate): List<Attendance> {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)

        val snapshot = collection
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("date", dateStr)
            .get()
            .get()
        return snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
    }

    fun findByScheduleAndStudent(scheduleId: String, studentId: String): Attendance? {
        logger.info("Querying attendance for scheduleId: {} and studentId: {}", scheduleId, studentId)
        val snapshot = collection
            .whereEqualTo("scheduleId", scheduleId)
            .whereEqualTo("studentId", studentId)
            .get()
            .get()
        logger.info("Query returned {} records", snapshot.documents.size)
        return snapshot.documents.firstOrNull()?.toObject(Attendance::class.java)
    }

    fun findUnverifiedAttendance(courseId: String): List<Attendance> {
        val snapshot = collection
            .whereEqualTo("course_id", courseId)
            .whereEqualTo("verified_by", null)
            .get()
            .get()
        return snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
    }

    fun save(attendance: Attendance): Attendance {
        attendance.updatedAt = System.currentTimeMillis()
        if (attendance.id.isBlank()) {
            val docRef = collection.document()
            attendance.id = docRef.id
            attendance.createdAt = System.currentTimeMillis()
            docRef.set(attendance).get()
        } else {
            collection.document(attendance.id).set(attendance).get()
        }
        logger.info("Saved attendance: ${attendance.id}")
        return attendance
    }

    fun deleteById(id: String) {
        collection.document(id).delete().get()
        logger.info("Deleted attendance: $id")
    }

    fun verifyAttendance(id: String, verifierId: String): Attendance? {
        val attendance = findById(id) ?: return null
        attendance.verifiedBy = verifierId
        return save(attendance)
    }

    fun findByCourseId(courseId: String): List<Attendance> {
        logger.info("Fetching attendance records for courseId: {}", courseId)
        val snapshot = collection.whereEqualTo("courseId", courseId).get().get()
        logger.info("Query returned {} records", snapshot.documents.size)
        if (snapshot.documents.isEmpty()) {
            logger.warn("No attendance records found for courseId: {}", courseId)
        }
        return snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
    }

    fun findByCourseAndStudent(courseId: String, studentId: String): List<Attendance> {
        logger.info("Querying attendance for courseId={} and studentId={}", courseId, studentId)
        val snapshot = collection
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("studentId", studentId)
            .get()
            .get()
        logger.info("Documents retrieved: {}", snapshot.documents.map { it.data })
        return snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
    }
}