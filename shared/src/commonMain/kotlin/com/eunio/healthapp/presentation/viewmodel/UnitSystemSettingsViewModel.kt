package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.UnitSystemManager
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.UnitSystemSettingsUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing unit system settings state and user interactions.
 * Provides reactive state management and handles preference changes with feedback.
 */
class UnitSystemSettingsViewModel(
    private val unitSystemManager: UnitSystemManager,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : BaseViewModel<UnitSystemSettingsUiState>(dispatcher) {
    
    override val initialState = UnitSystemSettingsUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    init {
        loadCurrentUnitSystem()
        observeUnitSystemChanges()
    }
    
    /**
     * Loads the current unit system preference.
     */
    private fun loadCurrentUnitSystem() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            try {
                val currentUnitSystem = unitSystemManager.getCurrentUnitSystem()
                updateState { 
                    it.copy(
                        currentUnitSystem = currentUnitSystem,
                        loadingState = LoadingState.Success(currentUnitSystem)
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        loadingState = LoadingState.Error("Failed to load unit system preference: ${e.message}")
                    )
                }
                _showErrorMessage.emit("Failed to load preferences")
            }
        }
    }
    
    /**
     * Observes unit system changes for reactive updates.
     */
    private fun observeUnitSystemChanges() {
        viewModelScope.launch {
            unitSystemManager.observeUnitSystemChanges()
                .collect { unitSystem ->
                    updateState { 
                        it.copy(
                            currentUnitSystem = unitSystem,
                            loadingState = LoadingState.Success(unitSystem)
                        )
                    }
                }
        }
    }
    
    /**
     * Changes the unit system preference with user feedback.
     * 
     * @param unitSystem The new unit system to set
     */
    fun changeUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch {
            updateState { it.copy(isChangingUnitSystem = true) }
            
            unitSystemManager.setUnitSystem(unitSystem, isManuallySet = true)
                .onSuccess {
                    updateState { 
                        it.copy(
                            currentUnitSystem = unitSystem,
                            isChangingUnitSystem = false
                        )
                    }
                    _showSuccessMessage.emit("Unit system changed to ${unitSystem.displayName}")
                }
                .onError { error ->
                    updateState { it.copy(isChangingUnitSystem = false) }
                    _showErrorMessage.emit("Failed to change unit system: ${error.message}")
                }
        }
    }
    
    /**
     * Refreshes the unit system preference from the repository.
     */
    fun refresh() {
        loadCurrentUnitSystem()
    }
}

