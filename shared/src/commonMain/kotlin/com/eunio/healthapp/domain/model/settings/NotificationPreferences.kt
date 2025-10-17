package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    val dailyLoggingReminder: NotificationSetting = NotificationSetting.disabled(),
    val periodPredictionAlert: NotificationSetting = NotificationSetting.disabled(),
    val ovulationAlert: NotificationSetting = NotificationSetting.disabled(),
    val insightNotifications: NotificationSetting = NotificationSetting.disabled(),
    val globalNotificationsEnabled: Boolean = true
) {
    /**
     * Validates that all notification preferences are in a valid state
     * 
     * @return true if all preferences are valid, false otherwise
     */
    fun isValid(): Boolean {
        return dailyLoggingReminder.isValid() &&
                periodPredictionAlert.isValid() &&
                ovulationAlert.isValid() &&
                insightNotifications.isValid()
    }
    
    /**
     * Returns true if any notifications are enabled
     */
    fun hasEnabledNotifications(): Boolean {
        return globalNotificationsEnabled && (
                dailyLoggingReminder.enabled ||
                periodPredictionAlert.enabled ||
                ovulationAlert.enabled ||
                insightNotifications.enabled
        )
    }
    
    /**
     * Returns a list of all enabled notification settings
     */
    fun getEnabledNotifications(): List<Pair<String, NotificationSetting>> {
        if (!globalNotificationsEnabled) return emptyList()
        
        val enabled = mutableListOf<Pair<String, NotificationSetting>>()
        
        if (dailyLoggingReminder.enabled) {
            enabled.add("dailyLoggingReminder" to dailyLoggingReminder)
        }
        if (periodPredictionAlert.enabled) {
            enabled.add("periodPredictionAlert" to periodPredictionAlert)
        }
        if (ovulationAlert.enabled) {
            enabled.add("ovulationAlert" to ovulationAlert)
        }
        if (insightNotifications.enabled) {
            enabled.add("insightNotifications" to insightNotifications)
        }
        
        return enabled
    }
    
    companion object {
        fun default(): NotificationPreferences {
            return NotificationPreferences()
        }
        
        /**
         * Creates notification preferences with commonly used defaults
         */
        fun withDefaults(): NotificationPreferences {
            return NotificationPreferences(
                dailyLoggingReminder = NotificationSetting.defaultEnabled(),
                periodPredictionAlert = NotificationSetting.defaultEnabled(),
                ovulationAlert = NotificationSetting.defaultEnabled(),
                insightNotifications = NotificationSetting.disabled(),
                globalNotificationsEnabled = true
            )
        }
    }
}