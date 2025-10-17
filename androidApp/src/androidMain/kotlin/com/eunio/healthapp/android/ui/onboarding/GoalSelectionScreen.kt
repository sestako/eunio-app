package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunio.healthapp.android.ui.theme.EunioColors
import com.eunio.healthapp.domain.model.HealthGoal

/**
 * Goal selection screen for onboarding flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSelectionScreen(
    selectedGoal: HealthGoal?,
    onGoalSelected: (HealthGoal) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button
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
                text = "Step 1 of 2",
                style = MaterialTheme.typography.bodyMedium,
                color = EunioColors.OnBackground.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title and description
        Text(
            text = "What's your primary goal?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = EunioColors.OnBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This helps us personalize your experience and provide relevant insights.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            color = EunioColors.OnBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Goal options
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoalOption(
                goal = HealthGoal.CONCEPTION,
                title = "Trying to Conceive",
                description = "Track fertility signs and optimize conception timing",
                isSelected = selectedGoal == HealthGoal.CONCEPTION,
                onSelected = { onGoalSelected(HealthGoal.CONCEPTION) }
            )
            
            GoalOption(
                goal = HealthGoal.CONTRACEPTION,
                title = "Natural Contraception",
                description = "Use fertility awareness for natural birth control",
                isSelected = selectedGoal == HealthGoal.CONTRACEPTION,
                onSelected = { onGoalSelected(HealthGoal.CONTRACEPTION) }
            )
            
            GoalOption(
                goal = HealthGoal.CYCLE_TRACKING,
                title = "Cycle Tracking",
                description = "Understand your menstrual cycle patterns",
                isSelected = selectedGoal == HealthGoal.CYCLE_TRACKING,
                onSelected = { onGoalSelected(HealthGoal.CYCLE_TRACKING) }
            )
            
            GoalOption(
                goal = HealthGoal.GENERAL_HEALTH,
                title = "General Health",
                description = "Monitor overall wellness and health patterns",
                isSelected = selectedGoal == HealthGoal.GENERAL_HEALTH,
                onSelected = { onGoalSelected(HealthGoal.GENERAL_HEALTH) }
            )
        }
        
        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Continue button
        Button(
            onClick = onNext,
            enabled = !isLoading && selectedGoal != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EunioColors.Primary,
                contentColor = EunioColors.OnPrimary,
                disabledContainerColor = EunioColors.Primary.copy(alpha = 0.3f)
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
                    text = "Continue",
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
private fun GoalOption(
    goal: HealthGoal,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                EunioColors.Primary.copy(alpha = 0.1f)
            } else {
                EunioColors.Surface
            }
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) {
                EunioColors.Primary
            } else {
                EunioColors.OnSurface.copy(alpha = 0.12f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = EunioColors.OnSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EunioColors.OnSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = EunioColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}