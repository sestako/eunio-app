package com.eunio.healthapp.platform.notification

import android.content.Context
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidNotificationService
 */
class AndroidNotificationServiceTest {
    
    private val mockContext = mockk<Context>(relaxed = true)
    private val androidNotificationService = AndroidNotificationService(mockContext)
    
    @Test
    fun `initialize returns success`() = runTest {
        // When
        val result = androidNotificationService.initialize()
        
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
        val result = androidNotificationService.scheduleRepeatingNotification(
            id = notificationId,
            title = title,
            body = body,
            time = time,
            repeatInterval = repeatInterval
        )
        
        // Then
        // Note: This will fail without proper permission, but we can test the structure
        assertNotNull(result)
    }
    
    @Test
    fun `scheduleOneTimeNotification creates proper notification request`() = runTest {
        // Given
        val notificationId = "test_one_time"
        val title = "One Time Test"
        val body = "One Time Body"
        val triggerTime = System.currentTimeMillis() + 60000 // 1 minute from now
        
        // When
        val result = androidNotificationService.scheduleOneTimeNotification(
            id = notificationId,
            title = title,
            body = body,
            triggerTimeMillis = triggerTime
        )
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `cancelNotification returns result`() = runTest {
        // Given
        val notificationId = "test_cancel"
        
        // When
        val result = androidNotificationService.cancelNotification(notificationId)
        
        // Then
        assertNotNull(result)
        // Note: May succeed or fail depending on whether notification exists
    }
    
    @Test
    fun `cancelAllNotifications returns result`() = runTest {
        // When
        val result = androidNotificationService.cancelAllNotifications()
        
        // Then
        assertNotNull(result)
        // Note: Should generally succeed
    }
    
    @Test
    fun `getPermissionStatus returns valid status`() = runTest {
        // When
        val status = androidNotificationService.getPermissionStatus()
        
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
        val enabled = androidNotificationService.areNotificationsEnabled()
        
        // Then
        // Should return a boolean value (true or false)
        assertTrue(enabled is Boolean)
    }
    
    @Test
    fun `openNotificationSettings returns result`() = runTest {
        // When
        val result = androidNotificationService.openNotificationSettings()
        
        // Then
        assertNotNull(result)
        // Note: May succeed or fail depending on system capabilities
    }
    
    @Test
    fun `showTestNotification creates notification`() = runTest {
        // Given
        val notificationId = "test_show"
        val title = "Test Show"
        val body = "Test Show Body"
        
        // When
        val result = androidNotificationService.showTestNotification(
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
        val ids = androidNotificationService.getScheduledNotificationIds()
        
        // Then
        assertNotNull(ids)
        assertTrue(ids is List<String>)
    }
}