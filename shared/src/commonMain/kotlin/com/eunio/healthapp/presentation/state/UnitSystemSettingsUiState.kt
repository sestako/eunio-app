package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for unit system settings screen.
 */
data class UnitSystemSettingsUiState(
    val currentUnitSystem: UnitSystem = UnitSystem.METRIC,
    val isChangingUnitSystem: Boolean = false,
    val loadingState: LoadingState = LoadingState.Idle
) : UiState {
    
    /**
     * Whether the settings are currently loading.
     */
    val isLoading: Boolean
        get() = loadingState is LoadingState.Loading
    
    /**
     * Whether the settings are enabled for interaction.
     */
    val isEnabled: Boolean
        get() = !isLoading && !isChangingUnitSystem
    
    /**
     * Error message if loading failed.
     */
    val errorMessage: String?
        get() = (loadingState as? LoadingState.Error)?.message
}