package com.eunio.healthapp.performance

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.CachedUnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime
import kotlinx.coroutines.test.runTest

/**
 * Simplified performance tests for caching functionality.
 * Focuses on core caching benefits without complex scenarios.
 */
class SimpleCachePerformanceTest {
    
    @Test
    fun testBasicCachePerformance() = runTest {
        val baseConverter = UnitConverterImpl()
        val cachedConverter = CachedUnitConverter(baseConverter, maxCacheSize = 100)
        
        val testValue = 100.0
        
        // First access (cache miss)
        val firstTime = measureTime {
            repeat(10) {
                cachedConverter.convertWeight(testValue, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            }
        }
        
        // Second access (cache hit)
        val secondTime = measureTime {
            repeat(10) {
                cachedConverter.convertWeight(testValue, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            }
        }
        
        println("First access time: $firstTime")
        println("Second access time: $secondTime")
        
        // Cache should improve performance (allow some variance)
        assertTrue(secondTime <= firstTime * 2, "Cached access should not be significantly slower")
        
        val stats = cachedConverter.getCacheStats()
        assertTrue(stats.conversionCacheSize > 0, "Cache should contain conversions")
    }
    
    @Test
    fun testBatchOperationBasics() = runTest {
        val cachedConverter = CachedUnitConverter(maxCacheSize = 200)
        val testValues = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        
        // Test batch conversion
        val batchResult = cachedConverter.convertWeightBatch(
            testValues, 
            UnitSystem.METRIC, 
            UnitSystem.IMPERIAL
        )
        
        assertTrue(batchResult.size == testValues.size, "Batch result should match input size")
        assertTrue(batchResult.all { it > 0 }, "All converted values should be positive")
        
        // Test batch formatting
        val formatResult = cachedConverter.formatWeightBatch(testValues, UnitSystem.METRIC)
        
        assertTrue(formatResult.size == testValues.size, "Format result should match input size")
        assertTrue(formatResult.all { it.contains("kg") }, "All formatted values should contain unit")
    }
    
    @Test
    fun testCacheEviction() = runTest {
        val smallCacheConverter = CachedUnitConverter(maxCacheSize = 5)
        
        // Fill cache beyond capacity
        repeat(10) { i ->
            smallCacheConverter.convertWeight(
                i.toDouble(), 
                UnitSystem.METRIC, 
                UnitSystem.IMPERIAL
            )
        }
        
        val stats = smallCacheConverter.getCacheStats()
        assertTrue(stats.conversionCacheSize <= 5, "Cache should not exceed max size")
        assertTrue(stats.cacheUtilization <= 1.0, "Cache utilization should be reasonable")
    }
    
    @Test
    fun testCacheClearance() = runTest {
        val converter = CachedUnitConverter(maxCacheSize = 50)
        
        // Add some data to cache
        repeat(10) { i ->
            converter.convertWeight(i.toDouble(), UnitSystem.METRIC, UnitSystem.IMPERIAL)
            converter.formatWeight(i.toDouble(), UnitSystem.METRIC)
        }
        
        val statsBeforeClear = converter.getCacheStats()
        assertTrue(statsBeforeClear.totalCacheSize > 0, "Cache should contain data before clearing")
        
        // Clear cache
        converter.clearCache()
        
        val statsAfterClear = converter.getCacheStats()
        assertTrue(statsAfterClear.totalCacheSize == 0, "Cache should be empty after clearing")
    }
}