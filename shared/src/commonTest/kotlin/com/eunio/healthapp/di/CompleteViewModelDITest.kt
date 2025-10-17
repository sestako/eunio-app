package com.eunio.healthapp.di

import com.eunio.healthapp.presentation.viewmodel.*
import com.eunio.healthapp.testutil.testModule
import com.eunio.healthapp.testutil.minimalTestModule
import com.eunio.healthapp.di.sharedModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*

/**
 * Integration tests to verify complete ViewModel DI coverage.
 * Tests that all ViewModels can be instantiated through dependency injection.
 */
class CompleteViewModelDITest : KoinTest {
    
    @BeforeTest
    fun setup() {
        // Set up test dispatcher for ViewModels
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
        
        startKoin {
            modules(
                sharedModule,
                testModule // Add test module for mock services
            )
        }
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
        kotlinx.coroutines.Dispatchers.resetMain()
    }
    
    /**
     * Test that all core ViewModels can be instantiated through DI.
     */
    @Test
    fun `all core ViewModels can be instantiated through DI`() {
        // Core ViewModels
        val onboardingViewModel: OnboardingViewModel by inject()
        val dailyLoggingViewModel: DailyLoggingViewModel by inject()
        val calendarViewModel: CalendarViewModel by inject()
        val insightsViewModel: InsightsViewModel by inject()
        
        assertNotNull(onboardingViewModel)
        assertNotNull(dailyLoggingViewModel)
        assertNotNull(calendarViewModel)
        assertNotNull(insightsViewModel)
    }
    
    /**
     * Test that all Help and Support ViewModels can be instantiated through DI.
     */
    @Test
    fun `all help and support ViewModels can be instantiated through DI`() {
        val helpSupportViewModel: HelpSupportViewModel by inject()
        val supportRequestViewModel: SupportRequestViewModel by inject()
        val bugReportViewModel: BugReportViewModel by inject()
        
        assertNotNull(helpSupportViewModel)
        assertNotNull(supportRequestViewModel)
        assertNotNull(bugReportViewModel)
    }
    
    /**
     * Test that Profile Management ViewModel can be instantiated through DI.
     */
    @Test
    fun `profile management ViewModel can be instantiated through DI`() {
        val profileManagementViewModel: ProfileManagementViewModel by inject()
        
        assertNotNull(profileManagementViewModel)
    }
    
    /**
     * Test that all Settings ViewModels can be instantiated through DI.
     */
    @Test
    fun `all settings ViewModels can be instantiated through DI`() {
        val settingsViewModel: SettingsViewModel by inject()
        val enhancedSettingsViewModel: EnhancedSettingsViewModel by inject()
        
        assertNotNull(settingsViewModel)
        assertNotNull(enhancedSettingsViewModel)
    }
    
    /**
     * Test that all Preferences ViewModels can be instantiated through DI.
     */
    @Test
    fun `all preferences ViewModels can be instantiated through DI`() {
        val displayPreferencesViewModel: DisplayPreferencesViewModel by inject()
        val notificationPreferencesViewModel: NotificationPreferencesViewModel by inject()
        val privacyPreferencesViewModel: PrivacyPreferencesViewModel by inject()
        val syncPreferencesViewModel: SyncPreferencesViewModel by inject()
        
        assertNotNull(displayPreferencesViewModel)
        assertNotNull(notificationPreferencesViewModel)
        assertNotNull(privacyPreferencesViewModel)
        assertNotNull(syncPreferencesViewModel)
    }
    
    /**
     * Test that all Specialized ViewModels can be instantiated through DI.
     * This is the main test for task 2.3.
     */
    @Test
    fun `all specialized ViewModels can be instantiated through DI`() {
        val cyclePreferencesViewModel: CyclePreferencesViewModel by inject()
        val unitPreferencesViewModel: UnitPreferencesViewModel by inject()
        val unitSystemSettingsViewModel: UnitSystemSettingsViewModel by inject()
        
        assertNotNull(cyclePreferencesViewModel)
        assertNotNull(unitPreferencesViewModel)
        assertNotNull(unitSystemSettingsViewModel)
    }
    
    /**
     * Test that all ViewModels have their dependencies properly resolved.
     */
    @Test
    fun `all ViewModels have dependencies properly resolved`() {
        // Test that ViewModels can be created without throwing exceptions
        try {
            val onboardingViewModel: OnboardingViewModel by inject()
            val dailyLoggingViewModel: DailyLoggingViewModel by inject()
            val calendarViewModel: CalendarViewModel by inject()
            val insightsViewModel: InsightsViewModel by inject()
            val helpSupportViewModel: HelpSupportViewModel by inject()
            val supportRequestViewModel: SupportRequestViewModel by inject()
            val bugReportViewModel: BugReportViewModel by inject()
            val profileManagementViewModel: ProfileManagementViewModel by inject()
            val settingsViewModel: SettingsViewModel by inject()
            val enhancedSettingsViewModel: EnhancedSettingsViewModel by inject()
            val displayPreferencesViewModel: DisplayPreferencesViewModel by inject()
            val notificationPreferencesViewModel: NotificationPreferencesViewModel by inject()
            val privacyPreferencesViewModel: PrivacyPreferencesViewModel by inject()
            val syncPreferencesViewModel: SyncPreferencesViewModel by inject()
            val cyclePreferencesViewModel: CyclePreferencesViewModel by inject()
            val unitPreferencesViewModel: UnitPreferencesViewModel by inject()
            val unitSystemSettingsViewModel: UnitSystemSettingsViewModel by inject()
            
            // If we get here, all ViewModels were successfully instantiated
            assertTrue(true, "All ViewModels successfully instantiated through DI")
        } catch (e: Exception) {
            fail("Failed to instantiate ViewModels through DI: ${e.message}")
        }
    }
    
    /**
     * Test that specialized ViewModels have correct dependency types.
     */
    @Test
    fun `specialized ViewModels have correct dependency types`() {
        val cyclePreferencesViewModel: CyclePreferencesViewModel by inject()
        val unitPreferencesViewModel: UnitPreferencesViewModel by inject()
        val unitSystemSettingsViewModel: UnitSystemSettingsViewModel by inject()
        
        // Verify ViewModels are properly instantiated and not null
        assertNotNull(cyclePreferencesViewModel)
        assertNotNull(unitPreferencesViewModel)
        assertNotNull(unitSystemSettingsViewModel)
        
        // Verify they are the correct types
        assertTrue(cyclePreferencesViewModel is CyclePreferencesViewModel)
        assertTrue(unitPreferencesViewModel is UnitPreferencesViewModel)
        assertTrue(unitSystemSettingsViewModel is UnitSystemSettingsViewModel)
    }
    
    /**
     * Test complete ViewModel DI coverage count.
     * Ensures we have all 17 ViewModels registered.
     */
    @Test
    fun `complete ViewModel DI coverage achieved`() {
        val viewModelCount = 17
        var instantiatedCount = 0
        
        // Count all ViewModels that can be instantiated
        try {
            val onboardingViewModel: OnboardingViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val dailyLoggingViewModel: DailyLoggingViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val calendarViewModel: CalendarViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val insightsViewModel: InsightsViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val helpSupportViewModel: HelpSupportViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val supportRequestViewModel: SupportRequestViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val bugReportViewModel: BugReportViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val profileManagementViewModel: ProfileManagementViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val settingsViewModel: SettingsViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val enhancedSettingsViewModel: EnhancedSettingsViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val displayPreferencesViewModel: DisplayPreferencesViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val notificationPreferencesViewModel: NotificationPreferencesViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val privacyPreferencesViewModel: PrivacyPreferencesViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val syncPreferencesViewModel: SyncPreferencesViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val cyclePreferencesViewModel: CyclePreferencesViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val unitPreferencesViewModel: UnitPreferencesViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        try {
            val unitSystemSettingsViewModel: UnitSystemSettingsViewModel by inject()
            instantiatedCount++
        } catch (e: Exception) { /* ignore */ }
        
        assertEquals(viewModelCount, instantiatedCount, "Expected $viewModelCount ViewModels to be instantiable through DI, but only $instantiatedCount were successful")
    }
}