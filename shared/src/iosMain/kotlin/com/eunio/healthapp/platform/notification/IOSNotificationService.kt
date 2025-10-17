package com.eunio.healthapp.platform.notification

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalTime
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import kotlin.coroutines.resume

/**
 * iOS-specific implementation of PlatformNotificationService.
 * Uses UserNotifications framework for iOS 10+ notification management.
 */
class IOSNotificationService : PlatformNotificationService {
    
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    
    override suspend fun initialize(): Result<Unit> {
        return try {
            setupNotificationCategories()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to initialize iOS notification service: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun scheduleRepeatingNotification(
        id: String,
        title: String,
        body: String,
        time: LocalTime,
        repeatInterval: RepeatInterval
    ): Result<Unit> {
        return try {
            // Check permission first
            val permissionStatus = getPermissionStatus()
            if (!permissionStatus.canShowNotifications()) {
                return Result.failure(
                    AppError.PermissionError(
                        message = "Notification permission not granted",
                        requiredPermission = "notifications"
                    )
                )
            }
            
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound())
                setCategoryIdentifier(getCategoryForNotification(id))
            }
            
            val trigger = createTrigger(time, repeatInterval)
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = id,
                content = content,
                trigger = trigger
            )
            
            // Use continuation to handle the async callback
            return suspendCancellableCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        continuation.resume(
                            Result.failure(
                                AppError.ValidationError(
                                    message = "Failed to schedule notification: ${error.localizedDescription}",
                                    cause = Exception(error.localizedDescription)
                                )
                            )
                        )
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to schedule repeating notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun scheduleOneTimeNotification(
        id: String,
        title: String,
        body: String,
        triggerTimeMillis: Long
    ): Result<Unit> {
        return try {
            val permissionStatus = getPermissionStatus()
            if (!permissionStatus.canShowNotifications()) {
                return Result.failure(
                    AppError.PermissionError(
                        message = "Notification permission not granted",
                        requiredPermission = "notifications"
                    )
                )
            }
            
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound())
                setCategoryIdentifier(getCategoryForNotification(id))
            }
            
            val triggerDate = NSDate.dateWithTimeIntervalSince1970(triggerTimeMillis / 1000.0)
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = triggerDate.timeIntervalSinceNow,
                repeats = false
            )
            
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = id,
                content = content,
                trigger = trigger
            )
            
            return suspendCancellableCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        continuation.resume(
                            Result.failure(
                                AppError.ValidationError(
                                    message = "Failed to schedule one-time notification: ${error.localizedDescription}",
                                    cause = Exception(error.localizedDescription)
                                )
                            )
                        )
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to schedule one-time notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun cancelNotification(id: String): Result<Unit> {
        return try {
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(id))
            notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(id))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to cancel notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            notificationCenter.removeAllPendingNotificationRequests()
            notificationCenter.removeAllDeliveredNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to cancel all notifications: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun requestPermission(): Result<Boolean> {
        return try {
            suspendCancellableCoroutine { continuation ->
                notificationCenter.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, error ->
                    if (error != null) {
                        continuation.resume(
                            Result.failure(
                                AppError.PermissionError(
                                    message = "Failed to request notification permission: ${error.localizedDescription}",
                                    requiredPermission = "notifications",
                                    cause = Exception(error.localizedDescription)
                                )
                            )
                        )
                    } else {
                        continuation.resume(Result.success(granted))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.PermissionError(
                    message = "Failed to request notification permission: ${e.message}",
                    requiredPermission = "notifications",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun getPermissionStatus(): NotificationPermissionStatus {
        return try {
            suspendCancellableCoroutine { continuation ->
                notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                    val status = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized -> NotificationPermissionStatus.GRANTED
                        UNAuthorizationStatusDenied -> NotificationPermissionStatus.DENIED
                        UNAuthorizationStatusNotDetermined -> NotificationPermissionStatus.NOT_REQUESTED
                        UNAuthorizationStatusProvisional -> NotificationPermissionStatus.GRANTED
                        else -> NotificationPermissionStatus.UNKNOWN
                    }
                    continuation.resume(status)
                }
            }
        } catch (e: Exception) {
            NotificationPermissionStatus.UNKNOWN
        }
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return try {
            suspendCancellableCoroutine { continuation ->
                notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                    val enabled = settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                            settings?.authorizationStatus == UNAuthorizationStatusProvisional
                    continuation.resume(enabled)
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun openNotificationSettings(): Result<Unit> {
        return try {
            val settingsUrl = NSURL.URLWithString("app-settings:")
            if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                UIApplication.sharedApplication.openURL(settingsUrl)
                Result.success(Unit)
            } else {
                Result.failure(
                    AppError.ValidationError(
                        message = "Cannot open notification settings"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to open notification settings: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun showTestNotification(
        id: String,
        title: String,
        body: String
    ): Result<Unit> {
        return try {
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound())
                setCategoryIdentifier(getCategoryForNotification(id))
            }
            
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = 1.0, // Show after 1 second
                repeats = false
            )
            
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "${id}_test",
                content = content,
                trigger = trigger
            )
            
            suspendCancellableCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        continuation.resume(
                            Result.failure(
                                AppError.ValidationError(
                                    message = "Failed to show test notification: ${error.localizedDescription}",
                                    cause = Exception(error.localizedDescription)
                                )
                            )
                        )
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.ValidationError(
                    message = "Failed to show test notification: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun getScheduledNotificationIds(): List<String> {
        return try {
            suspendCancellableCoroutine { continuation ->
                notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
                    val ids = requests?.mapNotNull { request ->
                        (request as? platform.UserNotifications.UNNotificationRequest)?.identifier
                    } ?: emptyList()
                    continuation.resume(ids)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun createTrigger(time: LocalTime, repeatInterval: RepeatInterval): UNNotificationTrigger {
        return when (repeatInterval) {
            RepeatInterval.DAILY -> {
                val dateComponents = NSDateComponents().apply {
                    hour = time.hour.toLong()
                    minute = time.minute.toLong()
                }
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = dateComponents,
                    repeats = true
                )
            }
            RepeatInterval.WEEKLY -> {
                val dateComponents = NSDateComponents().apply {
                    weekday = 2L // Monday (1 = Sunday, 2 = Monday, etc.)
                    hour = time.hour.toLong()
                    minute = time.minute.toLong()
                }
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = dateComponents,
                    repeats = true
                )
            }
            RepeatInterval.MONTHLY -> {
                val dateComponents = NSDateComponents().apply {
                    day = 1L // First day of month
                    hour = time.hour.toLong()
                    minute = time.minute.toLong()
                }
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = dateComponents,
                    repeats = true
                )
            }
            RepeatInterval.NONE -> {
                val calendar = NSCalendar.currentCalendar()
                val now = NSDate()
                val dateComponents = calendar.components(
                    NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
                    fromDate = now
                ).apply {
                    hour = time.hour.toLong()
                    minute = time.minute.toLong()
                }
                
                val triggerDate = calendar.dateFromComponents(dateComponents)
                val timeInterval = triggerDate?.timeIntervalSinceNow ?: 60.0
                
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = if (timeInterval > 0) timeInterval else timeInterval + 86400, // Add a day if time has passed
                    repeats = false
                )
            }
        }
    }
    
    private fun setupNotificationCategories() {
        val healthCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "health_tracking",
            actions = listOf<UNNotificationAction>(),
            intentIdentifiers = listOf<String>(),
            options = 0u
        )
        
        val cycleCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "cycle_tracking", 
            actions = listOf<UNNotificationAction>(),
            intentIdentifiers = listOf<String>(),
            options = 0u
        )
        
        val insightsCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = "insights",
            actions = listOf<UNNotificationAction>(),
            intentIdentifiers = listOf<String>(),
            options = 0u
        )
        
        val categories = setOf(healthCategory, cycleCategory, insightsCategory)
        notificationCenter.setNotificationCategories(categories)
    }
    
    private fun getCategoryForNotification(id: String): String {
        return when {
            id.contains("daily_logging") -> "health_tracking"
            id.contains("period") || id.contains("ovulation") -> "cycle_tracking"
            id.contains("insight") -> "insights"
            else -> "health_tracking"
        }
    }
}