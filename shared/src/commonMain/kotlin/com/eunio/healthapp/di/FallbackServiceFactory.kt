package com.eunio.healthapp.di

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.manager.AuthManager
import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.notification.*
import com.eunio.healthapp.domain.service.EnhancedSettingsManager
import com.eunio.healthapp.domain.service.EnhancedNotificationManager
import com.eunio.healthapp.domain.service.EnhancedAuthManager
import com.eunio.healthapp.domain.util.UserNotificationService
import com.eunio.healthapp.presentation.viewmodel.*
import com.eunio.healthapp.domain.usecase.auth.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalTime

/**
 * Factory for creating fallback service implementations when dependency injection fails.
 * These implementations provide basic functionality to prevent application crashes.
 * Now includes enhanced error handling and graceful degradation.
 */
object FallbackServiceFactory {
    
    /**
     * Create an enhanced SettingsManager with fallback capabilities
     */
    fun createEnhancedSettingsManager(primaryManager: SettingsManager?): SettingsManager {
        return if (primaryManager != null) {
            KoinLogger.logServiceFallback("SettingsManager", "Enhanced with In-Memory Fallback")
            EnhancedSettingsManager(primaryManager, InMemorySettingsManager())
        } else {
            KoinLogger.logServiceFallback("SettingsManager", "In-Memory Implementation Only")
            InMemorySettingsManager()
        }
    }
    
    /**
     * Create an enhanced NotificationManager with fallback capabilities
     */
    fun createEnhancedNotificationManager(primaryManager: NotificationManager?): NotificationManager {
        return if (primaryManager != null) {
            KoinLogger.logServiceFallback("NotificationManager", "Enhanced with Logging Fallback")
            EnhancedNotificationManager(primaryManager, LoggingNotificationManager())
        } else {
            KoinLogger.logServiceFallback("NotificationManager", "Logging Implementation Only")
            LoggingNotificationManager()
        }
    }
    
    /**
     * Create an enhanced AuthManager with fallback capabilities
     */
    fun createEnhancedAuthManager(primaryManager: AuthManager?): AuthManager {
        return if (primaryManager != null) {
            KoinLogger.logServiceFallback("AuthManager", "Enhanced with Mock Fallback")
            EnhancedAuthManager(primaryManager, MockAuthManager())
        } else {
            KoinLogger.logServiceFallback("AuthManager", "Mock Implementation Only")
            MockAuthManager()
        }
    }
    
    /**
     * Create a fallback SettingsManager that stores settings in memory
     */
    fun createFallbackSettingsManager(): SettingsManager {
        KoinLogger.logServiceFallback("SettingsManager", "In-Memory Implementation")
        return InMemorySettingsManager()
    }
    
    /**
     * Create a fallback NotificationManager that logs notifications instead of showing them
     */
    fun createFallbackNotificationManager(): NotificationManager {
        KoinLogger.logServiceFallback("NotificationManager", "Logging Implementation")
        return LoggingNotificationManager()
    }
    
    /**
     * Create a fallback AuthManager that provides mock authentication
     */
    fun createFallbackAuthManager(): AuthManager {
        KoinLogger.logServiceFallback("AuthManager", "Mock Implementation")
        return MockAuthManager()
    }
    
    /**
     * Create a fallback DatabaseManager that provides basic functionality
     */
    fun createFallbackDatabaseManager(): DatabaseManager {
        KoinLogger.logServiceFallback("DatabaseManager", "Mock Implementation")
        // Return null since DatabaseManager is a concrete class that requires actual database setup
        // In practice, this would need proper fallback implementation
        throw NotImplementedError("DatabaseManager fallback not implemented - requires actual database setup")
    }
}

/**
 * In-memory implementation of SettingsManager for fallback scenarios
 */
private class InMemorySettingsManager : SettingsManager {
    private var userSettings = createDefaultUserSettings()
    
    private fun createDefaultUserSettings(): UserSettings {
        return UserSettings.createDefault("fallback_user_id")
    }
    
    override suspend fun getUserSettings(): Result<UserSettings> {
        KoinLogger.logInfo("Fallback SettingsManager: Returning default user settings")
        return Result.Success(userSettings)
    }
    
    override fun observeSettingsChanges(): Flow<UserSettings> {
        return flowOf(userSettings)
    }
    
    override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> {
        userSettings = userSettings.copy(unitPreferences = preferences)
        KoinLogger.logInfo("Fallback SettingsManager: Updated unit preferences")
        return Result.Success(Unit)
    }
    
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        userSettings = userSettings.copy(notificationPreferences = preferences)
        KoinLogger.logInfo("Fallback SettingsManager: Updated notification preferences")
        return Result.Success(Unit)
    }
    
    override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> {
        userSettings = userSettings.copy(cyclePreferences = preferences)
        KoinLogger.logInfo("Fallback SettingsManager: Updated cycle preferences")
        return Result.Success(Unit)
    }
    
    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit> {
        userSettings = userSettings.copy(privacyPreferences = preferences)
        KoinLogger.logInfo("Fallback SettingsManager: Updated privacy preferences")
        return Result.Success(Unit)
    }
    
    override suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit> {
        userSettings = userSettings.copy(displayPreferences = preferences)
        KoinLogger.logInfo("Fallback SettingsManager: Updated display preferences")
        return Result.Success(Unit)
    }
    
    override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> {
        userSettings = userSettings.copy(syncPreferences = preferences)
        KoinLogger.logInfo("Fallback SettingsManager: Updated sync preferences")
        return Result.Success(Unit)
    }
    
    override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
        userSettings = updateFunction(userSettings)
        KoinLogger.logInfo("Fallback SettingsManager: Updated settings with function")
        return Result.Success(userSettings)
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        KoinLogger.logInfo("Fallback SettingsManager: Settings validation passed (mock)")
        return Result.Success(Unit)
    }
    
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> {
        val oldUnitPrefs = if (preserveUnitPreferences) userSettings.unitPreferences else UnitPreferences()
        userSettings = createDefaultUserSettings().copy(unitPreferences = oldUnitPrefs)
        KoinLogger.logInfo("Fallback SettingsManager: Reset to defaults")
        return Result.Success(userSettings)
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        KoinLogger.logInfo("Fallback SettingsManager: Sync completed (mock)")
        return Result.Success(Unit)
    }
    
    override suspend fun exportSettings(): Result<String> {
        KoinLogger.logInfo("Fallback SettingsManager: Exported settings (mock)")
        return Result.Success("{\"mock\": \"settings\"}")
    }
    
    override suspend fun importSettings(backupData: String): Result<Unit> {
        KoinLogger.logInfo("Fallback SettingsManager: Imported settings (mock)")
        return Result.Success(Unit)
    }
    
    override suspend fun isSynced(): Boolean {
        return true // Always synced in fallback
    }
    
    override fun observeSyncStatus(): Flow<Boolean> {
        return flowOf(true)
    }
}

/**
 * Logging implementation of NotificationManager for fallback scenarios
 */
private class LoggingNotificationManager : NotificationManager {
    
    override suspend fun updateNotificationSchedule(preferences: NotificationPreferences): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would update notification schedule")
        try {
            UserNotificationService.showWarning(
                "Notifications are temporarily unavailable. Your preferences have been saved and will apply when notifications are restored."
            )
        } catch (e: Exception) {
            // Ignore notification service errors in fallback mode
            KoinLogger.logInfo("UserNotificationService error ignored in fallback mode: ${e.message}")
        }
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun scheduleNotification(type: NotificationType, setting: NotificationSetting): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would schedule notification - $type")
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun scheduleNotification(
        type: NotificationType,
        time: LocalTime,
        repeatInterval: RepeatInterval,
        daysInAdvance: Int
    ): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would schedule notification - $type at $time")
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun cancelNotification(type: NotificationType): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would cancel notification - $type")
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would cancel all notifications")
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun requestNotificationPermission(): kotlin.Result<Boolean> {
        KoinLogger.logInfo("Fallback NotificationManager: Would request notification permission")
        try {
            UserNotificationService.showInfo(
                "Notification permissions cannot be requested at this time. Please enable notifications in your device settings."
            )
        } catch (e: Exception) {
            // Ignore notification service errors in fallback mode
            KoinLogger.logInfo("UserNotificationService error ignored in fallback mode: ${e.message}")
        }
        return kotlin.Result.success(false)
    }
    
    override suspend fun getNotificationPermissionStatus(): NotificationPermissionStatus {
        KoinLogger.logInfo("Fallback NotificationManager: Returning denied permission status")
        return NotificationPermissionStatus.DENIED
    }
    
    override suspend fun areNotificationsEnabled(): Boolean {
        KoinLogger.logInfo("Fallback NotificationManager: Returning false for notifications enabled")
        return false
    }
    
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would open notification settings")
        return kotlin.Result.success(Unit)
    }
    
    override suspend fun getScheduledNotifications(): List<NotificationType> {
        KoinLogger.logInfo("Fallback NotificationManager: Returning empty scheduled notifications list")
        return emptyList()
    }
    
    override suspend fun testNotification(type: NotificationType): kotlin.Result<Unit> {
        KoinLogger.logInfo("Fallback NotificationManager: Would test notification - $type")
        return kotlin.Result.success(Unit)
    }
}

/**
 * Mock implementation of AuthManager for fallback scenarios
 */
private class MockAuthManager : AuthManager {
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        KoinLogger.logInfo("Fallback AuthManager: Mock sign in for $email")
        return Result.Error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("Fallback auth manager - sign in not available"))
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        KoinLogger.logInfo("Fallback AuthManager: Mock sign up for $email")
        return Result.Error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("Fallback auth manager - sign up not available"))
    }
    
    override suspend fun signOut(): Result<Unit> {
        KoinLogger.logInfo("Fallback AuthManager: Mock sign out")
        return Result.Success(Unit)
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        KoinLogger.logInfo("Fallback AuthManager: No current user")
        return Result.Success(null)
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        KoinLogger.logInfo("Fallback AuthManager: Mock password reset for $email")
        return Result.Error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("Fallback auth manager - password reset not available"))
    }
    
    override suspend fun isAuthenticated(): Boolean {
        KoinLogger.logInfo("Fallback AuthManager: Not authenticated")
        return false
    }
}