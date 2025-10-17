package com.eunio.healthapp.di

import com.eunio.healthapp.data.repository.SyncManager
import com.eunio.healthapp.data.service.DatabaseServiceImpl
import com.eunio.healthapp.data.sync.BackgroundSyncService
import com.eunio.healthapp.data.sync.ConflictResolutionService
import com.eunio.healthapp.data.sync.OfflineModeService
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.domain.util.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val sharedModule = module {
    // Error Handler
    single { ErrorHandler() }
    
    // Coroutine Scope for sync operations
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Database Manager - Critical for DAO creation
    single { 
        com.eunio.healthapp.data.local.DatabaseManager(
            driverFactory = get()
        )
    }
    
    // Database Service - Service interface for database operations
    single<DatabaseService> { 
        DatabaseServiceImpl(
            databaseManager = get()
        )
    }
    
    // DAOs - Critical for data persistence
    single { 
        get<com.eunio.healthapp.data.local.DatabaseManager>().getUserDao()
    }
    
    single { 
        get<com.eunio.healthapp.data.local.DatabaseManager>().getDailyLogDao()
    }
    
    single { 
        get<com.eunio.healthapp.data.local.DatabaseManager>().getUserSettingsDao()
    }
    
    // Include core modules - All modules are now properly configured
    includes(serviceModule)
    includes(repositoryModule)
    includes(unitSystemModule)
    includes(useCaseModule) 
    includes(viewModelModule)
    includes(settingsIntegrationModule)
}