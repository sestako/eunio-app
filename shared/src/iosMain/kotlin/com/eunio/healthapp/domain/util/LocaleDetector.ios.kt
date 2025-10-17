package com.eunio.healthapp.domain.util

import platform.Foundation.*

/**
 * iOS implementation of LocaleDetector using Foundation NSLocale API with enhanced platform-specific features.
 * Provides comprehensive locale detection including system locale, app locale, and regional preferences.
 */
class IOSLocaleDetector : LocaleDetector {
    
    override fun getCurrentLocaleCountryCode(): String? {
        return try {
            val locale = getCurrentLocale()
            locale.objectForKey(NSLocaleCountryCode) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getCurrentLocaleString(): String? {
        return try {
            val locale = getCurrentLocale()
            locale.objectForKey(NSLocaleIdentifier) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the current locale with iOS-specific optimizations.
     * Prioritizes app-specific locale settings when available.
     */
    private fun getCurrentLocale(): NSLocale {
        return try {
            // Try to get the preferred locale from user defaults
            val userDefaults = NSUserDefaults.standardUserDefaults
            val preferredLanguages = userDefaults.objectForKey("AppleLanguages") as? NSArray
            
            if (preferredLanguages != null && preferredLanguages.count > 0u) {
                val preferredLanguage = preferredLanguages.objectAtIndex(0u) as? String
                preferredLanguage?.let { lang ->
                    NSLocale(lang)
                } ?: NSLocale.currentLocale
            } else {
                NSLocale.currentLocale
            }
        } catch (e: Exception) {
            NSLocale.currentLocale
        }
    }
    
    /**
     * iOS-specific method to detect if the device uses 24-hour format.
     * This can influence unit system preferences in some regions.
     */
    fun uses24HourFormat(): Boolean {
        return try {
            val formatter = NSDateFormatter()
            formatter.setLocale(getCurrentLocale())
            formatter.dateStyle = NSDateFormatterNoStyle
            formatter.timeStyle = NSDateFormatterShortStyle
            
            val template = formatter.stringFromDate(NSDate())
            // Check if the time format contains AM/PM indicators
            !template.contains("AM") && !template.contains("PM") && 
            !template.contains("am") && !template.contains("pm")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * iOS-specific method to get the device's measurement system preference.
     * iOS has built-in measurement system preferences in Settings.
     */
    fun getSystemMeasurementPreference(): String? {
        return try {
            val locale = getCurrentLocale()
            val measurementSystem = locale.objectForKey(NSLocaleMeasurementSystem) as? String
            
            when (measurementSystem) {
                "Metric" -> "metric"
                "U.S." -> "imperial"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * iOS-specific method to get temperature unit preference from system settings.
     */
    fun getSystemTemperatureUnit(): String? {
        return try {
            val locale = getCurrentLocale()
            // Note: NSLocaleTemperatureUnit may not be available in all iOS versions
            // Using a string key as fallback
            val temperatureUnit = locale.objectForKey("temperature") as? String
            
            when (temperatureUnit) {
                "Celsius" -> "celsius"
                "Fahrenheit" -> "fahrenheit"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * iOS-specific method to get currency code which can help determine region preferences.
     */
    fun getSystemCurrencyCode(): String? {
        return try {
            val locale = getCurrentLocale()
            locale.objectForKey(NSLocaleCurrencyCode) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * iOS-specific method to check if the device uses metric system based on multiple indicators.
     */
    fun shouldUseMetricSystem(): Boolean? {
        return try {
            // Check measurement system preference first
            getSystemMeasurementPreference()?.let { system ->
                return system == "metric"
            }
            
            // Fallback to country code analysis
            getCurrentLocaleCountryCode()?.let { countryCode ->
                // Countries that primarily use imperial system
                val imperialCountries = setOf("US", "LR", "MM")
                return !imperialCountries.contains(countryCode.uppercase())
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Creates the iOS-specific LocaleDetector implementation.
 */
actual fun createLocaleDetector(): LocaleDetector = IOSLocaleDetector()