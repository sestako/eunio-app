package com.eunio.healthapp.di

import com.eunio.healthapp.data.local.datasource.*
import com.eunio.healthapp.data.remote.datasource.*
import com.eunio.healthapp.data.repository.*
import com.eunio.healthapp.domain.repository.*
import org.koin.dsl.module

/**
 * Koin dependency injection module for Repositories.
 * Provides singleton instances of all repositories with their required dependencies.
 */
val repositoryModule = module {
    
    // Data Sources - Settings
    single<SettingsLocalDataSource> { 
        SettingsLocalDataSourceImpl(
            databaseManager = get()
        )
    }
    
    single<SettingsRemoteDataSource> { 
        SettingsRemoteDataSourceImpl(
            firestoreService = get()
        )
    }
    
    // Data Sources - Preferences
    single<PreferencesLocalDataSource> { 
        PreferencesLocalDataSourceImpl(
            databaseManager = get()
        )
    }
    
    single<PreferencesRemoteDataSource> { 
        PreferencesRemoteDataSourceImpl(
            firestoreService = get()
        )
    }
    
    // User Repository
    single<UserRepository> { 
        UserRepositoryImpl(
            authService = get(),
            firestoreService = get(),
            userDao = get(),
            errorHandler = get()
        )
    }
    
    // Log Repository
    single<LogRepository> { 
        LogRepositoryImpl(
            firestoreService = get(),
            dailyLogDao = get(),
            errorHandler = get()
        )
    }
    
    // Cycle Repository
    single<CycleRepository> { 
        CycleRepositoryImpl(
            firestoreService = get(),
            errorHandler = get()
        )
    }
    
    // Insight Repository
    single<InsightRepository> { 
        InsightRepositoryImpl(
            firestoreService = get(),
            errorHandler = get()
        )
    }
    
    // Health Report Repository
    single<HealthReportRepository> { 
        HealthReportRepositoryImpl(
            firestoreService = get(),
            cycleRepository = get(),
            logRepository = get(),
            insightRepository = get(),
            pdfGenerationService = get()
        )
    }
    
    // Help and Support Repository
    single<HelpSupportRepository> { 
        HelpSupportRepositoryImpl(
            platformManager = get()
        )
    }
    
    // Preferences Repository
    single<PreferencesRepository> { 
        PreferencesRepositoryImpl(
            localDataSource = get(),
            remoteDataSource = get(),
            networkConnectivity = get(),
            userRepository = get(),
            errorHandler = get()
        )
    }
    
    // Settings Repository
    single<SettingsRepository> { 
        SettingsRepositoryImpl(
            localDataSource = get(),
            remoteDataSource = get(),
            networkConnectivity = get(),
            userRepository = get(),
            errorHandler = get()
        )
    }
}