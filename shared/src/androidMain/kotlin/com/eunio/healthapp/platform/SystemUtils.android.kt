package com.eunio.healthapp.platform

import java.text.DecimalFormat

/**
 * Android implementation of system utilities
 */
actual object SystemUtils {
    actual fun getProperty(key: String): String? {
        return System.getProperty(key)
    }
    
    actual fun getEnvironmentVariable(key: String): String? {
        return System.getenv(key)
    }
    
    actual fun getUsedMemoryBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    actual fun getTotalMemoryBytes(): Long {
        return Runtime.getRuntime().totalMemory()
    }
    
    actual fun getFreeMemoryBytes(): Long {
        return Runtime.getRuntime().freeMemory()
    }
    
    actual fun formatDouble(value: Double, decimalPlaces: Int): String {
        val pattern = "0." + "0".repeat(decimalPlaces)
        val formatter = DecimalFormat(pattern)
        return formatter.format(value)
    }
}