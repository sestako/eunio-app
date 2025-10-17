package com.eunio.healthapp.di

import com.eunio.healthapp.presentation.viewmodel.*
import com.eunio.healthapp.domain.manager.*
import com.eunio.healthapp.domain.usecase.auth.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Simple backward compatibility implementation that preserves existing manual instantiation patterns
 * while providing clear migration guidance.
 * 
 * This implementation maintains interface compatibility but throws helpful errors when DI is not available,
 * encouraging migration to proper dependency injection.
 */
object BackwardCompatibilityImplementation : KoinComponent {
    
    // MARK: - ViewModel Creation Methods
    
    fun createOnboardingViewModelSafely(): OnboardingViewModel {
        return try {
            get<OnboardingViewModel>()
        } catch (e: Exception) {
            KoinLogger.logInfo("OnboardingViewModel creation failed: ${e.message}")
            // Re-throw the original exception to see what's actually failing
            throw e
        }
    }
    
    fun createDailyLoggingViewModelSafely(): DailyLoggingViewModel {
        return try {
            get<DailyLoggingViewModel>()
        } catch (e: Exception) {
            KoinLogger.logInfo("DailyLoggingViewModel creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "DailyLoggingViewModel requires dependency injection. " +
                "Please ensure Koin is initialized before creating ViewModels. " +
                "Migration guide: Use get<DailyLoggingViewModel>() in a KoinComponent."
            )
        }
    }
    
    fun createCalendarViewModelSafely(): CalendarViewModel {
        return try {
            get<CalendarViewModel>()
        } catch (e: Exception) {
            KoinLogger.logInfo("CalendarViewModel creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "CalendarViewModel requires dependency injection. " +
                "Please ensure Koin is initialized before creating ViewModels. " +
                "Migration guide: Use get<CalendarViewModel>() in a KoinComponent."
            )
        }
    }
    
    fun createInsightsViewModelSafely(): InsightsViewModel {
        return try {
            get<InsightsViewModel>()
        } catch (e: Exception) {
            KoinLogger.logInfo("InsightsViewModel creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "InsightsViewModel requires dependency injection. " +
                "Please ensure Koin is initialized before creating ViewModels. " +
                "Migration guide: Use get<InsightsViewModel>() in a KoinComponent."
            )
        }
    }
    
    fun createUnitSystemSettingsViewModelSafely(): UnitSystemSettingsViewModel {
        return try {
            get<UnitSystemSettingsViewModel>()
        } catch (e: Exception) {
            KoinLogger.logInfo("UnitSystemSettingsViewModel creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "UnitSystemSettingsViewModel requires dependency injection. " +
                "Please ensure Koin is initialized before creating ViewModels. " +
                "Migration guide: Use get<UnitSystemSettingsViewModel>() in a KoinComponent."
            )
        }
    }
    
    // MARK: - Service Creation Methods
    
    fun createSettingsManagerSafely(): SettingsManager {
        return try {
            get<SettingsManager>()
        } catch (e: Exception) {
            KoinLogger.logInfo("SettingsManager creation failed - falling back to in-memory implementation: ${e.message}")
            return FallbackServiceFactory.createFallbackSettingsManager()
        }
    }
    
    fun createNotificationManagerSafely(): NotificationManager {
        return try {
            get<NotificationManager>()
        } catch (e: Exception) {
            KoinLogger.logInfo("NotificationManager creation failed - falling back to logging implementation: ${e.message}")
            return FallbackServiceFactory.createFallbackNotificationManager()
        }
    }
    
    fun createAuthManagerSafely(): AuthManager {
        return try {
            get<AuthManager>()
        } catch (e: Exception) {
            KoinLogger.logInfo("AuthManager creation failed - falling back to mock implementation: ${e.message}")
            return FallbackServiceFactory.createFallbackAuthManager()
        }
    }
    
    // MARK: - Use Case Creation Methods
    
    fun createSignInUseCaseSafely(): SignInUseCase {
        return try {
            get<SignInUseCase>()
        } catch (e: Exception) {
            KoinLogger.logInfo("SignInUseCase creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "SignInUseCase requires dependency injection. " +
                "Please ensure Koin is initialized before creating use cases. " +
                "Migration guide: Use get<SignInUseCase>() in a KoinComponent."
            )
        }
    }
    
    fun createSignUpUseCaseSafely(): SignUpUseCase {
        return try {
            get<SignUpUseCase>()
        } catch (e: Exception) {
            KoinLogger.logInfo("SignUpUseCase creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "SignUpUseCase requires dependency injection. " +
                "Please ensure Koin is initialized before creating use cases. " +
                "Migration guide: Use get<SignUpUseCase>() in a KoinComponent."
            )
        }
    }
    
    fun createSignOutUseCaseSafely(): SignOutUseCase {
        return try {
            get<SignOutUseCase>()
        } catch (e: Exception) {
            KoinLogger.logInfo("SignOutUseCase creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "SignOutUseCase requires dependency injection. " +
                "Please ensure Koin is initialized before creating use cases. " +
                "Migration guide: Use get<SignOutUseCase>() in a KoinComponent."
            )
        }
    }
    
    fun createGetCurrentUserUseCaseSafely(): GetCurrentUserUseCase {
        return try {
            get<GetCurrentUserUseCase>()
        } catch (e: Exception) {
            KoinLogger.logInfo("GetCurrentUserUseCase creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "GetCurrentUserUseCase requires dependency injection. " +
                "Please ensure Koin is initialized before creating use cases. " +
                "Migration guide: Use get<GetCurrentUserUseCase>() in a KoinComponent."
            )
        }
    }
    
    fun createSendPasswordResetUseCaseSafely(): SendPasswordResetUseCase {
        return try {
            get<SendPasswordResetUseCase>()
        } catch (e: Exception) {
            KoinLogger.logInfo("SendPasswordResetUseCase creation failed - Koin not initialized: ${e.message}")
            throw NotImplementedError(
                "SendPasswordResetUseCase requires dependency injection. " +
                "Please ensure Koin is initialized before creating use cases. " +
                "Migration guide: Use get<SendPasswordResetUseCase>() in a KoinComponent."
            )
        }
    }
}