package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus
import com.eunio.healthapp.domain.model.notification.NotificationType
import com.eunio.healthapp.domain.model.settings.NotificationPreferences
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for notification preferences screen.
 */
data class NotificationPreferencesUiState(
    val preferences: NotificationPreferences = NotificationPreferences.default(),
    val permissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.NOT_REQUESTED,
    val loadingState: LoadingState = LoadingState.Idle,
    val isUpdating: Boolean = false,
    val isRequestingPermission: Boolean = false,
    val scheduledNotifications: List<NotificationType> = emptyList(),
    val showPermissionRationale: Boolean = false,
    val testingNotification: NotificationType? = null
) : UiState {
    
    /**
     * Whether the preferences are currently loading.
     */
    val isLoading: Boolean
        get() = loadingState is LoadingState.Loading
    
    /**
     * Whether the preferences are enabled for interaction.
     */
    val isEnabled: Boolean
        get() = !isLoading && !isUpdating && !isRequestingPermission
    
    /**
     * Error message if loading failed.
     */
    val errorMessage: String?
        get() = (loadingState as? LoadingState.Error)?.message
    
    /**
     * Whether preferences have been loaded successfully.
     */
    val hasPreferences: Boolean
        get() = loadingState is LoadingState.Success<*>
    
    /**
     * Whether notifications are permitted by the system.
     */
    val areNotificationsPermitted: Boolean
        get() = permissionStatus == NotificationPermissionStatus.GRANTED
    
    /**
     * Whether permission can be requested.
     */
    val canRequestPermission: Boolean
        get() = permissionStatus.canRequestPermission()
    
    /**
     * Whether user needs to manually enable notifications in settings.
     */
    val needsManualPermission: Boolean
        get() = permissionStatus == NotificationPermissionStatus.DENIED || 
                permissionStatus == NotificationPermissionStatus.PERMANENTLY_DENIED
    
    /**
     * Whether any notifications are enabled.
     */
    val hasEnabledNotifications: Boolean
        get() = preferences.hasEnabledNotifications()
    
    /**
     * Whether global notifications are effectively enabled (permission + preference).
     */
    val areNotificationsEffectivelyEnabled: Boolean
        get() = areNotificationsPermitted && preferences.globalNotificationsEnabled
    
    /**
     * Gets the count of enabled notification types.
     */
    fun getEnabledNotificationCount(): Int {
        var count = 0
        if (preferences.dailyLoggingReminder.enabled) count++
        if (preferences.periodPredictionAlert.enabled) count++
        if (preferences.ovulationAlert.enabled) count++
        if (preferences.insightNotifications.enabled) count++
        return count
    }
    
    /**
     * Gets the count of scheduled notifications.
     */
    fun getScheduledNotificationCount(): Int = scheduledNotifications.size
    
    /**
     * Whether a specific notification type is currently being tested.
     */
    fun isTestingNotification(type: NotificationType): Boolean = testingNotification == type
}