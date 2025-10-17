package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.manager.SettingsBackupManager
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result

/**
 * Use case for restoring user settings when logging in on a new device.
 * Automatically called during the authentication flow to ensure user preferences
 * are available immediately after login.
 */
class RestoreSettingsOnNewDeviceUseCase(
    private val settingsBackupManager: SettingsBackupManager,
    private val userRepository: UserRepository
) {
    
    /**
     * Restores settings for a user on a new device.
     * This is typically called after successful authentication.
     * 
     * @param userId The user ID to restore settings for
     * @param isNewDevice Whether this is a new device (affects backup strategy)
     * @return Result indicating restore success or failure
     */
    suspend fun execute(userId: String, isNewDevice: Boolean = true): Result<Unit> {
        return try {
            if (isNewDevice) {
                // For new devices, attempt to restore from remote backup
                settingsBackupManager.restoreOnNewDevice(userId, backupData = null)
            } else {
                // For existing devices, just ensure settings are synced
                // This is handled by the regular sync process
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.error(
                com.eunio.healthapp.domain.error.SettingsError.BackupError(
                    "Failed to restore settings on new device: ${e.message}",
                    "NEW_DEVICE_RESTORE"
                )
            )
        }
    }
    
    /**
     * Checks if the current device needs settings restoration.
     * This can be used to determine if the restore process should be triggered.
     * 
     * @param userId The user ID to check
     * @return Result containing true if restoration is needed, false otherwise
     */
    suspend fun needsRestore(userId: String): Result<Boolean> {
        return try {
            // Check if user has any local settings
            val currentUser = userRepository.getCurrentUser()
            when {
                currentUser.isSuccess -> {
                    val user = currentUser.getOrNull()
                    if (user != null && user.id == userId) {
                        // User exists locally, check if they have settings
                        // This would typically check local storage for existing settings
                        Result.success(false) // Assume settings exist if user exists
                    } else {
                        // Different user or no user, needs restore
                        Result.success(true)
                    }
                }
                else -> {
                    // Error getting current user, assume restore needed
                    Result.success(true)
                }
            }
        } catch (e: Exception) {
            // On error, assume restore is needed to be safe
            Result.success(true)
        }
    }
}