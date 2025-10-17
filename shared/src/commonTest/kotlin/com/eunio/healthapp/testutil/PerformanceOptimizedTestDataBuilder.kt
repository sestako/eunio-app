package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Instant

/**
 * Performance-optimized test data builder that uses caching and lazy initialization
 * to reduce test setup time and memory usage.
 */
object PerformanceOptimizedTestDataBuilder {
    
    // Cache frequently used objects to avoid repeated creation
    private val cachedInstances = mutableMapOf<String, Any>()
    private val defaultTimestamp = Clock.System.now()
    
    // Pre-computed default values to avoid repeated calculations
    private val defaultDisplayPreferences by lazy {
        DisplayPreferences(
            textSizeScale = 1.0f,
            highContrastMode = false,
            hapticFeedbackEnabled = true,
            hapticIntensity = HapticIntensity.MEDIUM
        )
    }
    
    private val defaultNotificationPreferences by lazy {
        NotificationPreferences(
            dailyLoggingReminder = NotificationSetting.defaultEnabled(),
            periodPredictionAlert = NotificationSetting.defaultEnabled(),
            ovulationAlert = NotificationSetting.disabled(),
            insightNotifications = NotificationSetting.disabled(),
            globalNotificationsEnabled = true
        )
    }
    
    private val defaultUnitPreferences by lazy {
        UnitPreferences.default()
    }
    
    private val defaultCyclePreferences by lazy {
        CyclePreferences.default()
    }
    
    private val defaultPrivacyPreferences by lazy {
        PrivacyPreferences.default()
    }
    
    private val defaultSyncPreferences by lazy {
        SyncPreferences.default()
    }
    
    /**
     * Fast user preferences creation with minimal object allocation
     */
    fun createFastUserPreferences(
        userId: String = "test-user",
        unitSystem: UnitSystem = UnitSystem.METRIC,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): UserPreferences {
        val cacheKey = "user_prefs_${userId}_${unitSystem}_${syncStatus}"
        
        @Suppress("UNCHECKED_CAST")
        return cachedInstances.getOrPut(cacheKey) {
            UserPreferences(
                userId = userId,
                unitSystem = unitSystem,
                isManuallySet = false,
                lastModified = defaultTimestamp,
                syncStatus = syncStatus
            )
        } as UserPreferences
    }
    
    /**
     * Fast user settings creation with cached defaults
     */
    fun createFastUserSettings(
        userId: String = "test-user",
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): UserSettings {
        val cacheKey = "user_settings_${userId}_${syncStatus}"
        
        @Suppress("UNCHECKED_CAST")
        return cachedInstances.getOrPut(cacheKey) {
            UserSettings(
                userId = userId,
                unitPreferences = defaultUnitPreferences,
                notificationPreferences = defaultNotificationPreferences,
                cyclePreferences = defaultCyclePreferences,
                privacyPreferences = defaultPrivacyPreferences,
                displayPreferences = defaultDisplayPreferences,
                syncPreferences = defaultSyncPreferences,
                lastModified = defaultTimestamp,
                syncStatus = syncStatus,
                version = UserSettings.CURRENT_VERSION
            )
        } as UserSettings
    }
    
    /**
     * Fast user creation with minimal allocations
     */
    fun createFastUser(
        id: String = "test-user",
        email: String = "test@example.com",
        primaryGoal: HealthGoal = HealthGoal.CYCLE_TRACKING
    ): User {
        val cacheKey = "user_${id}_${primaryGoal}"
        
        @Suppress("UNCHECKED_CAST")
        return cachedInstances.getOrPut(cacheKey) {
            User(
                id = id,
                email = email,
                name = "Test User",
                onboardingComplete = true,
                primaryGoal = primaryGoal,
                unitSystem = UnitSystem.METRIC,
                createdAt = defaultTimestamp,
                updatedAt = defaultTimestamp
            )
        } as User
    }
    
    /**
     * Batch creation of test users for performance tests
     */
    fun createUserBatch(count: Int, prefix: String = "user"): List<User> {
        val cacheKey = "user_batch_${count}_${prefix}"
        
        @Suppress("UNCHECKED_CAST")
        return cachedInstances.getOrPut(cacheKey) {
            (1..count).map { index ->
                User(
                    id = "${prefix}-${index}",
                    email = "${prefix}${index}@example.com",
                    name = "Test User $index",
                    onboardingComplete = true,
                    primaryGoal = HealthGoal.CYCLE_TRACKING,
                    unitSystem = UnitSystem.METRIC,
                    createdAt = defaultTimestamp,
                    updatedAt = defaultTimestamp
                )
            }
        } as List<User>
    }
    
    /**
     * Batch creation of user settings for performance tests
     */
    fun createUserSettingsBatch(count: Int, prefix: String = "user"): List<UserSettings> {
        val cacheKey = "settings_batch_${count}_${prefix}"
        
        @Suppress("UNCHECKED_CAST")
        return cachedInstances.getOrPut(cacheKey) {
            (1..count).map { index ->
                UserSettings(
                    userId = "${prefix}-${index}",
                    unitPreferences = defaultUnitPreferences,
                    notificationPreferences = defaultNotificationPreferences,
                    cyclePreferences = defaultCyclePreferences,
                    privacyPreferences = defaultPrivacyPreferences,
                    displayPreferences = defaultDisplayPreferences,
                    syncPreferences = defaultSyncPreferences,
                    lastModified = defaultTimestamp,
                    syncStatus = SyncStatus.PENDING,
                    version = UserSettings.CURRENT_VERSION
                )
            }
        } as List<UserSettings>
    }
    
    /**
     * Create minimal test data for performance-critical tests
     */
    fun createMinimalTestData(): TestDataSet {
        return TestDataSet(
            user = createFastUser(),
            userSettings = createFastUserSettings(),
            userPreferences = createFastUserPreferences()
        )
    }
    
    /**
     * Clear cache to prevent memory leaks in long-running test suites
     */
    fun clearCache() {
        cachedInstances.clear()
    }
    
    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = cachedInstances.size,
            memoryEstimate = cachedInstances.size * 1024L // Rough estimate
        )
    }
    
    /**
     * Minimal test data container
     */
    data class TestDataSet(
        val user: User,
        val userSettings: UserSettings,
        val userPreferences: UserPreferences
    )
    
    /**
     * Cache statistics for monitoring
     */
    data class CacheStats(
        val size: Int,
        val memoryEstimate: Long
    )
}