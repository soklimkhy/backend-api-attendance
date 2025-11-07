package com.example.api_attendance.repository

import com.example.api_attendance.model.User
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository

@Repository
open class UserRepository(private val firestore: Firestore? = null) {

    // If firestore is null, repository runs in in-memory test mode
    private val inMemory = mutableMapOf<String, User>()
    private val collection = firestore?.collection("users")

    open fun findByUsername(username: String): User? {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.firstOrNull { it.username == username }
        }

        val snapshot = collection
            .whereEqualTo("username", username)
            .get() // blocking call
            .get() // wait for result
        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }

    open fun findById(id: String): User? {
        // test/in-memory mode
        if (collection == null) {
            return inMemory[id]
        }

        val doc = collection.document(id).get().get()
        return if (doc.exists()) doc.toObject(User::class.java) else null
    }

    open fun findAll(): List<User> {
        // test/in-memory mode
        if (collection == null) {
            return inMemory.values.toList()
        }

        val snapshot = collection
            .get()
            .get()
        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
    }

    open fun save(user: User): User {
        // test/in-memory mode
        if (collection == null) {
            if (user.id.isBlank()) user.id = "id_${inMemory.size + 1}"
            inMemory[user.id] = user
            return user
        }

        val docRef = if (user.id.isNotBlank()) collection.document(user.id) else collection.document()
        if (user.id.isBlank()) user.id = docRef.id
        docRef.set(user).get() // blocking
        return user
    }
}
