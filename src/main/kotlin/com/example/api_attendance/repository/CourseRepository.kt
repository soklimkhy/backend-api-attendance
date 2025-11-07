package com.example.api_attendance.repository

import com.example.api_attendance.model.Course
import com.example.api_attendance.model.User
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository
import org.slf4j.LoggerFactory

@Repository
class CourseRepository(private val firestore: Firestore? = null) {
    
    private val inMemory = mutableMapOf<String, Course>()
    private val collection = firestore?.collection("courses")
    private val logger = LoggerFactory.getLogger(CourseRepository::class.java)

    fun findById(id: String): Course? {
        // test/in-memory mode
        if (collection == null) {
            return inMemory[id]
        }

        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(Course::class.java) else null
    }

    fun findByCode(code: String): Course? {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.firstOrNull { it.code == code }
        }

        val snapshot = collection
            .whereEqualTo("code", code)
            .get()
            .get()
        return snapshot.documents.firstOrNull()?.toObject(Course::class.java)
    }

    fun findByTeacherId(teacherId: String): List<Course> {
        logger.info("Querying courses for teacherId: $teacherId")
        // test/in-memory mode
        if (collection == null) {
            val inMemoryCourses = inMemory.values.filter { it.teacherId == teacherId }
            logger.info("In-memory courses found: $inMemoryCourses")
            return inMemoryCourses
        }

        val snapshot = collection
            .whereEqualTo("teacherId", teacherId) // Updated field name to match Firestore
            .get()
            .get()
        val courses = snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
        logger.info("Courses retrieved from Firestore: $courses")
        return courses
    }

    fun findByStudentId(studentId: String): List<Course> {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.filter { it.studentIds.contains(studentId) }
        }

        val snapshot = collection
            .whereArrayContains("students", studentId)
            .get()
            .get()
        return snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
    }

    fun findCoursesByStudentIdAndActive(studentId: String, active: Boolean): List<Course> {
        logger.info("Querying courses for studentId: $studentId and active: $active")
        // test/in-memory mode
        if (collection == null) {
            val results = inMemory.values.filter { it.studentIds.contains(studentId) && it.active == active }
            logger.info("In-memory courses found: $results")
            return results
        }

        val snapshot = collection
            .whereArrayContains("studentIds", studentId) // Corrected field name to match Firestore
            .whereEqualTo("active", active)
            .get()
            .get()
        val courses = snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
        logger.info("Courses retrieved from Firestore: $courses")
        return courses
    }

    fun findActive(academicYear: String? = null, semester: String? = null): List<Course> {
        if (collection == null) {
            var results = inMemory.values.filter { it.active }
            if (academicYear != null) {
                results = results.filter { it.academicYear == academicYear }
            }
            if (semester != null) {
                results = results.filter { it.semester == semester }
            }
            return results
        }

        var query = collection.whereEqualTo("active", true)
        if (academicYear != null) {
            query = query.whereEqualTo("academic_year", academicYear)
        }
        if (semester != null) {
            query = query.whereEqualTo("semester", semester)
        }

        val snapshot = query.get().get()
        return snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
    }

    fun save(course: Course): Course {
        course.validate() // Validate before saving

        // test/in-memory mode
        if (collection == null) {
            if (course.id.isBlank()) course.id = "course_${inMemory.size + 1}"
            course.updatedAt = System.currentTimeMillis()
            inMemory[course.id] = course
            return course
        }

        val docRef = if (course.id.isBlank()) collection.document() else collection.document(course.id)
        if (course.id.isBlank()) course.id = docRef.id
        course.updatedAt = System.currentTimeMillis()
        docRef.set(course).get()
        return course
    }

    fun delete(id: String) {
        // test/in-memory mode
        if (collection == null) {
            inMemory.remove(id)
            return
        }

        collection.document(id).delete().get()
    }

    fun findAll(): List<Course> {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.toList()
        }

        val snapshot = collection.get().get()
        return snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
    }

    fun findStudentById(studentId: String): User? {
        // test/in-memory mode
        if (collection == null) {
            val course = inMemory.values.firstOrNull { it.studentIds.contains(studentId) }
            return course?.let {
                User(
                    id = studentId,
                    fullName = "Unknown",
                    username = "unknown",
                    email = "unknown@example.com",
                    role = "STUDENT",
                    authorities = emptyList(),
                    active = true,
                    emailVerified = false,
                    phoneVerified = false,
                    twoFactorEnabled = false,
                    createdAt = 0L
                 
                )
            }
        }

        val snapshot = firestore?.collection("users")
            ?.whereEqualTo(com.google.cloud.firestore.FieldPath.documentId(), studentId)
            ?.get()
            ?.get()

        return snapshot?.documents?.firstOrNull()?.toObject(User::class.java)
    }
}