package com.example.api_attendance.repository

import com.example.api_attendance.model.Token
import com.google.cloud.firestore.Firestore
import org.springframework.stereotype.Repository

@Repository
class TokenRepository(private val firestore: Firestore? = null) {
    private val collection = firestore?.collection("tokens")

    fun save(token: Token): Token {
        val docRef = if (token.id.isNotBlank()) collection?.document(token.id) else collection?.document()
        if (token.id.isBlank()) token.id = docRef?.id ?: ""
        docRef?.set(token)?.get() // blocking
        return token
    }

    fun findByUserId(userId: String): List<Token> {
        val snapshot = collection
            ?.whereEqualTo("userId", userId)
            ?.whereEqualTo("revoked", false)
            ?.get()
            ?.get()
        return snapshot?.documents?.mapNotNull { it.toObject(Token::class.java) } ?: emptyList()
    }

    fun findByRefreshToken(refreshToken: String): Token? {
        val snapshot = collection
            ?.whereEqualTo("refreshToken", refreshToken)
            ?.whereEqualTo("revoked", false)
            ?.get()
            ?.get()
        return snapshot?.documents?.firstOrNull()?.toObject(Token::class.java)
    }

    fun revokeAllUserTokens(userId: String) {
        val tokens = findByUserId(userId)
        tokens.forEach { token ->
            token.revoked = true
            save(token)
        }
    }
}