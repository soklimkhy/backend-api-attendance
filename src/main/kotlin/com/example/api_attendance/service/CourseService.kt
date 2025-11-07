package com.example.api_attendance.service

import com.example.api_attendance.model.Course
import com.example.api_attendance.model.User
import com.example.api_attendance.repository.CourseRepository
import com.example.api_attendance.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CourseService(private val courseRepository: CourseRepository, private val userRepository: UserRepository) {
    private val logger = LoggerFactory.getLogger(CourseService::class.java)

    fun createCourse(course: Course): Course {
        course.validate()
        val saved = courseRepository.save(course)
        logger.info("Created course id={}", saved.id)
        return saved
    }

    fun updateCourse(courseId: String, updates: Course): Course? {
        val existing = courseRepository.findById(courseId) ?: return null
        // apply allowed updates
        existing.code = updates.code.takeIf { it.isNotBlank() } ?: existing.code
        existing.name = updates.name.takeIf { it.isNotBlank() } ?: existing.name
        existing.description = updates.description.takeIf { it.isNotBlank() } ?: existing.description
        existing.teacherId = updates.teacherId.takeIf { it.isNotBlank() } ?: existing.teacherId
        existing.academicYear = updates.academicYear.takeIf { it.isNotBlank() } ?: existing.academicYear
        existing.semester = updates.semester.takeIf { it.isNotBlank() } ?: existing.semester
        existing.studentIds = if (updates.studentIds.isNotEmpty()) updates.studentIds else existing.studentIds
        existing.active = updates.active

        existing.validate()
        return courseRepository.save(existing)
    }

    fun getCourse(courseId: String): Course? = courseRepository.findById(courseId)

    fun getCourseByCode(code: String): Course? = courseRepository.findByCode(code)

    fun getCoursesByTeacher(teacherId: String): List<Course> = courseRepository.findByTeacherId(teacherId)

    fun getCoursesByStudent(studentId: String): List<Course> = courseRepository.findByStudentId(studentId)

    fun listActive(academicYear: String? = null, semester: String? = null): List<Course> = courseRepository.findActive(academicYear, semester)

    fun addStudentToCourse(courseId: String, studentId: String): Course? {
        val course = courseRepository.findById(courseId) ?: return null
        if (!course.studentIds.contains(studentId)) {
            course.studentIds = course.studentIds + studentId
            return courseRepository.save(course)
        }
        return course
    }

    fun removeStudentFromCourse(courseId: String, studentId: String): Course? {
        val course = courseRepository.findById(courseId) ?: return null
        if (course.studentIds.contains(studentId)) {
            course.studentIds = course.studentIds.filter { it != studentId }
            return courseRepository.save(course)
        }
        return course
    }

    fun deleteCourse(courseId: String) {
        courseRepository.delete(courseId)
        logger.info("Deleted course id={}", courseId)
    }

    fun getAllCourses(): List<Course> {
        return courseRepository.findAll()
    }

    fun getStudentsForCourse(courseId: String, teacherId: String): List<String> {
        val course = courseRepository.findById(courseId)
            ?: throw IllegalArgumentException("Course not found")

        require(course.teacherId == teacherId) {
            "Teacher does not have access to this course"
        }

        return course.studentIds
    }

    fun getCourseById(courseId: String): Course? {
        return courseRepository.findById(courseId)
    }

    fun getActiveCoursesForStudent(studentId: String): List<Course> {
        // Query the course collection to find active courses for the given student ID
        return courseRepository.findCoursesByStudentIdAndActive(studentId, true)
    }

    fun getStudentDetailsById(studentId: String, teacherId: String): List<User> {
        val courses = courseRepository.findByTeacherId(teacherId)
        val studentDetails = mutableListOf<User>()

        courses.forEach { course ->
            if (course.studentIds.contains(studentId)) {
                val user = userRepository.findById(studentId)
                user?.let { studentDetails.add(it) }
            }
        }

        return studentDetails
    }
}
