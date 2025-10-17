package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.notification.RepeatInterval
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.NotificationPreferencesUiState
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlin.test.*

/**
 * Fake implementation of NotificationManager for testing
 */
class FakeNotificationManager : NotificationManager {
    private var permissionStatus = NotificationPermissionStatus.NOT_REQUESTED
    private var scheduledNotifications = mutableListOf<NotificationType>()
    private var shouldFailOnPermissionRequest = false
    private var shouldFailOnSchedule = false
    
    fun setPermissionStatus(status: NotificationPermissionStatus) {
        permissionStatus = status
    }
    
    fun setShouldFailOnPermissionRequest(shouldFail: Boolean) {
        shouldFailOnPermissionRequest = shouldFail
    }
    
    fun setShouldFailOnSchedule(shouldFail: Boolean) {
        shouldFailOnSchedule = shouldFail
    }
    
    override suspend fun updateNotificationSchedule(preferences: NotificationPreferences): kotlin.Result<Unit> {
        if (shouldFailOnSchedule) {
            return kotlin.Result.failure(Exception("Schedule failed"))
        }
        
        scheduledNotifications.clear()
        if (preferences.globalNotificationsEnabled) {
            if (preferences.dailyLoggingReminder.enabled) {
                scheduledNotifications.add(NotificationType.DAILY_LOGGING)
            }
            if (preferences.periodPredictionAlert.enabled) {
                scheduledNotifications.add(NotificationType.PERIOD_PREDICTION)
            }
            if (preferences.ovulationAlert.enabled) {
                scheduledNotifications.add(NotificationType.OVULATION_ALERT)
            }
            if (preferences.insightNotifications.enabled) {
                scheduledNotifications.add(NotificationType.INSIGHTS)
            }
        }
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun scheduleNotification(type: NotificationType, setting: NotificationSetting): kotlin.Result<Unit> {
        if (shouldFailOnSchedule) {
            return kotlin.Result.failure(Exception("Schedule failed"))
        }
        
        if (!scheduledNotifications.contains(type)) {
            scheduledNotifications.add(type)
        }
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun scheduleNotification(
        type: NotificationType,
        time: LocalTime,
        repeatInterval: RepeatInterval,
        daysInAdvance: Int
    ): kotlin.Result<Unit> {
        if (shouldFailOnSchedule) {
            return kotlin.Result.failure(Exception("Schedule failed"))
        }
        
        if (!scheduledNotifications.contains(type)) {
            scheduledNotifications.add(type)
        }
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun cancelNotification(type: NotificationType): kotlin.Result<Unit> {
        scheduledNotifications.remove(type)
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> {
        scheduledNotifications.clear()
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun requestNotificationPermission(): kotlin.Result<Boolean> {
        if (shouldFailOnPermissionRequest) {
            return kotlin.Result.failure(Exception("Permission request failed"))
        }
        
        val granted = permissionStatus != NotificationPermissionStatus.DENIED
        if (granted) {
            permissionStatus = NotificationPermissionStatus.GRANTED
        } else {
            permissionStatus = NotificationPermissionStatus.DENIED
        }
        return kotlin.Result.success(granted)
    }
    
    override suspend fun getNotificationPermissionStatus(): NotificationPermissionStatus {
        return permissionStatus
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        return permissionStatus == NotificationPermissionStatus.GRANTED
    }
    
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> {
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun getScheduledNotifications(): List<NotificationType> {
        return scheduledNotifications.toList()
    }
    
    override suspend fun testNotification(type: NotificationType): kotlin.Result<Unit> {
        if (permissionStatus != NotificationPermissionStatus.GRANTED) {
            return kotlin.Result.failure(Exception("Permission not granted"))
        }
        return kotlin.Result.success(Unit)
    }
}

/**
 * Unit tests for NotificationPreferencesViewModel.
 * Tests permission handling, notification scheduling, and state management.
 */
class NotificationPreferencesViewModelTest {
    
    @Test
    fun uiState_initialValues_areCorrect() {
        val uiState = NotificationPreferencesUiState()
        
        assertEquals(NotificationPreferences.default(), uiState.preferences)
        assertEquals(NotificationPermissionStatus.NOT_REQUESTED, uiState.permissionStatus)
        assertEquals(LoadingState.Idle, uiState.loadingState)
        assertFalse(uiState.isUpdating)
        assertFalse(uiState.isRequestingPermission)
        assertEquals(emptyList(), uiState.scheduledNotifications)
        assertFalse(uiState.showPermissionRationale)
        assertNull(uiState.testingNotification)
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isEnabled)
        assertNull(uiState.errorMessage)
        assertFalse(uiState.hasPreferences)
        assertFalse(uiState.areNotificationsPermitted)
        assertTrue(uiState.canRequestPermission)
        assertFalse(uiState.needsManualPermission)
        assertFalse(uiState.hasEnabledNotifications)
        assertFalse(uiState.areNotificationsEffectivelyEnabled)
    }
    
    @Test
    fun uiState_isLoading_trueWhenLoadingState() {
        val uiState = NotificationPreferencesUiState(
            loadingState = LoadingState.Loading
        )
        
        assertTrue(uiState.isLoading)
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenUpdating() {
        val uiState = NotificationPreferencesUiState(
            isUpdating = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenRequestingPermission() {
        val uiState = NotificationPreferencesUiState(
            isRequestingPermission = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_areNotificationsPermitted_trueWhenGranted() {
        val uiState = NotificationPreferencesUiState(
            permissionStatus = NotificationPermissionStatus.GRANTED
        )
        
        assertTrue(uiState.areNotificationsPermitted)
        assertFalse(uiState.canRequestPermission)
        assertFalse(uiState.needsManualPermission)
    }
    
    @Test
    fun uiState_canRequestPermission_trueWhenNotRequested() {
        val uiState = NotificationPreferencesUiState(
            permissionStatus = NotificationPermissionStatus.NOT_REQUESTED
        )
        
        assertTrue(uiState.canRequestPermission)
        assertFalse(uiState.areNotificationsPermitted)
        assertFalse(uiState.needsManualPermission)
    }
    
    @Test
    fun uiState_needsManualPermission_trueWhenDenied() {
        val uiState = NotificationPreferencesUiState(
            permissionStatus = NotificationPermissionStatus.DENIED
        )
        
        assertTrue(uiState.needsManualPermission)
        assertFalse(uiState.areNotificationsPermitted)
        assertFalse(uiState.canRequestPermission)
    }
    
    @Test
    fun uiState_hasEnabledNotifications_trueWhenAnyEnabled() {
        val preferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(enabled = true),
            periodPredictionAlert = NotificationSetting(enabled = false),
            ovulationAlert = NotificationSetting(enabled = false),
            insightNotifications = NotificationSetting(enabled = false),
            globalNotificationsEnabled = true
        )
        
        val uiState = NotificationPreferencesUiState(
            preferences = preferences
        )
        
        assertTrue(uiState.hasEnabledNotifications)
    }
    
    @Test
    fun uiState_areNotificationsEffectivelyEnabled_trueWhenPermittedAndEnabled() {
        val preferences = NotificationPreferences(
            globalNotificationsEnabled = true
        )
        
        val uiState = NotificationPreferencesUiState(
            preferences = preferences,
            permissionStatus = NotificationPermissionStatus.GRANTED
        )
        
        assertTrue(uiState.areNotificationsEffectivelyEnabled)
    }
    
    @Test
    fun uiState_getEnabledNotificationCount_worksCorrectly() {
        val preferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(enabled = true),
            periodPredictionAlert = NotificationSetting(enabled = true),
            ovulationAlert = NotificationSetting(enabled = false),
            insightNotifications = NotificationSetting(enabled = true),
            globalNotificationsEnabled = true
        )
        
        val uiState = NotificationPreferencesUiState(
            preferences = preferences
        )
        
        assertEquals(3, uiState.getEnabledNotificationCount())
    }
    
    @Test
    fun uiState_getScheduledNotificationCount_worksCorrectly() {
        val scheduledNotifications = listOf(
            NotificationType.DAILY_LOGGING,
            NotificationType.PERIOD_PREDICTION
        )
        
        val uiState = NotificationPreferencesUiState(
            scheduledNotifications = scheduledNotifications
        )
        
        assertEquals(2, uiState.getScheduledNotificationCount())
    }
    
    @Test
    fun uiState_isTestingNotification_worksCorrectly() {
        val uiState = NotificationPreferencesUiState(
            testingNotification = NotificationType.DAILY_LOGGING
        )
        
        assertTrue(uiState.isTestingNotification(NotificationType.DAILY_LOGGING))
        assertFalse(uiState.isTestingNotification(NotificationType.PERIOD_PREDICTION))
    }
    
    @Test
    fun fakeNotificationManager_worksCorrectly() = runTest {
        val fakeManager = FakeNotificationManager()
        
        // Test initial state
        assertEquals(NotificationPermissionStatus.NOT_REQUESTED, fakeManager.getNotificationPermissionStatus())
        assertFalse(fakeManager.areNotificationsEnabled())
        assertEquals(emptyList(), fakeManager.getScheduledNotifications())
        
        // Test permission request
        val permissionResult = fakeManager.requestNotificationPermission()
        assertTrue(permissionResult.isSuccess)
        assertTrue(permissionResult.getOrNull() == true)
        assertEquals(NotificationPermissionStatus.GRANTED, fakeManager.getNotificationPermissionStatus())
        assertTrue(fakeManager.areNotificationsEnabled())
        
        // Test scheduling notifications
        val preferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(enabled = true),
            periodPredictionAlert = NotificationSetting(enabled = true),
            globalNotificationsEnabled = true
        )
        
        val scheduleResult = fakeManager.updateNotificationSchedule(preferences)
        assertTrue(scheduleResult.isSuccess)
        
        val scheduled = fakeManager.getScheduledNotifications()
        assertEquals(2, scheduled.size)
        assertTrue(scheduled.contains(NotificationType.DAILY_LOGGING))
        assertTrue(scheduled.contains(NotificationType.PERIOD_PREDICTION))
        
        // Test cancelling all notifications
        val cancelResult = fakeManager.cancelAllNotifications()
        assertTrue(cancelResult.isSuccess)
        assertEquals(emptyList(), fakeManager.getScheduledNotifications())
    }
    
    @Test
    fun fakeNotificationManager_permissionDenied_worksCorrectly() = runTest {
        val fakeManager = FakeNotificationManager()
        fakeManager.setPermissionStatus(NotificationPermissionStatus.DENIED)
        
        val permissionResult = fakeManager.requestNotificationPermission()
        assertTrue(permissionResult.isSuccess)
        assertFalse(permissionResult.getOrNull() == true)
        assertEquals(NotificationPermissionStatus.DENIED, fakeManager.getNotificationPermissionStatus())
        assertFalse(fakeManager.areNotificationsEnabled())
    }
    
    @Test
    fun fakeNotificationManager_errorHandling_worksCorrectly() = runTest {
        val fakeManager = FakeNotificationManager()
        
        // Test permission request failure
        fakeManager.setShouldFailOnPermissionRequest(true)
        val permissionResult = fakeManager.requestNotificationPermission()
        assertTrue(permissionResult.isFailure)
        
        // Test schedule failure
        fakeManager.setShouldFailOnSchedule(true)
        val preferences = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(enabled = true),
            globalNotificationsEnabled = true
        )
        val scheduleResult = fakeManager.updateNotificationSchedule(preferences)
        assertTrue(scheduleResult.isFailure)
    }
    
    @Test
    fun fakeNotificationManager_testNotification_worksCorrectly() = runTest {
        val fakeManager = FakeNotificationManager()
        
        // Test without permission
        val noPermissionResult = fakeManager.testNotification(NotificationType.DAILY_LOGGING)
        assertTrue(noPermissionResult.isFailure)
        
        // Test with permission
        fakeManager.setPermissionStatus(NotificationPermissionStatus.GRANTED)
        val withPermissionResult = fakeManager.testNotification(NotificationType.DAILY_LOGGING)
        assertTrue(withPermissionResult.isSuccess)
    }
    
    @Test
    fun notificationPreferences_default_valuesAreCorrect() {
        val defaultPrefs = NotificationPreferences.default()
        
        assertFalse(defaultPrefs.dailyLoggingReminder.enabled)
        assertFalse(defaultPrefs.periodPredictionAlert.enabled)
        assertFalse(defaultPrefs.ovulationAlert.enabled)
        assertFalse(defaultPrefs.insightNotifications.enabled)
        assertTrue(defaultPrefs.globalNotificationsEnabled)
    }
    
    @Test
    fun notificationSetting_default_valuesAreCorrect() {
        val defaultSetting = NotificationSetting()
        
        assertFalse(defaultSetting.enabled)
        assertNull(defaultSetting.time)
        assertEquals(1, defaultSetting.daysInAdvance)
    }
    
    @Test
    fun notificationPreferences_hasEnabledNotifications_worksCorrectly() {
        val noEnabledPrefs = NotificationPreferences.default()
        assertFalse(noEnabledPrefs.hasEnabledNotifications())
        
        val enabledPrefs = NotificationPreferences(
            dailyLoggingReminder = NotificationSetting(enabled = true),
            globalNotificationsEnabled = true
        )
        assertTrue(enabledPrefs.hasEnabledNotifications())
    }
}