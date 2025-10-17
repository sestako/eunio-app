package com.eunio.healthapp.domain.model.support

import kotlinx.serialization.Serializable

@Serializable
data class Tutorial(
    val id: String,
    val title: String,
    val description: String,
    val category: TutorialCategory,
    val steps: List<TutorialStep>,
    val estimatedDuration: Int, // in minutes
    val isCompleted: Boolean = false
)

@Serializable
data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val actionText: String? = null,
    val isCompleted: Boolean = false
)

enum class TutorialCategory(val displayName: String) {
    ONBOARDING("Getting Started"),
    CYCLE_TRACKING("Cycle Tracking"),
    DAILY_LOGGING("Daily Logging"),
    INSIGHTS("Understanding Insights"),
    SETTINGS("App Settings"),
    ADVANCED_FEATURES("Advanced Features")
}