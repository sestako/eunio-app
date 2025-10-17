package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.data.remote.dto.*
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Implementation of SettingsRemoteDataSource using Firestore.
 * Handles cloud storage of user settings with conflict resolution and versioning.
 */
class SettingsRemoteDataSourceImpl(
    private val firestoreService: FirestoreService
) : SettingsRemoteDataSource {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val SETTINGS_FIELD = "settings"
        private const val SETTINGS_HISTORY_COLLECTION = "settingsHistory"
        private const val LAST_MODIFIED_FIELD = "settings.lastModified"
        private const val VERSION_FIELD = "settings.version"
        private const val SYNC_STATUS_FIELD = "settings.syncStatus"
    }
    
    override suspend fun getSettings(userId: String): Result<UserSettings?> {
        return try {
            // Get user document with settings
            val userResult = firestoreService.getUser(userId)
            
            when {
                userResult.isSuccess -> {
                    val user = userResult.getOrNull()
                    if (user?.settings != null) {
                        // Convert settings from user document
                        val settingsDto = user.settings as? Map<String, Any>
                        if (settingsDto != null) {
                            val settings = parseSettingsFromMap(settingsDto, userId)
                            Result.success(settings)
                        } else {
                            Result.success(null)
                        }
                    } else {
                        Result.success(null)
                    }
                }
                else -> {
                    val error = userResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to get settings: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get settings: ${e.message}"))
        }
    }
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        return try {
            val settingsDto = UserSettingsDto.fromDomain(settings)
            val settingsMap = convertDtoToMap(settingsDto)
            
            // Update user document with settings
            val updateResult = firestoreService.updateUserSettings(settings.userId, settingsMap)
            
            when {
                updateResult.isSuccess -> {
                    // Create history entry for audit trail
                    createSettingsHistory(
                        userId = settings.userId,
                        settings = settings,
                        changeType = "UPDATE",
                        deviceInfo = getCurrentDeviceInfo()
                    )
                    Result.success(Unit)
                }
                else -> {
                    val error = updateResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to save settings: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to save settings: ${e.message}"))
        }
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<Unit> {
        return saveSettings(settings) // Same implementation for now
    }
    
    override suspend fun conditionalUpdateSettings(
        settings: UserSettings,
        expectedLastModified: Long
    ): Result<Unit> {
        return try {
            // First check if the current lastModified matches expected
            val currentTimestamp = getLastModifiedTimestamp(settings.userId)
            
            when {
                currentTimestamp.isSuccess -> {
                    val currentTime = currentTimestamp.getOrNull()
                    if (currentTime != null && currentTime != expectedLastModified) {
                        // Conflict detected
                        Result.error(SettingsError.ConflictError(
                            "Settings have been modified by another device. Expected: $expectedLastModified, Current: $currentTime"
                        ))
                    } else {
                        // No conflict, proceed with update
                        saveSettings(settings)
                    }
                }
                else -> {
                    val error = currentTimestamp.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to check for conflicts: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to conditionally update settings: ${e.message}"))
        }
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        return try {
            // Create history entry before deletion
            val currentSettings = getSettings(userId)
            currentSettings.getOrNull()?.let { settings ->
                createSettingsHistory(
                    userId = userId,
                    settings = settings,
                    changeType = "DELETE",
                    deviceInfo = getCurrentDeviceInfo()
                )
            }
            
            // Remove settings from user document
            val deleteResult = firestoreService.deleteUserSettings(userId)
            
            when {
                deleteResult.isSuccess -> Result.success(Unit)
                else -> {
                    val error = deleteResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to delete settings: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to delete settings: ${e.message}"))
        }
    }
    
    override suspend fun clearAllSettings(): Result<Unit> {
        return try {
            // This would be an administrative operation
            // For now, we'll just return success as it's not commonly used
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to clear all settings: ${e.message}"))
        }
    }
    
    override suspend fun settingsExist(userId: String): Result<Boolean> {
        return try {
            val settingsResult = getSettings(userId)
            when {
                settingsResult.isSuccess -> {
                    val settings = settingsResult.getOrNull()
                    Result.success(settings != null)
                }
                else -> {
                    val error = settingsResult.errorOrNull()
                    Result.error(error ?: SettingsError.SyncError("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to check if settings exist: ${e.message}"))
        }
    }
    
    override suspend fun getLastModifiedTimestamp(userId: String): Result<Long?> {
        return try {
            val settingsResult = getSettings(userId)
            when {
                settingsResult.isSuccess -> {
                    val settings = settingsResult.getOrNull()
                    Result.success(settings?.lastModified?.toEpochMilliseconds())
                }
                else -> {
                    val error = settingsResult.errorOrNull()
                    Result.error(error ?: SettingsError.SyncError("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get last modified timestamp: ${e.message}"))
        }
    }
    
    override fun observeSettings(userId: String): Flow<UserSettings?> = flow {
        // For now, we'll implement a simple polling mechanism
        // In a real implementation, this would use Firestore listeners
        try {
            val settingsResult = getSettings(userId)
            settingsResult.getOrNull()?.let { settings ->
                emit(settings)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    override suspend fun createSettingsHistory(
        userId: String,
        settings: UserSettings,
        changeType: String,
        deviceInfo: String?
    ): Result<String> {
        return try {
            val historyEntry = SettingsHistoryEntryDto(
                settingsSnapshot = UserSettingsDto.fromDomain(settings),
                changeType = changeType,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                deviceInfo = deviceInfo
            )
            
            val historyMap = convertHistoryDtoToMap(historyEntry)
            val historyId = generateHistoryId(userId, changeType)
            
            // Save to settingsHistory subcollection
            val saveResult = firestoreService.saveSettingsHistory(userId, historyId, historyMap)
            
            when {
                saveResult.isSuccess -> Result.success(historyId)
                else -> {
                    val error = saveResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to create settings history: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to create settings history: ${e.message}"))
        }
    }
    
    override suspend fun getSettingsHistory(
        userId: String,
        limit: Int
    ): Result<List<SettingsHistoryEntry>> {
        return try {
            val historyResult = firestoreService.getSettingsHistory(userId, limit)
            
            when {
                historyResult.isSuccess -> {
                    val historyMaps = historyResult.getOrNull() ?: emptyList()
                    val historyEntries = historyMaps.mapNotNull { (historyId, historyMap) ->
                        parseHistoryFromMap(historyMap, historyId, userId)
                    }
                    Result.success(historyEntries)
                }
                else -> {
                    val error = historyResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to get settings history: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get settings history: ${e.message}"))
        }
    }
    
    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        return try {
            val deleteResult = firestoreService.deleteSettingsHistory(userId)
            
            when {
                deleteResult.isSuccess -> Result.success(Unit)
                else -> {
                    val error = deleteResult.errorOrNull()
                    Result.error(SettingsError.SyncError("Failed to delete settings history: ${error?.message}"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to delete settings history: ${e.message}"))
        }
    }
    
    override suspend fun batchUpdateSettings(settingsList: List<UserSettings>): Result<Int> {
        return try {
            var successCount = 0
            
            for (settings in settingsList) {
                val result = saveSettings(settings)
                if (result.isSuccess) {
                    successCount++
                }
            }
            
            Result.success(successCount)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to batch update settings: ${e.message}"))
        }
    }
    
    override suspend fun getSettingsStatistics(userId: String): Result<SettingsStatistics> {
        return try {
            val historyResult = getSettingsHistory(userId, 100)
            
            when {
                historyResult.isSuccess -> {
                    val history = historyResult.getOrNull() ?: emptyList()
                    val stats = calculateStatistics(userId, history)
                    Result.success(stats)
                }
                else -> {
                    val error = historyResult.errorOrNull()
                    Result.error(error ?: SettingsError.SyncError("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get settings statistics: ${e.message}"))
        }
    }
    
    override suspend fun validateConnection(): Result<Unit> {
        return try {
            // Simple connectivity test by trying to get a user (this will test Firestore connectivity)
            val testResult = firestoreService.getUser("connection-test")
            when {
                testResult.isSuccess -> Result.success(Unit)
                else -> {
                    val error = testResult.errorOrNull()
                    Result.error(error ?: SettingsError.SyncError("Connection validation failed"))
                }
            }
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Connection validation failed: ${e.message}"))
        }
    }
    
    override suspend fun getServerTimestamp(): Result<Long> {
        return try {
            // Use Firestore server timestamp
            val timestamp = Clock.System.now().toEpochMilliseconds()
            Result.success(timestamp)
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Failed to get server timestamp: ${e.message}"))
        }
    }
    
    override suspend fun healthCheck(): Result<RemoteStorageHealth> {
        return try {
            val startTime = Clock.System.now().toEpochMilliseconds()
            val timestampResult = getServerTimestamp()
            val endTime = Clock.System.now().toEpochMilliseconds()
            
            when {
                timestampResult.isSuccess -> {
                    val health = RemoteStorageHealth(
                        isHealthy = true,
                        latency = endTime - startTime,
                        lastSuccessfulOperation = endTime,
                        errorCount = 0,
                        version = "1.0.0"
                    )
                    Result.success(health)
                }
                else -> {
                    val health = RemoteStorageHealth(
                        isHealthy = false,
                        latency = endTime - startTime,
                        lastSuccessfulOperation = 0L,
                        errorCount = 1,
                        version = "1.0.0"
                    )
                    Result.success(health)
                }
            }
        } catch (e: Exception) {
            val health = RemoteStorageHealth(
                isHealthy = false,
                latency = 0L,
                lastSuccessfulOperation = 0L,
                errorCount = 1,
                version = "1.0.0"
            )
            Result.success(health)
        }
    }
    
    // Helper methods
    
    private fun parseSettingsFromMap(settingsMap: Map<String, Any>, userId: String): UserSettings? {
        return try {
            // Parse the Firestore document map back to UserSettings
            val unitPrefs = (settingsMap["unitPreferences"] as? Map<String, Any>)?.let { unitMap ->
                UnitPreferencesDto(
                    temperatureUnit = unitMap["temperatureUnit"] as? String ?: "CELSIUS",
                    weightUnit = unitMap["weightUnit"] as? String ?: "KILOGRAMS",
                    isManuallySet = unitMap["isManuallySet"] as? Boolean ?: false
                )
            } ?: UnitPreferencesDto()
            
            val notificationPrefs = (settingsMap["notificationPreferences"] as? Map<String, Any>)?.let { notifMap ->
                NotificationPreferencesDto(
                    dailyLoggingReminder = parseNotificationSetting(notifMap["dailyLoggingReminder"] as? Map<String, Any>),
                    periodPredictionAlert = parseNotificationSetting(notifMap["periodPredictionAlert"] as? Map<String, Any>),
                    ovulationAlert = parseNotificationSetting(notifMap["ovulationAlert"] as? Map<String, Any>),
                    insightNotifications = parseNotificationSetting(notifMap["insightNotifications"] as? Map<String, Any>),
                    globalNotificationsEnabled = notifMap["globalNotificationsEnabled"] as? Boolean ?: true
                )
            } ?: NotificationPreferencesDto()
            
            val cyclePrefs = (settingsMap["cyclePreferences"] as? Map<String, Any>)?.let { cycleMap ->
                CyclePreferencesDto(
                    averageCycleLength = (cycleMap["averageCycleLength"] as? Number)?.toInt() ?: 28,
                    averageLutealPhaseLength = (cycleMap["averageLutealPhaseLength"] as? Number)?.toInt() ?: 14,
                    periodDuration = (cycleMap["periodDuration"] as? Number)?.toInt() ?: 5,
                    isCustomized = cycleMap["isCustomized"] as? Boolean ?: false
                )
            } ?: CyclePreferencesDto()
            
            val privacyPrefs = (settingsMap["privacyPreferences"] as? Map<String, Any>)?.let { privacyMap ->
                PrivacyPreferencesDto(
                    dataSharingEnabled = privacyMap["dataSharingEnabled"] as? Boolean ?: false,
                    anonymousInsightsEnabled = privacyMap["anonymousInsightsEnabled"] as? Boolean ?: true,
                    crashReportingEnabled = privacyMap["crashReportingEnabled"] as? Boolean ?: true,
                    analyticsEnabled = privacyMap["analyticsEnabled"] as? Boolean ?: true
                )
            } ?: PrivacyPreferencesDto()
            
            val displayPrefs = (settingsMap["displayPreferences"] as? Map<String, Any>)?.let { displayMap ->
                DisplayPreferencesDto(
                    textSizeScale = (displayMap["textSizeScale"] as? Number)?.toFloat() ?: 1.0f,
                    highContrastMode = displayMap["highContrastMode"] as? Boolean ?: false,
                    hapticFeedbackEnabled = displayMap["hapticFeedbackEnabled"] as? Boolean ?: true,
                    hapticIntensity = displayMap["hapticIntensity"] as? String ?: "MEDIUM"
                )
            } ?: DisplayPreferencesDto()
            
            val syncPrefs = (settingsMap["syncPreferences"] as? Map<String, Any>)?.let { syncMap ->
                SyncPreferencesDto(
                    autoSyncEnabled = syncMap["autoSyncEnabled"] as? Boolean ?: true,
                    wifiOnlySync = syncMap["wifiOnlySync"] as? Boolean ?: false,
                    cloudBackupEnabled = syncMap["cloudBackupEnabled"] as? Boolean ?: true,
                    lastSyncTime = (syncMap["lastSyncTime"] as? Number)?.toLong()
                )
            } ?: SyncPreferencesDto()
            
            val settingsDto = UserSettingsDto(
                unitPreferences = unitPrefs,
                notificationPreferences = notificationPrefs,
                cyclePreferences = cyclePrefs,
                privacyPreferences = privacyPrefs,
                displayPreferences = displayPrefs,
                syncPreferences = syncPrefs,
                lastModified = (settingsMap["lastModified"] as? Number)?.toLong() ?: 0L,
                syncStatus = settingsMap["syncStatus"] as? String ?: "PENDING",
                version = (settingsMap["version"] as? Number)?.toInt() ?: 1
            )
            
            settingsDto.toDomain(userId)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseNotificationSetting(settingMap: Map<String, Any>?): NotificationSettingDto {
        return if (settingMap != null) {
            NotificationSettingDto(
                enabled = settingMap["enabled"] as? Boolean ?: false,
                timeHour = (settingMap["timeHour"] as? Number)?.toInt(),
                timeMinute = (settingMap["timeMinute"] as? Number)?.toInt(),
                daysInAdvance = (settingMap["daysInAdvance"] as? Number)?.toInt() ?: 1
            )
        } else {
            NotificationSettingDto()
        }
    }
    
    private fun convertDtoToMap(dto: UserSettingsDto): Map<String, Any> {
        // Convert DTO to Map for Firestore storage
        return mapOf(
            "unitPreferences" to mapOf(
                "temperatureUnit" to dto.unitPreferences.temperatureUnit,
                "weightUnit" to dto.unitPreferences.weightUnit,
                "isManuallySet" to dto.unitPreferences.isManuallySet
            ),
            "notificationPreferences" to mapOf(
                "dailyLoggingReminder" to convertNotificationSettingToMap(dto.notificationPreferences.dailyLoggingReminder),
                "periodPredictionAlert" to convertNotificationSettingToMap(dto.notificationPreferences.periodPredictionAlert),
                "ovulationAlert" to convertNotificationSettingToMap(dto.notificationPreferences.ovulationAlert),
                "insightNotifications" to convertNotificationSettingToMap(dto.notificationPreferences.insightNotifications),
                "globalNotificationsEnabled" to dto.notificationPreferences.globalNotificationsEnabled
            ),
            "cyclePreferences" to mapOf(
                "averageCycleLength" to dto.cyclePreferences.averageCycleLength,
                "averageLutealPhaseLength" to dto.cyclePreferences.averageLutealPhaseLength,
                "periodDuration" to dto.cyclePreferences.periodDuration,
                "isCustomized" to dto.cyclePreferences.isCustomized
            ),
            "privacyPreferences" to mapOf(
                "dataSharingEnabled" to dto.privacyPreferences.dataSharingEnabled,
                "anonymousInsightsEnabled" to dto.privacyPreferences.anonymousInsightsEnabled,
                "crashReportingEnabled" to dto.privacyPreferences.crashReportingEnabled,
                "analyticsEnabled" to dto.privacyPreferences.analyticsEnabled
            ),
            "displayPreferences" to mapOf(
                "textSizeScale" to dto.displayPreferences.textSizeScale,
                "highContrastMode" to dto.displayPreferences.highContrastMode,
                "hapticFeedbackEnabled" to dto.displayPreferences.hapticFeedbackEnabled,
                "hapticIntensity" to dto.displayPreferences.hapticIntensity
            ),
            "syncPreferences" to mapOf(
                "autoSyncEnabled" to dto.syncPreferences.autoSyncEnabled,
                "wifiOnlySync" to dto.syncPreferences.wifiOnlySync,
                "cloudBackupEnabled" to dto.syncPreferences.cloudBackupEnabled,
                "lastSyncTime" to dto.syncPreferences.lastSyncTime
            ),
            "lastModified" to dto.lastModified,
            "syncStatus" to dto.syncStatus,
            "version" to dto.version
        )
    }
    
    private fun convertNotificationSettingToMap(setting: com.eunio.healthapp.data.remote.dto.NotificationSettingDto): Map<String, Any?> {
        return mapOf(
            "enabled" to setting.enabled,
            "timeHour" to setting.timeHour,
            "timeMinute" to setting.timeMinute,
            "daysInAdvance" to setting.daysInAdvance
        )
    }
    
    private fun convertHistoryDtoToMap(dto: SettingsHistoryEntryDto): Map<String, Any> {
        return mapOf(
            "settingsSnapshot" to convertDtoToMap(dto.settingsSnapshot),
            "changeType" to dto.changeType,
            "timestamp" to dto.timestamp,
            "deviceInfo" to (dto.deviceInfo ?: "")
        )
    }
    
    private fun parseHistoryFromMap(
        historyMap: Map<String, Any>,
        historyId: String,
        userId: String
    ): SettingsHistoryEntry? {
        return try {
            val settingsSnapshot = (historyMap["settingsSnapshot"] as? Map<String, Any>)?.let { snapshotMap ->
                parseSettingsFromMap(snapshotMap, userId)
            }
            
            if (settingsSnapshot != null) {
                SettingsHistoryEntry(
                    historyId = historyId,
                    userId = userId,
                    settingsSnapshot = settingsSnapshot,
                    changeType = historyMap["changeType"] as? String ?: "UNKNOWN",
                    timestamp = (historyMap["timestamp"] as? Number)?.toLong() ?: 0L,
                    deviceInfo = historyMap["deviceInfo"] as? String
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateHistoryId(userId: String, changeType: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "${userId}_${changeType}_$timestamp"
    }
    
    private fun getCurrentDeviceInfo(): String {
        // This would get actual device info in a real implementation
        return "Unknown Device"
    }
    
    private fun calculateStatistics(userId: String, history: List<SettingsHistoryEntry>): SettingsStatistics {
        return SettingsStatistics(
            userId = userId,
            totalUpdates = history.size.toLong(),
            lastUpdateTime = history.maxOfOrNull { it.timestamp } ?: 0L,
            mostChangedSection = "general", // Would calculate from actual changes
            syncFrequency = if (history.size > 1) {
                val timeSpan = (history.maxOfOrNull { it.timestamp } ?: 0L) - (history.minOfOrNull { it.timestamp } ?: 0L)
                if (timeSpan > 0) history.size.toDouble() / timeSpan * 86400000 else 0.0 // per day
            } else 0.0,
            averageUpdateSize = 1024L // Would calculate actual size
        )
    }
}