package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import kotlinx.datetime.LocalTime

/**
 * Interface for managing notifications across platforms.
 * Handles scheduling, cancellation, and permission management for health app notifications.
 */
interface NotificationManager {
    
    /**
     * Updates the entire notification schedule based on user preferences.
     * Cancels existing notifications and schedules new ones based on enabled preferences.
     * 
     * @param preferences The notification preferences to apply
     * @return Result indicating success or failure with error details
     */
    suspend fun updateNotificationSchedule(preferences: NotificationPreferences): kotlin.Result<Unit>
    
    /**
     * Schedules a single notification with the specified settings.
     * 
     * @param type The type of notification to schedule
     * @param setting The notification settings including time and repeat options
     * @return Result indicating success or failure with error details
     */
    suspend fun scheduleNotification(
        type: NotificationType, 
        setting: NotificationSetting
    ): kotlin.Result<Unit>
    
    /**
     * Schedules a notification with custom parameters.
     * 
     * @param type The type of notification to schedule
     * @param time The time to show the notification
     * @param repeatInterval How often to repeat the notification
     * @param daysInAdvance How many days in advance to show the notification (for cycle-related notifications)
     * @return Result indicating success or failure with error details
     */
    suspend fun scheduleNotification(
        type: NotificationType,
        time: LocalTime,
        repeatInterval: RepeatInterval = RepeatInterval.DAILY,
        daysInAdvance: Int = 1
    ): kotlin.Result<Unit>
    
    /**
     * Cancels a specific type of notification.
     * 
     * @param type The type of notification to cancel
     * @return Result indicating success or failure with error details
     */
    suspend fun cancelNotification(type: NotificationType): kotlin.Result<Unit>
    
    /**
     * Cancels all scheduled notifications.
     * 
     * @return Result indicating success or failure with error details
     */
    suspend fun cancelAllNotifications(): kotlin.Result<Unit>
    
    /**
     * Requests notification permission from the user.
     * On iOS, this shows the system permission dialog.
     * On Android, this handles the permission request flow.
     * 
     * @return Result with true if permission was granted, false otherwise
     */
    suspend fun requestNotificationPermission(): kotlin.Result<Boolean>
    
    /**
     * Checks the current notification permission status.
     * 
     * @return The current permission status
     */
    suspend fun getNotificationPermissionStatus(): NotificationPermissionStatus
    
    /**
     * Checks if notifications are currently enabled in system settings.
     * This can be different from permission status on some platforms.
     * 
     * @return true if notifications are enabled, false otherwise
     */
    suspend fun areNotificationsEnabled(): Boolean
    
    /**
     * Opens the system settings for notification permissions.
     * Useful when user needs to manually enable notifications.
     * 
     * @return Result indicating if settings were successfully opened
     */
    suspend fun openNotificationSettings(): kotlin.Result<Unit>
    
    /**
     * Gets a list of currently scheduled notifications.
     * Useful for debugging and showing user what notifications are active.
     * 
     * @return List of scheduled notification types
     */
    suspend fun getScheduledNotifications(): List<NotificationType>
    
    /**
     * Tests a notification by showing it immediately.
     * Useful for letting users preview notification settings.
     * 
     * @param type The type of notification to test
     * @return Result indicating success or failure
     */
    suspend fun testNotification(type: NotificationType): kotlin.Result<Unit>
}