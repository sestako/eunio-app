package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for User entity in Firestore.
 * Handles serialization/deserialization between domain model and Firestore document.
 */
@Serializable
data class UserDto(
    val email: String,
    val name: String,
    val onboardingComplete: Boolean = false,
    val primaryGoal: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        /**
         * Converts domain User model to Firestore DTO
         */
        fun fromDomain(user: User): UserDto {
            return UserDto(
                email = user.email,
                name = user.name,
                onboardingComplete = user.onboardingComplete,
                primaryGoal = user.primaryGoal.name,
                createdAt = user.createdAt.epochSeconds,
                updatedAt = user.updatedAt.epochSeconds
            )
        }
    }
    
    /**
     * Converts Firestore DTO to domain User model
     */
    fun toDomain(id: String, settings: Map<String, Any>? = null): User {
        return User(
            id = id,
            email = email,
            name = name,
            onboardingComplete = onboardingComplete,
            primaryGoal = HealthGoal.valueOf(primaryGoal),
            createdAt = Instant.fromEpochSeconds(createdAt),
            updatedAt = Instant.fromEpochSeconds(updatedAt),
            settings = settings
        )
    }
}