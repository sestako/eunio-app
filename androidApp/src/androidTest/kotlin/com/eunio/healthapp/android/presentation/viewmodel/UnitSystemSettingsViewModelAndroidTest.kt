package com.eunio.healthapp.android.presentation.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.viewmodel.UnitSystemSettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android-specific tests for UnitSystemSettingsViewModel.
 * These tests can run with Android's Main dispatcher.
 */
@RunWith(AndroidJUnit4::class)
class UnitSystemSettingsViewModelAndroidTest {
    
    /**
     * Fake implementation for Android tests
     */
    private class FakeUnitSystemManagerAndroid : UnitSystemManager {
        private val _currentUnitSystem = MutableStateFlow(UnitSystem.METRIC)
        private var shouldFailOnSet = false
        
        fun setShouldFailOnSet(shouldFail: Boolean) {
            shouldFailOnSet = shouldFail
        }
        
        override suspend fun getCurrentUnitSystem(): UnitSystem {
            return _currentUnitSystem.value
        }
        
        override suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean): Result<Unit> {
            return if (shouldFailOnSet) {
                Result.error(com.eunio.healthapp.domain.error.AppError.NetworkError("Android test error"))
            } else {
                _currentUnitSystem.value = unitSystem
                Result.success(Unit)
            }
        }
        
        override suspend fun initializeFromLocale(locale: String): UnitSystem {
            val unitSystem = UnitSystem.fromLocale(locale)
            _currentUnitSystem.value = unitSystem
            return unitSystem
        }
        
        override suspend fun initializeFromCurrentLocale(): UnitSystem {
            // For testing, assume US locale
            val unitSystem = UnitSystem.IMPERIAL
            _currentUnitSystem.value = unitSystem
            return unitSystem
        }
        
        override fun observeUnitSystemChanges(): Flow<UnitSystem> {
            return _currentUnitSystem.asStateFlow()
        }
        
        override suspend fun clearCache() {
            // No-op for testing
        }
    }
    
    private lateinit var fakeManager: FakeUnitSystemManagerAndroid
    private lateinit var viewModel: UnitSystemSettingsViewModel
    
    @Before
    fun setup() {
        fakeManager = FakeUnitSystemManagerAndroid()
        viewModel = UnitSystemSettingsViewModel(fakeManager)
    }
    
    @Test
    fun viewModel_onAndroid_initialStateIsCorrect() = runTest {
        // Give the ViewModel time to initialize
        delay(100)
        
        val initialState = viewModel.uiState.value
        assertEquals(UnitSystem.METRIC, initialState.currentUnitSystem)
        assertFalse(initialState.isChangingUnitSystem)
        assertTrue(initialState.isEnabled)
    }
    
    @Test
    fun viewModel_onAndroid_changeUnitSystemWorksCorrectly() = runTest {
        // Give the ViewModel time to initialize
        delay(100)
        
        // When changing to Imperial on Android
        viewModel.changeUnitSystem(UnitSystem.IMPERIAL)
        
        // Give time for the change to process
        delay(100)
        
        // Then state should update correctly
        val finalState = viewModel.uiState.value
        assertEquals(UnitSystem.IMPERIAL, finalState.currentUnitSystem)
        assertFalse(finalState.isChangingUnitSystem)
        assertTrue(finalState.isEnabled)
    }
    
    @Test
    fun viewModel_onAndroid_handlesErrorsGracefully() = runTest {
        // Give the ViewModel time to initialize
        delay(100)
        
        // Given the manager will fail
        fakeManager.setShouldFailOnSet(true)
        
        // When changing a unit system on Android
        viewModel.changeUnitSystem(UnitSystem.IMPERIAL)
        
        // Give time for the error to process
        delay(100)
        
        // Then error should be handled gracefully
        val finalState = viewModel.uiState.value
        assertEquals(UnitSystem.METRIC, finalState.currentUnitSystem) // Should remain unchanged
        assertFalse(finalState.isChangingUnitSystem)
        assertTrue(finalState.isEnabled)
    }
    
    @Test
    fun viewModel_onAndroid_refreshWorks() = runTest {
        // Give the ViewModel time to initialize
        delay(100)
        
        // When refreshing on Android
        viewModel.refresh()
        
        // Give time for the refresh to process
        delay(100)
        
        // Then state should be refreshed
        val finalState = viewModel.uiState.value
        assertEquals(UnitSystem.METRIC, finalState.currentUnitSystem)
        assertTrue(finalState.isEnabled)
    }
    
    @Test
    fun viewModel_onAndroid_observesUnitSystemChanges() = runTest {
        // Give the ViewModel time to initialize
        delay(100)
        
        val states = mutableListOf<UnitSystem>()
        
        // Collect state changes
        val job = launch {
            viewModel.uiState.collect { state ->
                states.add(state.currentUnitSystem)
            }
        }
        
        // Make changes through the manager directly
        fakeManager.setUnitSystem(UnitSystem.IMPERIAL)
        delay(100)
        
        job.cancel()
        
        // Verify changes were observed
        assertTrue(states.contains(UnitSystem.IMPERIAL))
    }
    
    @Test
    fun viewModel_onAndroid_feedbackFlowsWork() = runTest {
        // Give the ViewModel time to initialize
        delay(100)
        
        val successMessages = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()
        
        // Collect feedback messages
        val successJob = launch {
            viewModel.showSuccessMessage.collect { message ->
                successMessages.add(message)
            }
        }
        
        val errorJob = launch {
            viewModel.showErrorMessage.collect { message ->
                errorMessages.add(message)
            }
        }
        
        // Test success message
        viewModel.changeUnitSystem(UnitSystem.IMPERIAL)
        delay(100)
        
        // Test error message
        fakeManager.setShouldFailOnSet(true)
        viewModel.changeUnitSystem(UnitSystem.METRIC)
        delay(100)
        
        successJob.cancel()
        errorJob.cancel()
        
        // Verify feedback messages were emitted
        assertTrue(successMessages.isNotEmpty())
        assertTrue(errorMessages.isNotEmpty())
    }
}