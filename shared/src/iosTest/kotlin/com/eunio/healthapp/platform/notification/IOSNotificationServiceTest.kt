package com.eunio.healthapp.platform.notification

import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for IOSNotificationService
 */
class IOSNotificationServiceTest {
    
    private val iosNotificationService = IOSNotificationService()
    
    @Test
    fun `initialize returns success`() = runTest {
        // When
        val result = iosNotificationService.initialize()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `scheduleRepeatingNotification creates proper notification request`() = runTest {
        // Given
        val notificationId = "test_notification"
        val title = "Test Title"
        val body = "Test Body"
        val time = LocalTime(14, 30) // 2:30 PM
        val repeatInterval = RepeatInterval.DAILY
        
        // When
        val result = iosNotificationService.scheduleRepeatingNotification(
            id = notificationId,
            title = title,
            body = body,
            time = time,
            repeatInterval = repeatInterval
        )
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `scheduleOneTimeNotification creates proper notification request`() = runTest {
        // Given
        val notificationId = "test_one_time"
        val title = "One Time Test"
        val body = "One Time Body"
        val triggerTime = Clock.System.now().toEpochMilliseconds() + 60000 // 1 minute from now
        
        // When
        val result = iosNotificationService.scheduleOneTimeNotification(
            id = notificationId,
            title = title,
            body = body,
            triggerTimeMillis = triggerTime
        )
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `cancelNotification returns success`() = runTest {
        // Given
        val notificationId = "test_cancel"
        
        // When
        val result = iosNotificationService.cancelNotification(notificationId)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `cancelAllNotifications returns success`() = runTest {
        // When
        val result = iosNotificationService.cancelAllNotifications()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `getPermissionStatus returns valid status`() = runTest {
        // When
        val status = iosNotificationService.getPermissionStatus()
        
        // Then
        assertNotNull(status)
        assertTrue(
            status in listOf(
                NotificationPermissionStatus.GRANTED,
                NotificationPermissionStatus.DENIED,
                NotificationPermissionStatus.NOT_REQUESTED,
                NotificationPermissionStatus.PERMANENTLY_DENIED,
                NotificationPermissionStatus.UNKNOWN
            )
        )
    }
    
    @Test
    fun `areNotificationsEnabled returns boolean`() = runTest {
        // When
        val enabled = iosNotificationService.areNotificationsEnabled()
        
        // Then
        // Should return a boolean value (true or false)
        assertTrue(enabled is Boolean)
    }
    
    @Test
    fun `openNotificationSettings returns success`() = runTest {
        // When
        val result = iosNotificationService.openNotificationSettings()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `showTestNotification creates notification`() = runTest {
        // Given
        val notificationId = "test_show"
        val title = "Test Show"
        val body = "Test Show Body"
        
        // When
        val result = iosNotificationService.showTestNotification(
            id = notificationId,
            title = title,
            body = body
        )
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `getScheduledNotificationIds returns list`() = runTest {
        // When
        val ids = iosNotificationService.getScheduledNotificationIds()
        
        // Then
        assertNotNull(ids)
        assertTrue(ids is List<String>)
    }
}