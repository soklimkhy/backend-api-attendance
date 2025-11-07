package com.example.api_attendance.service

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import org.springframework.stereotype.Service

@Service
class FirestoreTestService {

    fun testConnection(): String {
        return try {
            val firestore: Firestore = FirestoreClient.getFirestore()
            val collections = firestore.listCollections().toList().map { it.id }
            "Firestore connected successfully! Collections: $collections"
        } catch (e: Exception) {
            "Failed to connect to Firestore: ${e.message}"
        }
    }
}
