package com.eunio.healthapp.presentation.navigation

import kotlinx.datetime.LocalDate

/**
 * Navigation destinations in the app.
 */
sealed class NavigationDestination {
    object Onboarding : NavigationDestination()
    object DailyLogging : NavigationDestination()
    data class DailyLoggingWithDate(val date: LocalDate) : NavigationDestination()
    object Calendar : NavigationDestination()
    object Insights : NavigationDestination()
    object Settings : NavigationDestination()
    data class BBTChart(val cycleId: String? = null) : NavigationDestination()
}

/**
 * Navigation events that can be triggered from ViewModels.
 */
sealed class NavigationEvent {
    data class NavigateTo(val destination: NavigationDestination) : NavigationEvent()
    object NavigateBack : NavigationEvent()
    data class NavigateToWithClearStack(val destination: NavigationDestination) : NavigationEvent()
}

/**
 * Navigation state for managing app navigation.
 */
data class NavigationState(
    val currentDestination: NavigationDestination = NavigationDestination.Onboarding,
    val canNavigateBack: Boolean = false,
    val navigationStack: List<NavigationDestination> = emptyList()
)