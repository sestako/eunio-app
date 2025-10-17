package com.eunio.healthapp.domain.model.notification

import kotlinx.serialization.Serializable

/**
 * Enum representing different types of notifications in the health app
 */
@Serializable
enum class NotificationType(
    val id: String,
    val title: String,
    val body: String,
    val category: String
) {
    DAILY_LOGGING(
        id = "daily_logging_reminder",
        title = "Daily Health Log",
        body = "Don't forget to log your health data for today",
        category = "health_tracking"
    ),
    
    PERIOD_PREDICTION(
        id = "period_prediction_alert",
        title = "Period Reminder",
        body = "Your period is expected to start soon",
        category = "cycle_tracking"
    ),
    
    OVULATION_ALERT(
        id = "ovulation_alert",
        title = "Ovulation Window",
        body = "You're entering your fertile window",
        category = "cycle_tracking"
    ),
    
    INSIGHTS(
        id = "insight_notifications",
        title = "Health Insights",
        body = "New insights about your health patterns are available",
        category = "insights"
    );
    
    companion object {
        /**
         * Get notification type by ID
         */
        fun fromId(id: String): NotificationType? {
            return values().find { it.id == id }
        }
        
        /**
         * Get all notification types for a specific category
         */
        fun getByCategory(category: String): List<NotificationType> {
            return values().filter { it.category == category }
        }
    }
}