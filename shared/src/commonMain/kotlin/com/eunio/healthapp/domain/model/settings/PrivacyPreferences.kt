package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class PrivacyPreferences(
    val dataSharingEnabled: Boolean = false,
    val anonymousInsightsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true
) {
    /**
     * Validates that privacy preferences are in a valid state
     * All boolean preferences are inherently valid
     * 
     * @return true (always valid)
     */
    fun isValid(): Boolean = true
    
    /**
     * Returns true if any data collection is enabled
     */
    fun hasDataCollectionEnabled(): Boolean {
        return dataSharingEnabled || anonymousInsightsEnabled || crashReportingEnabled || analyticsEnabled
    }
    
    /**
     * Returns a privacy-focused configuration with minimal data collection
     */
    fun toPrivacyFocused(): PrivacyPreferences {
        return copy(
            dataSharingEnabled = false,
            anonymousInsightsEnabled = false,
            crashReportingEnabled = false,
            analyticsEnabled = false
        )
    }
    
    /**
     * Returns a configuration optimized for app improvement with user consent
     */
    fun toOptimizedForImprovement(): PrivacyPreferences {
        return copy(
            dataSharingEnabled = false, // Keep personal data private
            anonymousInsightsEnabled = true, // Help improve insights
            crashReportingEnabled = true, // Help fix bugs
            analyticsEnabled = true // Help improve app experience
        )
    }
    
    companion object {
        fun default(): PrivacyPreferences {
            return PrivacyPreferences()
        }
        
        /**
         * Creates privacy preferences with maximum privacy protection
         */
        fun maxPrivacy(): PrivacyPreferences {
            return PrivacyPreferences(
                dataSharingEnabled = false,
                anonymousInsightsEnabled = false,
                crashReportingEnabled = false,
                analyticsEnabled = false
            )
        }
        
        /**
         * Creates privacy preferences with balanced privacy and functionality
         */
        fun balanced(): PrivacyPreferences {
            return PrivacyPreferences(
                dataSharingEnabled = false,
                anonymousInsightsEnabled = true,
                crashReportingEnabled = true,
                analyticsEnabled = false
            )
        }
    }
}