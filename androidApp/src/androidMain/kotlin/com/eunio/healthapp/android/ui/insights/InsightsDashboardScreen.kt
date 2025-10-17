package com.eunio.healthapp.android.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.theme.EunioColors
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.presentation.state.InsightsUiState
import com.eunio.healthapp.presentation.viewmodel.InsightsViewModel
import org.koin.compose.koinInject

/**
 * Insights dashboard screen that displays personalized health insights with dismissible cards.
 * Follows Eunio design system with card-based layout and generous white space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState(initial = "")
    
    // Show snackbar for messages
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            // In a real app, you'd use SnackbarHostState here
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Health Insights",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh insights"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EunioColors.Background,
                    titleContentColor = EunioColors.OnBackground
                )
            )
        },
        containerColor = EunioColors.Background
    ) { paddingValues ->
        InsightsDashboardContent(
            uiState = uiState,
            onDismissInsight = viewModel::dismissInsight,
            onMarkAsRead = viewModel::markInsightAsRead,
            onClearError = viewModel::clearError,
            modifier = modifier.padding(paddingValues)
        )
    }
}

@Composable
internal fun InsightsDashboardContent(
    uiState: InsightsUiState,
    onDismissInsight: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            LoadingState(modifier = modifier)
        }
        uiState.errorMessage != null -> {
            ErrorState(
                message = uiState.errorMessage!!,
                onRetry = onClearError,
                modifier = modifier
            )
        }
        !uiState.hasInsights -> {
            EmptyState(modifier = modifier)
        }
        else -> {
            InsightsContent(
                uiState = uiState,
                onDismissInsight = onDismissInsight,
                onMarkAsRead = onMarkAsRead,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = EunioColors.Primary
            )
            Text(
                text = "Loading your insights...",
                style = MaterialTheme.typography.bodyMedium,
                color = EunioColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Unable to load insights",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.Error,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = EunioColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EunioColors.Primary
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "No insights yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Keep logging your health data and we'll start generating personalized insights for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = EunioColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InsightsContent(
    uiState: InsightsUiState,
    onDismissInsight: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Unread insights section
        if (uiState.unreadInsights.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "New Insights",
                    subtitle = "${uiState.unreadCount} new"
                )
            }
            
            items(
                items = uiState.unreadInsights.filter { it.id !in uiState.dismissedInsightIds },
                key = { it.id }
            ) { insight ->
                InsightCard(
                    insight = insight,
                    isRead = false,
                    onDismiss = { onDismissInsight(insight.id) },
                    onMarkAsRead = { onMarkAsRead(insight.id) }
                )
            }
        }
        
        // Read insights section
        if (uiState.readInsights.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Previous Insights",
                    subtitle = "${uiState.readInsights.size} insights"
                )
            }
            
            items(
                items = uiState.readInsights.filter { it.id !in uiState.dismissedInsightIds },
                key = { it.id }
            ) { insight ->
                InsightCard(
                    insight = insight,
                    isRead = true,
                    onDismiss = { onDismissInsight(insight.id) },
                    onMarkAsRead = { /* Already read */ }
                )
            }
        }
        
        // Medical disclaimer footer
        item {
            MedicalDisclaimerFooter()
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = EunioColors.OnBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = EunioColors.OnSurfaceVariant
        )
    }
}

@Composable
private fun MedicalDisclaimerFooter(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EunioColors.Warning.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Medical Disclaimer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.Warning
            )
            Text(
                text = "These insights are for informational purposes only and should not replace professional medical advice. Always consult with your healthcare provider for medical concerns.",
                style = MaterialTheme.typography.bodySmall,
                color = EunioColors.OnSurfaceVariant
            )
        }
    }
}