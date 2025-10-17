package com.eunio.healthapp.domain.model.notification

import kotlinx.serialization.Serializable

/**
 * Enum representing different repeat intervals for notifications
 */
@Serializable
enum class RepeatInterval(
    val displayName: String,
    val intervalInMillis: Long
) {
    NONE("No repeat", 0L),
    DAILY("Daily", 24 * 60 * 60 * 1000L),
    WEEKLY("Weekly", 7 * 24 * 60 * 60 * 1000L),
    MONTHLY("Monthly", 30 * 24 * 60 * 60 * 1000L);
    
    companion object {
        /**
         * Get repeat interval by display name
         */
        fun fromDisplayName(displayName: String): RepeatInterval? {
            return values().find { it.displayName == displayName }
        }
    }
}