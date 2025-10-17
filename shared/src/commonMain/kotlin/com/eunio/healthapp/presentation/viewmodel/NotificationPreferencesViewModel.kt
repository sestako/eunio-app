package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.domain.model.settings.NotificationSetting
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.NotificationPreferencesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

/**
 * ViewModel for notification preferences screen with permission handling.
 * Manages notification settings, permission requests, and system notification scheduling.
 */
class NotificationPreferencesViewModel(
    private val settingsManager: SettingsManager,
    private val notificationManager: NotificationManager
) : BaseViewModel<NotificationPreferencesUiState>() {
    
    override val initialState = NotificationPreferencesUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    private val _showPermissionDialog = MutableSharedFlow<Unit>()
    val showPermissionDialog: SharedFlow<Unit> = _showPermissionDialog.asSharedFlow()
    
    private val _openSystemSettings = MutableSharedFlow<Unit>()
    val openSystemSettings: SharedFlow<Unit> = _openSystemSettings.asSharedFlow()
    
    init {
        loadNotificationPreferences()
        checkNotificationPermission()
        observeSettingsChanges()
        loadScheduledNotifications()
    }
    
    /**
     * Loads the current notification preferences.
     */
    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            settingsManager.getUserSettings()
                .onSuccess { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.notificationPreferences,
                            loadingState = LoadingState.Success(settings.notificationPreferences)
                        )
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error("Failed to load notification preferences: ${error.message}")
                        )
                    }
                    _showErrorMessage.emit("Failed to load notification preferences")
                }
        }
    }
    
    /**
     * Checks the current notification permission status.
     */
    private fun checkNotificationPermission() {
        viewModelScope.launch {
            val permissionStatus = notificationManager.getNotificationPermissionStatus()
            updateState { it.copy(permissionStatus = permissionStatus) }
        }
    }
    
    /**
     * Observes settings changes for reactive updates.
     */
    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsManager.observeSettingsChanges()
                .catch { error ->
                    _showErrorMessage.emit("Settings sync error: ${error.message}")
                }
                .collect { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.notificationPreferences,
                            loadingState = LoadingState.Success(settings.notificationPreferences)
                        )
                    }
                }
        }
    }
    
    /**
     * Loads the list of currently scheduled notifications.
     */
    private fun loadScheduledNotifications() {
        viewModelScope.launch {
            val scheduledNotifications = notificationManager.getScheduledNotifications()
            updateState { it.copy(scheduledNotifications = scheduledNotifications) }
        }
    }
    
    /**
     * Toggles global notification preferences.
     * 
     * @param enabled Whether global notifications should be enabled
     */
    fun toggleGlobalNotifications(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !uiState.value.areNotificationsPermitted) {
                requestNotificationPermission()
                return@launch
            }
            
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(globalNotificationsEnabled = enabled)
            
            updateNotificationPreferences(newPreferences)
        }
    }
    
    /**
     * Updates daily logging reminder settings.
     * 
     * @param enabled Whether the reminder should be enabled
     * @param time The time for the reminder (optional)
     */
    fun updateDailyLoggingReminder(enabled: Boolean, time: LocalTime? = null) {
        viewModelScope.launch {
            if (enabled && !uiState.value.areNotificationsPermitted) {
                requestNotificationPermission()
                return@launch
            }
            
            val currentPreferences = uiState.value.preferences
            val newSetting = currentPreferences.dailyLoggingReminder.copy(
                enabled = enabled,
                time = time ?: currentPreferences.dailyLoggingReminder.time ?: LocalTime(20, 0)
            )
            val newPreferences = currentPreferences.copy(dailyLoggingReminder = newSetting)
            
            updateNotificationPreferences(newPreferences)
        }
    }
    
    /**
     * Updates period prediction alert settings.
     * 
     * @param enabled Whether the alert should be enabled
     * @param daysInAdvance How many days in advance to show the alert
     */
    fun updatePeriodPredictionAlert(enabled: Boolean, daysInAdvance: Int = 1) {
        viewModelScope.launch {
            if (enabled && !uiState.value.areNotificationsPermitted) {
                requestNotificationPermission()
                return@launch
            }
            
            val currentPreferences = uiState.value.preferences
            val newSetting = currentPreferences.periodPredictionAlert.copy(
                enabled = enabled,
                daysInAdvance = daysInAdvance
            )
            val newPreferences = currentPreferences.copy(periodPredictionAlert = newSetting)
            
            updateNotificationPreferences(newPreferences)
        }
    }
    
    /**
     * Updates ovulation alert settings.
     * 
     * @param enabled Whether the alert should be enabled
     * @param daysInAdvance How many days in advance to show the alert
     */
    fun updateOvulationAlert(enabled: Boolean, daysInAdvance: Int = 1) {
        viewModelScope.launch {
            if (enabled && !uiState.value.areNotificationsPermitted) {
                requestNotificationPermission()
                return@launch
            }
            
            val currentPreferences = uiState.value.preferences
            val newSetting = currentPreferences.ovulationAlert.copy(
                enabled = enabled,
                daysInAdvance = daysInAdvance
            )
            val newPreferences = currentPreferences.copy(ovulationAlert = newSetting)
            
            updateNotificationPreferences(newPreferences)
        }
    }
    
    /**
     * Updates insight notification settings.
     * 
     * @param enabled Whether insight notifications should be enabled
     */
    fun updateInsightNotifications(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !uiState.value.areNotificationsPermitted) {
                requestNotificationPermission()
                return@launch
            }
            
            val currentPreferences = uiState.value.preferences
            val newSetting = currentPreferences.insightNotifications.copy(enabled = enabled)
            val newPreferences = currentPreferences.copy(insightNotifications = newSetting)
            
            updateNotificationPreferences(newPreferences)
        }
    }
    
    /**
     * Updates notification preferences and schedules system notifications.
     */
    private suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        updateState { it.copy(isUpdating = true) }
        
        val result = settingsManager.updateNotificationPreferences(preferences)
        if (result.isSuccess) {
            updateState { 
                it.copy(
                    preferences = preferences,
                    isUpdating = false
                )
            }
            loadScheduledNotifications() // Refresh scheduled notifications
            _showSuccessMessage.emit("Notification preferences updated")
        } else {
            updateState { it.copy(isUpdating = false) }
            _showErrorMessage.emit("Failed to update notification preferences")
        }
    }
    
    /**
     * Requests notification permission from the system.
     */
    fun requestNotificationPermission() {
        viewModelScope.launch {
            updateState { it.copy(isRequestingPermission = true) }
            
            val permissionResult = notificationManager.requestNotificationPermission()
            if (permissionResult.isSuccess) {
                val granted = permissionResult.getOrNull() ?: false
                val newStatus = if (granted) {
                    NotificationPermissionStatus.GRANTED
                } else {
                    NotificationPermissionStatus.DENIED
                }
                
                updateState { 
                    it.copy(
                        permissionStatus = newStatus,
                        isRequestingPermission = false,
                        showPermissionRationale = !granted
                    )
                }
                
                if (granted) {
                    _showSuccessMessage.emit("Notification permission granted")
                } else {
                    _showErrorMessage.emit("Notification permission denied")
                }
            } else {
                updateState { 
                    it.copy(
                        isRequestingPermission = false,
                        showPermissionRationale = true
                    )
                }
                _showErrorMessage.emit("Failed to request permission")
            }
        }
    }
    
    /**
     * Opens system settings for notification permissions.
     */
    fun openNotificationSettings() {
        viewModelScope.launch {
            val result = notificationManager.openNotificationSettings()
            if (result.isSuccess) {
                _openSystemSettings.emit(Unit)
            } else {
                _showErrorMessage.emit("Failed to open settings")
            }
        }
    }
    
    /**
     * Tests a notification by showing it immediately.
     * 
     * @param type The type of notification to test
     */
    fun testNotification(type: NotificationType) {
        viewModelScope.launch {
            if (!uiState.value.areNotificationsPermitted) {
                _showErrorMessage.emit("Notification permission required to test notifications")
                return@launch
            }
            
            updateState { it.copy(testingNotification = type) }
            
            val result = notificationManager.testNotification(type)
            if (result.isSuccess) {
                _showSuccessMessage.emit("Test notification sent")
            } else {
                _showErrorMessage.emit("Failed to send test notification")
            }
            updateState { it.copy(testingNotification = null) }
        }
    }
    
    /**
     * Dismisses the permission rationale dialog.
     */
    fun dismissPermissionRationale() {
        updateState { it.copy(showPermissionRationale = false) }
    }
    
    /**
     * Shows the permission rationale dialog.
     */
    fun showPermissionRationale() {
        updateState { it.copy(showPermissionRationale = true) }
    }
    
    /**
     * Refreshes notification preferences and permission status.
     */
    fun refresh() {
        loadNotificationPreferences()
        checkNotificationPermission()
        loadScheduledNotifications()
    }
    
    /**
     * Cancels all notifications.
     */
    fun cancelAllNotifications() {
        viewModelScope.launch {
            val result = notificationManager.cancelAllNotifications()
            if (result.isSuccess) {
                loadScheduledNotifications()
                _showSuccessMessage.emit("All notifications cancelled")
            } else {
                _showErrorMessage.emit("Failed to cancel notifications")
            }
        }
    }
    
    /**
     * Gets the formatted time for a notification setting.
     */
    fun getFormattedTime(setting: NotificationSetting): String {
        return setting.time?.let { time ->
            "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
        } ?: "Not set"
    }
    
    /**
     * Checks if a specific notification type is enabled.
     */
    fun isNotificationEnabled(type: NotificationType): Boolean {
        val preferences = uiState.value.preferences
        return when (type) {
            NotificationType.DAILY_LOGGING -> preferences.dailyLoggingReminder.enabled
            NotificationType.PERIOD_PREDICTION -> preferences.periodPredictionAlert.enabled
            NotificationType.OVULATION_ALERT -> preferences.ovulationAlert.enabled
            NotificationType.INSIGHTS -> preferences.insightNotifications.enabled
        }
    }
}