package com.eunio.healthapp.di

import com.eunio.healthapp.data.network.IOSNetworkConnectivity
import com.eunio.healthapp.data.service.IOSDatabaseService
import com.eunio.healthapp.data.sync.BackgroundSyncService
import com.eunio.healthapp.domain.manager.AuthManager
import com.eunio.healthapp.domain.manager.IOSAuthManager
import com.eunio.healthapp.domain.manager.IOSSettingsManager
import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.manager.NotificationManagerImpl
import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.platform.CrossPlatformPerformanceCoordinator
import com.eunio.healthapp.platform.IOSLifecycleManager
import com.eunio.healthapp.platform.IOSNavigationManager

import com.eunio.healthapp.platform.IOSPlatformOptimizations
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.PlatformOptimizationCoordinator
import org.koin.dsl.module

val iosModule = module {
    // Network Connectivity
    single<NetworkConnectivity> { 
        IOSNetworkConnectivity()
    }
    
    // Database Driver Factory - Critical for database operations
    single<com.eunio.healthapp.data.local.DatabaseDriverFactoryInterface> {
        com.eunio.healthapp.data.local.DatabaseDriverFactory()
    }
    
    // Firebase Services - Using iOS implementation (currently mock, but functional)
    single<com.eunio.healthapp.data.remote.FirestoreService> {
        com.eunio.healthapp.data.remote.FirestoreServiceImpl(
            errorHandler = get()
        )
    }
    
    // Authentication Service - Create iOS implementation
    single<com.eunio.healthapp.data.remote.auth.AuthService> {
        com.eunio.healthapp.data.remote.auth.IOSAuthService(
            errorHandler = get()
        )
    }
    
    // PDF Generation Service - For reports
    single<com.eunio.healthapp.domain.service.PDFGenerationService> {
        com.eunio.healthapp.data.service.IOSPDFGenerationService()
    }
    
    // Platform Notification Service
    single<com.eunio.healthapp.platform.notification.PlatformNotificationService> {
        com.eunio.healthapp.platform.notification.IOSNotificationService()
    }
    
    // Haptic Feedback Manager
    single<com.eunio.healthapp.platform.haptic.HapticFeedbackManager> {
        com.eunio.healthapp.platform.haptic.IOSHapticFeedbackManager()
    }
    
    // Accessibility Manager
    single<com.eunio.healthapp.platform.accessibility.AccessibilityManager> {
        com.eunio.healthapp.platform.accessibility.IOSAccessibilityManager()
    }
    
    // Theme Manager
    single<com.eunio.healthapp.platform.theme.ThemeManager> {
        com.eunio.healthapp.platform.theme.IOSThemeManager()
    }
    
    // Settings Data Sources
    single<com.eunio.healthapp.data.local.datasource.SettingsLocalDataSource> {
        com.eunio.healthapp.data.local.datasource.SettingsLocalDataSourceImpl(
            databaseManager = get()
        )
    }
    
    single<com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSource> {
        com.eunio.healthapp.data.remote.datasource.SettingsRemoteDataSourceImpl(
            firestoreService = get()
        )
    }
    
    // Preferences Data Sources
    single<com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSource> {
        com.eunio.healthapp.data.local.datasource.IOSPreferencesLocalDataSource(
            databaseManager = get()
        )
    }
    
    single<com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource> {
        com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSourceImpl(
            firestoreService = get()
        )
    }
    
    // Platform-specific components
    single<PlatformManager> { IOSPlatformOptimizations() }
    single { IOSLifecycleManager(get<BackgroundSyncService>()) }
    single { PlatformOptimizationCoordinator(get<PlatformManager>()) }
    
    // Performance monitoring
    single { CrossPlatformPerformanceCoordinator(get<PlatformManager>(), get<PlatformOptimizationCoordinator>()) }
    
    // Navigation
    single { IOSNavigationManager(get()) } // Requires UIViewController
    
    // Settings Manager - iOS implementation with NSUserDefaults fallback
    single<SettingsManager> { 
        IOSSettingsManager(
            settingsRepository = get()
        )
    }
    
    // Database Service - iOS implementation with platform-specific optimizations
    single<DatabaseService> { 
        IOSDatabaseService(
            databaseManager = get()
        )
    }
    
    // Notification Manager - iOS implementation using platform notification service
    single<NotificationManager> { 
        NotificationManagerImpl(
            platformNotificationService = get()
        )
    }
    
    // Auth Manager - iOS implementation using Firebase Auth
    single<AuthManager> { 
        IOSAuthManager()
    }
}