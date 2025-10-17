package com.eunio.healthapp.di

import com.eunio.healthapp.presentation.viewmodel.CalendarViewModel
import com.eunio.healthapp.presentation.viewmodel.DailyLoggingViewModel
import com.eunio.healthapp.presentation.viewmodel.InsightsViewModel
import com.eunio.healthapp.presentation.viewmodel.OnboardingViewModel
import com.eunio.healthapp.presentation.viewmodel.UnitSystemSettingsViewModel
import com.eunio.healthapp.domain.usecase.auth.SignInUseCase
import com.eunio.healthapp.domain.usecase.auth.SignUpUseCase
import com.eunio.healthapp.domain.usecase.auth.SignOutUseCase
import com.eunio.healthapp.domain.usecase.auth.GetCurrentUserUseCase
import com.eunio.healthapp.domain.usecase.auth.SendPasswordResetUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.component.get

/**
 * Helper class to provide easy access to Koin dependencies from iOS Swift code.
 * This class acts as a bridge between Swift and Kotlin Multiplatform's Koin DI.
 * 
 * @deprecated This helper class is deprecated in favor of direct dependency injection.
 * Use SwiftUI environment-based dependency injection or service protocols instead.
 * See ModernViewModelWrapper.swift for the recommended approach.
 */
@Deprecated(
    message = "IOSKoinHelper is deprecated. Use SwiftUI environment-based dependency injection instead.",
    replaceWith = ReplaceWith("Use ViewModelFactory from SwiftUI environment"),
    level = DeprecationLevel.WARNING
)
class IOSKoinHelper : KoinComponent {
    
    // ViewModels - using lazy injection for performance
    private val onboardingViewModel: OnboardingViewModel by inject()
    private val dailyLoggingViewModel: DailyLoggingViewModel by inject()
    private val calendarViewModel: CalendarViewModel by inject()
    private val insightsViewModel: InsightsViewModel by inject()
    private val unitSystemSettingsViewModel: UnitSystemSettingsViewModel by inject()
    
    // Auth Use Cases - using lazy injection for performance
    private val signInUseCase: SignInUseCase by inject()
    private val signUpUseCase: SignUpUseCase by inject()
    private val signOutUseCase: SignOutUseCase by inject()
    private val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    private val sendPasswordResetUseCase: SendPasswordResetUseCase by inject()
    
    // Public methods to access ViewModels from Swift
    /**
     * @deprecated Use SwiftUI environment-based ViewModelFactory instead.
     * Migration: Replace IOSKoinHelper.shared.getOnboardingViewModel() with environment.viewModelFactory.createOnboardingViewModel()
     */
    @Deprecated(
        message = "Use SwiftUI environment-based ViewModelFactory instead",
        level = DeprecationLevel.WARNING
    )
    fun getOnboardingViewModel(): OnboardingViewModel = try {
        onboardingViewModel
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createOnboardingViewModel()
    }
    
    /**
     * @deprecated Use SwiftUI environment-based ViewModelFactory instead.
     * Migration: Replace IOSKoinHelper.shared.getDailyLoggingViewModel() with environment.viewModelFactory.createDailyLoggingViewModel()
     */
    @Deprecated(
        message = "Use SwiftUI environment-based ViewModelFactory instead",
        level = DeprecationLevel.WARNING
    )
    fun getDailyLoggingViewModel(): DailyLoggingViewModel = try {
        dailyLoggingViewModel
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createDailyLoggingViewModel()
    }
    
    /**
     * @deprecated Use SwiftUI environment-based ViewModelFactory instead.
     * Migration: Replace IOSKoinHelper.shared.getCalendarViewModel() with environment.viewModelFactory.createCalendarViewModel()
     */
    @Deprecated(
        message = "Use SwiftUI environment-based ViewModelFactory instead",
        level = DeprecationLevel.WARNING
    )
    fun getCalendarViewModel(): CalendarViewModel = try {
        calendarViewModel
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createCalendarViewModel()
    }
    
    /**
     * @deprecated Use SwiftUI environment-based ViewModelFactory instead.
     * Migration: Replace IOSKoinHelper.shared.getInsightsViewModel() with environment.viewModelFactory.createInsightsViewModel()
     */
    @Deprecated(
        message = "Use SwiftUI environment-based ViewModelFactory instead",
        level = DeprecationLevel.WARNING
    )
    fun getInsightsViewModel(): InsightsViewModel = try {
        insightsViewModel
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createInsightsViewModel()
    }
    
    /**
     * @deprecated Use SwiftUI environment-based ViewModelFactory instead.
     * Migration: Replace IOSKoinHelper.shared.getUnitSystemSettingsViewModel() with environment.viewModelFactory.createUnitSystemSettingsViewModel()
     */
    @Deprecated(
        message = "Use SwiftUI environment-based ViewModelFactory instead",
        level = DeprecationLevel.WARNING
    )
    fun getUnitSystemSettingsViewModel(): UnitSystemSettingsViewModel = try {
        unitSystemSettingsViewModel
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createUnitSystemSettingsViewModel()
    }
    
    // Public methods to access Auth Use Cases from Swift
    /**
     * @deprecated Use dependency injection through Koin instead.
     * Migration: Replace IOSKoinHelper.shared.getSignInUseCase() with get<SignInUseCase>() in your Koin component
     */
    @Deprecated(
        message = "Use dependency injection through Koin instead",
        level = DeprecationLevel.WARNING
    )
    fun getSignInUseCase(): SignInUseCase = try {
        signInUseCase
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createSignInUseCase()
    }
    
    /**
     * @deprecated Use dependency injection through Koin instead.
     * Migration: Replace IOSKoinHelper.shared.getSignUpUseCase() with get<SignUpUseCase>() in your Koin component
     */
    @Deprecated(
        message = "Use dependency injection through Koin instead",
        level = DeprecationLevel.WARNING
    )
    fun getSignUpUseCase(): SignUpUseCase = try {
        signUpUseCase
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createSignUpUseCase()
    }
    
    /**
     * @deprecated Use dependency injection through Koin instead.
     * Migration: Replace IOSKoinHelper.shared.getSignOutUseCase() with get<SignOutUseCase>() in your Koin component
     */
    @Deprecated(
        message = "Use dependency injection through Koin instead",
        level = DeprecationLevel.WARNING
    )
    fun getSignOutUseCase(): SignOutUseCase = try {
        signOutUseCase
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createSignOutUseCase()
    }
    
    /**
     * @deprecated Use dependency injection through Koin instead.
     * Migration: Replace IOSKoinHelper.shared.getCurrentUserUseCase() with get<GetCurrentUserUseCase>() in your Koin component
     */
    @Deprecated(
        message = "Use dependency injection through Koin instead",
        level = DeprecationLevel.WARNING
    )
    fun getCurrentUserUseCase(): GetCurrentUserUseCase = try {
        getCurrentUserUseCase
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createGetCurrentUserUseCase()
    }
    
    /**
     * @deprecated Use dependency injection through Koin instead.
     * Migration: Replace IOSKoinHelper.shared.getSendPasswordResetUseCase() with get<SendPasswordResetUseCase>() in your Koin component
     */
    @Deprecated(
        message = "Use dependency injection through Koin instead",
        level = DeprecationLevel.WARNING
    )
    fun getSendPasswordResetUseCase(): SendPasswordResetUseCase = try {
        sendPasswordResetUseCase
    } catch (e: Exception) {
        BackwardCompatibilitySupport.createSendPasswordResetUseCase()
    }
    
    companion object {
        /**
         * Singleton instance for easy access from Swift
         */
        val shared = IOSKoinHelper()
    }
}

/**
 * Modern Koin accessor for iOS.
 * Provides direct access to Koin dependencies without deprecation warnings.
 */
object KoinAccessor : KoinComponent {
    fun getOnboardingViewModel(): OnboardingViewModel = get()
    fun getDailyLoggingViewModel(): DailyLoggingViewModel = get()
    fun getCalendarViewModel(): CalendarViewModel = get()
    fun getInsightsViewModel(): InsightsViewModel = get()
    fun getUnitSystemSettingsViewModel(): UnitSystemSettingsViewModel = get()
}

// Top-level functions for easy Swift access
fun getOnboardingViewModel(): OnboardingViewModel = KoinAccessor.getOnboardingViewModel()
fun getDailyLoggingViewModel(): DailyLoggingViewModel = KoinAccessor.getDailyLoggingViewModel()
fun getCalendarViewModel(): CalendarViewModel = KoinAccessor.getCalendarViewModel()
fun getInsightsViewModel(): InsightsViewModel = KoinAccessor.getInsightsViewModel()
fun getUnitSystemSettingsViewModel(): UnitSystemSettingsViewModel = KoinAccessor.getUnitSystemSettingsViewModel()