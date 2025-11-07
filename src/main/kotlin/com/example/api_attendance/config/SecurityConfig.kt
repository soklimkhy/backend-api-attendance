package com.example.api_attendance.config

import com.example.api_attendance.security.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {
    
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF for API endpoints
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**").permitAll() // Public auth endpoints
                it.requestMatchers("/internal/**").permitAll() // Internal debug endpoints (dev only)
                it.requestMatchers("/api/user/2fa/**").authenticated() // 2FA endpoints for authenticated users
                it.requestMatchers("/api/user/profile").authenticated() // Profile endpoints for authenticated users
                it.requestMatchers("/api/user/password").authenticated() // Password update for authenticated users
                it.requestMatchers("/api/admin/courses/**").authenticated() // Admin course endpoints for authenticated users
                it.anyRequest().authenticated() // All other endpoints require authentication
            }
        return http.build()
    }
}
