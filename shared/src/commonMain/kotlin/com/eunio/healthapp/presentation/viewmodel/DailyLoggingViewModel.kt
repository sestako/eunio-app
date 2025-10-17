package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.usecase.logging.GetDailyLogUseCase
import com.eunio.healthapp.domain.usecase.logging.SaveDailyLogUseCase
import com.eunio.healthapp.presentation.state.DailyLoggingUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * ViewModel for managing daily logging functionality.
 */
class DailyLoggingViewModel(
    private val getDailyLogUseCase: GetDailyLogUseCase,
    private val saveDailyLogUseCase: SaveDailyLogUseCase,
    private val authManager: com.eunio.healthapp.domain.manager.AuthManager,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : BaseViewModel<DailyLoggingUiState>(dispatcher) {
    
    override val initialState = DailyLoggingUiState(
        selectedDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    )
    
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    
    init {
        loadLogForSelectedDate()
    }
    
    /**
     * Selects a date for logging.
     */
    fun selectDate(date: LocalDate) {
        if (date == uiState.value.selectedDate) return
        
        updateState { 
            it.copy(
                selectedDate = date,
                hasUnsavedChanges = false,
                errorMessage = null,
                successMessage = null
            )
        }
        loadLogForSelectedDate()
    }
    
    /**
     * Loads the log for the currently selected date.
     */
    private fun loadLogForSelectedDate() {
        val selectedDate = uiState.value.selectedDate ?: return
        
        println("📱 ViewModel.loadLogForSelectedDate() called for: $selectedDate")
        
        viewModelScope.launch {
            // Get current user ID
            val currentUser = authManager.getCurrentUser().getOrNull()
            val userId = currentUser?.id
            
            println("📱 Current user ID: $userId")
            
            if (userId == null) {
                println("📱 ❌ No user ID - not logged in")
                updateState { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Please log in to view your data"
                    )
                }
                return@launch
            }
            
            println("📱 Setting isLoading = true")
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            println("📱 Calling getDailyLogUseCase...")
            getDailyLogUseCase(userId, selectedDate)
                .onSuccess { log ->
                    println("📱 ✅ UseCase SUCCESS - log: ${if (log != null) "FOUND (${log.id})" else "NULL"}")
                    updateState { state ->
                        state.copy(
                            isLoading = false,
                            currentLog = log,
                            // Populate form fields from existing log
                            periodFlow = log?.periodFlow,
                            selectedSymptoms = log?.symptoms?.toSet() ?: emptySet(),
                            mood = log?.mood,
                            sexualActivity = log?.sexualActivity,
                            bbt = log?.bbt?.toString() ?: "",
                            cervicalMucus = log?.cervicalMucus,
                            opkResult = log?.opkResult,
                            notes = log?.notes ?: "",
                            hasUnsavedChanges = false
                        )
                    }
                }
                .onError { error ->
                    println("📱 ❌ UseCase ERROR: ${error.message}")
                    updateState { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load log: ${error.message}"
                        )
                    }
                }
        }
    }
    
    /**
     * Updates period flow.
     */
    fun updatePeriodFlow(flow: PeriodFlow?) {
        updateState { 
            it.copy(
                periodFlow = flow,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Toggles a symptom selection.
     */
    fun toggleSymptom(symptom: Symptom) {
        val currentSymptoms = uiState.value.selectedSymptoms.toMutableSet()
        if (currentSymptoms.contains(symptom)) {
            currentSymptoms.remove(symptom)
        } else {
            currentSymptoms.add(symptom)
        }
        
        updateState { 
            it.copy(
                selectedSymptoms = currentSymptoms,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Updates mood.
     */
    fun updateMood(mood: Mood?) {
        updateState { 
            it.copy(
                mood = mood,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Updates sexual activity.
     */
    fun updateSexualActivity(activity: SexualActivity?) {
        updateState { 
            it.copy(
                sexualActivity = activity,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Updates BBT value.
     */
    fun updateBBT(bbt: String) {
        updateState { 
            it.copy(
                bbt = bbt,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Updates cervical mucus.
     */
    fun updateCervicalMucus(mucus: CervicalMucus?) {
        updateState { 
            it.copy(
                cervicalMucus = mucus,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Updates OPK result.
     */
    fun updateOPKResult(result: OPKResult?) {
        updateState { 
            it.copy(
                opkResult = result,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Updates notes.
     */
    fun updateNotes(notes: String) {
        updateState { 
            it.copy(
                notes = notes,
                hasUnsavedChanges = true,
                errorMessage = null
            )
        }
    }
    
    /**
     * Saves the current log.
     */
    fun saveLog() {
        val state = uiState.value
        val selectedDate = state.selectedDate
        
        if (selectedDate == null) {
            updateState { it.copy(errorMessage = "No date selected") }
            return
        }
        
        if (!state.isBbtValid) {
            updateState { it.copy(errorMessage = "BBT must be between 95.0 and 105.0°F") }
            return
        }
        
        viewModelScope.launch {
            // Get current user ID
            val currentUser = authManager.getCurrentUser().getOrNull()
            val userId = currentUser?.id
            
            if (userId == null) {
                updateState { 
                    it.copy(
                        isSaving = false,
                        errorMessage = "Please log in to save your data"
                    )
                }
                return@launch
            }
            
            updateState { it.copy(isSaving = true, errorMessage = null) }
            
            val bbtValue = state.bbt.toDoubleOrNull()
            val now = Clock.System.now()
            
            val dailyLog = DailyLog(
                id = state.currentLog?.id ?: selectedDate.toString(), // Use date as ID (yyyy-MM-dd format)
                userId = userId, // Use real authenticated user ID
                date = selectedDate,
                periodFlow = state.periodFlow,
                symptoms = state.selectedSymptoms.toList(),
                mood = state.mood,
                sexualActivity = state.sexualActivity,
                bbt = bbtValue,
                cervicalMucus = state.cervicalMucus,
                opkResult = state.opkResult,
                notes = state.notes.takeIf { it.isNotBlank() },
                createdAt = state.currentLog?.createdAt ?: now,
                updatedAt = now
            )
            
            try {
                saveDailyLogUseCase(dailyLog)
                    .onSuccess {
                        println("✅ Save successful for user: $userId, date: $selectedDate")
                        updateState { 
                            it.copy(
                                isSaving = false,
                                hasUnsavedChanges = false,
                                currentLog = dailyLog,
                                successMessage = "Log saved successfully"
                            )
                        }
                        _messages.emit("Log saved for ${selectedDate}")
                    }
                    .onError { error ->
                        println("❌ Save failed: ${error.message}")
                        updateState { 
                            it.copy(
                                isSaving = false,
                                errorMessage = "Failed to save log: ${error.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                println("❌ Save exception: ${e.message}")
                updateState { 
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save log: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Clears success and error messages.
     */
    fun clearMessages() {
        updateState { 
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }
}