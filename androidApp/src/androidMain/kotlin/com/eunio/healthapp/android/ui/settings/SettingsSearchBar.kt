@file:OptIn(ExperimentalMaterial3Api::class)

package com.eunio.healthapp.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
// import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Search bar component for filtering settings with real-time search functionality.
 * 
 * @param query Current search query
 * @param onQueryChange Callback when search query changes
 * @param onClearSearch Callback to clear the search
 * @param enabled Whether the search bar is enabled
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .testTag("settings_search_bar"),
        enabled = enabled,
        placeholder = {
            Text(
                text = "Search settings...",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onClearSearch()
                        // keyboardController?.hide()
                    },
                    enabled = enabled,
                    modifier = Modifier.testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                // keyboardController?.hide()
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        shape = MaterialTheme.shapes.medium
    )
}

/**
 * Compact version of the search bar for smaller screens or specific layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.testTag("compact_settings_search_bar"),
        enabled = enabled,
        placeholder = {
            Text(
                text = "Search...",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearSearch,
                    enabled = enabled,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
        )
    )
}

/**
 * Search bar with suggestions dropdown (for future enhancement).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchBarWithSuggestions(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showSuggestions by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        SettingsSearchBar(
            query = query,
            onQueryChange = { newQuery ->
                onQueryChange(newQuery)
                showSuggestions = newQuery.isNotEmpty() && suggestions.isNotEmpty()
            },
            onClearSearch = {
                onClearSearch()
                showSuggestions = false
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Suggestions dropdown (placeholder for future implementation)
        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    suggestions.take(5).forEach { suggestion ->
                        TextButton(
                            onClick = {
                                onSuggestionClick(suggestion)
                                showSuggestions = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}