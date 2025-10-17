package com.eunio.healthapp.services

import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant

class AndroidUserSettingsService : UserSettingsService {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val settingsCollection = firestore.collection("user_settings")
    
    override suspend fun saveSettings(settings: UserSettings): Result<Unit> {
        return try {
            val settingsMap = userSettingsToMap(settings)
            settingsCollection.document(settings.userId)
                .set(settingsMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSettings(userId: String): Result<UserSettings?> {
        return try {
            val snapshot = settingsCollection.document(userId).get().await()
            
            if (!snapshot.exists()) {
                return Result.success(null)
            }
            
            val settings = mapToUserSettings(snapshot.data ?: return Result.success(null))
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<Unit> {
        return try {
            val updatedSettings = settings.withUpdate()
            val settingsMap = userSettingsToMap(updatedSettings)
            settingsCollection.document(settings.userId)
                .set(settingsMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSettings(userId: String): Result<Unit> {
        return try {
            settingsCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun userSettingsToMap(settings: UserSettings): Map<String, Any?> {
        return mapOf(
            "userId" to settings.userId,
            "unitPreferences" to mapOf(
                "weightUnit" to settings.unitPreferences.weightUnit.name,
                "temperatureUnit" to settings.unitPreferences.temperatureUnit.name,
                "isManuallySet" to settings.unitPreferences.isManuallySet
            ),
            "notificationPreferences" to mapOf(
                "globalNotificationsEnabled" to settings.notificationPreferences.globalNotificationsEnabled,
                "dailyLoggingReminder" to notificationSettingToMap(settings.notificationPreferences.dailyLoggingReminder),
                "periodPredictionAlert" to notificationSettingToMap(settings.notificationPreferences.periodPredictionAlert),
                "ovulationAlert" to notificationSettingToMap(settings.notificationPreferences.ovulationAlert),
                "insightNotifications" to notificationSettingToMap(settings.notificationPreferences.insightNotifications)
            ),
            "cyclePreferences" to mapOf(
                "averageCycleLength" to settings.cyclePreferences.averageCycleLength,
                "averageLutealPhaseLength" to settings.cyclePreferences.averageLutealPhaseLength,
                "periodDuration" to settings.cyclePreferences.periodDuration,
                "isCustomized" to settings.cyclePreferences.isCustomized
            ),
            "privacyPreferences" to mapOf(
                "dataSharingEnabled" to settings.privacyPreferences.dataSharingEnabled,
                "anonymousInsightsEnabled" to settings.privacyPreferences.anonymousInsightsEnabled,
                "crashReportingEnabled" to settings.privacyPreferences.crashReportingEnabled,
                "analyticsEnabled" to settings.privacyPreferences.analyticsEnabled
            ),
            "displayPreferences" to mapOf(
                "textSizeScale" to settings.displayPreferences.textSizeScale,
                "highContrastMode" to settings.displayPreferences.highContrastMode,
                "hapticFeedbackEnabled" to settings.displayPreferences.hapticFeedbackEnabled,
                "hapticIntensity" to settings.displayPreferences.hapticIntensity.name
            ),
            "syncPreferences" to mapOf(
                "autoSyncEnabled" to settings.syncPreferences.autoSyncEnabled,
                "wifiOnlySync" to settings.syncPreferences.wifiOnlySync,
                "cloudBackupEnabled" to settings.syncPreferences.cloudBackupEnabled,
                "lastSyncTime" to settings.syncPreferences.lastSyncTime?.toEpochMilliseconds()
            ),
            "lastModified" to settings.lastModified.toEpochMilliseconds(),
            "syncStatus" to settings.syncStatus.name,
            "version" to settings.version
        )
    }
    
    private fun notificationSettingToMap(setting: NotificationSetting): Map<String, Any?> {
        return mapOf(
            "enabled" to setting.enabled,
            "time" to setting.time?.toString(),
            "daysInAdvance" to setting.daysInAdvance
        )
    }
    
    private fun mapToUserSettings(data: Map<String, Any>): UserSettings {
        val unitPrefs = (data["unitPreferences"] as? Map<*, *>)?.let { up ->
            UnitPreferences(
                weightUnit = (up["weightUnit"] as? String)?.let { WeightUnit.valueOf(it) } ?: WeightUnit.KILOGRAMS,
                temperatureUnit = (up["temperatureUnit"] as? String)?.let { TemperatureUnit.valueOf(it) } ?: TemperatureUnit.CELSIUS,
                isManuallySet = up["isManuallySet"] as? Boolean ?: false
            )
        } ?: UnitPreferences.default()
        
        val notifPrefs = (data["notificationPreferences"] as? Map<*, *>)?.let { np ->
            NotificationPreferences(
                globalNotificationsEnabled = np["globalNotificationsEnabled"] as? Boolean ?: true,
                dailyLoggingReminder = mapToNotificationSetting(np["dailyLoggingReminder"] as? Map<*, *>),
                periodPredictionAlert = mapToNotificationSetting(np["periodPredictionAlert"] as? Map<*, *>),
                ovulationAlert = mapToNotificationSetting(np["ovulationAlert"] as? Map<*, *>),
                insightNotifications = mapToNotificationSetting(np["insightNotifications"] as? Map<*, *>)
            )
        } ?: NotificationPreferences.default()
        
        val cyclePrefs = (data["cyclePreferences"] as? Map<*, *>)?.let { cp ->
            CyclePreferences(
                averageCycleLength = (cp["averageCycleLength"] as? Number)?.toInt() ?: 28,
                averageLutealPhaseLength = (cp["averageLutealPhaseLength"] as? Number)?.toInt() ?: 14,
                periodDuration = (cp["periodDuration"] as? Number)?.toInt() ?: 5,
                isCustomized = cp["isCustomized"] as? Boolean ?: false
            )
        } ?: CyclePreferences.default()
        
        val privacyPrefs = (data["privacyPreferences"] as? Map<*, *>)?.let { pp ->
            PrivacyPreferences(
                dataSharingEnabled = pp["dataSharingEnabled"] as? Boolean ?: false,
                anonymousInsightsEnabled = pp["anonymousInsightsEnabled"] as? Boolean ?: true,
                crashReportingEnabled = pp["crashReportingEnabled"] as? Boolean ?: true,
                analyticsEnabled = pp["analyticsEnabled"] as? Boolean ?: true
            )
        } ?: PrivacyPreferences.default()
        
        val displayPrefs = (data["displayPreferences"] as? Map<*, *>)?.let { dp ->
            DisplayPreferences(
                textSizeScale = (dp["textSizeScale"] as? Number)?.toFloat() ?: 1.0f,
                highContrastMode = dp["highContrastMode"] as? Boolean ?: false,
                hapticFeedbackEnabled = dp["hapticFeedbackEnabled"] as? Boolean ?: true,
                hapticIntensity = (dp["hapticIntensity"] as? String)?.let { HapticIntensity.valueOf(it) } ?: HapticIntensity.MEDIUM
            )
        } ?: DisplayPreferences.default()
        
        val syncPrefs = (data["syncPreferences"] as? Map<*, *>)?.let { sp ->
            SyncPreferences(
                autoSyncEnabled = sp["autoSyncEnabled"] as? Boolean ?: true,
                wifiOnlySync = sp["wifiOnlySync"] as? Boolean ?: false,
                cloudBackupEnabled = sp["cloudBackupEnabled"] as? Boolean ?: true,
                lastSyncTime = (sp["lastSyncTime"] as? Long)?.let { Instant.fromEpochMilliseconds(it) }
            )
        } ?: SyncPreferences.default()
        
        return UserSettings(
            userId = data["userId"] as? String ?: "",
            unitPreferences = unitPrefs,
            notificationPreferences = notifPrefs,
            cyclePreferences = cyclePrefs,
            privacyPreferences = privacyPrefs,
            displayPreferences = displayPrefs,
            syncPreferences = syncPrefs,
            lastModified = Instant.fromEpochMilliseconds(data["lastModified"] as? Long ?: 0L),
            syncStatus = (data["syncStatus"] as? String)?.let { SyncStatus.valueOf(it) } ?: SyncStatus.PENDING,
            version = (data["version"] as? Number)?.toInt() ?: 1
        )
    }
    
    private fun mapToNotificationSetting(data: Map<*, *>?): NotificationSetting {
        if (data == null) return NotificationSetting.disabled()
        
        return NotificationSetting(
            enabled = data["enabled"] as? Boolean ?: false,
            time = (data["time"] as? String)?.let { 
                try {
                    kotlinx.datetime.LocalTime.parse(it)
                } catch (e: Exception) {
                    null
                }
            },
            daysInAdvance = (data["daysInAdvance"] as? Number)?.toInt() ?: 1
        )
    }
}
