package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import com.eunio.healthapp.platform.notification.MockPlatformNotificationService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlin.test.*

class NotificationManagerIntegrationTest {
    
    private val mockPlatformService = MockPlatformNotificationService()
    private val notificationManager = NotificationManagerImpl(mockPlatformService)
    
    @BeforeTest
    fun setup() {
        mockPlatformService.reset()
    }
    
    @Test
    fun `complete notification workflow with all types enabled`() = runTest {
        // Given - All notification types enabled with different times
        val preferences = NotificationPreferences(
            globalNotificationsEnabled = true,
            dailyLoggingReminder = NotificationSetting(
                enabled = true,
                time = LocalTime(9, 0),
                daysInAdvance = 1
            ),
            periodPredictionAlert = NotificationSetting(
                enabled = true,
                time = LocalTime(18, 0),
                daysInAdvance = 2
            ),
            ovulationAlert = NotificationSetting(
                enabled = true,
                time = LocalTime(12, 0),
                daysInAdvance = 1
            ),
            insightNotifications = NotificationSetting(
                enabled = true,
                time = LocalTime(20, 0),
                daysInAdvance = 1
            )
        )
        
        // When - Update notification schedule
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then - All notifications should be scheduled
        assertTrue(result.isSuccess)
        assertEquals(4, mockPlatformService.getScheduledNotificationCount())
        
        // Verify each notification is scheduled correctly
        val dailyLogging = mockPlatformService.getScheduledNotification(NotificationType.DAILY_LOGGING.id)
        assertNotNull(dailyLogging)
        assertEquals(LocalTime(9, 0), dailyLogging.time)
        assertEquals(RepeatInterval.DAILY, dailyLogging.repeatInterval)
        
        val periodPrediction = mockPlatformService.getScheduledNotification(NotificationType.PERIOD_PREDICTION.id)
        assertNotNull(periodPrediction)
        assertEquals(LocalTime(18, 0), periodPrediction.time)
        
        val ovulationAlert = mockPlatformService.getScheduledNotification(NotificationType.OVULATION_ALERT.id)
        assertNotNull(ovulationAlert)
        assertEquals(LocalTime(12, 0), ovulationAlert.time)
        
        val insights = mockPlatformService.getScheduledNotification(NotificationType.INSIGHTS.id)
        assertNotNull(insights)
        assertEquals(LocalTime(20, 0), insights.time)
    }
    
    @Test
    fun `partial notification workflow with some types disabled`() = runTest {
        // Given - Only some notification types enabled
        val preferences = NotificationPreferences(
            globalNotificationsEnabled = true,
            dailyLoggingReminder = NotificationSetting.defaultEnabled(),
            periodPredictionAlert = NotificationSetting.disabled(),
            ovulationAlert = NotificationSetting.defaultEnabled(),
            insightNotifications = NotificationSetting.disabled()
        )
        
        // When - Update notification schedule
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then - Only enabled notifications should be scheduled
        assertTrue(result.isSuccess)
        assertEquals(2, mockPlatformService.getScheduledNotificationCount())
        
        assertNotNull(mockPlatformService.getScheduledNotification(NotificationType.DAILY_LOGGING.id))
        assertNotNull(mockPlatformService.getScheduledNotification(NotificationType.OVULATION_ALERT.id))
        assertNull(mockPlatformService.getScheduledNotification(NotificationType.PERIOD_PREDICTION.id))
        assertNull(mockPlatformService.getScheduledNotification(NotificationType.INSIGHTS.id))
    }
    
    @Test
    fun `notification schedule update clears previous notifications`() = runTest {
        // Given - Initial schedule with default notifications (3 enabled)
        val initialPreferences = NotificationPreferences.withDefaults()
        notificationManager.updateNotificationSchedule(initialPreferences)
        assertEquals(3, mockPlatformService.getScheduledNotificationCount())
        
        // When - Update with only one notification enabled
        val updatedPreferences = NotificationPreferences(
            globalNotificationsEnabled = true,
            dailyLoggingReminder = NotificationSetting.defaultEnabled(),
            periodPredictionAlert = NotificationSetting.disabled(),
            ovulationAlert = NotificationSetting.disabled(),
            insightNotifications = NotificationSetting.disabled()
        )
        val result = notificationManager.updateNotificationSchedule(updatedPreferences)
        
        // Then - Only the enabled notification should remain
        assertTrue(result.isSuccess)
        assertEquals(1, mockPlatformService.getScheduledNotificationCount())
        assertNotNull(mockPlatformService.getScheduledNotification(NotificationType.DAILY_LOGGING.id))
    }
    
    @Test
    fun `permission denied prevents notification scheduling`() = runTest {
        // Given - Permission denied
        mockPlatformService.setPermissionStatus(NotificationPermissionStatus.DENIED)
        val preferences = NotificationPreferences.withDefaults()
        
        // When - Try to update notification schedule
        val result = notificationManager.updateNotificationSchedule(preferences)
        
        // Then - Should fail with permission error
        assertTrue(result.isFailure)
        assertEquals(0, mockPlatformService.getScheduledNotificationCount())
    }
    
    @Test
    fun `individual notification operations work correctly`() = runTest {
        // Test individual scheduling
        val result1 = notificationManager.scheduleNotification(
            NotificationType.DAILY_LOGGING,
            LocalTime(10, 30),
            RepeatInterval.WEEKLY,
            2
        )
        assertTrue(result1.isSuccess)
        assertEquals(1, mockPlatformService.getScheduledNotificationCount())
        
        val scheduled = mockPlatformService.getScheduledNotification(NotificationType.DAILY_LOGGING.id)
        assertNotNull(scheduled)
        assertEquals(LocalTime(10, 30), scheduled.time)
        assertEquals(RepeatInterval.WEEKLY, scheduled.repeatInterval)
        
        // Test individual cancellation
        val result2 = notificationManager.cancelNotification(NotificationType.DAILY_LOGGING)
        assertTrue(result2.isSuccess)
        assertEquals(0, mockPlatformService.getScheduledNotificationCount())
    }
    
    @Test
    fun `test notification functionality works`() = runTest {
        // When - Show test notification
        val result = notificationManager.testNotification(NotificationType.PERIOD_PREDICTION)
        
        // Then - Test notification should be shown
        assertTrue(result.isSuccess)
        val testNotificationId = "${NotificationType.PERIOD_PREDICTION.id}_test_shown"
        assertNotNull(mockPlatformService.getScheduledNotification(testNotificationId))
    }
    
    @Test
    fun `get scheduled notifications returns correct types`() = runTest {
        // Given - Schedule some notifications
        notificationManager.scheduleNotification(
            NotificationType.DAILY_LOGGING,
            NotificationSetting.defaultEnabled()
        )
        notificationManager.scheduleNotification(
            NotificationType.INSIGHTS,
            NotificationSetting.defaultEnabled()
        )
        
        // When - Get scheduled notifications
        val scheduled = notificationManager.getScheduledNotifications()
        
        // Then - Should return correct notification types
        assertEquals(2, scheduled.size)
        assertTrue(scheduled.contains(NotificationType.DAILY_LOGGING))
        assertTrue(scheduled.contains(NotificationType.INSIGHTS))
    }
    
    @Test
    fun `permission and settings queries work correctly`() = runTest {
        // Test permission status
        mockPlatformService.setPermissionStatus(NotificationPermissionStatus.GRANTED)
        assertEquals(NotificationPermissionStatus.GRANTED, notificationManager.getNotificationPermissionStatus())
        
        // Test notifications enabled
        mockPlatformService.setNotificationsEnabled(true)
        assertTrue(notificationManager.areNotificationsEnabled())
        
        mockPlatformService.setNotificationsEnabled(false)
        assertFalse(notificationManager.areNotificationsEnabled())
        
        // Test permission request
        val permissionResult = notificationManager.requestNotificationPermission()
        assertTrue(permissionResult.isSuccess)
        assertTrue(permissionResult.getOrNull() == true)
        
        // Test opening settings
        val settingsResult = notificationManager.openNotificationSettings()
        assertTrue(settingsResult.isSuccess)
    }
    
    @Test
    fun `error handling works correctly throughout the workflow`() = runTest {
        // Given - Platform service configured to fail
        mockPlatformService.setShouldFailOperations(true)
        val preferences = NotificationPreferences.withDefaults()
        
        // When - Try various operations
        val scheduleResult = notificationManager.updateNotificationSchedule(preferences)
        val cancelResult = notificationManager.cancelAllNotifications()
        val permissionResult = notificationManager.requestNotificationPermission()
        val testResult = notificationManager.testNotification(NotificationType.DAILY_LOGGING)
        
        // Then - All operations should fail gracefully
        assertTrue(scheduleResult.isFailure)
        assertTrue(cancelResult.isFailure)
        assertTrue(permissionResult.isFailure)
        assertTrue(testResult.isFailure)
        
        // But status queries should still work (they handle exceptions gracefully)
        assertEquals(NotificationPermissionStatus.GRANTED, notificationManager.getNotificationPermissionStatus())
        assertFalse(notificationManager.areNotificationsEnabled()) // Should return false on error
        assertTrue(notificationManager.getScheduledNotifications().isEmpty()) // Should return empty list on error
    }
}