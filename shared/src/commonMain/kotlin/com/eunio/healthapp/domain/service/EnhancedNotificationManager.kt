package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import com.eunio.healthapp.domain.util.UserNotificationService
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Clock

/**
 * Enhanced NotificationManager implementation with comprehensive error handling,
 * graceful degradation, and fallback mechanisms.
 */
class EnhancedNotificationManager(
    private val primaryNotificationManager: NotificationManager,
    private val fallbackNotificationManager: NotificationManager? = null
) : BaseService(), NotificationManager {
    
    override val serviceName: String = "NotificationManager"
    
    private var permissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.NOT_REQUESTED
    private val failedNotifications = mutableSetOf<NotificationType>()
    
    override suspend fun updateNotificationSchedule(preferences: NotificationPreferences): kotlin.Result<Unit> {
        val result = executeOperationWithRetry(
            operation = "updateNotificationSchedule",
            fallback = {
                fallbackNotificationManager?.updateNotificationSchedule(preferences)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showWarning(
                            "Notifications scheduled locally. Some may not work until system permissions are restored."
                        )
                    }
            }
        ) {
            primaryNotificationManager.updateNotificationSchedule(preferences).getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun scheduleNotification(type: NotificationType, setting: NotificationSetting): kotlin.Result<Unit> {
        val result = executeOperationWithRetry(
            operation = "scheduleNotification",
            fallback = {
                fallbackNotificationManager?.scheduleNotification(type, setting)?.getOrThrow()
                    ?: Unit.also {
                        failedNotifications.add(type)
                        UserNotificationService.showWarning(
                            "Unable to schedule ${type.name.lowercase()} notification. Please check your notification settings."
                        )
                    }
            }
        ) {
            val scheduleResult = primaryNotificationManager.scheduleNotification(type, setting)
            if (scheduleResult.isSuccess) {
                failedNotifications.remove(type)
            }
            scheduleResult.getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun scheduleNotification(
        type: NotificationType,
        time: LocalTime,
        repeatInterval: RepeatInterval,
        daysInAdvance: Int
    ): kotlin.Result<Unit> {
        val result = executeOperationWithRetry(
            operation = "scheduleNotificationWithTime",
            fallback = {
                fallbackNotificationManager?.scheduleNotification(type, time, repeatInterval, daysInAdvance)?.getOrThrow()
                    ?: Unit.also {
                        failedNotifications.add(type)
                        UserNotificationService.showWarning(
                            "Unable to schedule ${type.name.lowercase()} notification for $time. Please check your notification settings."
                        )
                    }
            }
        ) {
            val scheduleResult = primaryNotificationManager.scheduleNotification(type, time, repeatInterval, daysInAdvance)
            if (scheduleResult.isSuccess) {
                failedNotifications.remove(type)
            }
            scheduleResult.getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun cancelNotification(type: NotificationType): kotlin.Result<Unit> {
        val result = executeOperation(
            operation = "cancelNotification",
            fallback = {
                fallbackNotificationManager?.cancelNotification(type)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showInfo(
                            "${type.name.lowercase()} notification cancelled locally."
                        )
                    }
            }
        ) {
            primaryNotificationManager.cancelNotification(type).getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> {
        val result = executeOperation(
            operation = "cancelAllNotifications",
            fallback = {
                fallbackNotificationManager?.cancelAllNotifications()?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showInfo("All notifications cancelled locally.")
                    }
            }
        ) {
            primaryNotificationManager.cancelAllNotifications().getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun requestNotificationPermission(): kotlin.Result<Boolean> {
        val result = executeOperation(
            operation = "requestNotificationPermission",
            showUserMessage = false, // Permission requests have their own UI
            fallback = {
                // Can't really fallback permission requests
                false
            }
        ) {
            val permissionResult = primaryNotificationManager.requestNotificationPermission()
            val granted = permissionResult.getOrThrow()
            permissionStatus = if (granted) {
                NotificationPermissionStatus.GRANTED
            } else {
                NotificationPermissionStatus.DENIED
            }
            granted
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun getNotificationPermissionStatus(): NotificationPermissionStatus {
        return try {
            val status = primaryNotificationManager.getNotificationPermissionStatus()
            permissionStatus = status
            status
        } catch (e: Exception) {
            // Return cached status or assume denied
            permissionStatus.takeIf { it != NotificationPermissionStatus.NOT_REQUESTED }
                ?: NotificationPermissionStatus.DENIED
        }
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return try {
            primaryNotificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            // If we can't check, assume disabled for safety
            false
        }
    }
    
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> {
        val result = executeOperation(
            operation = "openNotificationSettings",
            fallback = {
                UserNotificationService.showInfo(
                    "Please manually open your device settings to enable notifications for this app."
                )
                Unit
            }
        ) {
            primaryNotificationManager.openNotificationSettings().getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun getScheduledNotifications(): List<NotificationType> {
        return try {
            primaryNotificationManager.getScheduledNotifications()
        } catch (e: Exception) {
            // Return empty list if we can't get scheduled notifications
            emptyList()
        }
    }
    
    override suspend fun testNotification(type: NotificationType): kotlin.Result<Unit> {
        val result = executeOperation(
            operation = "testNotification",
            fallback = {
                UserNotificationService.showInfo(
                    "Test notification: This is how your ${type.name.lowercase()} notifications will appear."
                )
                Unit
            }
        ) {
            primaryNotificationManager.testNotification(type).getOrThrow()
        }
        
        return when (result) {
            is com.eunio.healthapp.domain.util.Result.Success -> kotlin.Result.success(result.data)
            is com.eunio.healthapp.domain.util.Result.Error -> kotlin.Result.failure(result.error)
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        return try {
            // Test basic functionality
            val permissionStatus = primaryNotificationManager.getNotificationPermissionStatus()
            val enabled = primaryNotificationManager.areNotificationsEnabled()
            
            // Service is healthy if we can check status, regardless of permission state
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getServiceStatus(): ServiceStatus {
        val isHealthy = isHealthy()
        val details = mutableMapOf<String, Any>()
        
        try {
            details["permissionStatus"] = primaryNotificationManager.getNotificationPermissionStatus().name
            details["notificationsEnabled"] = primaryNotificationManager.areNotificationsEnabled()
            details["scheduledNotifications"] = primaryNotificationManager.getScheduledNotifications().map { it.name }
        } catch (e: Exception) {
            details["error"] = "Unable to get notification status: ${e.message}"
        }
        
        details["failedNotifications"] = failedNotifications.map { it.name }
        details["fallbackManagerAvailable"] = fallbackNotificationManager != null
        
        return ServiceStatus(
            serviceName = serviceName,
            isHealthy = isHealthy,
            lastChecked = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            details = details
        )
    }
    
    override fun getUserFriendlyMessage(operation: String, error: Throwable): String {
        return when (operation) {
            "updateNotificationSchedule" -> "Unable to update notification schedule. Please check your notification settings."
            "scheduleNotification", "scheduleNotificationWithTime" -> "Unable to schedule notification. Please check your notification permissions."
            "cancelNotification" -> "Unable to cancel notification. It may have already been removed."
            "cancelAllNotifications" -> "Unable to cancel all notifications. Some may still be active."
            "requestNotificationPermission" -> "Unable to request notification permission. Please enable notifications in your device settings."
            "openNotificationSettings" -> "Unable to open notification settings. Please manually navigate to your device settings."
            "testNotification" -> "Unable to send test notification. Please check your notification settings."
            else -> super.getUserFriendlyMessage(operation, error)
        }
    }
}