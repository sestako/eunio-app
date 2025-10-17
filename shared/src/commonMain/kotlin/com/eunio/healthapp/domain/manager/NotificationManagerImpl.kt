package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting

import com.eunio.healthapp.platform.notification.PlatformNotificationService
import kotlinx.datetime.LocalTime

/**
 * Implementation of NotificationManager that coordinates between domain logic
 * and platform-specific notification services.
 */
class NotificationManagerImpl(
    private val platformNotificationService: PlatformNotificationService
) : NotificationManager {
    
    override suspend fun updateNotificationSchedule(preferences: NotificationPreferences): kotlin.Result<Unit> {
        return try {
            // First, cancel all existing notifications
            val cancelResult = cancelAllNotifications()
            if (cancelResult.isFailure) {
                return cancelResult
            }
            
            // If global notifications are disabled, we're done
            if (!preferences.globalNotificationsEnabled) {
                return kotlin.Result.success(Unit)
            }
            
            // Check if we have permission to show notifications
            val permissionStatus = getNotificationPermissionStatus()
            if (!permissionStatus.canShowNotifications()) {
                return kotlin.Result.failure(
                    AppError.PermissionError(
                        message = "Notification permission not granted",
                        requiredPermission = "notifications"
                    )
                )
            }
            
            // Schedule enabled notifications
            val results = mutableListOf<Result<Unit>>()
            
            if (preferences.dailyLoggingReminder.enabled) {
                results.add(
                    scheduleNotification(NotificationType.DAILY_LOGGING, preferences.dailyLoggingReminder)
                )
            }
            
            if (preferences.periodPredictionAlert.enabled) {
                results.add(
                    scheduleNotification(NotificationType.PERIOD_PREDICTION, preferences.periodPredictionAlert)
                )
            }
            
            if (preferences.ovulationAlert.enabled) {
                results.add(
                    scheduleNotification(NotificationType.OVULATION_ALERT, preferences.ovulationAlert)
                )
            }
            
            if (preferences.insightNotifications.enabled) {
                results.add(
                    scheduleNotification(NotificationType.INSIGHTS, preferences.insightNotifications)
                )
            }
            
            // Check if any scheduling failed
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                val firstFailure = failures.first().exceptionOrNull()
                return kotlin.Result.failure(
                    AppError.ValidationError(
                        message = "Failed to schedule some notifications: ${firstFailure?.message}",
                        cause = firstFailure
                    )
                )
            }
            
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to update notification schedule: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun scheduleNotification(
        type: NotificationType, 
        setting: NotificationSetting
    ): kotlin.Result<Unit> {
        return try {
            // Validate the notification setting
            if (!setting.isValid()) {
                return kotlin.Result.failure(
                    AppError.ValidationError(
                        message = "Invalid notification setting for ${type.id}",
                        field = "notificationSetting"
                    )
                )
            }
            
            val time = setting.time ?: LocalTime(20, 0) // Default to 8 PM
            
            scheduleNotification(
                type = type,
                time = time,
                repeatInterval = RepeatInterval.DAILY,
                daysInAdvance = setting.daysInAdvance
            )
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to schedule notification ${type.id}: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun scheduleNotification(
        type: NotificationType,
        time: LocalTime,
        repeatInterval: RepeatInterval,
        daysInAdvance: Int
    ): kotlin.Result<Unit> {
        return try {
            // For cycle-related notifications, we might need to adjust the scheduling
            // based on daysInAdvance, but for now we'll use the basic scheduling
            platformNotificationService.scheduleRepeatingNotification(
                id = type.id,
                title = type.title,
                body = type.body,
                time = time,
                repeatInterval = repeatInterval
            )
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to schedule notification ${type.id}: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun cancelNotification(type: NotificationType): kotlin.Result<Unit> {
        return try {
            platformNotificationService.cancelNotification(type.id)
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to cancel notification ${type.id}: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> {
        return try {
            platformNotificationService.cancelAllNotifications()
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to cancel all notifications: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun requestNotificationPermission(): kotlin.Result<Boolean> {
        return try {
            platformNotificationService.requestPermission()
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.PermissionError(
                    message = "Failed to request notification permission: ${e.message}",
                    requiredPermission = "notifications",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun getNotificationPermissionStatus(): NotificationPermissionStatus {
        return try {
            platformNotificationService.getPermissionStatus()
        } catch (e: Exception) {
            NotificationPermissionStatus.UNKNOWN
        }
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return try {
            platformNotificationService.areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> {
        return try {
            platformNotificationService.openNotificationSettings()
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to open notification settings: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun getScheduledNotifications(): List<NotificationType> {
        return try {
            val scheduledIds = platformNotificationService.getScheduledNotificationIds()
            scheduledIds.mapNotNull { id ->
                NotificationType.fromId(id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun testNotification(type: NotificationType): kotlin.Result<Unit> {
        return try {
            platformNotificationService.showTestNotification(
                id = type.id,
                title = "${type.title} (Test)",
                body = type.body
            )
        } catch (e: Exception) {
            kotlin.Result.failure(
                AppError.ValidationError(
                    message = "Failed to show test notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
}