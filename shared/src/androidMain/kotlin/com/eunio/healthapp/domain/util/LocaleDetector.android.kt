package com.eunio.healthapp.domain.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Android implementation of LocaleDetector using Java Locale API with enhanced platform-specific features.
 * Provides comprehensive locale detection including system locale, app locale, and configuration changes.
 */
class AndroidLocaleDetector(private val context: Context? = null) : LocaleDetector {
    
    override fun getCurrentLocaleCountryCode(): String? {
        return try {
            val locale = getCurrentLocale()
            locale.country.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getCurrentLocaleString(): String? {
        return try {
            val locale = getCurrentLocale()
            "${locale.language}_${locale.country}".takeIf { 
                locale.language.isNotBlank() && locale.country.isNotBlank() 
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the current locale with Android-specific optimizations.
     * Prioritizes app-specific locale over system locale when available.
     */
    private fun getCurrentLocale(): Locale {
        return when {
            // Use context-specific locale if available (Android 7.0+)
            context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                getContextLocale(context)
            }
            // Fallback to system default
            else -> Locale.getDefault()
        }
    }
    
    /**
     * Gets locale from Android context configuration.
     * Handles both legacy and modern Android locale APIs.
     */
    private fun getContextLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use LocaleList for Android 7.0+
            configuration.locales.get(0)
        } else {
            // Use deprecated locale for older versions
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }
    
    /**
     * Android-specific method to detect if the device uses 24-hour format.
     * This can influence unit system preferences in some regions.
     */
    fun uses24HourFormat(): Boolean {
        return try {
            context?.let { ctx ->
                android.text.format.DateFormat.is24HourFormat(ctx)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Android-specific method to get the device's measurement system preference.
     * Some Android devices have built-in measurement system preferences.
     */
    fun getSystemMeasurementPreference(): String? {
        return try {
            context?.let { ctx ->
                val configuration = ctx.resources.configuration
                // Check for measurement system in configuration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    when (configuration.screenLayout and Configuration.SCREENLAYOUT_LAYOUTDIR_MASK) {
                        Configuration.SCREENLAYOUT_LAYOUTDIR_RTL -> "metric" // Most RTL regions use metric
                        else -> null // Let locale detection handle it
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Creates the Android-specific LocaleDetector implementation.
 * Can optionally accept a Context for enhanced locale detection.
 */
actual fun createLocaleDetector(): LocaleDetector = AndroidLocaleDetector()

/**
 * Creates an Android-specific LocaleDetector with Context for enhanced features.
 */
fun createAndroidLocaleDetector(context: Context): AndroidLocaleDetector = AndroidLocaleDetector(context)