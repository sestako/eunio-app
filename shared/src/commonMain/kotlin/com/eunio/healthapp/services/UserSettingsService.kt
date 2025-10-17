package com.eunio.healthapp.services

import com.eunio.healthapp.domain.model.settings.UserSettings

interface UserSettingsService {
    suspend fun saveSettings(settings: UserSettings): Result<Unit>
    suspend fun getSettings(userId: String): Result<UserSettings?>
    suspend fun updateSettings(settings: UserSettings): Result<Unit>
    suspend fun deleteSettings(userId: String): Result<Unit>
}
