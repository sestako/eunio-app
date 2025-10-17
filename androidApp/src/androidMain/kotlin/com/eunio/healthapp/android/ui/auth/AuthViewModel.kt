package com.eunio.healthapp.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunio.healthapp.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens
 */
class AuthViewModel(private val authService: AuthService) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authService.signIn(email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success(result.getOrNull()!!)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authService.signUp(email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success(result.getOrNull()!!)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authService.resetPassword(email)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.PasswordResetSent
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * UI state for authentication screens
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userId: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object PasswordResetSent : AuthUiState()
}
