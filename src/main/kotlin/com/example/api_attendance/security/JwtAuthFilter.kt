package com.example.api_attendance.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.example.api_attendance.security.JwtAuthentication
import com.example.api_attendance.security.JwtService

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {
    private val jwtLogger = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
    jwtLogger.debug("Incoming request ${request.method} ${request.requestURI} Authorization: ${if (authHeader != null) "present" else "absent"}")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val valid = jwtService.validateToken(token)
            jwtLogger.debug("Token present, valid=$valid")
            if (valid) {
                val userId = jwtService.extractUserId(token)
                jwtLogger.debug("Extracted userId from token: $userId")
                if (userId != null) {
                    SecurityContextHolder.getContext().authentication = JwtAuthentication(userId)
                    jwtLogger.info("Authenticated request for userId=$userId")
                }
            } else {
                jwtLogger.warn("Invalid JWT token for request ${request.method} ${request.requestURI}")
            }
        } else {
            if (authHeader != null) jwtLogger.warn("Authorization header present but does not start with Bearer")
        }

        filterChain.doFilter(request, response)
    }
}
