package com.eunio.healthapp.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.presentation.state.SettingsUiState
import com.eunio.healthapp.presentation.viewmodel.SettingsViewModel

/**
 * Enhanced settings screen with search functionality and organized sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    onNavigateToSubSetting: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel? = null
) {
    // For now, create a default state since ViewModel injection is not set up
    val uiState = SettingsUiState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        modifier = Modifier.testTag("settings_title")
                    ) 
                }
            )
        },
        modifier = modifier.testTag("enhanced_settings_screen")
    ) { paddingValues ->
        EnhancedSettingsContent(
            uiState = uiState,
            onSearchQueryChange = { /* TODO: Implement */ },
            onClearSearch = { /* TODO: Implement */ },
            onNavigateToSetting = onNavigateToSubSetting,
            onRefresh = { /* TODO: Implement */ },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

/**
 * Main content for the enhanced settings screen.
 */
@Composable
internal fun EnhancedSettingsContent(
    uiState: SettingsUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSetting: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar
        SettingsSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            enabled = uiState.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        when {
            uiState.isLoading -> {
                LoadingContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            uiState.errorMessage != null -> {
                ErrorContent(
                    message = uiState.errorMessage ?: "Unknown error",
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            uiState.hasSettings -> {
                SettingsContent(
                    uiState = uiState,
                    onNavigateToSetting = onNavigateToSetting,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                EmptyContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Main settings content with sections and items.
 */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onNavigateToSetting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("settings_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // User profile section (if settings are loaded)
        uiState.settings?.let { settings ->
            item {
                UserProfileSection(
                    userSettings = settings,
                    onEditProfile = { onNavigateToSetting("profile") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Settings sections
        items(
            items = uiState.filteredSections,
            key = { it.id }
        ) { section ->
            SettingsSection(
                section = section,
                onItemClick = onNavigateToSetting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Loading state content.
 */
@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("loading_content"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading settings...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state content.
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("error_content"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag("retry_button")
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Empty state content.
 */
@Composable
private fun EmptyContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("empty_content"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "No settings available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}