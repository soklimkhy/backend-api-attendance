package com.example.api_attendance.model

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Token(
    @DocumentId
    var id: String = "",

    @PropertyName("userId")
    var userId: String = "",

    @PropertyName("accessToken")
    var accessToken: String = "",

    @PropertyName("refreshToken")
    var refreshToken: String = "",

    @Contextual
    @PropertyName("expiredAt")
    var expiredAt: Instant? = null,

    @Contextual
    @PropertyName("createdAt")
    var createdAt: Instant = Instant.now(),

    @PropertyName("revoked")
    var revoked: Boolean = false
)
