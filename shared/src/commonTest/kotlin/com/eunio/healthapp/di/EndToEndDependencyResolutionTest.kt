package com.eunio.healthapp.di

import com.eunio.healthapp.presentation.viewmodel.*
import com.eunio.healthapp.domain.usecase.auth.*
import com.eunio.healthapp.domain.usecase.cycle.*
import com.eunio.healthapp.domain.usecase.fertility.*
import com.eunio.healthapp.domain.usecase.logging.*
import com.eunio.healthapp.domain.usecase.profile.*
import com.eunio.healthapp.domain.usecase.reports.*
import com.eunio.healthapp.domain.usecase.settings.*
import com.eunio.healthapp.domain.usecase.support.*
import com.eunio.healthapp.domain.repository.*
import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.manager.NotificationManager
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.testutil.testModule
import com.eunio.healthapp.di.sharedModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*

/**
 * End-to-end dependency resolution tests.
 * 
 * This test suite verifies that complete dependency chains work correctly
 * from ViewModels down to the lowest level services and repositories.
 * 
 * Requirements covered: 6.1, 6.2, 6.4
 */
class EndToEndDependencyResolutionTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        // Set up test dispatcher for ViewModels
        Dispatchers.setMain(UnconfinedTestDispatcher())
        
        startKoin {
            modules(
                sharedModule,
                testModule
            )
        }
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }
    
    // ========================================
    // Authentication Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `authentication flow - complete dependency chain resolution`() {
        // Test the complete authentication flow dependency chain
        
        // 1. ViewModel level
        val onboardingViewModel = get<OnboardingViewModel>()
        assertNotNull(onboardingViewModel, "OnboardingViewModel should be resolvable")
        
        // 2. Use Case level - verify ViewModel dependencies
        val getCurrentUserUseCase = get<GetCurrentUserUseCase>()
        val completeOnboardingUseCase = get<CompleteOnboardingUseCase>()
        assertNotNull(getCurrentUserUseCase, "GetCurrentUserUseCase should be resolvable")
        assertNotNull(completeOnboardingUseCase, "CompleteOnboardingUseCase should be resolvable")
        
        // 3. Repository level - verify Use Case dependencies
        val userRepository = get<UserRepository>()
        assertNotNull(userRepository, "UserRepository should be resolvable")
        
        // 4. Verify the complete chain works together
        val viewModel = get<OnboardingViewModel>()
        val useCase1 = get<GetCurrentUserUseCase>()
        val useCase2 = get<CompleteOnboardingUseCase>()
        val repository = get<UserRepository>()
        
        assertNotNull(viewModel)
        assertNotNull(useCase1)
        assertNotNull(useCase2)
        assertNotNull(repository)
    }
    
    @Test
    fun `authentication use cases - complete dependency resolution`() {
        // Test all authentication-related use cases can be resolved with their dependencies
        val getCurrentUserUseCase = get<GetCurrentUserUseCase>()
        assertNotNull(getCurrentUserUseCase, "GetCurrentUserUseCase should resolve with all dependencies")
        
        val completeOnboardingUseCase = get<CompleteOnboardingUseCase>()
        assertNotNull(completeOnboardingUseCase, "CompleteOnboardingUseCase should resolve with all dependencies")
        
        val signUpUseCase = get<SignUpUseCase>()
        assertNotNull(signUpUseCase, "SignUpUseCase should resolve with all dependencies")
        
        val signInUseCase = get<SignInUseCase>()
        assertNotNull(signInUseCase, "SignInUseCase should resolve with all dependencies")
        
        val signOutUseCase = get<SignOutUseCase>()
        assertNotNull(signOutUseCase, "SignOutUseCase should resolve with all dependencies")
        
        val sendPasswordResetUseCase = get<SendPasswordResetUseCase>()
        assertNotNull(sendPasswordResetUseCase, "SendPasswordResetUseCase should resolve with all dependencies")
        
        // Verify their common dependency (UserRepository) is also resolvable
        val userRepository = get<UserRepository>()
        assertNotNull(userRepository, "UserRepository should be resolvable for auth use cases")
    }
    
    // ========================================
    // Daily Logging Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `daily logging flow - complete dependency chain resolution`() {
        // Test the complete daily logging flow dependency chain
        
        // 1. ViewModel level
        val dailyLoggingViewModel = get<DailyLoggingViewModel>()
        assertNotNull(dailyLoggingViewModel, "DailyLoggingViewModel should be resolvable")
        
        // 2. Use Case level
        val getDailyLogUseCase = get<GetDailyLogUseCase>()
        val saveDailyLogUseCase = get<SaveDailyLogUseCase>()
        val getLogHistoryUseCase = get<GetLogHistoryUseCase>()
        assertNotNull(getDailyLogUseCase, "GetDailyLogUseCase should be resolvable")
        assertNotNull(saveDailyLogUseCase, "SaveDailyLogUseCase should be resolvable")
        assertNotNull(getLogHistoryUseCase, "GetLogHistoryUseCase should be resolvable")
        
        // 3. Repository level
        val logRepository = get<LogRepository>()
        assertNotNull(logRepository, "LogRepository should be resolvable")
        
        // 4. Verify the complete chain works together
        val viewModel = get<DailyLoggingViewModel>()
        val useCase1 = get<GetDailyLogUseCase>()
        val useCase2 = get<SaveDailyLogUseCase>()
        val useCase3 = get<GetLogHistoryUseCase>()
        val repository = get<LogRepository>()
        
        assertNotNull(viewModel)
        assertNotNull(useCase1)
        assertNotNull(useCase2)
        assertNotNull(useCase3)
        assertNotNull(repository)
    }
    
    @Test
    fun `fertility tracking use cases - complete dependency resolution`() {
        // Test fertility tracking use cases and their dependencies
        val logBBTUseCase = get<LogBBTUseCase>()
        assertNotNull(logBBTUseCase, "LogBBTUseCase should resolve with all dependencies")
        
        val logCervicalMucusUseCase = get<LogCervicalMucusUseCase>()
        assertNotNull(logCervicalMucusUseCase, "LogCervicalMucusUseCase should resolve with all dependencies")
        
        val logOPKResultUseCase = get<LogOPKResultUseCase>()
        assertNotNull(logOPKResultUseCase, "LogOPKResultUseCase should resolve with all dependencies")
        
        val confirmOvulationUseCase = get<ConfirmOvulationUseCase>()
        assertNotNull(confirmOvulationUseCase, "ConfirmOvulationUseCase should resolve with all dependencies")
        
        val calculateFertilityWindowUseCase = get<CalculateFertilityWindowUseCase>()
        assertNotNull(calculateFertilityWindowUseCase, "CalculateFertilityWindowUseCase should resolve with all dependencies")
        
        // Verify their dependencies are resolvable
        val logRepository = get<LogRepository>()
        val cycleRepository = get<CycleRepository>()
        assertNotNull(logRepository, "LogRepository should be resolvable for fertility use cases")
        assertNotNull(cycleRepository, "CycleRepository should be resolvable for fertility use cases")
    }
    
    // ========================================
    // Cycle Tracking Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `cycle tracking flow - complete dependency chain resolution`() {
        // Test the complete cycle tracking flow dependency chain
        
        // 1. ViewModel level
        val calendarViewModel = get<CalendarViewModel>()
        assertNotNull(calendarViewModel, "CalendarViewModel should be resolvable")
        
        // 2. Use Case level
        val getCurrentCycleUseCase = get<GetCurrentCycleUseCase>()
        val predictOvulationUseCase = get<PredictOvulationUseCase>()
        val getLogHistoryUseCase = get<GetLogHistoryUseCase>()
        assertNotNull(getCurrentCycleUseCase, "GetCurrentCycleUseCase should be resolvable")
        assertNotNull(predictOvulationUseCase, "PredictOvulationUseCase should be resolvable")
        assertNotNull(getLogHistoryUseCase, "GetLogHistoryUseCase should be resolvable")
        
        // 3. Repository level
        val cycleRepository = get<CycleRepository>()
        val logRepository = get<LogRepository>()
        assertNotNull(cycleRepository, "CycleRepository should be resolvable")
        assertNotNull(logRepository, "LogRepository should be resolvable")
        
        // 4. Verify the complete chain works together
        val viewModel = get<CalendarViewModel>()
        val useCase1 = get<GetCurrentCycleUseCase>()
        val useCase2 = get<PredictOvulationUseCase>()
        val useCase3 = get<GetLogHistoryUseCase>()
        val repository1 = get<CycleRepository>()
        val repository2 = get<LogRepository>()
        
        assertNotNull(viewModel)
        assertNotNull(useCase1)
        assertNotNull(useCase2)
        assertNotNull(useCase3)
        assertNotNull(repository1)
        assertNotNull(repository2)
    }
    
    @Test
    fun `cycle management use cases - complete dependency resolution`() {
        // Test cycle management use cases and their dependencies
        val getCurrentCycleUseCase = get<GetCurrentCycleUseCase>()
        assertNotNull(getCurrentCycleUseCase, "GetCurrentCycleUseCase should resolve with all dependencies")
        
        val startNewCycleUseCase = get<StartNewCycleUseCase>()
        assertNotNull(startNewCycleUseCase, "StartNewCycleUseCase should resolve with all dependencies")
        
        val updateCycleUseCase = get<UpdateCycleUseCase>()
        assertNotNull(updateCycleUseCase, "UpdateCycleUseCase should resolve with all dependencies")
        
        val predictOvulationUseCase = get<PredictOvulationUseCase>()
        assertNotNull(predictOvulationUseCase, "PredictOvulationUseCase should resolve with all dependencies")
        
        // Verify their dependencies are resolvable
        val cycleRepository = get<CycleRepository>()
        val logRepository = get<LogRepository>()
        assertNotNull(cycleRepository, "CycleRepository should be resolvable for cycle use cases")
        assertNotNull(logRepository, "LogRepository should be resolvable for cycle use cases")
    }
    
    // ========================================
    // Settings Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `settings flow - complete dependency chain resolution`() {
        // Test the complete settings flow dependency chain
        
        // 1. ViewModel level - Settings ViewModels
        val settingsViewModel = get<SettingsViewModel>()
        val enhancedSettingsViewModel = get<EnhancedSettingsViewModel>()
        assertNotNull(settingsViewModel, "SettingsViewModel should be resolvable")
        assertNotNull(enhancedSettingsViewModel, "EnhancedSettingsViewModel should be resolvable")
        
        // 2. ViewModel level - Preferences ViewModels
        val displayPreferencesViewModel = get<DisplayPreferencesViewModel>()
        val notificationPreferencesViewModel = get<NotificationPreferencesViewModel>()
        val privacyPreferencesViewModel = get<PrivacyPreferencesViewModel>()
        val syncPreferencesViewModel = get<SyncPreferencesViewModel>()
        assertNotNull(displayPreferencesViewModel, "DisplayPreferencesViewModel should be resolvable")
        assertNotNull(notificationPreferencesViewModel, "NotificationPreferencesViewModel should be resolvable")
        assertNotNull(privacyPreferencesViewModel, "PrivacyPreferencesViewModel should be resolvable")
        assertNotNull(syncPreferencesViewModel, "SyncPreferencesViewModel should be resolvable")
        
        // 3. Use Case level
        val getDisplayPreferencesUseCase = get<GetDisplayPreferencesUseCase>()
        val updateDisplayPreferencesUseCase = get<UpdateDisplayPreferencesUseCase>()
        assertNotNull(getDisplayPreferencesUseCase, "GetDisplayPreferencesUseCase should be resolvable")
        assertNotNull(updateDisplayPreferencesUseCase, "UpdateDisplayPreferencesUseCase should be resolvable")
        
        // 4. Repository level
        val settingsRepository = get<SettingsRepository>()
        val preferencesRepository = get<PreferencesRepository>()
        assertNotNull(settingsRepository, "SettingsRepository should be resolvable")
        assertNotNull(preferencesRepository, "PreferencesRepository should be resolvable")
        
        // 5. Service level
        val settingsManager = get<SettingsManager>()
        val notificationManager = get<NotificationManager>()
        assertNotNull(settingsManager, "SettingsManager should be resolvable")
        assertNotNull(notificationManager, "NotificationManager should be resolvable")
    }
    
    @Test
    fun `specialized settings flow - complete dependency chain resolution`() {
        // Test specialized settings ViewModels and their dependencies
        
        // 1. ViewModel level
        val cyclePreferencesViewModel = get<CyclePreferencesViewModel>()
        val unitPreferencesViewModel = get<UnitPreferencesViewModel>()
        val unitSystemSettingsViewModel = get<UnitSystemSettingsViewModel>()
        assertNotNull(cyclePreferencesViewModel, "CyclePreferencesViewModel should be resolvable")
        assertNotNull(unitPreferencesViewModel, "UnitPreferencesViewModel should be resolvable")
        assertNotNull(unitSystemSettingsViewModel, "UnitSystemSettingsViewModel should be resolvable")
        
        // 2. Service level dependencies
        val settingsManager = get<SettingsManager>()
        val unitConverter = get<UnitConverter>()
        val unitSystemManager = get<UnitSystemManager>()
        assertNotNull(settingsManager, "SettingsManager should be resolvable")
        assertNotNull(unitConverter, "UnitConverter should be resolvable")
        assertNotNull(unitSystemManager, "UnitSystemManager should be resolvable")
        
        // 3. Verify the complete chain works together
        val vm1 = get<CyclePreferencesViewModel>()
        val vm2 = get<UnitPreferencesViewModel>()
        val vm3 = get<UnitSystemSettingsViewModel>()
        val sm = get<SettingsManager>()
        val uc = get<UnitConverter>()
        val usm = get<UnitSystemManager>()
        
        assertNotNull(vm1)
        assertNotNull(vm2)
        assertNotNull(vm3)
        assertNotNull(sm)
        assertNotNull(uc)
        assertNotNull(usm)
    }
    
    // ========================================
    // Help and Support Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `help and support flow - complete dependency chain resolution`() {
        // Test the complete help and support flow dependency chain
        
        // 1. ViewModel level
        val helpSupportViewModel = get<HelpSupportViewModel>()
        val supportRequestViewModel = get<SupportRequestViewModel>()
        val bugReportViewModel = get<BugReportViewModel>()
        assertNotNull(helpSupportViewModel, "HelpSupportViewModel should be resolvable")
        assertNotNull(supportRequestViewModel, "SupportRequestViewModel should be resolvable")
        assertNotNull(bugReportViewModel, "BugReportViewModel should be resolvable")
        
        // 2. Use Case level
        val getHelpCategoriesUseCase = get<GetHelpCategoriesUseCase>()
        val searchFAQsUseCase = get<SearchFAQsUseCase>()
        val submitSupportRequestUseCase = get<SubmitSupportRequestUseCase>()
        val getTutorialsUseCase = get<GetTutorialsUseCase>()
        assertNotNull(getHelpCategoriesUseCase, "GetHelpCategoriesUseCase should be resolvable")
        assertNotNull(searchFAQsUseCase, "SearchFAQsUseCase should be resolvable")
        assertNotNull(submitSupportRequestUseCase, "SubmitSupportRequestUseCase should be resolvable")
        assertNotNull(getTutorialsUseCase, "GetTutorialsUseCase should be resolvable")
        
        // 3. Repository level
        val helpSupportRepository = get<HelpSupportRepository>()
        assertNotNull(helpSupportRepository, "HelpSupportRepository should be resolvable")
        
        // 4. Verify the complete chain works together
        val vm1 = get<HelpSupportViewModel>()
        val vm2 = get<SupportRequestViewModel>()
        val vm3 = get<BugReportViewModel>()
        val uc1 = get<GetHelpCategoriesUseCase>()
        val uc2 = get<SearchFAQsUseCase>()
        val uc3 = get<SubmitSupportRequestUseCase>()
        val uc4 = get<GetTutorialsUseCase>()
        val repo = get<HelpSupportRepository>()
        
        assertNotNull(vm1)
        assertNotNull(vm2)
        assertNotNull(vm3)
        assertNotNull(uc1)
        assertNotNull(uc2)
        assertNotNull(uc3)
        assertNotNull(uc4)
        assertNotNull(repo)
    }
    
    // ========================================
    // Profile Management Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `profile management flow - complete dependency chain resolution`() {
        // Test the complete profile management flow dependency chain
        
        // 1. ViewModel level
        val profileManagementViewModel = get<ProfileManagementViewModel>()
        assertNotNull(profileManagementViewModel, "ProfileManagementViewModel should be resolvable")
        
        // 2. Use Case level
        val getCurrentUserUseCase = get<GetCurrentUserUseCase>()
        val updateUserProfileUseCase = get<UpdateUserProfileUseCase>()
        val updateHealthGoalUseCase = get<UpdateHealthGoalUseCase>()
        val getUserStatisticsUseCase = get<GetUserStatisticsUseCase>()
        assertNotNull(getCurrentUserUseCase, "GetCurrentUserUseCase should be resolvable")
        assertNotNull(updateUserProfileUseCase, "UpdateUserProfileUseCase should be resolvable")
        assertNotNull(updateHealthGoalUseCase, "UpdateHealthGoalUseCase should be resolvable")
        assertNotNull(getUserStatisticsUseCase, "GetUserStatisticsUseCase should be resolvable")
        
        // 3. Repository level
        val userRepository = get<UserRepository>()
        assertNotNull(userRepository, "UserRepository should be resolvable")
        
        // 4. Verify the complete chain works together
        val vm = get<ProfileManagementViewModel>()
        val uc1 = get<GetCurrentUserUseCase>()
        val uc2 = get<UpdateUserProfileUseCase>()
        val uc3 = get<UpdateHealthGoalUseCase>()
        val uc4 = get<GetUserStatisticsUseCase>()
        val repo = get<UserRepository>()
        
        assertNotNull(vm)
        assertNotNull(uc1)
        assertNotNull(uc2)
        assertNotNull(uc3)
        assertNotNull(uc4)
        assertNotNull(repo)
    }
    
    // ========================================
    // Insights Flow End-to-End Tests
    // ========================================
    
    @Test
    fun `insights flow - complete dependency chain resolution`() {
        // Test the complete insights flow dependency chain
        
        // 1. ViewModel level
        val insightsViewModel = get<InsightsViewModel>()
        assertNotNull(insightsViewModel, "InsightsViewModel should be resolvable")
        
        // 2. Repository level (InsightsViewModel directly uses repository)
        val insightRepository = get<InsightRepository>()
        assertNotNull(insightRepository, "InsightRepository should be resolvable")
        
        // 3. Verify the complete chain works together
        val vm = get<InsightsViewModel>()
        val repo = get<InsightRepository>()
        
        assertNotNull(vm)
        assertNotNull(repo)
    }
    
    // ========================================
    // Cross-Flow Integration Tests
    // ========================================
    
    @Test
    fun `cross-flow integration - shared dependencies resolution`() {
        // Test that shared dependencies work correctly across different flows
        
        // UserRepository is used by multiple flows
        val userRepository1 = get<UserRepository>()
        val userRepository2 = get<UserRepository>()
        assertTrue(userRepository1 === userRepository2, "UserRepository should be a singleton")
        
        // LogRepository is used by multiple flows
        val logRepository1 = get<LogRepository>()
        val logRepository2 = get<LogRepository>()
        assertTrue(logRepository1 === logRepository2, "LogRepository should be a singleton")
        
        // SettingsManager is used by multiple ViewModels
        val settingsManager1 = get<SettingsManager>()
        val settingsManager2 = get<SettingsManager>()
        assertTrue(settingsManager1 === settingsManager2, "SettingsManager should be a singleton")
        
        // Verify that ViewModels using shared dependencies can all be instantiated
        val onboardingVM = get<OnboardingViewModel>() // uses UserRepository
        val profileVM = get<ProfileManagementViewModel>() // uses UserRepository
        val dailyLoggingVM = get<DailyLoggingViewModel>() // uses LogRepository
        val calendarVM = get<CalendarViewModel>() // uses LogRepository
        val settingsVM = get<SettingsViewModel>() // uses SettingsManager
        val cyclePrefsVM = get<CyclePreferencesViewModel>() // uses SettingsManager
        
        assertNotNull(onboardingVM)
        assertNotNull(profileVM)
        assertNotNull(dailyLoggingVM)
        assertNotNull(calendarVM)
        assertNotNull(settingsVM)
        assertNotNull(cyclePrefsVM)
    }
    
    @Test
    fun `complete application flow - all components working together`() {
        // This is the ultimate integration test - verify that all major application flows
        // can work together with their complete dependency chains
        
        val allFlowComponents = mutableMapOf<String, Any>()
        val failures = mutableListOf<String>()
        
        // Authentication flow
        try { allFlowComponents["OnboardingViewModel"] = get<OnboardingViewModel>() }
        catch (e: Exception) { failures.add("OnboardingViewModel: ${e.message}") }
        
        try { allFlowComponents["GetCurrentUserUseCase"] = get<GetCurrentUserUseCase>() }
        catch (e: Exception) { failures.add("GetCurrentUserUseCase: ${e.message}") }
        
        try { allFlowComponents["UserRepository"] = get<UserRepository>() }
        catch (e: Exception) { failures.add("UserRepository: ${e.message}") }
        
        // Daily logging flow
        try { allFlowComponents["DailyLoggingViewModel"] = get<DailyLoggingViewModel>() }
        catch (e: Exception) { failures.add("DailyLoggingViewModel: ${e.message}") }
        
        try { allFlowComponents["GetDailyLogUseCase"] = get<GetDailyLogUseCase>() }
        catch (e: Exception) { failures.add("GetDailyLogUseCase: ${e.message}") }
        
        try { allFlowComponents["LogRepository"] = get<LogRepository>() }
        catch (e: Exception) { failures.add("LogRepository: ${e.message}") }
        
        // Cycle tracking flow
        try { allFlowComponents["CalendarViewModel"] = get<CalendarViewModel>() }
        catch (e: Exception) { failures.add("CalendarViewModel: ${e.message}") }
        
        try { allFlowComponents["GetCurrentCycleUseCase"] = get<GetCurrentCycleUseCase>() }
        catch (e: Exception) { failures.add("GetCurrentCycleUseCase: ${e.message}") }
        
        try { allFlowComponents["CycleRepository"] = get<CycleRepository>() }
        catch (e: Exception) { failures.add("CycleRepository: ${e.message}") }
        
        // Settings flow
        try { allFlowComponents["SettingsViewModel"] = get<SettingsViewModel>() }
        catch (e: Exception) { failures.add("SettingsViewModel: ${e.message}") }
        
        try { allFlowComponents["DisplayPreferencesViewModel"] = get<DisplayPreferencesViewModel>() }
        catch (e: Exception) { failures.add("DisplayPreferencesViewModel: ${e.message}") }
        
        try { allFlowComponents["SettingsRepository"] = get<SettingsRepository>() }
        catch (e: Exception) { failures.add("SettingsRepository: ${e.message}") }
        
        try { allFlowComponents["SettingsManager"] = get<SettingsManager>() }
        catch (e: Exception) { failures.add("SettingsManager: ${e.message}") }
        
        // Help and support flow
        try { allFlowComponents["HelpSupportViewModel"] = get<HelpSupportViewModel>() }
        catch (e: Exception) { failures.add("HelpSupportViewModel: ${e.message}") }
        
        try { allFlowComponents["HelpSupportRepository"] = get<HelpSupportRepository>() }
        catch (e: Exception) { failures.add("HelpSupportRepository: ${e.message}") }
        
        // Profile management flow
        try { allFlowComponents["ProfileManagementViewModel"] = get<ProfileManagementViewModel>() }
        catch (e: Exception) { failures.add("ProfileManagementViewModel: ${e.message}") }
        
        // Insights flow
        try { allFlowComponents["InsightsViewModel"] = get<InsightsViewModel>() }
        catch (e: Exception) { failures.add("InsightsViewModel: ${e.message}") }
        
        try { allFlowComponents["InsightRepository"] = get<InsightRepository>() }
        catch (e: Exception) { failures.add("InsightRepository: ${e.message}") }
        
        // Report results
        if (failures.isNotEmpty()) {
            fail("Failed to resolve ${failures.size} components in complete application flow:\n${failures.joinToString("\n")}")
        }
        
        assertTrue(allFlowComponents.size >= 16, 
            "Should resolve at least 16 flow components, got ${allFlowComponents.size}")
        
        // Verify all components are not null and properly typed
        allFlowComponents.forEach { (name, component) ->
            assertNotNull(component, "$name should not be null")
        }
        
        println("✅ Complete application flow test passed with ${allFlowComponents.size} components resolved successfully")
    }
}