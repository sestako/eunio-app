package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for UserSettings used in Firestore operations.
 * Handles serialization/deserialization between domain model and Firestore document.
 */
@Serializable
data class UserSettingsDto(
    val unitPreferences: UnitPreferencesDto,
    val notificationPreferences: NotificationPreferencesDto,
    val cyclePreferences: CyclePreferencesDto,
    val privacyPreferences: PrivacyPreferencesDto,
    val displayPreferences: DisplayPreferencesDto,
    val syncPreferences: SyncPreferencesDto,
    val lastModified: Long,
    val syncStatus: String = "PENDING",
    val version: Int = 1
) {
    
    /**
     * Converts DTO to domain model.
     * @param userId The user ID to associate with the settings
     * @return UserSettings domain model
     */
    fun toDomain(userId: String): UserSettings {
        return UserSettings(
            userId = userId,
            unitPreferences = unitPreferences.toDomain(),
            notificationPreferences = notificationPreferences.toDomain(),
            cyclePreferences = cyclePreferences.toDomain(),
            privacyPreferences = privacyPreferences.toDomain(),
            displayPreferences = displayPreferences.toDomain(),
            syncPreferences = syncPreferences.toDomain(),
            lastModified = Instant.fromEpochMilliseconds(lastModified),
            syncStatus = SyncStatus.valueOf(syncStatus),
            version = version
        )
    }
    
    companion object {
        /**
         * Creates DTO from domain model.
         * @param settings The domain model to convert
         * @return UserSettingsDto for Firestore storage
         */
        fun fromDomain(settings: UserSettings): UserSettingsDto {
            return UserSettingsDto(
                unitPreferences = UnitPreferencesDto.fromDomain(settings.unitPreferences),
                notificationPreferences = NotificationPreferencesDto.fromDomain(settings.notificationPreferences),
                cyclePreferences = CyclePreferencesDto.fromDomain(settings.cyclePreferences),
                privacyPreferences = PrivacyPreferencesDto.fromDomain(settings.privacyPreferences),
                displayPreferences = DisplayPreferencesDto.fromDomain(settings.displayPreferences),
                syncPreferences = SyncPreferencesDto.fromDomain(settings.syncPreferences),
                lastModified = settings.lastModified.toEpochMilliseconds(),
                syncStatus = settings.syncStatus.name,
                version = settings.version
            )
        }
    }
}

@Serializable
data class UnitPreferencesDto(
    val temperatureUnit: String = "CELSIUS",
    val weightUnit: String = "KILOGRAMS",
    val isManuallySet: Boolean = false
) {
    fun toDomain(): UnitPreferences {
        return UnitPreferences(
            temperatureUnit = TemperatureUnit.valueOf(temperatureUnit),
            weightUnit = WeightUnit.valueOf(weightUnit),
            isManuallySet = isManuallySet
        )
    }
    
    companion object {
        fun fromDomain(preferences: UnitPreferences): UnitPreferencesDto {
            return UnitPreferencesDto(
                temperatureUnit = preferences.temperatureUnit.name,
                weightUnit = preferences.weightUnit.name,
                isManuallySet = preferences.isManuallySet
            )
        }
    }
}

@Serializable
data class NotificationPreferencesDto(
    val dailyLoggingReminder: NotificationSettingDto = NotificationSettingDto(),
    val periodPredictionAlert: NotificationSettingDto = NotificationSettingDto(),
    val ovulationAlert: NotificationSettingDto = NotificationSettingDto(),
    val insightNotifications: NotificationSettingDto = NotificationSettingDto(),
    val globalNotificationsEnabled: Boolean = true
) {
    fun toDomain(): NotificationPreferences {
        return NotificationPreferences(
            dailyLoggingReminder = dailyLoggingReminder.toDomain(),
            periodPredictionAlert = periodPredictionAlert.toDomain(),
            ovulationAlert = ovulationAlert.toDomain(),
            insightNotifications = insightNotifications.toDomain(),
            globalNotificationsEnabled = globalNotificationsEnabled
        )
    }
    
    companion object {
        fun fromDomain(preferences: NotificationPreferences): NotificationPreferencesDto {
            return NotificationPreferencesDto(
                dailyLoggingReminder = NotificationSettingDto.fromDomain(preferences.dailyLoggingReminder),
                periodPredictionAlert = NotificationSettingDto.fromDomain(preferences.periodPredictionAlert),
                ovulationAlert = NotificationSettingDto.fromDomain(preferences.ovulationAlert),
                insightNotifications = NotificationSettingDto.fromDomain(preferences.insightNotifications),
                globalNotificationsEnabled = preferences.globalNotificationsEnabled
            )
        }
    }
}

@Serializable
data class NotificationSettingDto(
    val enabled: Boolean = false,
    val timeHour: Int? = null,
    val timeMinute: Int? = null,
    val daysInAdvance: Int = 1
) {
    fun toDomain(): NotificationSetting {
        val time = if (timeHour != null && timeMinute != null) {
            kotlinx.datetime.LocalTime(timeHour, timeMinute)
        } else null
        
        return NotificationSetting(
            enabled = enabled,
            time = time,
            daysInAdvance = daysInAdvance
        )
    }
    
    companion object {
        fun fromDomain(setting: NotificationSetting): NotificationSettingDto {
            return NotificationSettingDto(
                enabled = setting.enabled,
                timeHour = setting.time?.hour,
                timeMinute = setting.time?.minute,
                daysInAdvance = setting.daysInAdvance
            )
        }
    }
}

@Serializable
data class CyclePreferencesDto(
    val averageCycleLength: Int = 28,
    val averageLutealPhaseLength: Int = 14,
    val periodDuration: Int = 5,
    val isCustomized: Boolean = false
) {
    fun toDomain(): CyclePreferences {
        return CyclePreferences(
            averageCycleLength = averageCycleLength,
            averageLutealPhaseLength = averageLutealPhaseLength,
            periodDuration = periodDuration,
            isCustomized = isCustomized
        )
    }
    
    companion object {
        fun fromDomain(preferences: CyclePreferences): CyclePreferencesDto {
            return CyclePreferencesDto(
                averageCycleLength = preferences.averageCycleLength,
                averageLutealPhaseLength = preferences.averageLutealPhaseLength,
                periodDuration = preferences.periodDuration,
                isCustomized = preferences.isCustomized
            )
        }
    }
}

@Serializable
data class PrivacyPreferencesDto(
    val dataSharingEnabled: Boolean = false,
    val anonymousInsightsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true
) {
    fun toDomain(): PrivacyPreferences {
        return PrivacyPreferences(
            dataSharingEnabled = dataSharingEnabled,
            anonymousInsightsEnabled = anonymousInsightsEnabled,
            crashReportingEnabled = crashReportingEnabled,
            analyticsEnabled = analyticsEnabled
        )
    }
    
    companion object {
        fun fromDomain(preferences: PrivacyPreferences): PrivacyPreferencesDto {
            return PrivacyPreferencesDto(
                dataSharingEnabled = preferences.dataSharingEnabled,
                anonymousInsightsEnabled = preferences.anonymousInsightsEnabled,
                crashReportingEnabled = preferences.crashReportingEnabled,
                analyticsEnabled = preferences.analyticsEnabled
            )
        }
    }
}

@Serializable
data class DisplayPreferencesDto(
    val textSizeScale: Float = 1.0f,
    val highContrastMode: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val hapticIntensity: String = "MEDIUM"
) {
    fun toDomain(): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = textSizeScale,
            highContrastMode = highContrastMode,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            hapticIntensity = HapticIntensity.valueOf(hapticIntensity)
        )
    }
    
    companion object {
        fun fromDomain(preferences: DisplayPreferences): DisplayPreferencesDto {
            return DisplayPreferencesDto(
                textSizeScale = preferences.textSizeScale,
                highContrastMode = preferences.highContrastMode,
                hapticFeedbackEnabled = preferences.hapticFeedbackEnabled,
                hapticIntensity = preferences.hapticIntensity.name
            )
        }
    }
}

@Serializable
data class SyncPreferencesDto(
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val cloudBackupEnabled: Boolean = true,
    val lastSyncTime: Long? = null
) {
    fun toDomain(): SyncPreferences {
        return SyncPreferences(
            autoSyncEnabled = autoSyncEnabled,
            wifiOnlySync = wifiOnlySync,
            cloudBackupEnabled = cloudBackupEnabled,
            lastSyncTime = lastSyncTime?.let { Instant.fromEpochMilliseconds(it) }
        )
    }
    
    companion object {
        fun fromDomain(preferences: SyncPreferences): SyncPreferencesDto {
            return SyncPreferencesDto(
                autoSyncEnabled = preferences.autoSyncEnabled,
                wifiOnlySync = preferences.wifiOnlySync,
                cloudBackupEnabled = preferences.cloudBackupEnabled,
                lastSyncTime = preferences.lastSyncTime?.toEpochMilliseconds()
            )
        }
    }
}

/**
 * Data Transfer Object for settings history entries in Firestore
 */
@Serializable
data class SettingsHistoryEntryDto(
    val settingsSnapshot: UserSettingsDto,
    val changeType: String,
    val timestamp: Long,
    val deviceInfo: String?
) {
    fun toDomain(historyId: String, userId: String): SettingsHistoryEntry {
        return SettingsHistoryEntry(
            historyId = historyId,
            userId = userId,
            settingsSnapshot = settingsSnapshot.toDomain(userId),
            changeType = changeType,
            timestamp = timestamp,
            deviceInfo = deviceInfo
        )
    }
    
    companion object {
        fun fromDomain(entry: SettingsHistoryEntry): SettingsHistoryEntryDto {
            return SettingsHistoryEntryDto(
                settingsSnapshot = UserSettingsDto.fromDomain(entry.settingsSnapshot),
                changeType = entry.changeType,
                timestamp = entry.timestamp,
                deviceInfo = entry.deviceInfo
            )
        }
    }
}