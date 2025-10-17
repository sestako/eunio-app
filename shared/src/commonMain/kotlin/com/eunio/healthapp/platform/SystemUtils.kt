package com.eunio.healthapp.platform

/**
 * Cross-platform system utilities
 */
expect object SystemUtils {
    /**
     * Get system property value
     */
    fun getProperty(key: String): String?
    
    /**
     * Get environment variable value
     */
    fun getEnvironmentVariable(key: String): String?
    
    /**
     * Get current memory usage in bytes
     */
    fun getUsedMemoryBytes(): Long
    
    /**
     * Get total available memory in bytes
     */
    fun getTotalMemoryBytes(): Long
    
    /**
     * Get free memory in bytes
     */
    fun getFreeMemoryBytes(): Long
    
    /**
     * Format a double with specified decimal places
     */
    fun formatDouble(value: Double, decimalPlaces: Int): String
}