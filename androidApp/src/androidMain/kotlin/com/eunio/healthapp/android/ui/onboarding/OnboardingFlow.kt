package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.eunio.healthapp.android.ui.main.MainScreen
import com.eunio.healthapp.android.ui.theme.EunioTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Main onboarding flow that manages authentication and onboarding screens.
 * Automatically checks if user is already authenticated and skips auth screens.
 */
@Composable
fun OnboardingFlow() {
    // Check if user is already authenticated
    val auth = FirebaseAuth.getInstance()
    var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
    var showMainApp by remember { mutableStateOf(auth.currentUser != null) }
    
    when {
        showMainApp -> {
            com.eunio.healthapp.android.ui.main.MainAppScreen(
                onSignOut = {
                    auth.signOut()
                    isAuthenticated = false
                    showMainApp = false
                }
            )
        }
        isAuthenticated -> {
            // User is authenticated but hasn't completed onboarding
            OnboardingScreen(
                onNavigateToMain = { showMainApp = true }
            )
        }
        else -> {
            // User needs to sign in or sign up
            AuthenticationScreen(
                onSignInSuccess = { 
                    isAuthenticated = true
                    showMainApp = true
                },
                onSignUpSuccess = { 
                    isAuthenticated = true
                    showMainApp = true
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingFlowPreview() {
    EunioTheme {
        OnboardingFlow()
    }
}