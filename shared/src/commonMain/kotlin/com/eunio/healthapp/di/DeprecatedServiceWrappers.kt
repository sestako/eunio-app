package com.eunio.healthapp.di

import com.eunio.healthapp.domain.manager.*
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.notification.*
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Deprecated wrapper classes that preserve existing manual instantiation patterns
 * while providing migration paths to dependency injection.
 * 
 * These wrappers maintain interface compatibility while adding deprecation warnings
 * and fallback mechanisms for when DI is not available.
 */

/**
 * Deprecated wrapper for SettingsManager that maintains backward compatibility.
 * 
 * @deprecated Use dependency injection with get<SettingsManager>() instead.
 * This wrapper will be removed in a future version.
 */
@Deprecated(
    message = "Use dependency injection with get<SettingsManager>() instead",
    replaceWith = ReplaceWith("get<SettingsManager>()", "org.koin.core.component.get"),
    level = DeprecationLevel.WARNING
)
class DeprecatedSettingsManagerWrapper : SettingsManager, KoinComponent {
    
    private val actualManager: SettingsManager by lazy {
        try {
            get<SettingsManager>()
        } catch (e: Exception) {
            FallbackServiceFactory.createFallbackSettingsManager()
        }
    }
    
    override suspend fun getUserSettings(): Result<UserSettings> = actualManager.getUserSettings()
    override fun observeSettingsChanges(): Flow<UserSettings> = actualManager.observeSettingsChanges()
    override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> = actualManager.updateUnitPreferences(preferences)
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> = actualManager.updateNotificationPreferences(preferences)
    override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> = actualManager.updateCyclePreferences(preferences)
    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit> = actualManager.updatePrivacyPreferences(preferences)
    override suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit> = actualManager.updateDisplayPreferences(preferences)
    override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> = actualManager.updateSyncPreferences(preferences)
    override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> = actualManager.updateSettings(updateFunction)
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> = actualManager.validateSettings(settings)
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> = actualManager.resetToDefaults(preserveUnitPreferences)
    override suspend fun syncSettings(): Result<Unit> = actualManager.syncSettings()
    override suspend fun exportSettings(): Result<String> = actualManager.exportSettings()
    override suspend fun importSettings(backupData: String): Result<Unit> = actualManager.importSettings(backupData)
    override suspend fun isSynced(): Boolean = actualManager.isSynced()
    override fun observeSyncStatus(): Flow<Boolean> = actualManager.observeSyncStatus()
}

/**
 * Deprecated wrapper for NotificationManager that maintains backward compatibility.
 * 
 * @deprecated Use dependency injection with get<NotificationManager>() instead.
 * This wrapper will be removed in a future version.
 */
@Deprecated(
    message = "Use dependency injection with get<NotificationManager>() instead",
    replaceWith = ReplaceWith("get<NotificationManager>()", "org.koin.core.component.get"),
    level = DeprecationLevel.WARNING
)
class DeprecatedNotificationManagerWrapper : NotificationManager, KoinComponent {
    
    private val actualManager: NotificationManager by lazy {
        try {
            get<NotificationManager>()
        } catch (e: Exception) {
            FallbackServiceFactory.createFallbackNotificationManager()
        }
    }
    
    override suspend fun updateNotificationSchedule(preferences: NotificationPreferences): kotlin.Result<Unit> = actualManager.updateNotificationSchedule(preferences)
    override suspend fun scheduleNotification(type: NotificationType, setting: NotificationSetting): kotlin.Result<Unit> = actualManager.scheduleNotification(type, setting)
    override suspend fun scheduleNotification(type: NotificationType, time: LocalTime, repeatInterval: RepeatInterval, daysInAdvance: Int): kotlin.Result<Unit> = actualManager.scheduleNotification(type, time, repeatInterval, daysInAdvance)
    override suspend fun cancelNotification(type: NotificationType): kotlin.Result<Unit> = actualManager.cancelNotification(type)
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> = actualManager.cancelAllNotifications()
    override suspend fun requestNotificationPermission(): kotlin.Result<Boolean> = actualManager.requestNotificationPermission()
    override suspend fun getNotificationPermissionStatus(): NotificationPermissionStatus = actualManager.getNotificationPermissionStatus()
    override suspend fun areNotificationsEnabled(): Boolean = actualManager.areNotificationsEnabled()
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> = actualManager.openNotificationSettings()
    override suspend fun getScheduledNotifications(): List<NotificationType> = actualManager.getScheduledNotifications()
    override suspend fun testNotification(type: NotificationType): kotlin.Result<Unit> = actualManager.testNotification(type)
}

/**
 * Deprecated wrapper for AuthManager that maintains backward compatibility.
 * 
 * @deprecated Use dependency injection with get<AuthManager>() instead.
 * This wrapper will be removed in a future version.
 */
@Deprecated(
    message = "Use dependency injection with get<AuthManager>() instead",
    replaceWith = ReplaceWith("get<AuthManager>()", "org.koin.core.component.get"),
    level = DeprecationLevel.WARNING
)
class DeprecatedAuthManagerWrapper : AuthManager, KoinComponent {
    
    private val actualManager: AuthManager by lazy {
        try {
            get<AuthManager>()
        } catch (e: Exception) {
            FallbackServiceFactory.createFallbackAuthManager()
        }
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> = actualManager.signIn(email, password)
    override suspend fun signUp(email: String, password: String, name: String): Result<User> = actualManager.signUp(email, password, name)
    override suspend fun signOut(): Result<Unit> = actualManager.signOut()
    override suspend fun getCurrentUser(): Result<User?> = actualManager.getCurrentUser()
    override suspend fun resetPassword(email: String): Result<Unit> = actualManager.resetPassword(email)
    override suspend fun isAuthenticated(): Boolean = actualManager.isAuthenticated()
}

/**
 * Deprecated factory object for creating service instances with backward compatibility.
 * 
 * @deprecated Use dependency injection through Koin instead.
 * This factory will be removed in a future version.
 */
@Deprecated(
    message = "Use dependency injection through Koin instead",
    replaceWith = ReplaceWith("get<T>() from Koin", "org.koin.core.component.get"),
    level = DeprecationLevel.WARNING
)
object DeprecatedServiceFactory {
    
    /**
     * Creates a SettingsManager instance with backward compatibility.
     * 
     * @deprecated Use get<SettingsManager>() from Koin instead.
     * Migration: Replace DeprecatedServiceFactory.createSettingsManager() 
     * with get<SettingsManager>() in your Koin-enabled component.
     */
    @Deprecated(
        message = "Use get<SettingsManager>() from Koin instead",
        replaceWith = ReplaceWith("get<SettingsManager>()", "org.koin.core.component.get"),
        level = DeprecationLevel.WARNING
    )
    fun createSettingsManager(): SettingsManager {
        return DeprecatedSettingsManagerWrapper()
    }
    
    /**
     * Creates a NotificationManager instance with backward compatibility.
     * 
     * @deprecated Use get<NotificationManager>() from Koin instead.
     * Migration: Replace DeprecatedServiceFactory.createNotificationManager() 
     * with get<NotificationManager>() in your Koin-enabled component.
     */
    @Deprecated(
        message = "Use get<NotificationManager>() from Koin instead",
        replaceWith = ReplaceWith("get<NotificationManager>()", "org.koin.core.component.get"),
        level = DeprecationLevel.WARNING
    )
    fun createNotificationManager(): NotificationManager {
        return DeprecatedNotificationManagerWrapper()
    }
    
    /**
     * Creates an AuthManager instance with backward compatibility.
     * 
     * @deprecated Use get<AuthManager>() from Koin instead.
     * Migration: Replace DeprecatedServiceFactory.createAuthManager() 
     * with get<AuthManager>() in your Koin-enabled component.
     */
    @Deprecated(
        message = "Use get<AuthManager>() from Koin instead",
        replaceWith = ReplaceWith("get<AuthManager>()", "org.koin.core.component.get"),
        level = DeprecationLevel.WARNING
    )
    fun createAuthManager(): AuthManager {
        return DeprecatedAuthManagerWrapper()
    }
}

/**
 * Deprecated platform-specific service factory methods.
 * These maintain compatibility with existing platform-specific instantiation patterns.
 */
object DeprecatedPlatformServiceFactory {
    
    /**
     * Creates platform-specific SettingsManager with backward compatibility.
     * 
     * @deprecated Use platform-specific Koin modules instead.
     */
    @Deprecated(
        message = "Use platform-specific Koin modules instead",
        level = DeprecationLevel.WARNING
    )
    fun createPlatformSettingsManager(): SettingsManager {
        return DeprecatedSettingsManagerWrapper()
    }
    
    /**
     * Creates platform-specific NotificationManager with backward compatibility.
     * 
     * @deprecated Use platform-specific Koin modules instead.
     */
    @Deprecated(
        message = "Use platform-specific Koin modules instead",
        level = DeprecationLevel.WARNING
    )
    fun createPlatformNotificationManager(): NotificationManager {
        return DeprecatedNotificationManagerWrapper()
    }
    
    /**
     * Creates platform-specific AuthManager with backward compatibility.
     * 
     * @deprecated Use platform-specific Koin modules instead.
     */
    @Deprecated(
        message = "Use platform-specific Koin modules instead",
        level = DeprecationLevel.WARNING
    )
    fun createPlatformAuthManager(): AuthManager {
        return DeprecatedAuthManagerWrapper()
    }
}