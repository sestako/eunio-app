package com.eunio.healthapp.platform.notification

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import kotlinx.datetime.LocalTime

/**
 * Mock implementation of PlatformNotificationService for testing purposes.
 * Provides controllable behavior for testing notification management logic.
 */
class MockPlatformNotificationService : PlatformNotificationService {
    
    private val scheduledNotifications = mutableMapOf<String, ScheduledNotification>()
    private var permissionStatus = NotificationPermissionStatus.GRANTED
    private var notificationsEnabled = true
    private var shouldFailOperations = false
    private var initializeCalled = false
    
    data class ScheduledNotification(
        val id: String,
        val title: String,
        val body: String,
        val time: LocalTime,
        val repeatInterval: RepeatInterval
    )
    
    // Test control methods
    fun setPermissionStatus(status: NotificationPermissionStatus) {
        permissionStatus = status
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }
    
    fun setShouldFailOperations(shouldFail: Boolean) {
        shouldFailOperations = shouldFail
    }
    
    fun getScheduledNotification(id: String): ScheduledNotification? {
        return scheduledNotifications[id]
    }
    
    fun getScheduledNotificationCount(): Int {
        return scheduledNotifications.size
    }
    
    fun isInitializeCalled(): Boolean {
        return initializeCalled
    }
    
    fun reset() {
        scheduledNotifications.clear()
        permissionStatus = NotificationPermissionStatus.GRANTED
        notificationsEnabled = true
        shouldFailOperations = false
        initializeCalled = false
    }
    
    // PlatformNotificationService implementation
    override suspend fun initialize(): Result<Unit> {
        initializeCalled = true
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock initialization failed"))
        } else {
            Result.success(Unit)
        }
    }
    
    override suspend fun scheduleRepeatingNotification(
        id: String,
        title: String,
        body: String,
        time: LocalTime,
        repeatInterval: RepeatInterval
    ): Result<Unit> {
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock scheduling failed"))
        } else {
            scheduledNotifications[id] = ScheduledNotification(id, title, body, time, repeatInterval)
            Result.success(Unit)
        }
    }
    
    override suspend fun scheduleOneTimeNotification(
        id: String,
        title: String,
        body: String,
        triggerTimeMillis: Long
    ): Result<Unit> {
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock one-time scheduling failed"))
        } else {
            // For testing, we'll store it as a repeating notification with NONE interval
            scheduledNotifications[id] = ScheduledNotification(
                id, title, body, LocalTime(0, 0), RepeatInterval.NONE
            )
            Result.success(Unit)
        }
    }
    
    override suspend fun cancelNotification(id: String): Result<Unit> {
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock cancellation failed"))
        } else {
            scheduledNotifications.remove(id)
            Result.success(Unit)
        }
    }
    
    override suspend fun cancelAllNotifications(): Result<Unit> {
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock cancel all failed"))
        } else {
            scheduledNotifications.clear()
            Result.success(Unit)
        }
    }
    
    override suspend fun requestPermission(): Result<Boolean> {
        return if (shouldFailOperations) {
            Result.failure(AppError.PermissionError("Mock permission request failed", "notifications"))
        } else {
            val granted = permissionStatus == NotificationPermissionStatus.GRANTED
            Result.success(granted)
        }
    }
    
    override suspend fun getPermissionStatus(): NotificationPermissionStatus {
        return permissionStatus
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return if (shouldFailOperations) {
            false
        } else {
            notificationsEnabled
        }
    }
    
    override suspend fun openNotificationSettings(): Result<Unit> {
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock open settings failed"))
        } else {
            Result.success(Unit)
        }
    }
    
    override suspend fun showTestNotification(
        id: String,
        title: String,
        body: String
    ): Result<Unit> {
        return if (shouldFailOperations) {
            Result.failure(AppError.ValidationError("Mock test notification failed"))
        } else {
            // For testing, we'll just track that it was called
            scheduledNotifications["${id}_test_shown"] = ScheduledNotification(
                id, title, body, LocalTime(0, 0), RepeatInterval.NONE
            )
            Result.success(Unit)
        }
    }
    
    override suspend fun getScheduledNotificationIds(): List<String> {
        return scheduledNotifications.keys.toList()
    }
}