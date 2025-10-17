package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for UserPreferences used in Firestore operations.
 * Handles serialization/deserialization between domain model and Firestore document.
 */
@Serializable
data class UserPreferencesDto(
    val unitSystem: String = "METRIC",
    val isManuallySet: Boolean = false,
    val lastModified: Long = 0L,
    val syncStatus: String = "PENDING"
) {
    
    /**
     * Converts DTO to domain model.
     * @param userId The user ID to associate with the preferences
     * @return UserPreferences domain model
     */
    fun toDomain(userId: String): UserPreferences {
        return UserPreferences(
            userId = userId,
            unitSystem = UnitSystem.valueOf(unitSystem),
            isManuallySet = isManuallySet,
            lastModified = Instant.fromEpochSeconds(lastModified),
            syncStatus = SyncStatus.valueOf(syncStatus)
        )
    }
    
    companion object {
        /**
         * Creates DTO from domain model.
         * @param preferences The domain model to convert
         * @return UserPreferencesDto for Firestore storage
         */
        fun fromDomain(preferences: UserPreferences): UserPreferencesDto {
            return UserPreferencesDto(
                unitSystem = preferences.unitSystem.name,
                isManuallySet = preferences.isManuallySet,
                lastModified = preferences.lastModified.epochSeconds,
                syncStatus = preferences.syncStatus.name
            )
        }
    }
}