package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime

/**
 * Centralized test data creation utilities for consistent model instantiation
 */
object TestDataBuilder {
    
    /**
     * Creates UserPreferences with current constructor parameters
     */
    fun createUserPreferences(
        userId: String = "test-user",
        unitSystem: UnitSystem = UnitSystem.METRIC,
        isManuallySet: Boolean = false,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ): UserPreferences {
        return UserPreferences(
            userId = userId,
            unitSystem = unitSystem,
            isManuallySet = isManuallySet,
            lastModified = Clock.System.now(),
            syncStatus = syncStatus
        )
    }
    
    /**
     * Creates NotificationSettings with enum-based configuration
     */
    fun createNotificationSettings(
        dailyLoggingEnabled: Boolean = true,
        dailyLoggingTime: LocalTime = LocalTime(20, 0), // 8:00 PM
        periodPredictionEnabled: Boolean = true,
        periodPredictionTime: LocalTime = LocalTime(9, 0), // 9:00 AM
        ovulationEnabled: Boolean = false,
        ovulationTime: LocalTime = LocalTime(10, 0), // 10:00 AM
        insightNotificationsEnabled: Boolean = false,
        globalNotificationsEnabled: Boolean = true
    ): NotificationPreferences {
        return NotificationPreferences(
            dailyLoggingReminder = if (dailyLoggingEnabled) {
                NotificationSetting(enabled = true, time = dailyLoggingTime, daysInAdvance = 0)
            } else {
                NotificationSetting.disabled()
            },
            periodPredictionAlert = if (periodPredictionEnabled) {
                NotificationSetting(enabled = true, time = periodPredictionTime, daysInAdvance = 1)
            } else {
                NotificationSetting.disabled()
            },
            ovulationAlert = if (ovulationEnabled) {
                NotificationSetting(enabled = true, time = ovulationTime, daysInAdvance = 1)
            } else {
                NotificationSetting.disabled()
            },
            insightNotifications = if (insightNotificationsEnabled) {
                NotificationSetting(enabled = true, time = LocalTime(18, 0), daysInAdvance = 0) // 6:00 PM
            } else {
                NotificationSetting.disabled()
            },
            globalNotificationsEnabled = globalNotificationsEnabled
        )
    }
    
    /**
     * Creates DisplayPreferences with current constructor parameters
     */
    fun createDisplayPreferences(
        textSizeScale: Float = 1.0f,
        highContrastMode: Boolean = false,
        hapticFeedbackEnabled: Boolean = true,
        hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM
    ): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = textSizeScale,
            highContrastMode = highContrastMode,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            hapticIntensity = hapticIntensity
        )
    }
    
    /**
     * Creates UserSettings with current constructor parameters and proper field mapping
     */
    fun createUserSettings(
        userId: String = "test-user",
        unitPreferences: UnitPreferences = UnitPreferences.default(),
        notificationPreferences: NotificationPreferences = NotificationPreferences.default(),
        cyclePreferences: CyclePreferences = CyclePreferences.default(),
        privacyPreferences: PrivacyPreferences = PrivacyPreferences.default(),
        displayPreferences: DisplayPreferences = createDisplayPreferences(),
        syncPreferences: SyncPreferences = SyncPreferences.default(),
        syncStatus: SyncStatus = SyncStatus.PENDING,
        version: Int = UserSettings.CURRENT_VERSION
    ): UserSettings {
        return UserSettings(
            userId = userId,
            unitPreferences = unitPreferences,
            notificationPreferences = notificationPreferences,
            cyclePreferences = cyclePreferences,
            privacyPreferences = privacyPreferences,
            displayPreferences = displayPreferences,
            syncPreferences = syncPreferences,
            lastModified = Clock.System.now(),
            syncStatus = syncStatus,
            version = version
        )
    }
    
    /**
     * Creates DisplayPreferences with accessibility features enabled
     */
    fun createAccessibilityDisplayPreferences(): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = 1.4f,
            highContrastMode = true,
            hapticFeedbackEnabled = true,
            hapticIntensity = HapticIntensity.STRONG
        )
    }
    
    /**
     * Creates DisplayPreferences with minimal settings
     */
    fun createMinimalDisplayPreferences(): DisplayPreferences {
        return DisplayPreferences(
            textSizeScale = 1.0f,
            highContrastMode = false,
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.DISABLED
        )
    }
    
    /**
     * Creates UserSettings with custom display preferences
     */
    fun createUserSettingsWithDisplayPreferences(
        userId: String = "test-user",
        displayPreferences: DisplayPreferences
    ): UserSettings {
        return createUserSettings(
            userId = userId,
            displayPreferences = displayPreferences
        )
    }
    
    /**
     * Creates UserSettings for testing validation scenarios
     */
    fun createInvalidUserSettings(): UserSettings {
        return UserSettings(
            userId = "", // Invalid: blank user ID
            unitPreferences = UnitPreferences.default(),
            notificationPreferences = NotificationPreferences.default(),
            cyclePreferences = CyclePreferences.default(),
            privacyPreferences = PrivacyPreferences.default(),
            displayPreferences = DisplayPreferences(
                textSizeScale = -1.0f, // Invalid: below minimum
                highContrastMode = false,
                hapticFeedbackEnabled = false,
                hapticIntensity = HapticIntensity.MEDIUM // Invalid: should be DISABLED when haptic is off
            ),
            syncPreferences = SyncPreferences.default(),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING,
            version = UserSettings.CURRENT_VERSION
        )
    }
    
    /**
     * Creates UserSettings with all preferences customized
     */
    fun createFullyCustomizedUserSettings(userId: String = "test-user"): UserSettings {
        return UserSettings(
            userId = userId,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting.defaultEnabled(),
                periodPredictionAlert = NotificationSetting.defaultEnabled(),
                ovulationAlert = NotificationSetting.defaultEnabled(),
                insightNotifications = NotificationSetting.disabled(),
                globalNotificationsEnabled = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 28,
                averageLutealPhaseLength = 14,
                periodDuration = 5,
                isCustomized = true
            ),
            privacyPreferences = PrivacyPreferences.balanced(),
            displayPreferences = createAccessibilityDisplayPreferences(),
            syncPreferences = SyncPreferences.default(),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED,
            version = UserSettings.CURRENT_VERSION
        )
    }
    
    /**
     * Creates User with proper HealthGoal enum values
     */
    fun createUser(
        id: String = "test-user",
        email: String = "test@example.com",
        name: String = "Test User",
        onboardingComplete: Boolean = true,
        primaryGoal: HealthGoal = HealthGoal.CYCLE_TRACKING,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): User {
        val now = Clock.System.now()
        return User(
            id = id,
            email = email,
            name = name,
            onboardingComplete = onboardingComplete,
            primaryGoal = primaryGoal,
            unitSystem = unitSystem,
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Creates User with CONCEPTION goal for fertility tracking tests
     */
    fun createConceptionUser(
        id: String = "conception-user",
        email: String = "conception@example.com",
        name: String = "Conception User"
    ): User {
        return createUser(
            id = id,
            email = email,
            name = name,
            primaryGoal = HealthGoal.CONCEPTION
        )
    }
    
    /**
     * Creates User with CONTRACEPTION goal for contraception tests
     */
    fun createContraceptionUser(
        id: String = "contraception-user",
        email: String = "contraception@example.com",
        name: String = "Contraception User"
    ): User {
        return createUser(
            id = id,
            email = email,
            name = name,
            primaryGoal = HealthGoal.CONTRACEPTION
        )
    }
    
    /**
     * Creates User with CYCLE_TRACKING goal for general cycle tracking tests
     */
    fun createCycleTrackingUser(
        id: String = "cycle-user",
        email: String = "cycle@example.com",
        name: String = "Cycle Tracking User"
    ): User {
        return createUser(
            id = id,
            email = email,
            name = name,
            primaryGoal = HealthGoal.CYCLE_TRACKING
        )
    }
    
    /**
     * Creates User with GENERAL_HEALTH goal for general health tests
     */
    fun createGeneralHealthUser(
        id: String = "health-user",
        email: String = "health@example.com",
        name: String = "General Health User"
    ): User {
        return createUser(
            id = id,
            email = email,
            name = name,
            primaryGoal = HealthGoal.GENERAL_HEALTH
        )
    }
    
    /**
     * Creates list of users with all HealthGoal types for comprehensive testing
     */
    fun createUsersWithAllHealthGoals(): List<User> {
        return listOf(
            createConceptionUser(),
            createContraceptionUser(),
            createCycleTrackingUser(),
            createGeneralHealthUser()
        )
    }
    
    /**
     * Creates NotificationSettings with all notifications enabled
     */
    fun createAllNotificationsEnabled(): NotificationPreferences {
        return createNotificationSettings(
            dailyLoggingEnabled = true,
            periodPredictionEnabled = true,
            ovulationEnabled = true,
            insightNotificationsEnabled = true,
            globalNotificationsEnabled = true
        )
    }
    
    /**
     * Creates NotificationSettings with all notifications disabled
     */
    fun createAllNotificationsDisabled(): NotificationPreferences {
        return createNotificationSettings(
            dailyLoggingEnabled = false,
            periodPredictionEnabled = false,
            ovulationEnabled = false,
            insightNotificationsEnabled = false,
            globalNotificationsEnabled = false
        )
    }
    
    /**
     * Creates NotificationSettings with only essential notifications enabled
     */
    fun createEssentialNotificationsOnly(): NotificationPreferences {
        return createNotificationSettings(
            dailyLoggingEnabled = true,
            periodPredictionEnabled = true,
            ovulationEnabled = false,
            insightNotificationsEnabled = false,
            globalNotificationsEnabled = true
        )
    }
    
    /**
     * Creates UserPreferences with Imperial unit system
     */
    fun createImperialUserPreferences(userId: String = "imperial-user"): UserPreferences {
        return createUserPreferences(
            userId = userId,
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true
        )
    }
    
    /**
     * Creates UserPreferences with Metric unit system
     */
    fun createMetricUserPreferences(userId: String = "metric-user"): UserPreferences {
        return createUserPreferences(
            userId = userId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = true
        )
    }
    
    /**
     * Creates UserPreferences with synced status
     */
    fun createSyncedUserPreferences(userId: String = "synced-user"): UserPreferences {
        return createUserPreferences(
            userId = userId,
            syncStatus = SyncStatus.SYNCED
        )
    }
    
    /**
     * Creates UserPreferences with failed sync status
     */
    fun createFailedSyncUserPreferences(userId: String = "failed-user"): UserPreferences {
        return createUserPreferences(
            userId = userId,
            syncStatus = SyncStatus.FAILED
        )
    }
}