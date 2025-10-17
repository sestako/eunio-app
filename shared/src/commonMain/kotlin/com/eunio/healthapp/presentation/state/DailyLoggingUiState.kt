package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.presentation.viewmodel.UiState
import kotlinx.datetime.LocalDate

/**
 * UI state for daily logging screen.
 */
data class DailyLoggingUiState(
    val selectedDate: LocalDate? = null,
    val currentLog: DailyLog? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val hasUnsavedChanges: Boolean = false,
    
    // Form state
    val periodFlow: PeriodFlow? = null,
    val selectedSymptoms: Set<Symptom> = emptySet(),
    val mood: Mood? = null,
    val sexualActivity: SexualActivity? = null,
    val bbt: String = "", // String for input validation
    val cervicalMucus: CervicalMucus? = null,
    val opkResult: OPKResult? = null,
    val notes: String = ""
) : UiState {
    
    /**
     * Whether the form can be saved.
     */
    val canSave: Boolean
        get() = !isSaving && hasUnsavedChanges && selectedDate != null
    
    /**
     * Whether BBT input is valid.
     */
    val isBbtValid: Boolean
        get() = bbt.isEmpty() || bbt.toDoubleOrNull()?.let { it in 95.0..105.0 } == true
}