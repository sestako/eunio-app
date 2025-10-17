package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.support.SupportRequestType
import com.eunio.healthapp.domain.repository.HelpSupportRepository
import com.eunio.healthapp.domain.usecase.support.SubmitSupportRequestUseCase
import com.eunio.healthapp.presentation.state.BugReportUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BugReportViewModel(
    private val submitSupportRequestUseCase: SubmitSupportRequestUseCase,
    private val helpSupportRepository: HelpSupportRepository
) : BaseViewModel<BugReportUiState>() {

    override val initialState = BugReportUiState()

    init {
        loadDeviceAndAppInfo()
    }

    fun updateTitle(title: String) {
        updateState { 
            it.copy(
                title = title,
                validationErrors = it.validationErrors - "title"
            ) 
        }
    }

    fun updateDescription(description: String) {
        updateState { 
            it.copy(
                description = description,
                validationErrors = it.validationErrors - "description"
            ) 
        }
    }

    fun updateStepsToReproduce(steps: String) {
        updateState { 
            it.copy(
                stepsToReproduce = steps,
                validationErrors = it.validationErrors - "stepsToReproduce"
            ) 
        }
    }

    fun updateExpectedBehavior(expected: String) {
        updateState { it.copy(expectedBehavior = expected) }
    }

    fun updateActualBehavior(actual: String) {
        updateState { it.copy(actualBehavior = actual) }
    }

    fun toggleAttachLogs() {
        updateState { it.copy(attachLogs = !it.attachLogs) }
    }

    fun toggleAttachScreenshots() {
        updateState { it.copy(attachScreenshots = !it.attachScreenshots) }
    }

    fun submitBugReport(userId: String) {
        val currentState = uiState.value
        val validationErrors = validateBugReport(currentState)
        
        if (validationErrors.isNotEmpty()) {
            updateState { it.copy(validationErrors = validationErrors) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            val description = buildBugReportDescription(currentState)
            
            submitSupportRequestUseCase(
                userId = userId,
                type = SupportRequestType.BUG_REPORT,
                subject = "Bug Report: ${currentState.title}",
                description = description,
                attachLogs = currentState.attachLogs
            )
                .onSuccess { requestId ->
                    updateState { 
                        it.copy(
                            isLoading = false,
                            isSubmitted = true
                        ) 
                    }
                }
                .onFailure { error ->
                    updateState { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to submit bug report"
                        ) 
                    }
                }
        }
    }

    fun resetForm() {
        updateState { 
            BugReportUiState(
                deviceInfo = it.deviceInfo,
                appInfo = it.appInfo
            ) 
        }
    }

    fun clearError() {
        updateState { it.copy(errorMessage = null) }
    }

    private fun loadDeviceAndAppInfo() {
        viewModelScope.launch {
            try {
                val deviceInfo = helpSupportRepository.getDeviceInfo()
                val appInfo = helpSupportRepository.getAppInfo()
                
                updateState { 
                    it.copy(
                        deviceInfo = deviceInfo,
                        appInfo = appInfo
                    ) 
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(errorMessage = "Failed to load device information") 
                }
            }
        }
    }

    private fun validateBugReport(state: BugReportUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (state.title.isBlank()) {
            errors["title"] = "Bug title is required"
        } else if (state.title.length < 5) {
            errors["title"] = "Title must be at least 5 characters"
        }
        
        if (state.description.isBlank()) {
            errors["description"] = "Bug description is required"
        } else if (state.description.length < 20) {
            errors["description"] = "Description must be at least 20 characters"
        }
        
        if (state.stepsToReproduce.isBlank()) {
            errors["stepsToReproduce"] = "Steps to reproduce are required"
        }
        
        return errors
    }

    private fun buildBugReportDescription(state: BugReportUiState): String {
        return buildString {
            appendLine("## Bug Description")
            appendLine(state.description)
            appendLine()
            
            appendLine("## Steps to Reproduce")
            appendLine(state.stepsToReproduce)
            appendLine()
            
            if (state.expectedBehavior.isNotBlank()) {
                appendLine("## Expected Behavior")
                appendLine(state.expectedBehavior)
                appendLine()
            }
            
            if (state.actualBehavior.isNotBlank()) {
                appendLine("## Actual Behavior")
                appendLine(state.actualBehavior)
                appendLine()
            }
            
            appendLine("## Additional Information")
            appendLine("- Logs attached: ${if (state.attachLogs) "Yes" else "No"}")
            appendLine("- Screenshots attached: ${if (state.attachScreenshots) "Yes" else "No"}")
        }
    }
}