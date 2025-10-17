package com.eunio.healthapp.platform.notification

import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import kotlinx.datetime.LocalTime

/**
 * Platform-specific notification service interface.
 * This interface is implemented differently for Android and iOS to handle
 * platform-specific notification APIs and behaviors.
 */
interface PlatformNotificationService {
    
    /**
     * Schedules a repeating notification with the specified parameters.
     * 
     * @param id Unique identifier for the notification
     * @param title Notification title
     * @param body Notification body text
     * @param time Time of day to show the notification
     * @param repeatInterval How often to repeat the notification
     * @return Result indicating success or failure
     */
    suspend fun scheduleRepeatingNotification(
        id: String,
        title: String,
        body: String,
        time: LocalTime,
        repeatInterval: RepeatInterval
    ): Result<Unit>
    
    /**
     * Schedules a one-time notification.
     * 
     * @param id Unique identifier for the notification
     * @param title Notification title
     * @param body Notification body text
     * @param triggerTimeMillis When to trigger the notification (timestamp)
     * @return Result indicating success or failure
     */
    suspend fun scheduleOneTimeNotification(
        id: String,
        title: String,
        body: String,
        triggerTimeMillis: Long
    ): Result<Unit>
    
    /**
     * Cancels a specific notification by ID.
     * 
     * @param id The notification ID to cancel
     * @return Result indicating success or failure
     */
    suspend fun cancelNotification(id: String): Result<Unit>
    
    /**
     * Cancels all scheduled notifications.
     * 
     * @return Result indicating success or failure
     */
    suspend fun cancelAllNotifications(): Result<Unit>
    
    /**
     * Requests notification permission from the user.
     * 
     * @return Result with true if permission granted, false otherwise
     */
    suspend fun requestPermission(): Result<Boolean>
    
    /**
     * Gets the current notification permission status.
     * 
     * @return Current permission status
     */
    suspend fun getPermissionStatus(): NotificationPermissionStatus
    
    /**
     * Checks if notifications are enabled in system settings.
     * 
     * @return true if enabled, false otherwise
     */
    suspend fun areNotificationsEnabled(): Boolean
    
    /**
     * Opens the system notification settings for this app.
     * 
     * @return Result indicating success or failure
     */
    suspend fun openNotificationSettings(): Result<Unit>
    
    /**
     * Shows an immediate test notification.
     * 
     * @param id Unique identifier for the test notification
     * @param title Test notification title
     * @param body Test notification body
     * @return Result indicating success or failure
     */
    suspend fun showTestNotification(
        id: String,
        title: String,
        body: String
    ): Result<Unit>
    
    /**
     * Gets a list of currently scheduled notification IDs.
     * 
     * @return List of scheduled notification IDs
     */
    suspend fun getScheduledNotificationIds(): List<String>
    
    /**
     * Initializes the notification service.
     * Called during app startup to set up notification channels (Android)
     * or configure notification categories (iOS).
     * 
     * @return Result indicating success or failure
     */
    suspend fun initialize(): Result<Unit>
}