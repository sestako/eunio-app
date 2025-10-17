package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.support.SupportRequestType
import com.eunio.healthapp.domain.repository.HelpSupportRepository
import com.eunio.healthapp.domain.usecase.support.SubmitSupportRequestUseCase
import com.eunio.healthapp.presentation.state.SupportRequestUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SupportRequestViewModel(
    private val submitSupportRequestUseCase: SubmitSupportRequestUseCase,
    private val helpSupportRepository: HelpSupportRepository
) : BaseViewModel<SupportRequestUiState>() {

    override val initialState = SupportRequestUiState()

    init {
        loadDeviceAndAppInfo()
    }

    fun updateRequestType(type: SupportRequestType) {
        updateState { it.copy(requestType = type) }
    }

    fun updateSubject(subject: String) {
        updateState { 
            it.copy(
                subject = subject,
                validationErrors = it.validationErrors - "subject"
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

    fun toggleAttachLogs() {
        updateState { it.copy(attachLogs = !it.attachLogs) }
    }

    fun submitRequest(userId: String) {
        val currentState = uiState.value
        val validationErrors = validateRequest(currentState)
        
        if (validationErrors.isNotEmpty()) {
            updateState { it.copy(validationErrors = validationErrors) }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            submitSupportRequestUseCase(
                userId = userId,
                type = currentState.requestType,
                subject = currentState.subject,
                description = currentState.description,
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
                            errorMessage = error.message ?: "Failed to submit support request"
                        ) 
                    }
                }
        }
    }

    fun resetForm() {
        updateState { 
            SupportRequestUiState(
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

    private fun validateRequest(state: SupportRequestUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (state.subject.isBlank()) {
            errors["subject"] = "Subject is required"
        } else if (state.subject.length < 5) {
            errors["subject"] = "Subject must be at least 5 characters"
        }
        
        if (state.description.isBlank()) {
            errors["description"] = "Description is required"
        } else if (state.description.length < 20) {
            errors["description"] = "Description must be at least 20 characters"
        }
        
        return errors
    }
}