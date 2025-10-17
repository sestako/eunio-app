package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result

/**
 * Use case for updating display preferences.
 */
class UpdateDisplayPreferencesUseCase(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Updates the display preferences.
     * 
     * @param preferences The new display preferences
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(preferences: DisplayPreferences): Result<Unit> {
        // Validate preferences before updating
        if (!preferences.isValid()) {
            return Result.error(
                com.eunio.healthapp.domain.error.AppError.ValidationError(
                    "Invalid display preferences: ${preferences.getValidationErrors().joinToString()}"
                )
            )
        }
        
        // Get current user settings and update
        return settingsRepository.getUserSettings()
            .flatMap { currentSettings ->
                if (currentSettings == null) {
                    Result.error(
                        com.eunio.healthapp.domain.error.AppError.DatabaseError("User settings not found")
                    )
                } else {
                    // Update display preferences
                    val updatedSettings = currentSettings.copy(
                        displayPreferences = preferences,
                        lastModified = kotlinx.datetime.Clock.System.now()
                    )
                    
                    // Save updated settings
                    settingsRepository.saveUserSettings(updatedSettings)
                }
            }
    }
}