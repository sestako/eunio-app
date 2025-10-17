package com.eunio.healthapp.android.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.android.ui.components.*
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitConverter
import com.eunio.healthapp.domain.util.UnitConverterImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * UI tests for reactive measurement display components.
 * Tests the reactive behavior when unit system preferences change.
 */
@RunWith(AndroidJUnit4::class)
class ReactiveMeasurementDisplayTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val unitSystemFlow = MutableStateFlow(UnitSystem.METRIC)
    
    private val mockUnitSystemManager = object : UnitSystemManager {
        override suspend fun getCurrentUnitSystem(): UnitSystem = unitSystemFlow.value
        
        override suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean): Result<Unit> {
            unitSystemFlow.value = unitSystem
            return Result.success(Unit)
        }
        
        override suspend fun initializeFromLocale(locale: String): UnitSystem {
            val system = UnitSystem.fromLocale(locale)
            unitSystemFlow.value = system
            return system
        }
        
        override suspend fun initializeFromCurrentLocale(): UnitSystem {
            // For testing, assume US locale
            val system = UnitSystem.IMPERIAL
            unitSystemFlow.value = system
            return system
        }
        
        override fun observeUnitSystemChanges(): Flow<UnitSystem> = unitSystemFlow.asStateFlow()
        
        override suspend fun clearCache() {
            // Mock implementation - no-op for testing
        }
    }
    
    private val testModule = module {
        single<UnitConverter> { UnitConverterImpl() }
        single<UnitSystemManager> { mockUnitSystemManager }
    }
    
    @Test
    fun reactiveWeightDisplay_updatesWhenUnitSystemChanges() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                ReactiveWeightDisplay(weightInKg = 70.0)
            }
        }
        
        // Initially shows metric
        composeTestRule.onNodeWithText("70 kg").assertIsDisplayed()
        
        // Change to imperial
        composeTestRule.runOnUiThread {
            unitSystemFlow.value = UnitSystem.IMPERIAL
        }
        
        // Should now show imperial
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("154.32 lbs").assertIsDisplayed()
        composeTestRule.onNodeWithText("70 kg").assertDoesNotExist()
    }
    
    @Test
    fun reactiveDistanceDisplay_updatesWhenUnitSystemChanges() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                ReactiveDistanceDisplay(distanceInKm = 10.0)
            }
        }
        
        // Initially shows metric
        composeTestRule.onNodeWithText("10 km").assertIsDisplayed()
        
        // Change to imperial
        composeTestRule.runOnUiThread {
            unitSystemFlow.value = UnitSystem.IMPERIAL
        }
        
        // Should now show imperial
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("6.21 miles").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 km").assertDoesNotExist()
    }
    
    @Test
    fun reactiveTemperatureDisplay_updatesWhenUnitSystemChanges() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                ReactiveTemperatureDisplay(temperatureInCelsius = 0.0)
            }
        }
        
        // Initially shows metric
        composeTestRule.onNodeWithText("0°C").assertIsDisplayed()
        
        // Change to imperial
        composeTestRule.runOnUiThread {
            unitSystemFlow.value = UnitSystem.IMPERIAL
        }
        
        // Should now show imperial
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("32°F").assertIsDisplayed()
        composeTestRule.onNodeWithText("0°C").assertDoesNotExist()
    }
    
    @Test
    fun reactiveWeightDisplay_handlesMultipleChanges() {
        composeTestRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                ReactiveWeightDisplay(weightInKg = 50.0)
            }
        }
        
        // Initially metric
        composeTestRule.onNodeWithText("50 kg").assertIsDisplayed()
        
        // Change to imperial
        composeTestRule.runOnUiThread {
            unitSystemFlow.value = UnitSystem.IMPERIAL
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("110.23 lbs").assertIsDisplayed()
        
        // Change back to metric
        composeTestRule.runOnUiThread {
            unitSystemFlow.value = UnitSystem.METRIC
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("50 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("110.23 lbs").assertDoesNotExist()
    }
}