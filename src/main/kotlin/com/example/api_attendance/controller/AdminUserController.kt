package com.example.api_attendance.controller

import com.example.api_attendance.dto.UserResponseDTO
import com.example.api_attendance.model.Role
import com.example.api_attendance.model.User
import com.example.api_attendance.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class AdminUserUpdateRequest(
    val role: String? = null,
    val active: Boolean? = null,
    val emailVerified: Boolean? = null,
    val phoneVerified: Boolean? = null
)

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(AdminUserController::class.java)

    private fun requireAdminOrRoleManage(): Pair<String, User> {
        val auth = SecurityContextHolder.getContext().authentication
        logger.debug("requireAdminOrRoleManage - authentication object: {}", auth)
        if (auth == null || !auth.isAuthenticated) throw IllegalArgumentException("Unauthorized")
        val currentUserId = auth.name ?: throw IllegalArgumentException("Unauthorized")
        val currentUser = userRepository.findById(currentUserId) ?: throw IllegalArgumentException("Unauthorized")

        // Allow if user role is ADMIN or has ROLE_MANAGE authority
        if (currentUser.role != Role.ADMIN.name && !currentUser.hasAuthority("ROLE_MANAGE")) {
            throw IllegalArgumentException("Forbidden")
        }

        return Pair(currentUserId, currentUser)
    }

    @GetMapping
    fun listUsers(): ResponseEntity<Any> {
        logger.debug("listUsers called - current authentication: {}", SecurityContextHolder.getContext().authentication)
        return try {
            requireAdminOrRoleManage()
            val users = userRepository.findAll().map { it.toResponseDTO() }
            ResponseEntity.ok(users)
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error listing users", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdminOrRoleManage()
            val user = userRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "User not found"))
            ResponseEntity.ok(user.toResponseDTO())
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error getting user $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: String, @RequestBody req: AdminUserUpdateRequest): ResponseEntity<Any> {
        return try {
            requireAdminOrRoleManage()
            val user = userRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "User not found"))

            req.role?.let { r ->
                user.role = Role.fromString(r).name
                // update authorities to defaults for the role
                user.authorities = Role.getDefaultAuthorities(user.role)
            }
            req.active?.let { user.active = it }
            req.emailVerified?.let { user.emailVerified = it }
            req.phoneVerified?.let { user.phoneVerified = it }

            user.updatedAt = System.currentTimeMillis()
            userRepository.save(user)
            ResponseEntity.ok(user.toResponseDTO())
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error updating user $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            requireAdminOrRoleManage()
            val user = userRepository.findById(id) ?: return ResponseEntity.status(404).body(mapOf("error" to "User not found"))
            // For in-memory repo we can remove; for Firestore this repo doesn't expose delete - emulate disable instead
            user.active = false
            userRepository.save(user)
            ResponseEntity.ok(mapOf("message" to "User disabled"))
        } catch (e: IllegalArgumentException) {
            when (e.message) {
                "Unauthorized" -> ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
                "Forbidden" -> ResponseEntity.status(403).body(mapOf("error" to "Forbidden"))
                else -> ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
            }
        } catch (e: Exception) {
            logger.error("Error deleting user $id", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }
}
