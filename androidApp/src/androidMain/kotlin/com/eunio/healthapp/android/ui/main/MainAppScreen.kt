package com.eunio.healthapp.android.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eunio.healthapp.android.ui.calendar.CalendarScreen
import com.eunio.healthapp.android.ui.components.OfflineBanner
import com.eunio.healthapp.android.ui.insights.InsightsScreen
import com.eunio.healthapp.android.ui.logging.DailyLoggingScreen
import com.eunio.healthapp.android.ui.navigation.AppDestination
import com.eunio.healthapp.android.ui.settings.SettingsScreen

/**
 * Main app screen with bottom navigation bar.
 * Manages navigation between Daily Logging, Calendar, Insights, and Settings screens.
 */
@Composable
fun MainAppScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.semantics {
                    contentDescription = "Main navigation bar"
                }
            ) {
                AppDestination.bottomNavItems.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route == destination.route 
                    } == true
                    
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null // Description is on the item
                            )
                        },
                        label = { Text(destination.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (selected) {
                                "${destination.title}, selected"
                            } else {
                                "Navigate to ${destination.title}"
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline banner at the top
            OfflineBanner()
            
            // Navigation host
            NavHost(
                navController = navController,
                startDestination = AppDestination.DailyLogging.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(AppDestination.DailyLogging.route) {
                    DailyLoggingScreen(
                        onNavigateBack = { /* No back action needed in bottom nav */ }
                    )
                }
                
                composable(AppDestination.Calendar.route) {
                    CalendarScreen()
                }
                
                composable(AppDestination.Insights.route) {
                    InsightsScreen()
                }
                
                composable(AppDestination.Settings.route) {
                    SettingsScreen(
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
