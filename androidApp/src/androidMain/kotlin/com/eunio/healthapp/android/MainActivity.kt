package com.eunio.healthapp.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.eunio.healthapp.android.ui.onboarding.OnboardingFlow
import com.eunio.healthapp.android.ui.splash.SplashScreen
import com.eunio.healthapp.android.ui.theme.EunioTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Test logs to verify logging is working
        Log.v(TAG, "========================================")
        Log.v(TAG, "MainActivity onCreate - App Starting")
        Log.d(TAG, "DEBUG: Logging system is working!")
        Log.i(TAG, "INFO: App initialized successfully")
        Log.w(TAG, "WARN: This is a test warning")
        Log.e(TAG, "ERROR: This is a test error (not a real error)")
        Log.v(TAG, "========================================")
        Log.d(TAG, "Setting up UI content...")
        
        setContent {
            EunioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        SplashScreen(
                            onInitComplete = {
                                Log.d(TAG, "Splash screen initialization complete")
                                showSplash = false
                            }
                        )
                    } else {
                        OnboardingFlow()
                    }
                }
            }
        }
    }
}

