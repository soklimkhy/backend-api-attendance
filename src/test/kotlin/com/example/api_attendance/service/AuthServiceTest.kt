package com.example.api_attendance.service

import com.example.api_attendance.dto.AuthResponseDTO
import com.example.api_attendance.exception.InvalidCredentialsException
import com.example.api_attendance.exception.UserNotFoundException
import com.example.api_attendance.exception.UsernameExistsException
import com.example.api_attendance.model.User
import com.example.api_attendance.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class AuthServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var authService: AuthService
    private val encoder = BCryptPasswordEncoder()

    @BeforeEach
    fun setup() {
        userRepository = UserRepository()
        authService = AuthService(userRepository)
    }

    @Test
    fun `register should create new user with encoded password`() {
        // Arrange
        val username = "testuser"
        val password = "password123"

        // Act
        val result = authService.register(username, password)

        // Assert
        assertEquals(username, result.username)
        assertNotNull(result.id)
        
        // Verify the user was saved
        val saved = userRepository.findByUsername(username)
        assertNotNull(saved)
        assertEquals(username, saved?.username)
    }

    @Test
    fun `register should throw UsernameExistsException when username exists`() {
        // Arrange
        val username = "existinguser"
        val password = "password123"
        userRepository.save(User(id = "existing-id", username = username, password = "encoded"))

        // Act & Assert
        assertThrows<UsernameExistsException> {
            authService.register(username, password)
        }
        // ensure repository still has the original user
        val found = userRepository.findByUsername(username)
        assertNotNull(found)
    }

    @Test
    fun `register should throw IllegalArgumentException for blank password`() {
        // Arrange
        val username = "testuser"
        val password = ""

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(username, password)
        }
        assertEquals("Password is required", exception.message)
        
        // Verify no user was saved
        assertNull(userRepository.findByUsername("testuser"))
    }

    @Test
    fun `register should throw IllegalArgumentException for short password`() {
        // Arrange
        val username = "testuser"
        val password = "short"

        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(username, password)
        }
        assertEquals("Password must be at least 8 characters", exception.message)
        
        // Verify no user was saved
        assertTrue(userRepository.findByUsername("testuser") == null)
    }

    @Test
    fun `login should return user when credentials are valid`() {
        // Arrange
        val username = "testuser"
        val password = "password123"
        val encodedPassword = encoder.encode(password)
        val user = User(id = "test-id-1", username = username, password = encodedPassword)
        
        userRepository.save(user)

        // Act
        val result = authService.login(username, password)

        // Assert
        assertEquals(username, result.user.username)
        assertEquals(user.id, result.user.id)
        assertNull(result.accessToken) // Since we didn't mock the token services
        assertNull(result.refreshToken)
    }

    @Test
    fun `login should throw UserNotFoundException when user not found`() {
        // Arrange
        val username = "nonexistent"
        val password = "password123"

        // Act & Assert
        assertThrows<UserNotFoundException> {
            authService.login(username, password)
        }
    }

    @Test
    fun `login should throw InvalidCredentialsException when password is incorrect`() {
        // Arrange
        val username = "testuser"
        val password = "wrongpassword"
        val user = User(
            id = "test-id-1",
            username = username,
            password = encoder.encode("correctpassword")
        )
        userRepository.save(user)

        // Act & Assert
        assertThrows<InvalidCredentialsException> {
            authService.login(username, password)
        }
    }
}