package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.support.*
import com.eunio.healthapp.presentation.viewmodel.UiState

data class SupportRequestUiState(
    val isLoading: Boolean = false,
    val requestType: SupportRequestType = SupportRequestType.GENERAL_INQUIRY,
    val subject: String = "",
    val description: String = "",
    val attachLogs: Boolean = false,
    val deviceInfo: DeviceInfo? = null,
    val appInfo: AppInfo? = null,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
) : UiState