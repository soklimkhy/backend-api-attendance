package com.example.api_attendance.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/protected")
class ProtectedController {

    @GetMapping
    fun getProtectedData(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        return ResponseEntity.ok(mapOf(
            "message" to "You have access to protected data",
            "userId" to authentication.name
        ))
    }
}