package com.eunio.healthapp.e2e

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
import com.eunio.healthapp.domain.manager.*
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.presentation.state.*
import com.eunio.healthapp.testutil.testModule
import com.eunio.healthapp.testutil.AsyncConditionWaiter
import com.eunio.healthapp.testutil.BaseKoinTest
import com.eunio.healthapp.di.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.*

/**
 * Comprehensive test suite for validating complete ViewModel to business logic connectivity.
 * 
 * This test validates:
 * - All ViewModels can access required Use Cases through DI
 * - State management works with real business logic operations
 * - UI component access to ViewModels through dependency injection
 * - Reactive state updates work end-to-end
 * 
 * Requirements covered: 2.2, 2.3, 4.4
 */
class ViewModelBusinessLogicConnectivityTest : KoinTest {
    
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val asyncWaiter = AsyncConditionWaiter(testDispatcher, testScope)
    
    @BeforeTest
    fun setup() {
        // Set up test dispatcher for ViewModels
        Dispatchers.setMain(testDispatcher)
        
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
    // Test 1: ViewModel DI Access Validation
    // ========================================
    
    @Test
    fun `all ViewModels can access required Use Cases through DI`() = runTest {
        val viewModelValidations = mutableMapOf<String, Boolean>()
        val failures = mutableListOf<String>()
        
        // Test OnboardingViewModel dependencies
        try {
            val onboardingVM = get<OnboardingViewModel>()
            val getCurrentUserUseCase = get<GetCurrentUserUseCase>()
            val completeOnboardingUseCase = get<CompleteOnboardingUseCase>()
            
            assertNotNull(onboardingVM, "OnboardingViewModel should be resolvable")
            assertNotNull(getCurrentUserUseCase, "GetCurrentUserUseCase should be resolvable")
            assertNotNull(completeOnboardingUseCase, "CompleteOnboardingUseCase should be resolvable")
            
            viewModelValidations["OnboardingViewModel"] = true
        } catch (e: Exception) {
            failures.add("OnboardingViewModel: ${e.message}")
            viewModelValidations["OnboardingViewModel"] = false
        }
        
        // Test DailyLoggingViewModel dependencies
        try {
            val dailyLoggingVM = get<DailyLoggingViewModel>()
            val getDailyLogUseCase = get<GetDailyLogUseCase>()
            val saveDailyLogUseCase = get<SaveDailyLogUseCase>()
            
            assertNotNull(dailyLoggingVM, "DailyLoggingViewModel should be resolvable")
            assertNotNull(getDailyLogUseCase, "GetDailyLogUseCase should be resolvable")
            assertNotNull(saveDailyLogUseCase, "SaveDailyLogUseCase should be resolvable")
            
            viewModelValidations["DailyLoggingViewModel"] = true
        } catch (e: Exception) {
            failures.add("DailyLoggingViewModel: ${e.message}")
            viewModelValidations["DailyLoggingViewModel"] = false
        }
        
        // Test CalendarViewModel dependencies
        try {
            val calendarVM = get<CalendarViewModel>()
            val getCurrentCycleUseCase = get<GetCurrentCycleUseCase>()
            val predictOvulationUseCase = get<PredictOvulationUseCase>()
            val getLogHistoryUseCase = get<GetLogHistoryUseCase>()
            
            assertNotNull(calendarVM, "CalendarViewModel should be resolvable")
            assertNotNull(getCurrentCycleUseCase, "GetCurrentCycleUseCase should be resolvable")
            assertNotNull(predictOvulationUseCase, "PredictOvulationUseCase should be resolvable")
            assertNotNull(getLogHistoryUseCase, "GetLogHistoryUseCase should be resolvable")
            
            viewModelValidations["CalendarViewModel"] = true
        } catch (e: Exception) {
            failures.add("CalendarViewModel: ${e.message}")
            viewModelValidations["CalendarViewModel"] = false
        }
        
        // Test InsightsViewModel dependencies
        try {
            val insightsVM = get<InsightsViewModel>()
            val insightRepository = get<InsightRepository>()
            
            assertNotNull(insightsVM, "InsightsViewModel should be resolvable")
            assertNotNull(insightRepository, "InsightRepository should be resolvable")
            
            viewModelValidations["InsightsViewModel"] = true
        } catch (e: Exception) {
            failures.add("InsightsViewModel: ${e.message}")
            viewModelValidations["InsightsViewModel"] = false
        }
        
        // Test Settings ViewModels dependencies
        try {
            val settingsVM = get<SettingsViewModel>()
            val enhancedSettingsVM = get<EnhancedSettingsViewModel>()
            val settingsManager = get<SettingsManager>()
            
            assertNotNull(settingsVM, "SettingsViewModel should be resolvable")
            assertNotNull(enhancedSettingsVM, "EnhancedSettingsViewModel should be resolvable")
            assertNotNull(settingsManager, "SettingsManager should be resolvable")
            
            viewModelValidations["SettingsViewModels"] = true
        } catch (e: Exception) {
            failures.add("SettingsViewModels: ${e.message}")
            viewModelValidations["SettingsViewModels"] = false
        }
        
        // Test Preferences ViewModels dependencies
        try {
            val displayPrefsVM = get<DisplayPreferencesViewModel>()
            val notificationPrefsVM = get<NotificationPreferencesViewModel>()
            val privacyPrefsVM = get<PrivacyPreferencesViewModel>()
            val syncPrefsVM = get<SyncPreferencesViewModel>()
            
            val getDisplayPreferencesUseCase = get<GetDisplayPreferencesUseCase>()
            val updateDisplayPreferencesUseCase = get<UpdateDisplayPreferencesUseCase>()
            val settingsManager = get<SettingsManager>()
            val notificationManager = get<NotificationManager>()
            val settingsRepository = get<SettingsRepository>()
            
            assertNotNull(displayPrefsVM, "DisplayPreferencesViewModel should be resolvable")
            assertNotNull(notificationPrefsVM, "NotificationPreferencesViewModel should be resolvable")
            assertNotNull(privacyPrefsVM, "PrivacyPreferencesViewModel should be resolvable")
            assertNotNull(syncPrefsVM, "SyncPreferencesViewModel should be resolvable")
            
            assertNotNull(getDisplayPreferencesUseCase, "GetDisplayPreferencesUseCase should be resolvable")
            assertNotNull(updateDisplayPreferencesUseCase, "UpdateDisplayPreferencesUseCase should be resolvable")
            assertNotNull(settingsManager, "SettingsManager should be resolvable")
            assertNotNull(notificationManager, "NotificationManager should be resolvable")
            assertNotNull(settingsRepository, "SettingsRepository should be resolvable")
            
            viewModelValidations["PreferencesViewModels"] = true
        } catch (e: Exception) {
            failures.add("PreferencesViewModels: ${e.message}")
            viewModelValidations["PreferencesViewModels"] = false
        }
        
        // Test Specialized ViewModels dependencies
        try {
            val cyclePrefsVM = get<CyclePreferencesViewModel>()
            val unitPrefsVM = get<UnitPreferencesViewModel>()
            val unitSystemSettingsVM = get<UnitSystemSettingsViewModel>()
            
            val settingsManager = get<SettingsManager>()
            val unitConverter = get<UnitConverter>()
            val unitSystemManager = get<UnitSystemManager>()
            
            assertNotNull(cyclePrefsVM, "CyclePreferencesViewModel should be resolvable")
            assertNotNull(unitPrefsVM, "UnitPreferencesViewModel should be resolvable")
            assertNotNull(unitSystemSettingsVM, "UnitSystemSettingsViewModel should be resolvable")
            
            assertNotNull(settingsManager, "SettingsManager should be resolvable")
            assertNotNull(unitConverter, "UnitConverter should be resolvable")
            assertNotNull(unitSystemManager, "UnitSystemManager should be resolvable")
            
            viewModelValidations["SpecializedViewModels"] = true
        } catch (e: Exception) {
            failures.add("SpecializedViewModels: ${e.message}")
            viewModelValidations["SpecializedViewModels"] = false
        }
        
        // Test Help and Support ViewModels dependencies
        try {
            val helpSupportVM = get<HelpSupportViewModel>()
            val supportRequestVM = get<SupportRequestViewModel>()
            val bugReportVM = get<BugReportViewModel>()
            
            val getHelpCategoriesUseCase = get<GetHelpCategoriesUseCase>()
            val searchFAQsUseCase = get<SearchFAQsUseCase>()
            val submitSupportRequestUseCase = get<SubmitSupportRequestUseCase>()
            val getTutorialsUseCase = get<GetTutorialsUseCase>()
            val helpSupportRepository = get<HelpSupportRepository>()
            
            assertNotNull(helpSupportVM, "HelpSupportViewModel should be resolvable")
            assertNotNull(supportRequestVM, "SupportRequestViewModel should be resolvable")
            assertNotNull(bugReportVM, "BugReportViewModel should be resolvable")
            
            assertNotNull(getHelpCategoriesUseCase, "GetHelpCategoriesUseCase should be resolvable")
            assertNotNull(searchFAQsUseCase, "SearchFAQsUseCase should be resolvable")
            assertNotNull(submitSupportRequestUseCase, "SubmitSupportRequestUseCase should be resolvable")
            assertNotNull(getTutorialsUseCase, "GetTutorialsUseCase should be resolvable")
            assertNotNull(helpSupportRepository, "HelpSupportRepository should be resolvable")
            
            viewModelValidations["HelpSupportViewModels"] = true
        } catch (e: Exception) {
            failures.add("HelpSupportViewModels: ${e.message}")
            viewModelValidations["HelpSupportViewModels"] = false
        }
        
        // Test ProfileManagementViewModel dependencies
        try {
            val profileVM = get<ProfileManagementViewModel>()
            val getCurrentUserUseCase = get<GetCurrentUserUseCase>()
            val updateUserProfileUseCase = get<UpdateUserProfileUseCase>()
            val updateHealthGoalUseCase = get<UpdateHealthGoalUseCase>()
            val getUserStatisticsUseCase = get<GetUserStatisticsUseCase>()
            
            assertNotNull(profileVM, "ProfileManagementViewModel should be resolvable")
            assertNotNull(getCurrentUserUseCase, "GetCurrentUserUseCase should be resolvable")
            assertNotNull(updateUserProfileUseCase, "UpdateUserProfileUseCase should be resolvable")
            assertNotNull(updateHealthGoalUseCase, "UpdateHealthGoalUseCase should be resolvable")
            assertNotNull(getUserStatisticsUseCase, "GetUserStatisticsUseCase should be resolvable")
            
            viewModelValidations["ProfileManagementViewModel"] = true
        } catch (e: Exception) {
            failures.add("ProfileManagementViewModel: ${e.message}")
            viewModelValidations["ProfileManagementViewModel"] = false
        }
        
        // Report results
        if (failures.isNotEmpty()) {
            fail("Failed to resolve dependencies for ${failures.size} ViewModels:\n${failures.joinToString("\n")}")
        }
        
        val successCount = viewModelValidations.values.count { it }
        val totalCount = viewModelValidations.size
        
        assertTrue(successCount == totalCount, 
            "All ViewModels should have their dependencies resolved. Success: $successCount/$totalCount")
        
        println("✅ ViewModel DI Access Validation: $successCount/$totalCount ViewModels successfully resolved with dependencies")
    }
    
    // ========================================
    // Test 2: State Management with Business Logic
    // ========================================
    
    @Test
    fun `state management works with real business logic operations`() = runTest {
        // Test OnboardingViewModel state management with business logic
        val onboardingVM = get<OnboardingViewModel>()
        
        // Initial state should be properly set
        val initialState = onboardingVM.uiState.first()
        assertEquals(OnboardingStep.WELCOME, initialState.currentStep)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.isCompleted)
        
        // Test state transitions
        onboardingVM.nextStep()
        val afterNextState = onboardingVM.uiState.first()
        assertEquals(OnboardingStep.GOAL_SELECTION, afterNextState.currentStep)
        
        // Test goal selection
        onboardingVM.selectGoal(HealthGoal.CYCLE_TRACKING)
        val afterGoalState = onboardingVM.uiState.first()
        assertEquals(HealthGoal.CYCLE_TRACKING, afterGoalState.selectedGoal)
        
        // Test DailyLoggingViewModel state management with business logic
        val dailyLoggingVM = get<DailyLoggingViewModel>()
        
        // Initial state should be properly set
        val dailyInitialState = dailyLoggingVM.uiState.first()
        assertEquals(Clock.System.todayIn(TimeZone.currentSystemDefault()), dailyInitialState.selectedDate)
        assertFalse(dailyInitialState.hasUnsavedChanges)
        
        // Test state updates through business operations
        dailyLoggingVM.updatePeriodFlow(PeriodFlow.LIGHT)
        val afterPeriodState = dailyLoggingVM.uiState.first()
        assertEquals(PeriodFlow.LIGHT, afterPeriodState.periodFlow)
        assertTrue(afterPeriodState.hasUnsavedChanges)
        
        dailyLoggingVM.updateMood(Mood.HAPPY)
        val afterMoodState = dailyLoggingVM.uiState.first()
        assertEquals(Mood.HAPPY, afterMoodState.mood)
        assertTrue(afterMoodState.hasUnsavedChanges)
        
        dailyLoggingVM.toggleSymptom(Symptom.CRAMPS)
        val afterSymptomState = dailyLoggingVM.uiState.first()
        assertTrue(afterSymptomState.selectedSymptoms.contains(Symptom.CRAMPS))
        assertTrue(afterSymptomState.hasUnsavedChanges)
        
        // Test CalendarViewModel state management
        val calendarVM = get<CalendarViewModel>()
        val calendarInitialState = calendarVM.uiState.first()
        assertNotNull(calendarInitialState, "CalendarViewModel should have initial state")
        
        println("✅ State Management with Business Logic: All ViewModels properly manage state through business operations")
    }
    
    // ========================================
    // Test 3: UI Component Access to ViewModels
    // ========================================
    
    @Test
    fun `UI components can access ViewModels through dependency injection`() = runTest {
        // Simulate UI component access patterns
        val uiComponentAccessResults = mutableMapOf<String, Boolean>()
        val accessFailures = mutableListOf<String>()
        
        // Test accessing ViewModels as UI components would
        try {
            // Onboarding Screen access
            val onboardingVM = get<OnboardingViewModel>()
            val onboardingState = onboardingVM.uiState.first()
            assertNotNull(onboardingState, "Onboarding UI should access ViewModel state")
            uiComponentAccessResults["OnboardingScreen"] = true
        } catch (e: Exception) {
            accessFailures.add("OnboardingScreen: ${e.message}")
            uiComponentAccessResults["OnboardingScreen"] = false
        }
        
        try {
            // Daily Logging Screen access
            val dailyLoggingVM = get<DailyLoggingViewModel>()
            val dailyLoggingState = dailyLoggingVM.uiState.first()
            assertNotNull(dailyLoggingState, "Daily Logging UI should access ViewModel state")
            
            // Test UI interaction methods
            dailyLoggingVM.updateNotes("Test note from UI")
            val updatedState = dailyLoggingVM.uiState.first()
            assertEquals("Test note from UI", updatedState.notes)
            
            uiComponentAccessResults["DailyLoggingScreen"] = true
        } catch (e: Exception) {
            accessFailures.add("DailyLoggingScreen: ${e.message}")
            uiComponentAccessResults["DailyLoggingScreen"] = false
        }
        
        try {
            // Calendar Screen access
            val calendarVM = get<CalendarViewModel>()
            val calendarState = calendarVM.uiState.first()
            assertNotNull(calendarState, "Calendar UI should access ViewModel state")
            uiComponentAccessResults["CalendarScreen"] = true
        } catch (e: Exception) {
            accessFailures.add("CalendarScreen: ${e.message}")
            uiComponentAccessResults["CalendarScreen"] = false
        }
        
        try {
            // Settings Screen access
            val settingsVM = get<SettingsViewModel>()
            val settingsState = settingsVM.uiState.first()
            assertNotNull(settingsState, "Settings UI should access ViewModel state")
            uiComponentAccessResults["SettingsScreen"] = true
        } catch (e: Exception) {
            accessFailures.add("SettingsScreen: ${e.message}")
            uiComponentAccessResults["SettingsScreen"] = false
        }
        
        try {
            // Preferences Screens access
            val displayPrefsVM = get<DisplayPreferencesViewModel>()
            val notificationPrefsVM = get<NotificationPreferencesViewModel>()
            val privacyPrefsVM = get<PrivacyPreferencesViewModel>()
            val syncPrefsVM = get<SyncPreferencesViewModel>()
            
            val displayState = displayPrefsVM.uiState.first()
            val notificationState = notificationPrefsVM.uiState.first()
            val privacyState = privacyPrefsVM.uiState.first()
            val syncState = syncPrefsVM.uiState.first()
            
            assertNotNull(displayState, "Display Preferences UI should access ViewModel state")
            assertNotNull(notificationState, "Notification Preferences UI should access ViewModel state")
            assertNotNull(privacyState, "Privacy Preferences UI should access ViewModel state")
            assertNotNull(syncState, "Sync Preferences UI should access ViewModel state")
            
            uiComponentAccessResults["PreferencesScreens"] = true
        } catch (e: Exception) {
            accessFailures.add("PreferencesScreens: ${e.message}")
            uiComponentAccessResults["PreferencesScreens"] = false
        }
        
        try {
            // Help and Support Screens access
            val helpSupportVM = get<HelpSupportViewModel>()
            val supportRequestVM = get<SupportRequestViewModel>()
            val bugReportVM = get<BugReportViewModel>()
            
            val helpState = helpSupportVM.uiState.first()
            val supportState = supportRequestVM.uiState.first()
            val bugState = bugReportVM.uiState.first()
            
            assertNotNull(helpState, "Help Support UI should access ViewModel state")
            assertNotNull(supportState, "Support Request UI should access ViewModel state")
            assertNotNull(bugState, "Bug Report UI should access ViewModel state")
            
            uiComponentAccessResults["HelpSupportScreens"] = true
        } catch (e: Exception) {
            accessFailures.add("HelpSupportScreens: ${e.message}")
            uiComponentAccessResults["HelpSupportScreens"] = false
        }
        
        try {
            // Profile Management Screen access
            val profileVM = get<ProfileManagementViewModel>()
            val profileState = profileVM.uiState.first()
            assertNotNull(profileState, "Profile Management UI should access ViewModel state")
            uiComponentAccessResults["ProfileManagementScreen"] = true
        } catch (e: Exception) {
            accessFailures.add("ProfileManagementScreen: ${e.message}")
            uiComponentAccessResults["ProfileManagementScreen"] = false
        }
        
        try {
            // Insights Screen access
            val insightsVM = get<InsightsViewModel>()
            val insightsState = insightsVM.uiState.first()
            assertNotNull(insightsState, "Insights UI should access ViewModel state")
            uiComponentAccessResults["InsightsScreen"] = true
        } catch (e: Exception) {
            accessFailures.add("InsightsScreen: ${e.message}")
            uiComponentAccessResults["InsightsScreen"] = false
        }
        
        // Report results
        if (accessFailures.isNotEmpty()) {
            fail("UI components failed to access ViewModels:\n${accessFailures.joinToString("\n")}")
        }
        
        val successCount = uiComponentAccessResults.values.count { it }
        val totalCount = uiComponentAccessResults.size
        
        assertTrue(successCount == totalCount, 
            "All UI components should access ViewModels successfully. Success: $successCount/$totalCount")
        
        println("✅ UI Component Access: $successCount/$totalCount UI components successfully access ViewModels through DI")
    }
    
    // ========================================
    // Test 4: Reactive State Updates End-to-End
    // ========================================
    
    @Test
    fun `reactive state updates work end-to-end`() = runTest {
        // Test reactive state updates across the complete flow
        val onboardingVM = get<OnboardingViewModel>()
        val dailyLoggingVM = get<DailyLoggingViewModel>()
        
        // Test OnboardingViewModel reactive updates
        val onboardingStates = mutableListOf<OnboardingUiState>()
        
        // Collect initial state
        onboardingStates.add(onboardingVM.uiState.first())
        
        // Trigger state changes and verify reactive updates
        onboardingVM.nextStep()
        
        // Verify state update immediately (since we're using UnconfinedTestDispatcher)
        val afterNextState = onboardingVM.uiState.first()
        assertEquals(OnboardingStep.GOAL_SELECTION, afterNextState.currentStep)
        onboardingStates.add(afterNextState)
        
        // Test goal selection reactive update
        onboardingVM.selectGoal(HealthGoal.CYCLE_TRACKING)
        
        val afterGoalState = onboardingVM.uiState.first()
        assertEquals(HealthGoal.CYCLE_TRACKING, afterGoalState.selectedGoal)
        onboardingStates.add(afterGoalState)
        
        // Test DailyLoggingViewModel reactive updates
        val dailyLoggingStates = mutableListOf<DailyLoggingUiState>()
        
        // Collect initial state
        dailyLoggingStates.add(dailyLoggingVM.uiState.first())
        
        // Test multiple rapid state changes
        dailyLoggingVM.updatePeriodFlow(PeriodFlow.MEDIUM)
        dailyLoggingVM.updateMood(Mood.NEUTRAL)
        dailyLoggingVM.toggleSymptom(Symptom.HEADACHE)
        dailyLoggingVM.updateNotes("Reactive test notes")
        
        // Verify all updates propagated immediately
        val finalDailyState = dailyLoggingVM.uiState.first()
        assertEquals(PeriodFlow.MEDIUM, finalDailyState.periodFlow)
        assertEquals(Mood.NEUTRAL, finalDailyState.mood)
        assertTrue(finalDailyState.selectedSymptoms.contains(Symptom.HEADACHE))
        assertEquals("Reactive test notes", finalDailyState.notes)
        assertTrue(finalDailyState.hasUnsavedChanges)
        
        dailyLoggingStates.add(finalDailyState)
        
        // Test error state reactive updates
        dailyLoggingVM.updateBBT("invalid_temperature")
        
        val afterInvalidBBTState = dailyLoggingVM.uiState.first()
        assertEquals("invalid_temperature", afterInvalidBBTState.bbt)
        assertFalse(afterInvalidBBTState.isBbtValid)
        
        // Verify state transitions are consistent
        assertTrue(onboardingStates.size >= 3, "Should capture multiple onboarding state transitions")
        assertTrue(dailyLoggingStates.size >= 2, "Should capture multiple daily logging state transitions")
        
        // Verify state changes are different from initial states
        assertNotEquals(onboardingStates[0].currentStep, onboardingStates[1].currentStep)
        assertNotEquals(onboardingStates[1].selectedGoal, onboardingStates[2].selectedGoal)
        assertNotEquals(dailyLoggingStates[0].hasUnsavedChanges, dailyLoggingStates[1].hasUnsavedChanges)
        
        println("✅ Reactive State Updates: End-to-end reactive state management working correctly")
        println("   - OnboardingViewModel: ${onboardingStates.size} state transitions captured")
        println("   - DailyLoggingViewModel: ${dailyLoggingStates.size} state transitions captured")
    }
    
    // ========================================
    // Test 5: Cross-ViewModel Integration
    // ========================================
    
    @Test
    fun `cross-ViewModel integration works with shared dependencies`() = runTest {
        // Test that ViewModels sharing dependencies work correctly together
        val settingsVM = get<SettingsViewModel>()
        val cyclePrefsVM = get<CyclePreferencesViewModel>()
        val unitPrefsVM = get<UnitPreferencesViewModel>()
        
        // Verify they share the same SettingsManager instance
        val settingsManager1 = get<SettingsManager>()
        val settingsManager2 = get<SettingsManager>()
        assertTrue(settingsManager1 === settingsManager2, "SettingsManager should be singleton")
        
        // Test that multiple ViewModels can access shared repositories
        val onboardingVM = get<OnboardingViewModel>()
        val profileVM = get<ProfileManagementViewModel>()
        
        // Both should use the same UserRepository
        val userRepository1 = get<UserRepository>()
        val userRepository2 = get<UserRepository>()
        assertTrue(userRepository1 === userRepository2, "UserRepository should be singleton")
        
        // Test that logging ViewModels share LogRepository
        val dailyLoggingVM = get<DailyLoggingViewModel>()
        val calendarVM = get<CalendarViewModel>()
        
        val logRepository1 = get<LogRepository>()
        val logRepository2 = get<LogRepository>()
        assertTrue(logRepository1 === logRepository2, "LogRepository should be singleton")
        
        // Verify all ViewModels can be instantiated simultaneously
        val allViewModels = listOf(
            settingsVM, cyclePrefsVM, unitPrefsVM, onboardingVM, 
            profileVM, dailyLoggingVM, calendarVM
        )
        
        allViewModels.forEach { vm ->
            assertNotNull(vm, "All ViewModels should be instantiated successfully")
        }
        
        println("✅ Cross-ViewModel Integration: Shared dependencies work correctly across ViewModels")
    }
    
    // ========================================
    // Test 6: Complete End-to-End Flow Validation
    // ========================================
    
    @Test
    fun `complete end-to-end flow validation - all components working together`() = runTest {
        val flowValidationResults = mutableMapOf<String, Boolean>()
        val flowFailures = mutableListOf<String>()
        
        try {
            // 1. Authentication Flow
            val onboardingVM = get<OnboardingViewModel>()
            val initialState = onboardingVM.uiState.first()
            
            onboardingVM.nextStep()
            onboardingVM.selectGoal(HealthGoal.CYCLE_TRACKING)
            
            val afterGoalState = onboardingVM.uiState.first()
            assertEquals(HealthGoal.CYCLE_TRACKING, afterGoalState.selectedGoal)
            
            flowValidationResults["AuthenticationFlow"] = true
        } catch (e: Exception) {
            flowFailures.add("AuthenticationFlow: ${e.message}")
            flowValidationResults["AuthenticationFlow"] = false
        }
        
        try {
            // 2. Daily Logging Flow
            val dailyLoggingVM = get<DailyLoggingViewModel>()
            
            dailyLoggingVM.updatePeriodFlow(PeriodFlow.HEAVY)
            dailyLoggingVM.updateMood(Mood.HAPPY)
            dailyLoggingVM.toggleSymptom(Symptom.BLOATING)
            dailyLoggingVM.updateBBT("98.6")
            dailyLoggingVM.updateNotes("Complete flow test")
            
            val finalState = dailyLoggingVM.uiState.first()
            assertEquals(PeriodFlow.HEAVY, finalState.periodFlow)
            assertEquals(Mood.HAPPY, finalState.mood)
            assertTrue(finalState.selectedSymptoms.contains(Symptom.BLOATING))
            assertEquals("98.6", finalState.bbt)
            assertEquals("Complete flow test", finalState.notes)
            assertTrue(finalState.hasUnsavedChanges)
            
            flowValidationResults["DailyLoggingFlow"] = true
        } catch (e: Exception) {
            flowFailures.add("DailyLoggingFlow: ${e.message}")
            flowValidationResults["DailyLoggingFlow"] = false
        }
        
        try {
            // 3. Settings Flow
            val settingsVM = get<SettingsViewModel>()
            val displayPrefsVM = get<DisplayPreferencesViewModel>()
            val notificationPrefsVM = get<NotificationPreferencesViewModel>()
            
            val settingsState = settingsVM.uiState.first()
            val displayState = displayPrefsVM.uiState.first()
            val notificationState = notificationPrefsVM.uiState.first()
            
            assertNotNull(settingsState)
            assertNotNull(displayState)
            assertNotNull(notificationState)
            
            flowValidationResults["SettingsFlow"] = true
        } catch (e: Exception) {
            flowFailures.add("SettingsFlow: ${e.message}")
            flowValidationResults["SettingsFlow"] = false
        }
        
        try {
            // 4. Calendar and Insights Flow
            val calendarVM = get<CalendarViewModel>()
            val insightsVM = get<InsightsViewModel>()
            
            val calendarState = calendarVM.uiState.first()
            val insightsState = insightsVM.uiState.first()
            
            assertNotNull(calendarState)
            assertNotNull(insightsState)
            
            flowValidationResults["CalendarInsightsFlow"] = true
        } catch (e: Exception) {
            flowFailures.add("CalendarInsightsFlow: ${e.message}")
            flowValidationResults["CalendarInsightsFlow"] = false
        }
        
        try {
            // 5. Help and Support Flow
            val helpSupportVM = get<HelpSupportViewModel>()
            val supportRequestVM = get<SupportRequestViewModel>()
            val bugReportVM = get<BugReportViewModel>()
            
            val helpState = helpSupportVM.uiState.first()
            val supportState = supportRequestVM.uiState.first()
            val bugState = bugReportVM.uiState.first()
            
            assertNotNull(helpState)
            assertNotNull(supportState)
            assertNotNull(bugState)
            
            flowValidationResults["HelpSupportFlow"] = true
        } catch (e: Exception) {
            flowFailures.add("HelpSupportFlow: ${e.message}")
            flowValidationResults["HelpSupportFlow"] = false
        }
        
        try {
            // 6. Profile Management Flow
            val profileVM = get<ProfileManagementViewModel>()
            val profileState = profileVM.uiState.first()
            assertNotNull(profileState)
            
            flowValidationResults["ProfileManagementFlow"] = true
        } catch (e: Exception) {
            flowFailures.add("ProfileManagementFlow: ${e.message}")
            flowValidationResults["ProfileManagementFlow"] = false
        }
        
        // Report final results
        if (flowFailures.isNotEmpty()) {
            fail("End-to-end flow validation failed for ${flowFailures.size} flows:\n${flowFailures.joinToString("\n")}")
        }
        
        val successCount = flowValidationResults.values.count { it }
        val totalCount = flowValidationResults.size
        
        assertTrue(successCount == totalCount, 
            "All end-to-end flows should work correctly. Success: $successCount/$totalCount")
        
        println("✅ Complete End-to-End Flow Validation: $successCount/$totalCount flows working correctly")
        println("   - All ViewModels connect to business logic through DI")
        println("   - State management works with real business operations")
        println("   - UI components can access ViewModels through DI")
        println("   - Reactive state updates work end-to-end")
        println("   - Cross-ViewModel integration works with shared dependencies")
    }
}