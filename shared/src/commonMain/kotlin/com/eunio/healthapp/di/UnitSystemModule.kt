package com.eunio.healthapp.di

import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSource
import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSourceFactory
import com.eunio.healthapp.data.local.datasource.PreferencesLocalDataSourceImpl
import com.eunio.healthapp.data.local.datasource.PlatformPreferencesLocalDataSourceFactory
import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSource
import com.eunio.healthapp.data.remote.datasource.PreferencesRemoteDataSourceImpl
import com.eunio.healthapp.data.remote.sync.OfflineSyncManager
import com.eunio.healthapp.data.remote.sync.PreferencesSyncService
import com.eunio.healthapp.data.repository.PreferencesRepositoryImpl
import com.eunio.healthapp.domain.manager.CrossPlatformUnitSystemManager
import com.eunio.healthapp.domain.manager.UnitSystemInitializer
import com.eunio.healthapp.domain.manager.UnitSystemInitializerImpl
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.manager.UnitSystemManagerImpl
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.data.repository.CachedPreferencesRepository
import com.eunio.healthapp.domain.manager.CachedUnitSystemManager
import com.eunio.healthapp.domain.util.CachedUnitConverter

import com.eunio.healthapp.domain.util.LazyUnitSystemComponents
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import com.eunio.healthapp.domain.util.UnitPreferencesConverter
import com.eunio.healthapp.domain.util.UnitPreferencesConverterImpl
import com.eunio.healthapp.domain.util.UnitSystemComponentsFactory
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import com.eunio.healthapp.domain.util.createLocaleDetector
import com.eunio.healthapp.presentation.viewmodel.UnitSystemSettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Koin dependency injection module for unit system preferences.
 * Provides all necessary components for unit system management including:
 * - Unit conversion utilities
 * - Preference management
 * - Data sources (local and remote)
 * - Repository implementations
 */
val unitSystemModule = module {
    
    // Base Unit Converter - Singleton for performance and consistency
    single<UnitConverter> { 
        UnitConverterImpl() 
    }
    
    // Unit Preferences Converter - Singleton for individual unit conversions
    single<UnitPreferencesConverter> { 
        UnitPreferencesConverterImpl() 
    }
    
    // Cached Unit Converter - Performance optimized with caching
    single<CachedUnitConverter> { 
        CachedUnitConverter(
            delegate = get<UnitConverter>(),
            maxCacheSize = 1000
        )
    }
    
    // Locale Detector - Singleton platform-specific implementation
    single<LocaleDetector> { 
        createLocaleDetector() 
    }
    
    // Base Unit System Manager - Singleton with repository dependencies
    single<UnitSystemManager> { 
        UnitSystemManagerImpl(
            preferencesRepository = get<CachedPreferencesRepository>(),
            userRepository = get(),
            localeDetector = get()
        )
    }
    
    // Cached Unit System Manager - Performance optimized with caching
    single<CachedUnitSystemManager> {
        CachedUnitSystemManager(
            delegate = get<UnitSystemManager>()
        )
    }
    
    // Unit System Initializer - Singleton for handling locale-based initialization
    single<UnitSystemInitializer> { 
        UnitSystemInitializerImpl(
            localeDetector = get(),
            unitSystemManager = get()
        )
    }
    
    // Base Preferences Repository - Singleton with data source dependencies
    single<PreferencesRepository> {
        PreferencesRepositoryImpl(
            localDataSource = get(),
            remoteDataSource = get(),
            networkConnectivity = get(),
            userRepository = get(),
            errorHandler = get()
        )
    }
    
    // Cached Preferences Repository - Performance optimized with caching
    single<CachedPreferencesRepository> {
        CachedPreferencesRepository(
            delegate = get<PreferencesRepository>()
        )
    }
    
    // Platform-specific Data Source Factory
    single<PreferencesLocalDataSourceFactory> { 
        PlatformPreferencesLocalDataSourceFactory()
    }
    
    // Local Data Source - Platform-optimized implementation
    single<PreferencesLocalDataSource> { 
        get<PreferencesLocalDataSourceFactory>().create(get()) // DatabaseManager
    }
    
    // Cross-Platform Unit System Manager - Enhanced manager with platform consistency
    single<CrossPlatformUnitSystemManager> {
        CrossPlatformUnitSystemManager(
            preferencesRepository = get(),
            userRepository = get(),
            localeDetector = get()
        )
    }
    
    // Remote Data Source - Singleton with Firestore dependency
    single<PreferencesRemoteDataSource> { 
        PreferencesRemoteDataSourceImpl(
            firestoreService = get()
        )
    }
    
    // Unit System Error Handler - Singleton for consistent error handling
    single<UnitSystemErrorHandler> { 
        UnitSystemErrorHandler() 
    }
    
    // Preferences Sync Service - Singleton for background sync operations
    single<PreferencesSyncService> { 
        PreferencesSyncService(
            preferencesRepository = get(),
            networkConnectivity = get(),
            errorHandler = get(),
            coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
    }
    
    // Offline Sync Manager - Singleton for comprehensive offline handling
    single<OfflineSyncManager> { 
        OfflineSyncManager(
            preferencesRepository = get(),
            networkConnectivity = get(),
            errorHandler = get(),
            coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
    }
    
    // Lazy Components Factory - Singleton for lazy loading optimization
    single<LazyUnitSystemComponents> {
        LazyUnitSystemComponents(
            preferencesRepositoryFactory = { get<PreferencesRepository>() },
            unitSystemManagerFactory = { prefsRepo -> 
                UnitSystemManagerImpl(
                    preferencesRepository = prefsRepo,
                    userRepository = get(),
                    localeDetector = get()
                )
            }
        )
    }
    
    // ViewModels - Factory for proper lifecycle management
    factory { 
        UnitSystemSettingsViewModel(
            unitSystemManager = get<CachedUnitSystemManager>()
        )
    }
}