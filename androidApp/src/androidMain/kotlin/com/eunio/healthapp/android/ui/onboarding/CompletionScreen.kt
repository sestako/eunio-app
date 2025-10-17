package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunio.healthapp.android.ui.theme.EunioColors

/**
 * Completion screen for onboarding flow.
 */
@Composable
fun CompletionScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    isCompleted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button (only show if not completed)
        if (!isCompleted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = EunioColors.OnBackground
                    )
                }
                
                Text(
                    text = "Step 2 of 2",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EunioColors.OnBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(60.dp))
        }
        
        // Content
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(80.dp),
                tint = if (isCompleted) EunioColors.Success else EunioColors.Primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = if (isCompleted) "Welcome to Eunio!" else "Ready to get started?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = EunioColors.OnBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = if (isCompleted) {
                    "Your account is set up and ready to go. Start tracking your health journey today!"
                } else {
                    "You're all set! Let's complete your setup and start your personalized health tracking journey."
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = EunioColors.OnBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Setup summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = EunioColors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "What happens next:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = EunioColors.OnSurface
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SetupStep(
                            step = "1",
                            title = "Start logging",
                            description = "Begin tracking your daily health data"
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SetupStep(
                            step = "2",
                            title = "Get insights",
                            description = "Receive personalized patterns and predictions"
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SetupStep(
                            step = "3",
                            title = "Track progress",
                            description = "Monitor your health journey over time"
                        )
                    }
                }
            }
        }
        
        // Complete button (only show if not completed)
        if (!isCompleted) {
            Button(
                onClick = onComplete,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EunioColors.Primary,
                    contentColor = EunioColors.OnPrimary
                ),
                shape = MaterialTheme.shapes.large
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = EunioColors.OnPrimary
                    )
                } else {
                    Text(
                        text = "Complete Setup",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SetupStep(
    step: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircle(
                    color = EunioColors.Primary,
                    radius = size.minDimension / 2
                )
            }
            
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = EunioColors.OnPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = EunioColors.OnSurface
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = EunioColors.OnSurface.copy(alpha = 0.7f)
            )
        }
    }
}