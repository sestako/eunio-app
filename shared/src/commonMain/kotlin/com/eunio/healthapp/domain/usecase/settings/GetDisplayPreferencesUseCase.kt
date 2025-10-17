package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.DisplayPreferences
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result

/**
 * Use case for retrieving display preferences.
 */
class GetDisplayPreferencesUseCase(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Retrieves the current display preferences.
     * 
     * @return Result containing DisplayPreferences or error
     */
    suspend operator fun invoke(): Result<DisplayPreferences> {
        return settingsRepository.getUserSettings()
            .flatMap { userSettings ->
                if (userSettings?.displayPreferences != null) {
                    Result.success(userSettings.displayPreferences)
                } else {
                    Result.error(SettingsError.ValidationError("User settings not found"))
                }
            }
    }
}