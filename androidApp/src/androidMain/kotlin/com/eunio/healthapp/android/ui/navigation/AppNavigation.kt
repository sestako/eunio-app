package com.eunio.healthapp.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing the main navigation destinations in the app.
 */
sealed class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    object DailyLogging : AppDestination(
        route = "daily_logging",
        title = "Daily Log",
        icon = Icons.Default.Edit,
        contentDescription = "Daily logging screen"
    )
    
    object Calendar : AppDestination(
        route = "calendar",
        title = "Calendar",
        icon = Icons.Default.DateRange,
        contentDescription = "Calendar view"
    )
    
    object Insights : AppDestination(
        route = "insights",
        title = "Insights",
        icon = Icons.Default.Analytics,
        contentDescription = "Insights and analytics"
    )
    
    object Settings : AppDestination(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings,
        contentDescription = "Settings and preferences"
    )
    
    companion object {
        val bottomNavItems = listOf(
            DailyLogging,
            Calendar,
            Insights,
            Settings
        )
    }
}
