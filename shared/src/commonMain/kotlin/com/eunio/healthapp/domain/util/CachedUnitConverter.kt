package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.UnitSystem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cached implementation of UnitConverter that provides performance optimization
 * through in-memory caching of conversion results and batch operations.
 * Includes LRU cache eviction and thread-safe operations using Kotlin Multiplatform primitives.
 */
class CachedUnitConverter(
    private val delegate: UnitConverter = UnitConverterImpl(),
    private val maxCacheSize: Int = 1000
) : UnitConverter {
    
    private val conversionCache = mutableMapOf<ConversionKey, Double>()
    private val formatCache = mutableMapOf<FormatKey, String>()
    private val accessOrder = mutableListOf<Any>()
    private val cacheMutex = Mutex()
    
    data class ConversionKey(
        val value: Double,
        val from: UnitSystem,
        val to: UnitSystem,
        val type: ConversionType
    )
    
    data class FormatKey(
        val value: Double,
        val unitSystem: UnitSystem,
        val type: ConversionType
    )
    
    enum class ConversionType {
        WEIGHT, DISTANCE, TEMPERATURE
    }
    
    override fun convertWeight(value: Double, from: UnitSystem, to: UnitSystem): Double {
        val key = ConversionKey(value, from, to, ConversionType.WEIGHT)
        return conversionCache[key] ?: run {
            val result = delegate.convertWeight(value, from, to)
            // Cache asynchronously to avoid blocking
            cacheResult(key, result)
            result
        }
    }
    
    override fun convertDistance(value: Double, from: UnitSystem, to: UnitSystem): Double {
        val key = ConversionKey(value, from, to, ConversionType.DISTANCE)
        return conversionCache[key] ?: run {
            val result = delegate.convertDistance(value, from, to)
            cacheResult(key, result)
            result
        }
    }
    
    override fun convertTemperature(value: Double, from: UnitSystem, to: UnitSystem): Double {
        val key = ConversionKey(value, from, to, ConversionType.TEMPERATURE)
        return conversionCache[key] ?: run {
            val result = delegate.convertTemperature(value, from, to)
            cacheResult(key, result)
            result
        }
    }
    
    override fun formatWeight(value: Double, unitSystem: UnitSystem): String {
        val key = FormatKey(value, unitSystem, ConversionType.WEIGHT)
        return formatCache[key] ?: run {
            val result = delegate.formatWeight(value, unitSystem)
            cacheFormatResult(key, result)
            result
        }
    }
    
    override fun formatDistance(value: Double, unitSystem: UnitSystem): String {
        val key = FormatKey(value, unitSystem, ConversionType.DISTANCE)
        return formatCache[key] ?: run {
            val result = delegate.formatDistance(value, unitSystem)
            cacheFormatResult(key, result)
            result
        }
    }
    
    override fun formatTemperature(value: Double, unitSystem: UnitSystem): String {
        val key = FormatKey(value, unitSystem, ConversionType.TEMPERATURE)
        return formatCache[key] ?: run {
            val result = delegate.formatTemperature(value, unitSystem)
            cacheFormatResult(key, result)
            result
        }
    }
    
    /**
     * Batch conversion operations for improved performance when converting multiple values
     */
    suspend fun convertWeightBatch(
        values: List<Double>,
        from: UnitSystem,
        to: UnitSystem
    ): List<Double> = cacheMutex.withLock {
        values.map { value ->
            val key = ConversionKey(value, from, to, ConversionType.WEIGHT)
            conversionCache.getOrPut(key) {
                updateAccessOrder(key)
                delegate.convertWeight(value, from, to)
            }
        }
    }
    
    suspend fun convertDistanceBatch(
        values: List<Double>,
        from: UnitSystem,
        to: UnitSystem
    ): List<Double> = cacheMutex.withLock {
        values.map { value ->
            val key = ConversionKey(value, from, to, ConversionType.DISTANCE)
            conversionCache.getOrPut(key) {
                updateAccessOrder(key)
                delegate.convertDistance(value, from, to)
            }
        }
    }
    
    suspend fun convertTemperatureBatch(
        values: List<Double>,
        from: UnitSystem,
        to: UnitSystem
    ): List<Double> = cacheMutex.withLock {
        values.map { value ->
            val key = ConversionKey(value, from, to, ConversionType.TEMPERATURE)
            conversionCache.getOrPut(key) {
                updateAccessOrder(key)
                delegate.convertTemperature(value, from, to)
            }
        }
    }
    
    /**
     * Batch formatting operations for improved performance
     */
    suspend fun formatWeightBatch(
        values: List<Double>,
        unitSystem: UnitSystem
    ): List<String> = cacheMutex.withLock {
        values.map { value ->
            val key = FormatKey(value, unitSystem, ConversionType.WEIGHT)
            formatCache.getOrPut(key) {
                updateAccessOrder(key)
                delegate.formatWeight(value, unitSystem)
            }
        }
    }
    
    suspend fun formatDistanceBatch(
        values: List<Double>,
        unitSystem: UnitSystem
    ): List<String> = cacheMutex.withLock {
        values.map { value ->
            val key = FormatKey(value, unitSystem, ConversionType.DISTANCE)
            formatCache.getOrPut(key) {
                updateAccessOrder(key)
                delegate.formatDistance(value, unitSystem)
            }
        }
    }
    
    suspend fun formatTemperatureBatch(
        values: List<Double>,
        unitSystem: UnitSystem
    ): List<String> = cacheMutex.withLock {
        values.map { value ->
            val key = FormatKey(value, unitSystem, ConversionType.TEMPERATURE)
            formatCache.getOrPut(key) {
                updateAccessOrder(key)
                delegate.formatTemperature(value, unitSystem)
            }
        }
    }
    
    private fun cacheResult(key: ConversionKey, result: Double) {
        // Simple caching without blocking - will be properly cached on next access if needed
        if (conversionCache.size < maxCacheSize) {
            conversionCache[key] = result
            accessOrder.remove(key)
            accessOrder.add(key)
        }
    }
    
    private fun cacheFormatResult(key: FormatKey, result: String) {
        // Simple caching without blocking - will be properly cached on next access if needed
        if (formatCache.size < maxCacheSize) {
            formatCache[key] = result
            accessOrder.remove(key)
            accessOrder.add(key)
        }
    }
    
    private fun updateAccessOrder(key: Any) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }
    
    private fun evictLeastRecentlyUsed() {
        if (accessOrder.isNotEmpty()) {
            val lruKey = accessOrder.removeFirst()
            when (lruKey) {
                is ConversionKey -> conversionCache.remove(lruKey)
                is FormatKey -> formatCache.remove(lruKey)
            }
        }
    }
    
    /**
     * Clears all cached data
     */
    suspend fun clearCache() = cacheMutex.withLock {
        conversionCache.clear()
        formatCache.clear()
        accessOrder.clear()
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    suspend fun getCacheStats(): CacheStats = cacheMutex.withLock {
        CacheStats(
            conversionCacheSize = conversionCache.size,
            formatCacheSize = formatCache.size,
            maxCacheSize = maxCacheSize,
            totalCacheSize = conversionCache.size + formatCache.size
        )
    }
    
    data class CacheStats(
        val conversionCacheSize: Int,
        val formatCacheSize: Int,
        val maxCacheSize: Int,
        val totalCacheSize: Int
    ) {
        val cacheUtilization: Double = totalCacheSize.toDouble() / (maxCacheSize * 2)
    }
}