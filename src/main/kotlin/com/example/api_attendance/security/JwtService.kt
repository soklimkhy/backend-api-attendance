package com.example.api_attendance.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {
    @Value("\${jwt.access-token-expiration}")
    private var accessTokenExpiration: Long = 900000 // 15 minutes default

    @Value("\${jwt.refresh-token-expiration}")
    private var refreshTokenExpiration: Long = 604800000 // 7 days default

    private val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateAccessToken(userId: String): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpiration)
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiry = Date(now.time + refreshTokenExpiration)
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            !claims.body.expiration.before(Date())
        } catch (ex: Exception) {
            false
        }
    }

    fun extractUserId(token: String): String? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            claims.body.subject
        } catch (ex: Exception) {
            null
        }
    }

    fun getAccessTokenExpiration(): Long = accessTokenExpiration

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration
}
