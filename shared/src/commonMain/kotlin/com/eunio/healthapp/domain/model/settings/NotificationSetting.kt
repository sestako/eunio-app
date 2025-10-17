package com.eunio.healthapp.domain.model.settings

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class NotificationSetting(
    val enabled: Boolean = false,
    val time: LocalTime? = null,
    val daysInAdvance: Int = 1
) {
    /**
     * Validates that the notification setting is in a valid state
     * 
     * @return true if setting is valid, false otherwise
     */
    fun isValid(): Boolean {
        return when {
            enabled && time == null -> false // Enabled notifications must have a time
            daysInAdvance < 0 || daysInAdvance > 7 -> false // Days in advance must be 0-7
            else -> true
        }
    }
    
    companion object {
        /**
         * Creates a default enabled notification setting with 8 PM time
         */
        fun defaultEnabled(): NotificationSetting {
            return NotificationSetting(
                enabled = true,
                time = LocalTime(20, 0), // 8:00 PM
                daysInAdvance = 1
            )
        }
        
        /**
         * Creates a disabled notification setting
         */
        fun disabled(): NotificationSetting {
            return NotificationSetting(
                enabled = false,
                time = null,
                daysInAdvance = 1
            )
        }
    }
}