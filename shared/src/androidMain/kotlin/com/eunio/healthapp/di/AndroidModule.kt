package com.eunio.healthapp.di

import android.content.Context
import com.eunio.healthapp.data.network.AndroidNetworkConnectivity
import com.eunio.healthapp.data.service.AndroidDatabaseService
import com.eunio.healthapp.data.sync.BackgroundSyncService
import com.eunio.healthapp.domain.manager.AndroidAuthManager
import com.eunio.healthapp.domain.manager.AndroidSettingsManager
import com.eunio.healthapp.domain.manager.AuthManager
import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.manager.NotificationManagerImpl
import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.platform.AndroidLifecycleManager
import com.eunio.healthapp.platform.AndroidNavigationManager
import com.eunio.healthapp.platform.AndroidPerformanceMonitor
import com.eunio.healthapp.platform.AndroidPlatformOptimizations
import com.eunio.healthapp.platform.CrossPlatformPerformanceCoordinator
import com.eunio.healthapp.platform.PlatformManager
import com.eunio.healthapp.platform.PlatformOptimizationCoordinator
import org.koin.dsl.module

val androidModule = module {
    // Network Connectivity
    single<NetworkConnectivity> { 
        AndroidNetworkConnectivity(context = get<Context>())
    }
    
    // Database Driver Factory - Critical for database operations
    single<com.eunio.healthapp.data.local.DatabaseDriverFactoryInterface> {
        com.eunio.healthapp.data.local.DatabaseDriverFactory(context = get<Context>())
    }
    
    // Firebase Services - Critical for data operations
    single<com.eunio.healthapp.data.remote.FirestoreService> {
        com.eunio.healthapp.data.remote.FirestoreServiceImpl(
            firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(),
            errorHandler = get()
        )
    }
    
    // Authentication Service - Critical for user management
    single<com.eunio.healthapp.data.remote.auth.AuthService> {
        com.eunio.healthapp.data.remote.auth.FirebaseAuthService(
            errorHandler = get()
        )
    }
    
    // PDF Generation Service - For reports
    single<com.eunio.healthapp.domain.service.PDFGenerationService> {
        com.eunio.healthapp.data.service.AndroidPDFGenerationService(
            cacheDir = get<Context>().cacheDir
        )
    }
    
    // Platform Notification Service
    single<com.eunio.healthapp.platform.notification.PlatformNotificationService> {
        com.eunio.healthapp.platform.notification.AndroidNotificationService(
            context = get<Context>()
        )
    }
    
    // Haptic Feedback Manager
    single<com.eunio.healthapp.platform.haptic.HapticFeedbackManager> {
        com.eunio.healthapp.platform.haptic.AndroidHapticFeedbackManager(
            context = get<Context>()
        )
    }
    
    // Accessibility Manager
    single<com.eunio.healthapp.platform.accessibility.AccessibilityManager> {
        com.eunio.healthapp.platform.accessibility.AndroidAccessibilityManager(
            context = get<Context>()
        )
    }
    
    // Theme Manager
    single<com.eunio.healthapp.platform.theme.ThemeManager> {
        com.eunio.healthapp.platform.theme.AndroidThemeManager(
            context = get<Context>()
        )
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
        com.eunio.healthapp.data.local.datasource.AndroidPreferencesLocalDataSource(
            databaseManager = get(),
            context = get<Context>()
        )
    }
    
    single<com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource> {
        com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSourceImpl(
            firestoreService = get()
        )
    }
    
    // Platform-specific components
    single<PlatformManager> { AndroidPlatformOptimizations(get<Context>()) }
    single { AndroidLifecycleManager(get<BackgroundSyncService>()) }
    single { PlatformOptimizationCoordinator(get<PlatformManager>()) }
    
    // Performance monitoring
    single { AndroidPerformanceMonitor(get<Context>()) }
    single { CrossPlatformPerformanceCoordinator(get<PlatformManager>(), get<PlatformOptimizationCoordinator>()) }
    
    // Navigation
    single { AndroidNavigationManager(get<Context>()) }
    
    // Settings Manager - Android implementation with SharedPreferences fallback
    single<SettingsManager> { 
        AndroidSettingsManager(
            context = get<Context>(),
            settingsRepository = get()
        )
    }
    
    // Database Service - Android implementation with platform-specific optimizations
    single<DatabaseService> { 
        AndroidDatabaseService(
            databaseManager = get(),
            context = get<Context>()
        )
    }
    
    // Notification Manager - Android implementation using platform notification service
    single<NotificationManager> { 
        NotificationManagerImpl(
            platformNotificationService = get()
        )
    }
    
    // Auth Manager - Android implementation using Firebase Auth
    single<AuthManager> { 
        AndroidAuthManager()
    }
}