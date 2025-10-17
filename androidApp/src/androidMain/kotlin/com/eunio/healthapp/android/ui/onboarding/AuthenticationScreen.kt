package com.eunio.healthapp.android.ui.onboarding

import androidx.compose.runtime.*
import com.eunio.healthapp.android.ui.auth.AuthViewModel
import com.eunio.healthapp.android.ui.auth.SignInScreen
import com.eunio.healthapp.android.ui.auth.SignUpScreen
import com.eunio.healthapp.android.ui.auth.ForgotPasswordScreen
import com.eunio.healthapp.auth.AuthService
import org.koin.compose.koinInject

/**
 * Authentication screen coordinator that manages sign in, sign up, and password reset flows.
 */
@Composable
fun AuthenticationScreen(
    onSignInSuccess: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val authService: AuthService = koinInject()
    val viewModel = remember { AuthViewModel(authService) }
    var currentScreen by remember { mutableStateOf(AuthScreen.SignIn) }

    
    when (currentScreen) {
        AuthScreen.SignIn -> {
            SignInScreen(
                viewModel = viewModel,
                onSignInSuccess = onSignInSuccess,
                onNavigateToSignUp = { currentScreen = AuthScreen.SignUp },
                onNavigateToForgotPassword = { currentScreen = AuthScreen.ForgotPassword }
            )
        }
        AuthScreen.SignUp -> {
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = onSignUpSuccess,
                onNavigateToSignIn = { currentScreen = AuthScreen.SignIn }
            )
        }
        AuthScreen.ForgotPassword -> {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AuthScreen.SignIn }
            )
        }
    }
}

/**
 * Authentication screen types
 */
private enum class AuthScreen {
    SignIn,
    SignUp,
    ForgotPassword
}