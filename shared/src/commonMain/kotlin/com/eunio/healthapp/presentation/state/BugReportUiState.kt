package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.support.*
import com.eunio.healthapp.presentation.viewmodel.UiState

data class BugReportUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val stepsToReproduce: String = "",
    val expectedBehavior: String = "",
    val actualBehavior: String = "",
    val attachLogs: Boolean = true,
    val attachScreenshots: Boolean = false,
    val deviceInfo: DeviceInfo? = null,
    val appInfo: AppInfo? = null,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
) : UiState