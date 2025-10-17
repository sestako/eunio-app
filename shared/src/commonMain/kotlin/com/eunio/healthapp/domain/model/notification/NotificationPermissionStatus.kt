package com.eunio.healthapp.domain.model.notification

import kotlinx.serialization.Serializable

/**
 * Enum representing the status of notification permissions
 */
@Serializable
enum class NotificationPermissionStatus {
    /**
     * Permission has been granted by the user
     */
    GRANTED,
    
    /**
     * Permission has been denied by the user
     */
    DENIED,
    
    /**
     * Permission has not been requested yet
     */
    NOT_REQUESTED,
    
    /**
     * Permission was denied and user selected "Don't ask again" (Android)
     * or permission is permanently denied
     */
    PERMANENTLY_DENIED,
    
    /**
     * Permission status is unknown or cannot be determined
     */
    UNKNOWN;
    
    /**
     * Returns true if notifications can be shown
     */
    fun canShowNotifications(): Boolean {
        return this == GRANTED
    }
    
    /**
     * Returns true if permission can be requested
     */
    fun canRequestPermission(): Boolean {
        return this == NOT_REQUESTED
    }
}