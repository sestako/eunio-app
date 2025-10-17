package com.eunio.healthapp.platform

import platform.Foundation.NSProcessInfo
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * iOS implementation of system utilities
 */
actual object SystemUtils {
    actual fun getProperty(key: String): String? {
        // iOS doesn't have system properties like JVM, return null
        return null
    }
    
    actual fun getEnvironmentVariable(key: String): String? {
        // Get environment variables from NSProcessInfo
        val processInfo = NSProcessInfo.processInfo
        return processInfo.environment[key] as? String
    }
    
    actual fun getUsedMemoryBytes(): Long {
        // iOS memory management is different, provide estimated values
        // This is a simplified implementation for testing purposes
        return 50_000_000L // 50MB estimate
    }
    
    actual fun getTotalMemoryBytes(): Long {
        // iOS total memory estimation
        return 256_000_000L // 256MB estimate
    }
    
    actual fun getFreeMemoryBytes(): Long {
        // iOS free memory estimation
        return getTotalMemoryBytes() - getUsedMemoryBytes()
    }
    
    actual fun formatDouble(value: Double, decimalPlaces: Int): String {
        // Simple formatting implementation for iOS
        val multiplier = 10.0.pow(decimalPlaces)
        val rounded = (value * multiplier).roundToLong() / multiplier
        return if (decimalPlaces == 0) {
            rounded.toLong().toString()
        } else {
            // Manual formatting for iOS since String.format is not available
            val integerPart = rounded.toLong()
            val fractionalPart = ((rounded - integerPart) * multiplier).roundToLong()
            val fractionalStr = fractionalPart.toString().padStart(decimalPlaces, '0')
            "$integerPart.$fractionalStr"
        }
    }
}