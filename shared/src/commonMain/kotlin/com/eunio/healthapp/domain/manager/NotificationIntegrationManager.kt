package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manager that integrates notification preferences with existing reminder systems.
 * Bridges the enhanced settings system with the existing notification infrastructure.
 */
interface NotificationIntegrationManager {
    
    /**
     * Updates all notification schedules based on current preferences
     */
    suspend fun updateAllNotifications(): Result<Unit>
    
    /**
     * Integrates notification preferences with existing reminder systems
     */
    suspend fun integrateWithExistingReminders(preferences: NotificationPreferences): Result<Unit>
    
    /**
     * Observes notification preference changes and updates systems accordingly
     */
    fun observeAndUpdateNotifications(): Flow<Unit>
    
    /**
     * Migrates existing reminder settings to new notification preferences
     */
    suspend fun migrateExistingReminders(): Result<NotificationPreferences>
}

/**
 * Implementation that integrates with existing notification systems
 */
class NotificationIntegrationManagerImpl(
    private val settingsManager: SettingsManager,
    private val notificationManager: NotificationManager,
    private val legacyReminderService: LegacyReminderService? = null
) : NotificationIntegrationManager {
    
    override suspend fun updateAllNotifications(): Result<Unit> {
        return try {
            val settings = settingsManager.getUserSettings().getOrNull()
                ?: return Result.error(AppError.ValidationError("No user settings found"))
            
            // Update notification schedule through settings manager
            // This will automatically trigger the notification manager
            settingsManager.updateNotificationPreferences(settings.notificationPreferences)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to update notifications: ${e.message}"))
        }
    }
    
    override suspend fun integrateWithExistingReminders(preferences: NotificationPreferences): Result<Unit> {
        return try {
            // Cancel any existing legacy reminders
            legacyReminderService?.cancelAllReminders()
            
            // Apply new notification preferences
            notificationManager.updateNotificationSchedule(preferences)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to integrate reminders: ${e.message}"))
        }
    }
    
    override fun observeAndUpdateNotifications(): Flow<Unit> {
        return settingsManager.observeSettingsChanges()
            .map { settings ->
                // Automatically update notifications when preferences change
                integrateWithExistingReminders(settings.notificationPreferences)
            }
            .map { Unit }
    }
    
    override suspend fun migrateExistingReminders(): Result<NotificationPreferences> {
        return try {
            // Get existing reminder settings from legacy service
            val existingReminders = legacyReminderService?.getExistingReminders() ?: emptyList()
            
            // Convert to new notification preferences format
            val preferences = NotificationPreferences(
                dailyLoggingReminder = existingReminders.find { it.type == "daily_logging" }?.let {
                    NotificationSetting(
                        enabled = it.enabled,
                        time = it.time,
                        daysInAdvance = 0
                    )
                } ?: NotificationSetting(),
                
                periodPredictionAlert = existingReminders.find { it.type == "period_prediction" }?.let {
                    NotificationSetting(
                        enabled = it.enabled,
                        time = it.time,
                        daysInAdvance = it.daysInAdvance
                    )
                } ?: NotificationSetting(),
                
                ovulationAlert = existingReminders.find { it.type == "ovulation" }?.let {
                    NotificationSetting(
                        enabled = it.enabled,
                        time = it.time,
                        daysInAdvance = it.daysInAdvance
                    )
                } ?: NotificationSetting(),
                
                insightNotifications = existingReminders.find { it.type == "insights" }?.let {
                    NotificationSetting(
                        enabled = it.enabled,
                        time = it.time,
                        daysInAdvance = 0
                    )
                } ?: NotificationSetting(),
                
                globalNotificationsEnabled = existingReminders.any { it.enabled }
            )
            
            // Save migrated preferences
            settingsManager.updateNotificationPreferences(preferences)
                .onSuccess {
                    // Clean up legacy reminders after successful migration
                    legacyReminderService?.clearLegacyReminders()
                }
            
            Result.success(preferences)
        } catch (e: Exception) {
            Result.error(AppError.ValidationError("Failed to migrate reminders: ${e.message}"))
        }
    }
}

/**
 * Interface for legacy reminder service (to be implemented by existing reminder system)
 */
interface LegacyReminderService {
    suspend fun getExistingReminders(): List<LegacyReminder>
    suspend fun cancelAllReminders(): Result<Unit>
    suspend fun clearLegacyReminders(): Result<Unit>
}

/**
 * Data class representing legacy reminder settings
 */
data class LegacyReminder(
    val type: String,
    val enabled: Boolean,
    val time: kotlinx.datetime.LocalTime?,
    val daysInAdvance: Int = 0
)