package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Resource manager for mock services that prevents memory leaks,
 * manages data lifecycle, and optimizes mock service performance.
 * 
 * Addresses Requirement 7.4:
 * - Fix memory leaks in mock services and test utilities
 * - Add proper resource management for test execution
 * - Implement efficient test data creation and cleanup
 */
class MockServiceResourceManager {
    
    /**
     * Configuration for mock service resource management
     */
    data class ResourceConfiguration(
        val maxDataRetentionTime: Duration = 5.minutes,
        val maxCacheSize: Int = 1000,
        val cleanupInterval: Duration = 30.seconds,
        val enableAutomaticCleanup: Boolean = true,
        val enableDataCompression: Boolean = false,
        val maxMemoryUsageBytes: Long = 50 * 1024 * 1024 // 50MB
    )
    
    /**
     * Tracked data entry with metadata
     */
    private data class TrackedData<T>(
        val data: T,
        val createdAt: Instant,
        val lastAccessedAt: Instant,
        val accessCount: Int,
        val estimatedSizeBytes: Long,
        val tags: Set<String> = emptySet()
    ) {
        fun updateAccess(): TrackedData<T> {
            return copy(
                lastAccessedAt = Clock.System.now(),
                accessCount = accessCount + 1
            )
        }
        
        fun isExpired(maxAge: Duration): Boolean {
            return Clock.System.now() - createdAt > maxAge
        }
        
        fun isStale(maxIdleTime: Duration): Boolean {
            return Clock.System.now() - lastAccessedAt > maxIdleTime
        }
    }
    
    /**
     * Resource pool for efficient object reuse
     */
    private class ManagedResourcePool<T>(
        private val factory: () -> T,
        private val reset: (T) -> Unit,
        private val estimateSize: (T) -> Long,
        private val maxSize: Int = 50
    ) {
        private val available = mutableListOf<TrackedData<T>>()
        private val inUse = mutableMapOf<T, TrackedData<T>>()
        
        fun acquire(tags: Set<String> = emptySet()): T {
            // Try to reuse an available object
            val reusable = available.removeFirstOrNull()
            
            val data = if (reusable != null) {
                reset(reusable.data)
                reusable.data
            } else {
                factory()
            }
            
            val tracked = TrackedData(
                data = data,
                createdAt = Clock.System.now(),
                lastAccessedAt = Clock.System.now(),
                accessCount = 1,
                estimatedSizeBytes = estimateSize(data),
                tags = tags
            )
            
            inUse[data] = tracked
            return data
        }
        
        fun release(item: T) {
            val tracked = inUse.remove(item)
            if (tracked != null && available.size < maxSize) {
                available.add(tracked)
            }
        }
        
        fun cleanup(maxAge: Duration, maxIdleTime: Duration): Int {
            var cleaned = 0
            
            // Clean up expired available items
            val expiredAvailable = available.filter { 
                it.isExpired(maxAge) || it.isStale(maxIdleTime) 
            }
            available.removeAll(expiredAvailable)
            cleaned += expiredAvailable.size
            
            // Clean up stale in-use items (force release)
            val staleInUse = inUse.values.filter { it.isStale(maxIdleTime * 2) }
            staleInUse.forEach { tracked ->
                inUse.remove(tracked.data)
                cleaned++
            }
            
            return cleaned
        }
        
        fun clear() {
            available.clear()
            inUse.clear()
        }
        
        fun getStats(): PoolStats {
            return PoolStats(
                available = available.size,
                inUse = inUse.size,
                totalMemoryBytes = (available + inUse.values).sumOf { it.estimatedSizeBytes }
            )
        }
    }
    
    data class PoolStats(
        val available: Int,
        val inUse: Int,
        val totalMemoryBytes: Long
    ) {
        val total: Int get() = available + inUse
    }
    
    /**
     * Data cache with automatic cleanup
     */
    private class ManagedDataCache<K, V>(
        private val maxSize: Int,
        private val estimateSize: (V) -> Long
    ) {
        private val cache = mutableMapOf<K, TrackedData<V>>()
        
        fun get(key: K): V? {
            val tracked = cache[key]
            return if (tracked != null) {
                cache[key] = tracked.updateAccess()
                tracked.data
            } else {
                null
            }
        }
        
        fun put(key: K, value: V, tags: Set<String> = emptySet()) {
            // Remove oldest entries if cache is full
            if (cache.size >= maxSize) {
                val oldestKey = cache.entries
                    .minByOrNull { it.value.lastAccessedAt }
                    ?.key
                
                if (oldestKey != null) {
                    cache.remove(oldestKey)
                }
            }
            
            cache[key] = TrackedData(
                data = value,
                createdAt = Clock.System.now(),
                lastAccessedAt = Clock.System.now(),
                accessCount = 1,
                estimatedSizeBytes = estimateSize(value),
                tags = tags
            )
        }
        
        fun remove(key: K): V? {
            return cache.remove(key)?.data
        }
        
        fun cleanup(maxAge: Duration, maxIdleTime: Duration): Int {
            val toRemove = cache.entries.filter { (_, tracked) ->
                tracked.isExpired(maxAge) || tracked.isStale(maxIdleTime)
            }.map { it.key }
            
            toRemove.forEach { key ->
                cache.remove(key)
            }
            
            return toRemove.size
        }
        
        fun clear() {
            cache.clear()
        }
        
        fun size(): Int = cache.size
        
        fun getTotalMemoryUsage(): Long {
            return cache.values.sumOf { it.estimatedSizeBytes }
        }
        
        fun getByTags(tags: Set<String>): List<Pair<K, V>> {
            return cache.entries
                .filter { (_, tracked) -> tracked.tags.intersect(tags).isNotEmpty() }
                .map { (key, tracked) -> key to tracked.data }
        }
    }
    
    private val configuration = ResourceConfiguration()
    
    // Resource pools for different types of mock data
    private val userPool = ManagedResourcePool(
        factory = { TestDataFactory.createTestUser("pool-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") },
        reset = { /* Users are immutable */ },
        estimateSize = { 1024L }, // Estimate 1KB per user
        maxSize = 100
    )
    
    private val dailyLogPool = ManagedResourcePool(
        factory = { TestDataFactory.createDailyLog("pool-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") },
        reset = { /* Logs are immutable */ },
        estimateSize = { 2048L }, // Estimate 2KB per log
        maxSize = 200
    )
    
    private val cyclePool = ManagedResourcePool(
        factory = { TestDataFactory.createTestCycle("pool-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}") },
        reset = { /* Cycles are immutable */ },
        estimateSize = { 4096L }, // Estimate 4KB per cycle
        maxSize = 50
    )
    
    // Data caches for mock repositories
    private val userCache = ManagedDataCache<String, User>(
        maxSize = configuration.maxCacheSize,
        estimateSize = { 1024L }
    )
    
    private val logCache = ManagedDataCache<String, DailyLog>(
        maxSize = configuration.maxCacheSize * 2,
        estimateSize = { 2048L }
    )
    
    private val cycleCache = ManagedDataCache<String, Cycle>(
        maxSize = configuration.maxCacheSize / 2,
        estimateSize = { 4096L }
    )
    
    private val sessionCache = ManagedDataCache<String, Any>(
        maxSize = 500,
        estimateSize = { 512L }
    )
    
    // Cleanup job for automatic resource management
    private var cleanupJob: Job? = null
    private val cleanupScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        if (configuration.enableAutomaticCleanup) {
            startAutomaticCleanup()
        }
    }
    
    /**
     * Get or create a user with resource management
     */
    fun getOrCreateUser(userId: String, factory: () -> User): User {
        // Try cache first
        val cached = userCache.get(userId)
        if (cached != null) {
            return cached
        }
        
        // Create new user
        val user = factory()
        userCache.put(userId, user, setOf("user", "test-data"))
        return user
    }
    
    /**
     * Get or create a daily log with resource management
     */
    fun getOrCreateDailyLog(logKey: String, factory: () -> DailyLog): DailyLog {
        val cached = logCache.get(logKey)
        if (cached != null) {
            return cached
        }
        
        val log = factory()
        logCache.put(logKey, log, setOf("log", "test-data"))
        return log
    }
    
    /**
     * Get or create a cycle with resource management
     */
    fun getOrCreateCycle(cycleKey: String, factory: () -> Cycle): Cycle {
        val cached = cycleCache.get(cycleKey)
        if (cached != null) {
            return cached
        }
        
        val cycle = factory()
        cycleCache.put(cycleKey, cycle, setOf("cycle", "test-data"))
        return cycle
    }
    
    /**
     * Store session data with automatic cleanup
     */
    fun storeSession(sessionId: String, sessionData: Any) {
        sessionCache.put(sessionId, sessionData, setOf("session", "auth"))
    }
    
    /**
     * Get session data
     */
    fun getSession(sessionId: String): Any? {
        return sessionCache.get(sessionId)
    }
    
    /**
     * Remove session data
     */
    fun removeSession(sessionId: String): Any? {
        return sessionCache.remove(sessionId)
    }
    
    /**
     * Acquire a user from the pool
     */
    fun acquirePooledUser(tags: Set<String> = emptySet()): User {
        return userPool.acquire(tags)
    }
    
    /**
     * Release a user back to the pool
     */
    fun releasePooledUser(user: User) {
        userPool.release(user)
    }
    
    /**
     * Acquire a daily log from the pool
     */
    fun acquirePooledDailyLog(tags: Set<String> = emptySet()): DailyLog {
        return dailyLogPool.acquire(tags)
    }
    
    /**
     * Release a daily log back to the pool
     */
    fun releasePooledDailyLog(log: DailyLog) {
        dailyLogPool.release(log)
    }
    
    /**
     * Acquire a cycle from the pool
     */
    fun acquirePooledCycle(tags: Set<String> = emptySet()): Cycle {
        return cyclePool.acquire(tags)
    }
    
    /**
     * Release a cycle back to the pool
     */
    fun releasePooledCycle(cycle: Cycle) {
        cyclePool.release(cycle)
    }
    
    /**
     * Perform manual cleanup of expired resources
     */
    fun performCleanup(): CleanupResult {
        val maxAge = configuration.maxDataRetentionTime
        val maxIdleTime = maxAge / 2
        
        val usersCleaned = userCache.cleanup(maxAge, maxIdleTime)
        val logsCleaned = logCache.cleanup(maxAge, maxIdleTime)
        val cyclesCleaned = cycleCache.cleanup(maxAge, maxIdleTime)
        val sessionsCleaned = sessionCache.cleanup(maxAge / 4, maxIdleTime / 4) // Sessions expire faster
        
        val poolUsersCleaned = userPool.cleanup(maxAge, maxIdleTime)
        val poolLogsCleaned = dailyLogPool.cleanup(maxAge, maxIdleTime)
        val poolCyclesCleaned = cyclePool.cleanup(maxAge, maxIdleTime)
        
        return CleanupResult(
            cacheItemsCleaned = usersCleaned + logsCleaned + cyclesCleaned + sessionsCleaned,
            poolItemsCleaned = poolUsersCleaned + poolLogsCleaned + poolCyclesCleaned,
            totalItemsCleaned = usersCleaned + logsCleaned + cyclesCleaned + sessionsCleaned + 
                               poolUsersCleaned + poolLogsCleaned + poolCyclesCleaned
        )
    }
    
    data class CleanupResult(
        val cacheItemsCleaned: Int,
        val poolItemsCleaned: Int,
        val totalItemsCleaned: Int
    )
    
    /**
     * Get current resource usage statistics
     */
    fun getResourceStats(): ResourceStats {
        return ResourceStats(
            userCacheSize = userCache.size(),
            logCacheSize = logCache.size(),
            cycleCacheSize = cycleCache.size(),
            sessionCacheSize = sessionCache.size(),
            userPoolStats = userPool.getStats(),
            dailyLogPoolStats = dailyLogPool.getStats(),
            cyclePoolStats = cyclePool.getStats(),
            totalMemoryUsageBytes = getTotalMemoryUsage(),
            isMemoryUsageHigh = getTotalMemoryUsage() > configuration.maxMemoryUsageBytes * 0.8
        )
    }
    
    data class ResourceStats(
        val userCacheSize: Int,
        val logCacheSize: Int,
        val cycleCacheSize: Int,
        val sessionCacheSize: Int,
        val userPoolStats: PoolStats,
        val dailyLogPoolStats: PoolStats,
        val cyclePoolStats: PoolStats,
        val totalMemoryUsageBytes: Long,
        val isMemoryUsageHigh: Boolean
    ) {
        val totalCacheSize: Int get() = userCacheSize + logCacheSize + cycleCacheSize + sessionCacheSize
        val totalPoolSize: Int get() = userPoolStats.total + dailyLogPoolStats.total + cyclePoolStats.total
    }
    
    /**
     * Clear all cached data and pools
     */
    fun clearAll() {
        userCache.clear()
        logCache.clear()
        cycleCache.clear()
        sessionCache.clear()
        
        userPool.clear()
        dailyLogPool.clear()
        cyclePool.clear()
    }
    
    /**
     * Clear data by tags
     */
    fun clearByTags(tags: Set<String>) {
        // Remove cache entries with matching tags
        val userEntries = userCache.getByTags(tags)
        userEntries.forEach { (key, _) -> userCache.remove(key) }
        
        val logEntries = logCache.getByTags(tags)
        logEntries.forEach { (key, _) -> logCache.remove(key) }
        
        val cycleEntries = cycleCache.getByTags(tags)
        cycleEntries.forEach { (key, _) -> cycleCache.remove(key) }
        
        val sessionEntries = sessionCache.getByTags(tags)
        sessionEntries.forEach { (key, _) -> sessionCache.remove(key) }
    }
    
    /**
     * Start automatic cleanup process
     */
    private fun startAutomaticCleanup() {
        cleanupJob = cleanupScope.launch {
            while (isActive) {
                try {
                    delay(configuration.cleanupInterval)
                    performCleanup()
                    
                    // Force cleanup if memory usage is too high
                    if (getTotalMemoryUsage() > configuration.maxMemoryUsageBytes) {
                        forceCleanup()
                    }
                } catch (e: Exception) {
                    // Log error but continue cleanup process
                    println("Warning: Automatic cleanup failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Stop automatic cleanup process
     */
    fun stopAutomaticCleanup() {
        cleanupJob?.cancel()
        cleanupJob = null
    }
    
    /**
     * Force aggressive cleanup when memory usage is high
     */
    private fun forceCleanup() {
        // Clear half of the cache entries, starting with least recently used
        val targetReduction = 0.5
        
        // This is a simplified implementation - in practice, you'd implement LRU eviction
        if (userCache.size() > 10) {
            repeat((userCache.size() * targetReduction).toInt()) {
                // Remove random entries (in practice, remove LRU entries)
                userCache.clear()
            }
        }
        
        // Similar for other caches
        if (logCache.size() > 20) {
            logCache.clear()
        }
        
        if (cycleCache.size() > 10) {
            cycleCache.clear()
        }
        
        // Clear session cache more aggressively
        sessionCache.clear()
    }
    
    /**
     * Calculate total memory usage across all caches and pools
     */
    private fun getTotalMemoryUsage(): Long {
        return userCache.getTotalMemoryUsage() +
               logCache.getTotalMemoryUsage() +
               cycleCache.getTotalMemoryUsage() +
               sessionCache.getTotalMemoryUsage() +
               userPool.getStats().totalMemoryBytes +
               dailyLogPool.getStats().totalMemoryBytes +
               cyclePool.getStats().totalMemoryBytes
    }
    
    /**
     * Generate detailed resource usage report
     */
    fun generateResourceReport(): String {
        val stats = getResourceStats()
        
        return buildString {
            appendLine("=== Mock Service Resource Manager Report ===")
            appendLine("Total Memory Usage: ${stats.totalMemoryUsageBytes / 1024}KB")
            appendLine("Memory Usage High: ${stats.isMemoryUsageHigh}")
            
            appendLine("\nCache Statistics:")
            appendLine("  Users: ${stats.userCacheSize} entries")
            appendLine("  Daily Logs: ${stats.logCacheSize} entries")
            appendLine("  Cycles: ${stats.cycleCacheSize} entries")
            appendLine("  Sessions: ${stats.sessionCacheSize} entries")
            appendLine("  Total Cache: ${stats.totalCacheSize} entries")
            
            appendLine("\nPool Statistics:")
            appendLine("  User Pool: ${stats.userPoolStats.inUse} in use, ${stats.userPoolStats.available} available (${stats.userPoolStats.totalMemoryBytes / 1024}KB)")
            appendLine("  DailyLog Pool: ${stats.dailyLogPoolStats.inUse} in use, ${stats.dailyLogPoolStats.available} available (${stats.dailyLogPoolStats.totalMemoryBytes / 1024}KB)")
            appendLine("  Cycle Pool: ${stats.cyclePoolStats.inUse} in use, ${stats.cyclePoolStats.available} available (${stats.cyclePoolStats.totalMemoryBytes / 1024}KB)")
            appendLine("  Total Pool: ${stats.totalPoolSize} objects")
            
            appendLine("\nConfiguration:")
            appendLine("  Max Data Retention: ${configuration.maxDataRetentionTime}")
            appendLine("  Max Cache Size: ${configuration.maxCacheSize}")
            appendLine("  Cleanup Interval: ${configuration.cleanupInterval}")
            appendLine("  Automatic Cleanup: ${configuration.enableAutomaticCleanup}")
            appendLine("  Max Memory Usage: ${configuration.maxMemoryUsageBytes / 1024}KB")
        }
    }
    
    /**
     * Validate resource usage and detect potential issues
     */
    fun validateResourceUsage(): ValidationResult {
        val stats = getResourceStats()
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Check memory usage
        if (stats.isMemoryUsageHigh) {
            issues.add("High memory usage: ${stats.totalMemoryUsageBytes / 1024}KB")
            recommendations.add("Consider reducing cache sizes or enabling more aggressive cleanup")
        }
        
        // Check cache efficiency
        val totalCacheCapacity = configuration.maxCacheSize * 4 // Approximate total capacity
        if (stats.totalCacheSize > totalCacheCapacity * 0.9) {
            issues.add("Cache utilization is very high: ${stats.totalCacheSize}/$totalCacheCapacity")
            recommendations.add("Consider increasing cache sizes or reducing data retention time")
        }
        
        // Check pool efficiency
        val poolEfficiency = if (stats.totalPoolSize > 0) {
            (stats.userPoolStats.inUse + stats.dailyLogPoolStats.inUse + stats.cyclePoolStats.inUse).toDouble() / stats.totalPoolSize
        } else {
            0.0
        }
        
        if (poolEfficiency < 0.3 && stats.totalPoolSize > 50) {
            recommendations.add("Object pools are underutilized (${(poolEfficiency * 100).toInt()}% efficiency)")
        }
        
        // Check session cache size (should be smaller than data caches)
        if (stats.sessionCacheSize > stats.userCacheSize) {
            issues.add("Session cache is larger than user cache - potential session leak")
            recommendations.add("Review session cleanup logic")
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            recommendations = recommendations,
            resourceStats = stats
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>,
        val recommendations: List<String>,
        val resourceStats: ResourceStats
    )
    
    /**
     * Shutdown the resource manager and cleanup all resources
     */
    fun shutdown() {
        stopAutomaticCleanup()
        clearAll()
        cleanupScope.cancel()
    }
}

/**
 * Extension functions for easier integration with mock services
 */
fun MockServiceResourceManager.withManagedUser(userId: String, action: (User) -> Unit) {
    val user = getOrCreateUser(userId) { TestDataFactory.createTestUser(userId) }
    action(user)
}

fun MockServiceResourceManager.withManagedDailyLog(userId: String, date: kotlinx.datetime.LocalDate, action: (DailyLog) -> Unit) {
    val logKey = "$userId-$date"
    val log = getOrCreateDailyLog(logKey) { TestDataFactory.createDailyLog(userId) }
    action(log)
}

fun MockServiceResourceManager.withManagedCycle(userId: String, cycleId: String, action: (Cycle) -> Unit) {
    val cycleKey = "$userId-$cycleId"
    val cycle = getOrCreateCycle(cycleKey) { TestDataFactory.createTestCycle(userId) }
    action(cycle)
}

/**
 * Scoped resource management for test methods
 */
class ScopedResourceManager(private val resourceManager: MockServiceResourceManager) {
    private val scopedTags = setOf("scoped-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}")
    
    fun createUser(userId: String): User {
        return resourceManager.getOrCreateUser(userId) { 
            TestDataFactory.createTestUser(userId) 
        }
    }
    
    fun createDailyLog(userId: String, date: kotlinx.datetime.LocalDate = kotlinx.datetime.LocalDate(2024, 1, 15)): DailyLog {
        val logKey = "$userId-$date"
        return resourceManager.getOrCreateDailyLog(logKey) { 
            TestDataFactory.createDailyLog(userId) 
        }
    }
    
    fun createCycle(userId: String): Cycle {
        val cycleKey = "$userId-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
        return resourceManager.getOrCreateCycle(cycleKey) { 
            TestDataFactory.createTestCycle(userId) 
        }
    }
    
    fun cleanup() {
        resourceManager.clearByTags(scopedTags)
    }
}

fun MockServiceResourceManager.createScope(): ScopedResourceManager {
    return ScopedResourceManager(this)
}