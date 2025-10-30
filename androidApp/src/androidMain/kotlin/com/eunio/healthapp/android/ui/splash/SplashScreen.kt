package com.eunio.healthapp.android.ui.splash

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun SplashScreen(
    onInitComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    // Pulsing animation for logo
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Rotating animation for loading indicator
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    LaunchedEffect(Unit) {
        scope.launch {
            // Perform initialization silently in background
            performInitialization()
            
            // Wait a bit longer to ensure everything is loaded
            delay(1500)
            onInitComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A).copy(alpha = 0.9f + animatedOffset * 0.1f),
                        Color(0xFF3B82F6).copy(alpha = 0.8f + animatedOffset * 0.2f),
                        Color(0xFF60A5FA).copy(alpha = 0.7f + animatedOffset * 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo with pulse animation
            Surface(
                modifier = Modifier
                    .size((100 * pulseScale).dp),
                shape = MaterialTheme.shapes.large,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "❤️",
                        fontSize = (56 * pulseScale).sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Eunio Health",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}

private suspend fun performInitialization() {
    try {
        // Perform all initialization silently
        delay(300)
        
        // Initialize Koin
        try {
            GlobalContext.get()
            Log.d("SplashScreen", "Koin initialized")
        } catch (e: Exception) {
            Log.e("SplashScreen", "Koin initialization failed", e)
        }
        
        delay(300)
        
        // Check Firebase Auth
        try {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            Log.d("SplashScreen", "Firebase Auth: ${if (currentUser != null) "User signed in" else "No user"}")
        } catch (e: Exception) {
            Log.e("SplashScreen", "Firebase Auth check failed", e)
        }
        
        delay(300)
        
        // Check Firestore
        try {
            val firestore = Firebase.firestore
            Log.d("SplashScreen", "Firestore initialized")
        } catch (e: Exception) {
            Log.e("SplashScreen", "Firestore check failed", e)
        }
        
        delay(300)
        
        Log.d("SplashScreen", "Initialization complete")
        
    } catch (e: Exception) {
        Log.e("SplashScreen", "Initialization error", e)
    }
}
