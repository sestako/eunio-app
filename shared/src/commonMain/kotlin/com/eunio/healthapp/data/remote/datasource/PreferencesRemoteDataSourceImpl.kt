package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.data.remote.dto.UserPreferencesDto
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result

/**
 * Implementation of PreferencesRemoteDataSource using FirestoreService.
 * Handles cloud storage operations for user preferences.
 */
class PreferencesRemoteDataSourceImpl(
    private val firestoreService: FirestoreService
) : PreferencesRemoteDataSource {
    
    companion object {
        private const val PREFERENCES_COLLECTION = "preferences"
        private const val UNIT_SYSTEM_DOCUMENT = "unitSystem"
    }
    
    override suspend fun getPreferences(userId: String): Result<UserPreferences?> {
        return try {
            // For now, we'll extend the existing user document to include preferences
            // In a full implementation, we might use a separate preferences subcollection
            val userResult = firestoreService.getUser(userId)
            
            when (userResult) {
                is Result.Success -> {
                    val user = userResult.data
                    if (user != null) {
                        // Extract preferences from user model
                        val preferences = UserPreferences(
                            userId = user.id,
                            unitSystem = user.unitSystem,
                            isManuallySet = true, // Assume manually set if stored
                            lastModified = user.updatedAt,
                            syncStatus = com.eunio.healthapp.domain.model.SyncStatus.SYNCED
                        )
                        Result.success(preferences)
                    } else {
                        Result.success(null)
                    }
                }
                is Result.Error -> {
                    Result.error(AppError.NetworkError("Failed to get preferences: ${userResult.error.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(AppError.NetworkError("Failed to get preferences: ${e.message}"))
        }
    }
    
    override suspend fun savePreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            // Update the user document with the new unit system preference
            val userResult = firestoreService.getUser(preferences.userId)
            
            when (userResult) {
                is Result.Success -> {
                    val user = userResult.data
                    if (user != null) {
                        val updatedUser = user.copy(
                            unitSystem = preferences.unitSystem,
                            updatedAt = preferences.lastModified
                        )
                        val updateResult = firestoreService.updateUser(updatedUser)
                        when (updateResult) {
                            is Result.Success -> Result.success(Unit)
                            is Result.Error -> Result.error(AppError.NetworkError("Failed to save preferences: ${updateResult.error.message}"))
                        }
                    } else {
                        Result.error(AppError.ValidationError("User not found for preferences update"))
                    }
                }
                is Result.Error -> {
                    Result.error(AppError.NetworkError("Failed to save preferences: ${userResult.error.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(AppError.NetworkError("Failed to save preferences: ${e.message}"))
        }
    }
    
    override suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
        // For this implementation, update and save are the same operation
        return savePreferences(preferences)
    }
    
    override suspend fun deletePreferences(userId: String): Result<Unit> {
        return try {
            // Reset user preferences to default values
            val userResult = firestoreService.getUser(userId)
            
            when (userResult) {
                is Result.Success -> {
                    val user = userResult.data
                    if (user != null) {
                        val updatedUser = user.copy(
                            unitSystem = com.eunio.healthapp.domain.model.UnitSystem.METRIC,
                            updatedAt = kotlinx.datetime.Clock.System.now()
                        )
                        val updateResult = firestoreService.updateUser(updatedUser)
                        when (updateResult) {
                            is Result.Success -> Result.success(Unit)
                            is Result.Error -> Result.error(AppError.NetworkError("Failed to delete preferences: ${updateResult.error.message}"))
                        }
                    } else {
                        Result.error(AppError.ValidationError("User not found for preferences deletion"))
                    }
                }
                is Result.Error -> {
                    Result.error(AppError.NetworkError("Failed to delete preferences: ${userResult.error.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(AppError.NetworkError("Failed to delete preferences: ${e.message}"))
        }
    }
    
    override suspend fun clearPreferences(): Result<Unit> {
        return try {
            // This would typically be used for administrative purposes
            // For now, we'll return success as individual user preferences
            // are cleared through deletePreferences
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.NetworkError("Failed to clear preferences: ${e.message}"))
        }
    }
    
    override suspend fun preferencesExist(userId: String): Result<Boolean> {
        return try {
            val userResult = firestoreService.getUser(userId)
            
            when (userResult) {
                is Result.Success -> {
                    Result.success(userResult.data != null)
                }
                is Result.Error -> {
                    Result.error(AppError.NetworkError("Failed to check preferences existence: ${userResult.error.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(AppError.NetworkError("Failed to check preferences existence: ${e.message}"))
        }
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        return try {
            val userResult = firestoreService.getUser(userId)
            
            when (userResult) {
                is Result.Success -> {
                    Result.success(userResult.data?.updatedAt?.epochSeconds)
                }
                is Result.Error -> {
                    Result.error(AppError.NetworkError("Failed to get last modified timestamp: ${userResult.error.message}"))
                }
            }
        } catch (e: Exception) {
Result.error(AppError.NetworkError("Failed to get last modified timestamp: ${e.message}"))
        }
    }
}