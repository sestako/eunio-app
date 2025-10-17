package com.eunio.healthapp.presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<T : UiState>(
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    protected val viewModelScope = CoroutineScope(SupervisorJob() + dispatcher)
    
    protected abstract val initialState: T
    
    private val _uiState by lazy { MutableStateFlow(initialState) }
    val uiState: StateFlow<T> by lazy { _uiState.asStateFlow() }
    
    protected fun updateState(update: (T) -> T) {
        _uiState.value = update(_uiState.value)
    }
    
    open fun onCleared() {
        viewModelScope.cancel()
    }
}

interface UiState