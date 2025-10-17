package com.eunio.healthapp.di

import com.eunio.healthapp.domain.usecase.auth.*
import com.eunio.healthapp.domain.usecase.cycle.*
import com.eunio.healthapp.domain.usecase.fertility.*
import com.eunio.healthapp.domain.usecase.logging.*
import com.eunio.healthapp.domain.usecase.profile.*
import com.eunio.healthapp.domain.usecase.reports.*
import com.eunio.healthapp.domain.usecase.settings.*
import com.eunio.healthapp.domain.usecase.support.*
import org.koin.dsl.module

/**
 * Koin dependency injection module for Use Cases.
 * Provides factory instances of all use cases with their required dependencies.
 */
val useCaseModule = module {
    
    // Authentication Use Cases
    factory { 
        GetCurrentUserUseCase(
            userRepository = get(),
            authService = get()
        )
    }
    
    factory { 
        CompleteOnboardingUseCase(
            userRepository = get(),
            unitSystemInitializer = get()
        )
    }
    
    factory { 
        SignUpUseCase(
            authService = get()
        )
    }
    
    factory { 
        SignInUseCase(
            authService = get()
        )
    }
    
    factory { 
        SignOutUseCase(
            authService = get()
        )
    }
    
    factory { 
        SendPasswordResetUseCase(
            authService = get()
        )
    }
    
    // Daily Logging Use Cases
    factory { 
        GetDailyLogUseCase(
            logRepository = get()
        )
    }
    
    factory { 
        SaveDailyLogUseCase(
            logRepository = get()
        )
    }
    
    factory { 
        GetLogHistoryUseCase(
            logRepository = get()
        )
    }
    
    // Cycle Tracking Use Cases
    factory { 
        GetCurrentCycleUseCase(
            cycleRepository = get()
        )
    }
    
    factory { 
        StartNewCycleUseCase(
            cycleRepository = get(),
            logRepository = get()
        )
    }
    
    factory { 
        UpdateCycleUseCase(
            cycleRepository = get(),
            logRepository = get()
        )
    }
    
    factory { 
        PredictOvulationUseCase(
            cycleRepository = get(),
            logRepository = get()
        )
    }
    
    // Fertility Tracking Use Cases
    factory { 
        LogBBTUseCase(
            logRepository = get()
        )
    }
    
    factory { 
        LogCervicalMucusUseCase(
            logRepository = get()
        )
    }
    
    factory { 
        LogOPKResultUseCase(
            logRepository = get()
        )
    }
    
    factory { 
        ConfirmOvulationUseCase(
            logRepository = get(),
            cycleRepository = get()
        )
    }
    
    factory { 
        CalculateFertilityWindowUseCase(
            cycleRepository = get(),
            logRepository = get()
        )
    }
    
    // Health Report Use Cases
    factory { 
        GenerateHealthReportUseCase(
            healthReportRepository = get()
        )
    }
    
    factory { 
        GenerateReportPDFUseCase(
            healthReportRepository = get()
        )
    }
    
    factory { 
        ShareHealthReportUseCase(
            healthReportRepository = get()
        )
    }
    
    factory { 
        ValidateReportDataUseCase(
            healthReportRepository = get()
        )
    }
    
    // Help and Support Use Cases
    factory { 
        GetHelpCategoriesUseCase(
            helpSupportRepository = get()
        )
    }
    
    factory { 
        SearchFAQsUseCase(
            helpSupportRepository = get()
        )
    }
    
    factory { 
        SubmitSupportRequestUseCase(
            helpSupportRepository = get()
        )
    }
    
    factory { 
        GetTutorialsUseCase(
            helpSupportRepository = get()
        )
    }
    
    // Profile Management Use Cases
    factory { 
        UpdateUserProfileUseCase(
            userRepository = get()
        )
    }
    
    factory { 
        UpdateHealthGoalUseCase(
            userRepository = get()
        )
    }
    
    factory { 
        GetUserStatisticsUseCase(
            userRepository = get()
        )
    }
    
    // Settings Use Cases
    factory { 
        GetDisplayPreferencesUseCase(
            settingsRepository = get()
        )
    }
    
    factory { 
        UpdateDisplayPreferencesUseCase(
            settingsRepository = get()
        )
    }
    
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