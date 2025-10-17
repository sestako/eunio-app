package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunio.healthapp.android.ui.theme.EunioColors

/**
 * Welcome screen for onboarding flow.
 */
@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Logo and branding section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            // App icon/logo placeholder
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Eunio Logo",
                modifier = Modifier.size(80.dp),
                tint = EunioColors.Primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Welcome to Eunio",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = EunioColors.OnBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your personal health companion for understanding your body and tracking your wellness journey.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = EunioColors.OnBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // Features section
        Column(
            modifier = Modifier.padding(vertical = 32.dp)
        ) {
            FeatureItem(
                title = "Smart Tracking",
                description = "Log your daily health data with intelligent insights"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureItem(
                title = "Cycle Prediction",
                description = "Understand your patterns with personalized predictions"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureItem(
                title = "Privacy First",
                description = "Your data is encrypted and completely private"
            )
        }
        
        // Get started button
        Button(
            onClick = onNext,
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
                    text = "Get Started",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(2.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawCircle(
                        color = EunioColors.Primary,
                        radius = size.minDimension / 2
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = EunioColors.OnBackground
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = EunioColors.OnBackground.copy(alpha = 0.7f)
            )
        }
    }
}