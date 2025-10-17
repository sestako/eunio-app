package com.eunio.healthapp.presentation.state

sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    data class Success<T>(val data: T) : LoadingState()
    data class Error(val message: String) : LoadingState()
}