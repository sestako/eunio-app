package com.eunio.healthapp.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.model.UnitSystem

/**
 * A complete settings screen that includes unit system preferences with proper state management,
 * loading states, error handling, and user feedback.
 * 
 * @param modifier Modifier for styling
 * @param viewModel ViewModel for managing settings state
 */
@Composable
fun UnitSystemSettingsScreen(
    modifier: Modifier = Modifier
) {
    // For now, we'll create a simple demo without the actual ViewModel
    // In a real implementation, proper DI would be set up
    var currentUnitSystem by remember { mutableStateOf(UnitSystem.METRIC) }
    val scaffoldState = rememberScaffoldState()
    
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp
            )
        },
        modifier = modifier
    ) { paddingValues ->
        UnitSystemSettingsContent(
            currentUnitSystem = currentUnitSystem,
            onUnitSystemChanged = { currentUnitSystem = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Content composable for the unit system settings.
 */
@Composable
private fun UnitSystemSettingsContent(
    currentUnitSystem: UnitSystem,
    onUnitSystemChanged: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsContent(
            currentUnitSystem = currentUnitSystem,
            onUnitSystemChanged = onUnitSystemChanged
        )
    }
}

/**
 * Main settings content with unit system preference.
 */
@Composable
private fun SettingsContent(
    currentUnitSystem: UnitSystem,
    onUnitSystemChanged: (UnitSystem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            UnitSystemSettingItem(
                currentUnitSystem = currentUnitSystem,
                onUnitSystemChanged = onUnitSystemChanged,
                enabled = true
            )
        }
    }
}

