package com.example.api_attendance.model

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Session(
    @DocumentId
    var id: String = "",

    @PropertyName("userId")
    var userId: String = "",

    @PropertyName("device")
    var device: String = "",

    @PropertyName("ipAddress")
    var ipAddress: String? = null,

    @Contextual
    @PropertyName("lastLoginAt")
    var lastLoginAt: Instant = Instant.now(),

    @PropertyName("active")
    var active: Boolean = true
)
