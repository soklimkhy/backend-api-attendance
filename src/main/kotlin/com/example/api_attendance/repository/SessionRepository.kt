package com.example.api_attendance.repository

import com.example.api_attendance.model.Session
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class SessionRepository(private val firestore: Firestore? = null) {
    private val collection = firestore?.collection("sessions")

    fun save(session: Session): Session {
        val docRef = if (session.id.isNotBlank()) collection?.document(session.id) else collection?.document()
        if (session.id.isBlank()) session.id = docRef?.id ?: ""
        docRef?.set(session)?.get() // blocking
        return session
    }

    fun findActiveByUserId(userId: String): List<Session> {
        val snapshot = collection
            ?.whereEqualTo("userId", userId)
            ?.whereEqualTo("active", true)
            ?.get()
            ?.get()
        return snapshot?.documents?.mapNotNull { it.toObject(Session::class.java) } ?: emptyList()
    }

    fun invalidateAllUserSessions(userId: String) {
        val sessions = findActiveByUserId(userId)
        sessions.forEach { session ->
            session.active = false
            save(session)
        }
    }
}