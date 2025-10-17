package com.eunio.healthapp.di

import com.eunio.healthapp.presentation.viewmodel.*
import com.eunio.healthapp.domain.manager.*
import com.eunio.healthapp.domain.usecase.auth.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Backward compatibility support for manual instantiation patterns.
 * 
 * This class provides deprecated factory methods that preserve existing manual instantiation
 * patterns while migrating to dependency injection. All methods are marked as deprecated
 * with clear migration paths to encourage adoption of DI patterns.
 * 
 * Note: Use dependency injection through Koin instead of manual instantiation.
 * See migration guide in the documentation.
 */
object BackwardCompatibilitySupport : KoinComponent {
    
    // MARK: - ViewModel Factory Methods
    
    @Deprecated("Use get<OnboardingViewModel>() from Koin instead")
    fun createOnboardingViewModel(): OnboardingViewModel {
        return BackwardCompatibilityImplementation.createOnboardingViewModelSafely()
    }
    
    @Deprecated("Use get<DailyLoggingViewModel>() from Koin instead")
    fun createDailyLoggingViewModel(): DailyLoggingViewModel {
        return BackwardCompatibilityImplementation.createDailyLoggingViewModelSafely()
    }
    
    @Deprecated("Use get<CalendarViewModel>() from Koin instead")
    fun createCalendarViewModel(): CalendarViewModel {
        return BackwardCompatibilityImplementation.createCalendarViewModelSafely()
    }
    
    @Deprecated("Use get<InsightsViewModel>() from Koin instead")
    fun createInsightsViewModel(): InsightsViewModel {
        return BackwardCompatibilityImplementation.createInsightsViewModelSafely()
    }
    
    @Deprecated("Use get<UnitSystemSettingsViewModel>() from Koin instead")
    fun createUnitSystemSettingsViewModel(): UnitSystemSettingsViewModel {
        return BackwardCompatibilityImplementation.createUnitSystemSettingsViewModelSafely()
    }
    
    // MARK: - Service Factory Methods
    
    @Deprecated("Use get<SettingsManager>() from Koin instead")
    fun createSettingsManager(): SettingsManager {
        return BackwardCompatibilityImplementation.createSettingsManagerSafely()
    }
    
    @Deprecated("Use get<NotificationManager>() from Koin instead")
    fun createNotificationManager(): NotificationManager {
        return BackwardCompatibilityImplementation.createNotificationManagerSafely()
    }
    
    @Deprecated("Use get<AuthManager>() from Koin instead")
    fun createAuthManager(): AuthManager {
        return BackwardCompatibilityImplementation.createAuthManagerSafely()
    }
    
    // MARK: - Use Case Factory Methods
    
    @Deprecated("Use get<SignInUseCase>() from Koin instead")
    fun createSignInUseCase(): SignInUseCase {
        return BackwardCompatibilityImplementation.createSignInUseCaseSafely()
    }
    
    @Deprecated("Use get<SignUpUseCase>() from Koin instead")
    fun createSignUpUseCase(): SignUpUseCase {
        return BackwardCompatibilityImplementation.createSignUpUseCaseSafely()
    }
    
    @Deprecated("Use get<SignOutUseCase>() from Koin instead")
    fun createSignOutUseCase(): SignOutUseCase {
        return BackwardCompatibilityImplementation.createSignOutUseCaseSafely()
    }
    
    @Deprecated("Use get<GetCurrentUserUseCase>() from Koin instead")
    fun createGetCurrentUserUseCase(): GetCurrentUserUseCase {
        return BackwardCompatibilityImplementation.createGetCurrentUserUseCaseSafely()
    }
    
    @Deprecated("Use get<SendPasswordResetUseCase>() from Koin instead")
    fun createSendPasswordResetUseCase(): SendPasswordResetUseCase {
        return BackwardCompatibilityImplementation.createSendPasswordResetUseCaseSafely()
    }
}