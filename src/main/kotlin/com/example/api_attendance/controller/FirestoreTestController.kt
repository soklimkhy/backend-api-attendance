package com.example.api_attendance.controller

import com.example.api_attendance.service.FirestoreTestService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FirestoreTestController(
    private val firestoreTestService: FirestoreTestService
) {

    @GetMapping("/test-firestore")
    fun testFirestore(): String {
        return firestoreTestService.testConnection()
    }
}
