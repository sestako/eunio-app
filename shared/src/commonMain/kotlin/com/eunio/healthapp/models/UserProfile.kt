package com.eunio.healthapp.models

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    companion object {
        fun create(userId: String, email: String, displayName: String): UserProfile {
            val now = Clock.System.now().toEpochMilliseconds()
            return UserProfile(
                userId = userId,
                email = email,
                displayName = displayName,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
