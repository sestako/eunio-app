package com.eunio.healthapp.di

import com.eunio.healthapp.presentation.viewmodel.*
import org.koin.dsl.module

/**
 * Koin dependency injection module for ViewModels.
 * Provides factory instances of all ViewModels with their required dependencies.
 */
val viewModelModule = module {
    
    // Onboarding ViewModel
    factory { 
        OnboardingViewModel(
            getCurrentUserUseCase = get(),
            completeOnboardingUseCase = get()
        )
    }
    
    // Daily Logging ViewModel
    factory { 
        DailyLoggingViewModel(
            getDailyLogUseCase = get(),
            saveDailyLogUseCase = get(),
            authManager = get()
        )
    }
    
    // Calendar ViewModel
    factory { 
        CalendarViewModel(
            getCurrentCycleUseCase = get(),
            predictOvulationUseCase = get(),
            getLogHistoryUseCase = get()
        )
    }
    
    // Insights ViewModel
    factory { 
        InsightsViewModel(
            insightRepository = get()
        )
    }
    
    // Help and Support ViewModels
    factory { 
        HelpSupportViewModel(
            getHelpCategoriesUseCase = get(),
            searchFAQsUseCase = get(),
            getTutorialsUseCase = get()
        )
    }
    
    factory { 
        SupportRequestViewModel(
            submitSupportRequestUseCase = get(),
            helpSupportRepository = get()
        )
    }
    
    factory { 
        BugReportViewModel(
            submitSupportRequestUseCase = get(),
            helpSupportRepository = get()
        )
    }
    
    // Profile Management ViewModel
    factory { 
        ProfileManagementViewModel(
            getCurrentUserUseCase = get(),
            updateUserProfileUseCase = get(),
            updateHealthGoalUseCase = get(),
            getUserStatisticsUseCase = get()
        )
    }
    
    // Settings ViewModels
    factory { 
        SettingsViewModel(
            settingsManager = get()
        )
    }
    
    factory { 
        EnhancedSettingsViewModel(
            settingsManager = get()
        )
    }
    
    // Preferences ViewModels
    factory { 
        DisplayPreferencesViewModel(
            getDisplayPreferencesUseCase = get(),
            updateDisplayPreferencesUseCase = get(),
            hapticFeedbackManager = get(),
            accessibilityManager = get()
        )
    }
    
    factory { 
        NotificationPreferencesViewModel(
            settingsManager = get(),
            notificationManager = get()
        )
    }
    
    factory { 
        PrivacyPreferencesViewModel(
            settingsManager = get(),
            settingsRepository = get()
        )
    }
    
    factory { 
        SyncPreferencesViewModel(
            settingsManager = get(),
            settingsRepository = get()
        )
    }
    
    // Specialized ViewModels
    factory { 
        CyclePreferencesViewModel(
            settingsManager = get()
        )
    }
    
    factory { 
        UnitPreferencesViewModel(
            settingsManager = get(),
            unitConverter = get()
        )
    }
    
    factory { 
        UnitSystemSettingsViewModel(
            unitSystemManager = get()
        )
    }
}