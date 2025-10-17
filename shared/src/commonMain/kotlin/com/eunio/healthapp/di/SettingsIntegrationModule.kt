package com.eunio.healthapp.di

import com.eunio.healthapp.domain.manager.*
import com.eunio.healthapp.domain.usecase.settings.RestoreSettingsOnNewDeviceUseCase
import com.eunio.healthapp.domain.usecase.settings.ResolveSettingsConflictUseCase
import com.eunio.healthapp.domain.util.EnhancedUnitConverter
import com.eunio.healthapp.domain.util.EnhancedUnitConverterImpl
import org.koin.dsl.module

/**
 * Koin module for settings integration components.
 * Provides dependency injection for enhanced settings functionality,
 * backup and restore capabilities, and integration with existing app components.
 */
val settingsIntegrationModule = module {
    
    // Enhanced Unit Converter (no dependencies)
    single<EnhancedUnitConverter> { 
        EnhancedUnitConverterImpl() 
    }
    
    // Core Notification Manager (depends on platformNotificationService)
    single<NotificationManager> { 
        NotificationManagerImpl(
            platformNotificationService = get()
        )
    }
    
    // Settings Backup Manager (must be before SettingsManager)
    single<SettingsBackupManager> { 
        SettingsBackupManagerImpl(
            settingsRepository = get(),
            localDataSource = get(),
            remoteDataSource = get(),
            networkConnectivity = get()
        )
    }
    
    // Core Settings Manager (depends on SettingsBackupManager)
    single<SettingsManager> { 
        SettingsManagerImpl(
            settingsRepository = get(),
            settingsBackupManager = get(),
            notificationManager = get(),
            predictOvulationUseCase = get(),
            updateCycleUseCase = get(),
            currentUserId = { "current-user" } // TODO: Get from auth service
        )
    }
    
    // Settings-Aware Unit System Manager
    single<SettingsAwareUnitSystemManager> { 
        SettingsAwareUnitSystemManagerImpl(
            settingsManager = get()
        )
    }
    
    // Notification Integration Manager
    single<NotificationIntegrationManager> { 
        NotificationIntegrationManagerImpl(
            settingsManager = get(),
            notificationManager = get(),
            legacyReminderService = getOrNull() // Optional legacy service
        )
    }
    
    // Display Preferences Integration Manager
    single<DisplayPreferencesIntegrationManager> { 
        DisplayPreferencesIntegrationManagerImpl(
            settingsManager = get(),
            hapticFeedbackManager = get(),
            themeManager = get()
        )
    }
    
    // Settings Use Cases
    factory { 
        RestoreSettingsOnNewDeviceUseCase(
            settingsBackupManager = get(),
            userRepository = get()
        )
    }
    
    factory { 
        ResolveSettingsConflictUseCase(
            settingsRepository = get(),
            settingsBackupManager = get()
        )
    }
}