package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import com.eunio.healthapp.platform.notification.PlatformNotificationService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlin.test.*

class NotificationManagerTest {
    
    private val mockPlatformService = mockk<PlatformNotificationService>()
    private val notificationManager = NotificationManagerImpl(mockPlatformService)
    
    @BeforeTest
    fun setup() {
        clearAllMocks()
    }
    
    @Test
    fun `updateNotificationSchedule with global notifications disabled cancels all notifications`() = runTest {
        // Given
        val preferences = NotificationPreferences(globalNotificationsEnabled = false)
        coEvery { mockPlatformService.cancelAllNotifications() } returns Result.success(Unit)
        
        // When
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockPlatformService.cancelAllNotifications() }
        coVerify(exactly = 0) { mockPlatformService.scheduleRepeatingNotification(any(), any(), any(), any(), any()) }
    }
    
    @Test
    fun `updateNotificationSchedule with permission denied returns error`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            globalNotificationsEnabled = true,
            dailyLoggingReminder = NotificationSetting.defaultEnabled()
        )
        coEvery { mockPlatformService.cancelAllNotifications() } returns Result.success(Unit)
        coEvery { mockPlatformService.getPermissionStatus() } returns NotificationPermissionStatus.DENIED
        
        // When
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.PermissionError)
        assertEquals("Notification permission not granted", error.message)
    }
    
    @Test
    fun `updateNotificationSchedule with enabled notifications schedules them correctly`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            globalNotificationsEnabled = true,
            dailyLoggingReminder = NotificationSetting(
                enabled = true,
                time = LocalTime(9, 0),
                daysInAdvance = 1
            ),
            periodPredictionAlert = NotificationSetting(
                enabled = true,
                time = LocalTime(20, 0),
                daysInAdvance = 2
            ),
            ovulationAlert = NotificationSetting.disabled(),
            insightNotifications = NotificationSetting.disabled()
        )
        
        coEvery { mockPlatformService.cancelAllNotifications() } returns Result.success(Unit)
        coEvery { mockPlatformService.getPermissionStatus() } returns NotificationPermissionStatus.GRANTED
        coEvery { 
            mockPlatformService.scheduleRepeatingNotification(any(), any(), any(), any(), any()) 
        } returns Result.success(Unit)
        
        // When
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockPlatformService.cancelAllNotifications() }
        coVerify { 
            mockPlatformService.scheduleRepeatingNotification(
                NotificationType.DAILY_LOGGING.id,
                NotificationType.DAILY_LOGGING.title,
                NotificationType.DAILY_LOGGING.body,
                LocalTime(9, 0),
                RepeatInterval.DAILY
            )
        }
        coVerify { 
            mockPlatformService.scheduleRepeatingNotification(
                NotificationType.PERIOD_PREDICTION.id,
                NotificationType.PERIOD_PREDICTION.title,
                NotificationType.PERIOD_PREDICTION.body,
                LocalTime(20, 0),
                RepeatInterval.DAILY
            )
        }
        coVerify(exactly = 0) { 
            mockPlatformService.scheduleRepeatingNotification(
                NotificationType.OVULATION_ALERT.id,
                any(), any(), any(), any()
            )
        }
    }
    
    @Test
    fun `updateNotificationSchedule handles scheduling failures`() = runTest {
        // Given
        val preferences = NotificationPreferences(
            globalNotificationsEnabled = true,
            dailyLoggingReminder = NotificationSetting.defaultEnabled()
        )
        
        coEvery { mockPlatformService.cancelAllNotifications() } returns Result.success(Unit)
        coEvery { mockPlatformService.getPermissionStatus() } returns NotificationPermissionStatus.GRANTED
        coEvery { 
            mockPlatformService.scheduleRepeatingNotification(any(), any(), any(), any(), any()) 
        } returns Result.failure(AppError.ValidationError("Scheduling failed"))
        
        // When
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertTrue(error.message.contains("Failed to schedule some notifications"))
    }
    
    @Test
    fun `scheduleNotification with invalid setting returns error`() = runTest {
        // Given
        val invalidSetting = NotificationSetting(
            enabled = true,
            time = null, // Invalid: enabled but no time
            daysInAdvance = 1
        )
        
        // When
        val result = notificationManager.scheduleNotification(NotificationType.DAILY_LOGGING, invalidSetting)
        
        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertTrue(error.message.contains("Invalid notification setting"))
    }
    
    @Test
    fun `scheduleNotification with valid setting succeeds`() = runTest {
        // Given
        val validSetting = NotificationSetting.defaultEnabled()
        coEvery { 
            mockPlatformService.scheduleRepeatingNotification(any(), any(), any(), any(), any()) 
        } returns Result.success(Unit)
        
        // When
        val result = notificationManager.scheduleNotification(NotificationType.DAILY_LOGGING, validSetting)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            mockPlatformService.scheduleRepeatingNotification(
                NotificationType.DAILY_LOGGING.id,
                NotificationType.DAILY_LOGGING.title,
                NotificationType.DAILY_LOGGING.body,
                LocalTime(20, 0),
                RepeatInterval.DAILY
            )
        }
    }
    
    @Test
    fun `scheduleNotification with custom parameters works correctly`() = runTest {
        // Given
        val customTime = LocalTime(15, 30)
        val customRepeat = RepeatInterval.WEEKLY
        coEvery { 
            mockPlatformService.scheduleRepeatingNotification(any(), any(), any(), any(), any()) 
        } returns Result.success(Unit)
        
        // When
        val result = notificationManager.scheduleNotification(
            NotificationType.PERIOD_PREDICTION,
            customTime,
            customRepeat,
            3
        )
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            mockPlatformService.scheduleRepeatingNotification(
                NotificationType.PERIOD_PREDICTION.id,
                NotificationType.PERIOD_PREDICTION.title,
                NotificationType.PERIOD_PREDICTION.body,
                customTime,
                customRepeat
            )
        }
    }
    
    @Test
    fun `cancelNotification delegates to platform service`() = runTest {
        // Given
        coEvery { mockPlatformService.cancelNotification(any()) } returns Result.success(Unit)
        
        // When
        val result = notificationManager.cancelNotification(NotificationType.DAILY_LOGGING)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockPlatformService.cancelNotification(NotificationType.DAILY_LOGGING.id) }
    }
    
    @Test
    fun `cancelAllNotifications delegates to platform service`() = runTest {
        // Given
        coEvery { mockPlatformService.cancelAllNotifications() } returns Result.success(Unit)
        
        // When
        val result = notificationManager.cancelAllNotifications()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockPlatformService.cancelAllNotifications() }
    }
    
    @Test
    fun `requestNotificationPermission delegates to platform service`() = runTest {
        // Given
        coEvery { mockPlatformService.requestPermission() } returns Result.success(true)
        
        // When
        val result = notificationManager.requestNotificationPermission()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        coVerify { mockPlatformService.requestPermission() }
    }
    
    @Test
    fun `getNotificationPermissionStatus delegates to platform service`() = runTest {
        // Given
        coEvery { mockPlatformService.getPermissionStatus() } returns NotificationPermissionStatus.GRANTED
        
        // When
        val status = notificationManager.getNotificationPermissionStatus()
        
        // Then
        assertEquals(NotificationPermissionStatus.GRANTED, status)
        coVerify { mockPlatformService.getPermissionStatus() }
    }
    
    @Test
    fun `getNotificationPermissionStatus handles exceptions gracefully`() = runTest {
        // Given
        coEvery { mockPlatformService.getPermissionStatus() } throws RuntimeException("Platform error")
        
        // When
        val status = notificationManager.getNotificationPermissionStatus()
        
        // Then
        assertEquals(NotificationPermissionStatus.UNKNOWN, status)
    }
    
    @Test
    fun `areNotificationsEnabled delegates to platform service`() = runTest {
        // Given
        coEvery { mockPlatformService.areNotificationsEnabled() } returns true
        
        // When
        val enabled = notificationManager.areNotificationsEnabled()
        
        // Then
        assertTrue(enabled)
        coVerify { mockPlatformService.areNotificationsEnabled() }
    }
    
    @Test
    fun `areNotificationsEnabled handles exceptions gracefully`() = runTest {
        // Given
        coEvery { mockPlatformService.areNotificationsEnabled() } throws RuntimeException("Platform error")
        
        // When
        val enabled = notificationManager.areNotificationsEnabled()
        
        // Then
        assertFalse(enabled)
    }
    
    @Test
    fun `openNotificationSettings delegates to platform service`() = runTest {
        // Given
        coEvery { mockPlatformService.openNotificationSettings() } returns Result.success(Unit)
        
        // When
        val result = notificationManager.openNotificationSettings()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockPlatformService.openNotificationSettings() }
    }
    
    @Test
    fun `getScheduledNotifications returns mapped notification types`() = runTest {
        // Given
        val scheduledIds = listOf(
            NotificationType.DAILY_LOGGING.id,
            NotificationType.PERIOD_PREDICTION.id,
            "unknown_id"
        )
        coEvery { mockPlatformService.getScheduledNotificationIds() } returns scheduledIds
        
        // When
        val notifications = notificationManager.getScheduledNotifications()
        
        // Then
        assertEquals(2, notifications.size)
        assertTrue(notifications.contains(NotificationType.DAILY_LOGGING))
        assertTrue(notifications.contains(NotificationType.PERIOD_PREDICTION))
        assertFalse(notifications.any { it.id == "unknown_id" })
    }
    
    @Test
    fun `getScheduledNotifications handles exceptions gracefully`() = runTest {
        // Given
        coEvery { mockPlatformService.getScheduledNotificationIds() } throws RuntimeException("Platform error")
        
        // When
        val notifications = notificationManager.getScheduledNotifications()
        
        // Then
        assertTrue(notifications.isEmpty())
    }
    
    @Test
    fun `testNotification shows test notification with correct parameters`() = runTest {
        // Given
        coEvery { 
            mockPlatformService.showTestNotification(any(), any(), any()) 
        } returns Result.success(Unit)
        
        // When
        val result = notificationManager.testNotification(NotificationType.INSIGHTS)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            mockPlatformService.showTestNotification(
                NotificationType.INSIGHTS.id,
                "${NotificationType.INSIGHTS.title} (Test)",
                NotificationType.INSIGHTS.body
            )
        }
    }
}