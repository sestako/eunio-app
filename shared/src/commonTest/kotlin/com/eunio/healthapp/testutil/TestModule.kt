package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.repository.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.haptic.HapticFeedbackManager
import com.eunio.healthapp.platform.theme.ThemeManager
import com.eunio.healthapp.platform.notification.PlatformNotificationService
import com.eunio.healthapp.platform.accessibility.AccessibilityManager
import com.eunio.healthapp.platform.PlatformLifecycleManager
import com.eunio.healthapp.platform.PlatformNavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Test module that provides mock implementations for all dependencies.
 * This module replaces production services with test-friendly mocks.
 */
val testModule = module {
    
    // Core utilities
    single { ErrorHandler() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Create a single MockServices instance to share across all dependencies
    single { MockServices() }
    
    // Create a single MockServiceSet instance for platform services
    single { MockServiceFactory.createMockServiceSet() }
    
    // Mock repositories using the shared MockServices instance
    single<UserRepository> { get<MockServices>().userRepository }
    single<LogRepository> { get<MockServices>().logRepository }
    single<CycleRepository> { get<MockServices>().cycleRepository }
    single<InsightRepository> { get<MockServices>().insightRepository }
    single<HealthReportRepository> { get<MockServices>().healthReportRepository }
    
    // Additional repositories needed by tests
    single<HelpSupportRepository> { MockHelpSupportRepository() }
    single<PreferencesRepository> { MockPreferencesRepository() }
    single<SettingsRepository> { MockSettingsRepository() }
    
    // Mock auth service
    single<AuthService> { get<MockServices>().authService }
    
    // Mock platform services with Koin registration
    single<NetworkConnectivity> { get<MockServiceSet>().networkConnectivity }
    single<PlatformManager> { get<MockServiceSet>().platformManager }
    single<HapticFeedbackManager> { get<MockServiceSet>().hapticFeedbackManager }
    single<ThemeManager> { get<MockServiceSet>().themeManager }
    single<PlatformNotificationService> { get<MockServiceSet>().notificationService }
    single<AccessibilityManager> { get<MockServiceSet>().accessibilityManager }
    single<PlatformLifecycleManager> { get<MockServiceSet>().lifecycleManager }
    single<PlatformNavigationManager> { get<MockServiceSet>().navigationManager }
    
    // Additional services that might be missing - using existing mock services from MockServices
    single { get<MockServices>().firestoreService }
    single { MockSettingsLocalDataSource() }
    single { MockSettingsRemoteDataSource() }
    single { MockPDFGenerationService() }
    single { MockTestDriverFactory() }
    single { MockUnitSystemInitializer() }
    
    // Database-related services
    single<com.eunio.healthapp.domain.service.DatabaseService> { MockDatabaseService() }
    single<com.eunio.healthapp.data.local.DatabaseDriverFactoryInterface> { MockTestDriverFactory() }
    
    // Additional managers and services needed by use cases
    single<com.eunio.healthapp.domain.manager.SettingsManager> { MockSettingsManager() }
    single<com.eunio.healthapp.domain.manager.AuthManager> { com.eunio.healthapp.testutil.MockAuthManager() }
    single<com.eunio.healthapp.domain.manager.NotificationManager> { MockNotificationManager() }
    single<com.eunio.healthapp.domain.manager.UnitSystemManager> { MockUnitSystemManager() }
    single<com.eunio.healthapp.domain.util.UnitConverter> { MockUnitConverter() }
    single<UnitPreferencesConverter> { MockUnitPreferencesConverter() }
    single<com.eunio.healthapp.domain.manager.UnitSystemInitializer> { MockUnitSystemInitializer() }
    single<com.eunio.healthapp.domain.service.PDFGenerationService> { MockPDFGenerationService() }
    
    // Settings-related services
    single<com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource> { MockSettingsLocalDataSource() }
    single<com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource> { MockSettingsRemoteDataSource() }
    single<com.eunio.healthapp.domain.manager.SettingsBackupManager> { MockSettingsBackupManager() }
    
    // Use Cases needed by ViewModels
    factory { com.eunio.healthapp.domain.usecase.auth.GetCurrentUserUseCase(get(), get()) }
    factory { com.eunio.healthapp.domain.usecase.auth.CompleteOnboardingUseCase(get(), get()) }
    factory { com.eunio.healthapp.domain.usecase.logging.GetDailyLogUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.logging.SaveDailyLogUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.cycle.GetCurrentCycleUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.cycle.PredictOvulationUseCase(get(), get()) }
    factory { com.eunio.healthapp.domain.usecase.logging.GetLogHistoryUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.support.GetHelpCategoriesUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.support.SearchFAQsUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.support.GetTutorialsUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.support.SubmitSupportRequestUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.profile.UpdateUserProfileUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.profile.UpdateHealthGoalUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.profile.GetUserStatisticsUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.settings.GetDisplayPreferencesUseCase(get()) }
    factory { com.eunio.healthapp.domain.usecase.settings.UpdateDisplayPreferencesUseCase(get()) }
    
    // DAOs for database operations - create simple mocks
    single { SimpleMockUserDao() }
    single { SimpleMockDailyLogDao() }
    single { SimpleMockUserSettingsDao() }
    
    // Set up test dispatcher for ViewModels to avoid Dispatchers.Main issues
    single<kotlinx.coroutines.CoroutineDispatcher> { kotlinx.coroutines.test.UnconfinedTestDispatcher() }
    
    // ViewModels with proper test setup - these will use the test dispatcher
    factory { 
        com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel(
            getCurrentUserUseCase = get(),
            completeOnboardingUseCase = get(),
            dispatcher = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.DailyLoggingViewModel(
            getDailyLogUseCase = get(),
            saveDailyLogUseCase = get(),
            authManager = get(),
            dispatcher = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.CalendarViewModel(
            getCurrentCycleUseCase = get(),
            predictOvulationUseCase = get(),
            getLogHistoryUseCase = get(),
            dispatcher = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.InsightsViewModel(
            insightRepository = get(),
            dispatcher = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.HelpSupportViewModel(
            getHelpCategoriesUseCase = get(),
            searchFAQsUseCase = get(),
            getTutorialsUseCase = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.SupportRequestViewModel(
            submitSupportRequestUseCase = get(),
            helpSupportRepository = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.BugReportViewModel(
            submitSupportRequestUseCase = get(),
            helpSupportRepository = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.ProfileManagementViewModel(
            getCurrentUserUseCase = get(),
            updateUserProfileUseCase = get(),
            updateHealthGoalUseCase = get(),
            getUserStatisticsUseCase = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.SettingsViewModel(
            settingsManager = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.EnhancedSettingsViewModel(
            settingsManager = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.DisplayPreferencesViewModel(
            getDisplayPreferencesUseCase = get(),
            updateDisplayPreferencesUseCase = get(),
            hapticFeedbackManager = get(),
            accessibilityManager = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.NotificationPreferencesViewModel(
            settingsManager = get(),
            notificationManager = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.PrivacyPreferencesViewModel(
            settingsManager = get(),
            settingsRepository = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.SyncPreferencesViewModel(
            settingsManager = get(),
            settingsRepository = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.CyclePreferencesViewModel(
            settingsManager = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.UnitPreferencesViewModel(
            settingsManager = get(),
            unitConverter = get()
        )
    }
    factory { 
        com.eunio.healthapp.presentation.viewmodel.UnitSystemSettingsViewModel(
            unitSystemManager = get(),
            dispatcher = get()
        )
    }
}

/**
 * Minimal test module with only essential services.
 * Use this for tests that don't need full dependency injection.
 */
val minimalTestModule = module {
    single { ErrorHandler() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { MockServices() }
    single<AuthService> { get<MockServices>().authService }
    single<UserRepository> { get<MockServices>().userRepository }
    
    // Essential platform services
    single<NetworkConnectivity> { com.eunio.healthapp.testutil.MockNetworkConnectivity() }
    single<HapticFeedbackManager> { MockServiceFactory.createMockHapticFeedbackManager() }
    single<ThemeManager> { MockServiceFactory.createMockThemeManager() }
}

/**
 * Repository-focused test module for testing repository implementations.
 */
val repositoryTestModule = module {
    // Core utilities
    single { ErrorHandler() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Mock services
    single { MockServices() }
    
    // Mock repositories
    single<UserRepository> { get<MockServices>().userRepository }
    single<LogRepository> { get<MockServices>().logRepository }
    single<CycleRepository> { get<MockServices>().cycleRepository }
    single<InsightRepository> { get<MockServices>().insightRepository }
    single<HealthReportRepository> { get<MockServices>().healthReportRepository }
    
    // Mock auth service
    single<AuthService> { get<MockServices>().authService }
    
    // Network connectivity for repository tests
    single<NetworkConnectivity> { com.eunio.healthapp.testutil.MockNetworkConnectivity() }
}

/**
 * Platform services test module for testing platform-specific functionality.
 * This module provides all platform services with proper mock implementations.
 */
val platformServicesTestModule = module {
    // Core utilities
    single { ErrorHandler() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Create mock service set for platform services
    single { MockServiceFactory.createMockServiceSet() }
    
    // Platform services with Koin registration
    single<NetworkConnectivity> { get<MockServiceSet>().networkConnectivity }
    single<PlatformManager> { get<MockServiceSet>().platformManager }
    single<HapticFeedbackManager> { get<MockServiceSet>().hapticFeedbackManager }
    single<ThemeManager> { get<MockServiceSet>().themeManager }
    single<PlatformNotificationService> { get<MockServiceSet>().notificationService }
    single<AccessibilityManager> { get<MockServiceSet>().accessibilityManager }
    single<PlatformLifecycleManager> { get<MockServiceSet>().lifecycleManager }
    single<PlatformNavigationManager> { get<MockServiceSet>().navigationManager }
}
// Additional mock classes for missing services
class MockSettingsLocalDataSource : com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource {
    override suspend fun getSettings(): com.eunio.healthapp.domain.model.settings.UserSettings? = null
    override suspend fun getSettings(userId: String): com.eunio.healthapp.domain.model.settings.UserSettings? = null
    override suspend fun saveSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun deleteSettings(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun getPendingSyncSettings(): List<com.eunio.healthapp.domain.model.settings.UserSettings> = emptyList()
    override suspend fun markAsSynced(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun markAsSyncFailed(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun clearAllSettings(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun clearSettings(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun settingsExist(userId: String): Boolean = false
    override suspend fun getLastModifiedTimestamp(userId: String): Long? = null
    override fun observeSettings(userId: String): kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.model.settings.UserSettings?> = 
        kotlinx.coroutines.flow.flowOf(null)
    override suspend fun createSettingsBackup(userId: String, backupType: String): com.eunio.healthapp.domain.util.Result<Long> = 
        com.eunio.healthapp.domain.util.Result.Success(1L)
    override suspend fun getSettingsBackup(backupId: Long): com.eunio.healthapp.domain.util.Result<String?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun getUserBackups(userId: String): com.eunio.healthapp.domain.util.Result<List<com.eunio.healthapp.data.local.datasource.SettingsBackupInfo>> = 
        com.eunio.healthapp.domain.util.Result.Success(emptyList())
    override suspend fun deleteSettingsBackup(backupId: Long): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun deleteUserBackups(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun getSettingsCount(): Long = 0L
    override suspend fun getSettingsDataSize(): Long = 0L
    override suspend fun performMaintenance(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
}

class MockSettingsRemoteDataSource : com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource {
    override suspend fun getSettings(userId: String): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun saveSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun conditionalUpdateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings, expectedLastModified: Long): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun deleteSettings(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun clearAllSettings(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun settingsExist(userId: String): com.eunio.healthapp.domain.util.Result<Boolean> = 
        com.eunio.healthapp.domain.util.Result.Success(false)
    override suspend fun getLastModifiedTimestamp(userId: String): com.eunio.healthapp.domain.util.Result<Long?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override fun observeSettings(userId: String): kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.model.settings.UserSettings?> = 
        kotlinx.coroutines.flow.flowOf(null)
    override suspend fun createSettingsHistory(userId: String, settings: com.eunio.healthapp.domain.model.settings.UserSettings, changeType: String, deviceInfo: String?): com.eunio.healthapp.domain.util.Result<String> = 
        com.eunio.healthapp.domain.util.Result.Success("mock-history-id")
    override suspend fun getSettingsHistory(userId: String, limit: Int): com.eunio.healthapp.domain.util.Result<List<com.eunio.healthapp.data.remote.datasource.SettingsHistoryEntry>> = 
        com.eunio.healthapp.domain.util.Result.Success(emptyList())
    override suspend fun deleteSettingsHistory(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun batchUpdateSettings(settingsList: List<com.eunio.healthapp.domain.model.settings.UserSettings>): com.eunio.healthapp.domain.util.Result<Int> = 
        com.eunio.healthapp.domain.util.Result.Success(settingsList.size)
    override suspend fun getSettingsStatistics(userId: String): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.data.remote.datasource.SettingsStatistics> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.data.remote.datasource.SettingsStatistics(userId, 0, 0, "", 0.0, 0))
    override suspend fun validateConnection(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun getServerTimestamp(): com.eunio.healthapp.domain.util.Result<Long> = 
        com.eunio.healthapp.domain.util.Result.Success(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
    override suspend fun healthCheck(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.data.remote.datasource.RemoteStorageHealth> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.data.remote.datasource.RemoteStorageHealth(true, 100, kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), 0, "1.0"))
}

class MockPDFGenerationService : com.eunio.healthapp.domain.service.PDFGenerationService {
    override suspend fun generateReportPDF(report: com.eunio.healthapp.domain.model.HealthReport): String = "mock-pdf-path"
    override suspend fun generateCustomPDF(report: com.eunio.healthapp.domain.model.HealthReport, templateType: com.eunio.healthapp.domain.service.PDFTemplate): String = "mock-custom-pdf-path"
    override fun validateReportForPDF(report: com.eunio.healthapp.domain.model.HealthReport): Boolean = true
}

class MockTestDriverFactory : com.eunio.healthapp.data.local.DatabaseDriverFactoryInterface {
    override fun createDriver(): app.cash.sqldelight.db.SqlDriver {
        throw UnsupportedOperationException("Database functionality not available in common tests")
    }
}

class MockUnitSystemInitializer : com.eunio.healthapp.domain.manager.UnitSystemInitializer {
    override suspend fun initializeForNewUser(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.UnitSystem> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.UnitSystem.METRIC)
    override suspend fun handleLocaleChange(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.UnitSystem> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.UnitSystem.METRIC)
    override fun getUnitSystemForCurrentLocale(): com.eunio.healthapp.domain.model.UnitSystem = 
        com.eunio.healthapp.domain.model.UnitSystem.METRIC
}

// Properly implemented mock classes with all required methods
class MockHelpSupportRepository : com.eunio.healthapp.domain.repository.HelpSupportRepository {
    override suspend fun getHelpCategories(): Result<List<com.eunio.healthapp.domain.model.support.HelpCategory>> = 
        Result.success(emptyList())
    override suspend fun searchFAQs(query: String): Result<List<com.eunio.healthapp.domain.model.support.FAQ>> = 
        Result.success(emptyList())
    override suspend fun getFAQsByCategory(categoryId: String): Result<List<com.eunio.healthapp.domain.model.support.FAQ>> = 
        Result.success(emptyList())
    override suspend fun submitSupportRequest(request: com.eunio.healthapp.domain.model.support.SupportRequest): Result<String> = 
        Result.success("mock-request-id")
    override suspend fun getSupportRequests(userId: String): Result<List<com.eunio.healthapp.domain.model.support.SupportRequest>> = 
        Result.success(emptyList())
    override suspend fun getTutorials(): Result<List<com.eunio.healthapp.domain.model.support.Tutorial>> = 
        Result.success(emptyList())
    override suspend fun getTutorialsByCategory(category: com.eunio.healthapp.domain.model.support.TutorialCategory): Result<List<com.eunio.healthapp.domain.model.support.Tutorial>> = 
        Result.success(emptyList())
    override suspend fun markTutorialCompleted(tutorialId: String, stepId: String?): Result<Unit> = 
        Result.success(Unit)
    override suspend fun getDeviceInfo(): com.eunio.healthapp.domain.model.support.DeviceInfo = 
        com.eunio.healthapp.domain.model.support.DeviceInfo("mock", "mock", "mock", "mock", "mock")
    override suspend fun getAppInfo(): com.eunio.healthapp.domain.model.support.AppInfo = 
        com.eunio.healthapp.domain.model.support.AppInfo("1.0", "1", null, null)
    override suspend fun collectDiagnosticLogs(): Result<String> = 
        Result.success("mock-diagnostic-logs")
}

class MockPreferencesRepository : com.eunio.healthapp.domain.repository.PreferencesRepository {
    override suspend fun getUserPreferences(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.UserPreferences?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun getUserPreferences(userId: String): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.UserPreferences?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun saveUserPreferences(preferences: com.eunio.healthapp.domain.model.UserPreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun syncPreferences(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun clearPreferences(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun clearPreferences(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun syncWithConflictResolution(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun handleOfflineMode(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun getSyncStatistics(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.SyncStatistics> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.SyncStatistics())
    override suspend fun recoverFromSyncFailure(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
}

class MockSettingsRepository : com.eunio.healthapp.domain.repository.SettingsRepository {
    override suspend fun getUserSettings(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun getUserSettings(userId: String): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun saveUserSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateUserSettings(userId: String, updateFunction: (com.eunio.healthapp.domain.model.settings.UserSettings) -> com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.settings.UserSettings("mock"))
    override suspend fun syncSettings(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun syncSettings(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun resolveSettingsConflict(userId: String, localSettings: com.eunio.healthapp.domain.model.settings.UserSettings, remoteSettings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(localSettings)
    override suspend fun backupUserSettings(userId: String): com.eunio.healthapp.domain.util.Result<String> = 
        com.eunio.healthapp.domain.util.Result.Success("{}")
    override suspend fun restoreUserSettings(userId: String, backupData: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun exportUserData(userId: String): com.eunio.healthapp.domain.util.Result<String> = 
        com.eunio.healthapp.domain.util.Result.Success("{}")
    override suspend fun deleteUserSettings(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun clearLocalSettings(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun resetToDefaults(userId: String, locale: String?): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.settings.UserSettings(userId))
    override fun observeUserSettings(userId: String): kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.model.settings.UserSettings?> = 
        kotlinx.coroutines.flow.flowOf(null)
    override fun observeSyncStatus(): kotlinx.coroutines.flow.Flow<Boolean> = 
        kotlinx.coroutines.flow.flowOf(true)
    override suspend fun getPendingSyncSettings(): com.eunio.healthapp.domain.util.Result<List<com.eunio.healthapp.domain.model.settings.UserSettings>> = 
        com.eunio.healthapp.domain.util.Result.Success(emptyList())
    override suspend fun markAsSynced(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun markAsSyncFailed(userId: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun settingsExist(userId: String): com.eunio.healthapp.domain.util.Result<Boolean> = 
        com.eunio.healthapp.domain.util.Result.Success(false)
    override suspend fun getLastModifiedTimestamp(userId: String): com.eunio.healthapp.domain.util.Result<Long?> = 
        com.eunio.healthapp.domain.util.Result.Success(null)
    override suspend fun validateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun migrateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings, targetVersion: Int): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(settings)
}

class MockDatabaseService : com.eunio.healthapp.domain.service.DatabaseService {
    // Create a mock database manager that provides mock DAOs
    private val mockManager = com.eunio.healthapp.testutil.createMockDatabaseManager()
    
    override suspend fun getUserDao(): com.eunio.healthapp.data.local.dao.UserDao = 
        mockManager.getUserDao()
    override suspend fun getDailyLogDao(): com.eunio.healthapp.data.local.dao.DailyLogDao = 
        mockManager.getDailyLogDao()
    override suspend fun getUserPreferencesDao(): com.eunio.healthapp.data.local.dao.UserPreferencesDao = 
        mockManager.getUserPreferencesDao()
    override suspend fun getUserSettingsDao(): com.eunio.healthapp.data.local.dao.UserSettingsDao = 
        mockManager.getUserSettingsDao()
    override suspend fun isHealthy(): Boolean = true
    override suspend fun recover(): Result<Unit> = Result.success(Unit)
    override suspend fun performMaintenance(): Result<Unit> = Result.success(Unit)
    override suspend fun close(): Unit = mockManager.closeDatabase()
}

class MockSettingsManager : com.eunio.healthapp.domain.manager.SettingsManager {
    override suspend fun getUserSettings(): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.settings.UserSettings("mock"))
    override fun observeSettingsChanges(): kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        kotlinx.coroutines.flow.flowOf(com.eunio.healthapp.domain.model.settings.UserSettings("mock"))
    override suspend fun updateUnitPreferences(preferences: com.eunio.healthapp.domain.model.settings.UnitPreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateNotificationPreferences(preferences: com.eunio.healthapp.domain.model.settings.NotificationPreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateCyclePreferences(preferences: com.eunio.healthapp.domain.model.settings.CyclePreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updatePrivacyPreferences(preferences: com.eunio.healthapp.domain.model.settings.PrivacyPreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateDisplayPreferences(preferences: com.eunio.healthapp.domain.model.settings.DisplayPreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateSyncPreferences(preferences: com.eunio.healthapp.domain.model.settings.SyncPreferences): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun updateSettings(updateFunction: (com.eunio.healthapp.domain.model.settings.UserSettings) -> com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.settings.UserSettings("mock"))
    override suspend fun validateSettings(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(com.eunio.healthapp.domain.model.settings.UserSettings("mock"))
    override suspend fun syncSettings(): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun exportSettings(): com.eunio.healthapp.domain.util.Result<String> = 
        com.eunio.healthapp.domain.util.Result.Success("{}")
    override suspend fun importSettings(backupData: String): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun isSynced(): Boolean = true
    override fun observeSyncStatus(): kotlinx.coroutines.flow.Flow<Boolean> = 
        kotlinx.coroutines.flow.flowOf(true)
}

class MockNotificationManager : com.eunio.healthapp.domain.manager.NotificationManager {
    override suspend fun updateNotificationSchedule(preferences: com.eunio.healthapp.domain.model.settings.NotificationPreferences): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
    override suspend fun scheduleNotification(type: com.eunio.healthapp.domain.model.notification.NotificationType, setting: com.eunio.healthapp.domain.model.settings.NotificationSetting): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
    override suspend fun scheduleNotification(type: com.eunio.healthapp.domain.model.notification.NotificationType, time: kotlinx.datetime.LocalTime, repeatInterval: com.eunio.healthapp.domain.model.notification.RepeatInterval, daysInAdvance: Int): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
    override suspend fun cancelNotification(type: com.eunio.healthapp.domain.model.notification.NotificationType): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
    override suspend fun cancelAllNotifications(): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
    override suspend fun requestNotificationPermission(): kotlin.Result<Boolean> = 
        kotlin.Result.success(true)
    override suspend fun getNotificationPermissionStatus(): com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus = 
        com.eunio.healthapp.domain.model.notification.NotificationPermissionStatus.GRANTED
    override suspend fun areNotificationsEnabled(): Boolean = true
    override suspend fun openNotificationSettings(): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
    override suspend fun getScheduledNotifications(): List<com.eunio.healthapp.domain.model.notification.NotificationType> = 
        emptyList()
    override suspend fun testNotification(type: com.eunio.healthapp.domain.model.notification.NotificationType): kotlin.Result<Unit> = 
        kotlin.Result.success(Unit)
}

class MockUnitSystemManager : com.eunio.healthapp.domain.manager.UnitSystemManager {
    override suspend fun getCurrentUnitSystem(): com.eunio.healthapp.domain.model.UnitSystem = 
        com.eunio.healthapp.domain.model.UnitSystem.METRIC
    override suspend fun setUnitSystem(unitSystem: com.eunio.healthapp.domain.model.UnitSystem, isManuallySet: Boolean): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun initializeFromLocale(locale: String): com.eunio.healthapp.domain.model.UnitSystem = 
        com.eunio.healthapp.domain.model.UnitSystem.METRIC
    override suspend fun initializeFromCurrentLocale(): com.eunio.healthapp.domain.model.UnitSystem = 
        com.eunio.healthapp.domain.model.UnitSystem.METRIC
    override fun observeUnitSystemChanges(): kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.model.UnitSystem> = 
        kotlinx.coroutines.flow.flowOf(com.eunio.healthapp.domain.model.UnitSystem.METRIC)
    override suspend fun clearCache(): Unit = Unit
}

class MockUnitConverter : com.eunio.healthapp.domain.util.UnitConverter {
    override fun convertWeight(value: Double, from: com.eunio.healthapp.domain.model.UnitSystem, to: com.eunio.healthapp.domain.model.UnitSystem): Double = value
    override fun convertDistance(value: Double, from: com.eunio.healthapp.domain.model.UnitSystem, to: com.eunio.healthapp.domain.model.UnitSystem): Double = value
    override fun convertTemperature(value: Double, from: com.eunio.healthapp.domain.model.UnitSystem, to: com.eunio.healthapp.domain.model.UnitSystem): Double = value
    override fun formatWeight(value: Double, unitSystem: com.eunio.healthapp.domain.model.UnitSystem): String = "$value"
    override fun formatDistance(value: Double, unitSystem: com.eunio.healthapp.domain.model.UnitSystem): String = "$value"
    override fun formatTemperature(value: Double, unitSystem: com.eunio.healthapp.domain.model.UnitSystem): String = "$value°"
}

class MockSettingsBackupManager : com.eunio.healthapp.domain.manager.SettingsBackupManager {
    override suspend fun createAutomaticBackup(settings: com.eunio.healthapp.domain.model.settings.UserSettings): com.eunio.healthapp.domain.util.Result<Long> = 
        com.eunio.healthapp.domain.util.Result.Success(1L)
    override suspend fun createManualBackup(userId: String): com.eunio.healthapp.domain.util.Result<String> = 
        com.eunio.healthapp.domain.util.Result.Success("{}")
    override suspend fun restoreOnNewDevice(userId: String, backupData: String?): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun resolveConflictWithUserChoice(userId: String, localSettings: com.eunio.healthapp.domain.model.settings.UserSettings, remoteSettings: com.eunio.healthapp.domain.model.settings.UserSettings, strategy: com.eunio.healthapp.domain.manager.ConflictResolutionStrategy): com.eunio.healthapp.domain.util.Result<com.eunio.healthapp.domain.model.settings.UserSettings> = 
        com.eunio.healthapp.domain.util.Result.Success(localSettings)
    override suspend fun importSettings(userId: String, backupData: String, mergeStrategy: com.eunio.healthapp.domain.manager.ImportMergeStrategy): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override suspend fun exportSettings(userId: String, includeMetadata: Boolean): com.eunio.healthapp.domain.util.Result<String> = 
        com.eunio.healthapp.domain.util.Result.Success("{}")
    override suspend fun getBackupHistory(userId: String): com.eunio.healthapp.domain.util.Result<List<com.eunio.healthapp.domain.manager.BackupMetadata>> = 
        com.eunio.healthapp.domain.util.Result.Success(emptyList())
    override suspend fun cleanupOldBackups(userId: String, keepCount: Int): com.eunio.healthapp.domain.util.Result<Unit> = 
        com.eunio.healthapp.domain.util.Result.Success(Unit)
    override fun observeBackupOperations(): kotlinx.coroutines.flow.Flow<com.eunio.healthapp.domain.manager.BackupOperation> = 
        kotlinx.coroutines.flow.flowOf()
}

// Additional missing interface implementations
interface UnitPreferencesConverter {
    fun convertToDisplayUnits(value: Double, unitType: String): String
    fun convertFromDisplayUnits(value: String, unitType: String): Double
    fun getDisplayUnit(unitType: String): String
}

class MockUnitPreferencesConverter : UnitPreferencesConverter {
    override fun convertToDisplayUnits(value: Double, unitType: String): String = "$value"
    override fun convertFromDisplayUnits(value: String, unitType: String): Double = 0.0
    override fun getDisplayUnit(unitType: String): String = "unit"
}

// Simple DAO mocks
class SimpleMockUserDao {
    fun insertUser(user: Any) {}
    fun getUserById(id: String): Any? = null
    fun updateUser(user: Any) {}
    fun deleteUser(id: String) {}
}

class SimpleMockDailyLogDao {
    fun insertLog(log: Any) {}
    fun getLogById(id: String): Any? = null
    fun updateLog(log: Any) {}
    fun deleteLog(id: String) {}
}

class SimpleMockUserSettingsDao {
    fun insertSettings(settings: Any) {}
    fun getSettingsById(id: String): Any? = null
    fun updateSettings(settings: Any) {}
    fun deleteSettings(id: String) {}
}

// DAO mock classes for DatabaseService - create simple mock objects
class TestMockUserDao {
    // Simple mock implementation
}

class TestMockDailyLogDao {
    // Simple mock implementation  
}

class TestMockUserPreferencesDao {
    // Simple mock implementation
}

class TestMockUserSettingsDao {
    // Simple mock implementation
}

// Test dispatcher setup to handle ViewModels that use Dispatchers.Main
class TestDispatcherSetup {
    fun setupTestDispatchers() {
        // Note: setMain is only available in Android/JVM tests, not in common tests
        // ViewModels will need to be tested in platform-specific test modules
        println("Test dispatcher setup - ViewModels may require platform-specific testing")
    }


}
