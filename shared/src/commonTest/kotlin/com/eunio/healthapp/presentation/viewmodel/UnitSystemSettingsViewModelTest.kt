package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.UnitSystemSettingsUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Fake implementation of UnitSystemManager for testing
 */
class FakeUnitSystemManager : UnitSystemManager {
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
            Result.error(com.eunio.healthapp.domain.error.AppError.NetworkError("Test error"))
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
        // For testing, just return the current unit system
        return _currentUnitSystem.value
    }
    
    override fun observeUnitSystemChanges(): Flow<UnitSystem> {
        return _currentUnitSystem.asStateFlow()
    }
    
    override suspend fun clearCache() {
        // No-op for testing
    }
}

/**
 * Unit tests for UnitSystemSettingsViewModel.
 * Tests basic functionality and state management.
 * Note: ViewModel instantiation tests are in platform-specific test modules due to dispatcher requirements.
 */
class UnitSystemSettingsViewModelTest {
    
    @Test
    fun uiState_initialValues_areCorrect() {
        val uiState = UnitSystemSettingsUiState()
        
        assertEquals(UnitSystem.METRIC, uiState.currentUnitSystem)
        assertFalse(uiState.isChangingUnitSystem)
        assertEquals(LoadingState.Idle, uiState.loadingState)
        assertTrue(uiState.isEnabled)
        assertNull(uiState.errorMessage)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenLoading() {
        val uiState = UnitSystemSettingsUiState(
            loadingState = LoadingState.Loading
        )
        
        assertFalse(uiState.isEnabled)
        assertTrue(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenChangingUnitSystem() {
        val uiState = UnitSystemSettingsUiState(
            isChangingUnitSystem = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_errorMessage_extractedFromLoadingState() {
        val errorMessage = "Test error"
        val uiState = UnitSystemSettingsUiState(
            loadingState = LoadingState.Error(errorMessage)
        )
        
        assertEquals(errorMessage, uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_trueWhenSuccessful() {
        val uiState = UnitSystemSettingsUiState(
            currentUnitSystem = UnitSystem.IMPERIAL,
            loadingState = LoadingState.Success(UnitSystem.IMPERIAL),
            isChangingUnitSystem = false
        )
        
        assertTrue(uiState.isEnabled)
        assertFalse(uiState.isLoading)
        assertNull(uiState.errorMessage)
    }
    
    @Test
    fun unitSystem_displayNames_areCorrect() {
        assertEquals("Metric", UnitSystem.METRIC.displayName)
        assertEquals("Imperial", UnitSystem.IMPERIAL.displayName)
    }
    
    @Test
    fun unitSystem_fromLocale_worksCorrectly() {
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("US"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("LR"))
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromLocale("MM"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("GB"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("DE"))
        assertEquals(UnitSystem.METRIC, UnitSystem.fromLocale("FR"))
    }
    
    @Test
    fun fakeUnitSystemManager_worksCorrectly() = runTest {
        val fakeManager = FakeUnitSystemManager()
        
        // Test initial state
        assertEquals(UnitSystem.METRIC, fakeManager.getCurrentUnitSystem())
        
        // Test setting unit system
        val result = fakeManager.setUnitSystem(UnitSystem.IMPERIAL)
        assertTrue(result.isSuccess)
        assertEquals(UnitSystem.IMPERIAL, fakeManager.getCurrentUnitSystem())
        
        // Test error handling
        fakeManager.setShouldFailOnSet(true)
        val errorResult = fakeManager.setUnitSystem(UnitSystem.METRIC)
        assertTrue(errorResult.isError)
        assertEquals(UnitSystem.IMPERIAL, fakeManager.getCurrentUnitSystem()) // Should remain unchanged
    }
    
    @Test
    fun fakeUnitSystemManager_initializeFromLocale_worksCorrectly() = runTest {
        val fakeManager = FakeUnitSystemManager()
        
        // Test US locale
        val usResult = fakeManager.initializeFromLocale("US")
        assertEquals(UnitSystem.IMPERIAL, usResult)
        assertEquals(UnitSystem.IMPERIAL, fakeManager.getCurrentUnitSystem())
        
        // Test European locale
        val gbResult = fakeManager.initializeFromLocale("GB")
        assertEquals(UnitSystem.METRIC, gbResult)
        assertEquals(UnitSystem.METRIC, fakeManager.getCurrentUnitSystem())
    }
    
    @Test
    fun fakeUnitSystemManager_observeChanges_worksCorrectly() = runTest {
        val fakeManager = FakeUnitSystemManager()
        
        // Test that the flow emits the current value
        val initialValue = fakeManager.observeUnitSystemChanges().first()
        assertEquals(UnitSystem.METRIC, initialValue)
        
        // Make a change and verify it's reflected
        fakeManager.setUnitSystem(UnitSystem.IMPERIAL)
        val updatedValue = fakeManager.observeUnitSystemChanges().first()
        assertEquals(UnitSystem.IMPERIAL, updatedValue)
    }
}