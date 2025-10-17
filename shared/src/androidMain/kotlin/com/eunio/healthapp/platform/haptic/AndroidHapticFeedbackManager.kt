package com.eunio.healthapp.platform.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.eunio.healthapp.domain.model.settings.HapticIntensity

/**
 * Android implementation of HapticFeedbackManager using Vibrator service.
 */
class AndroidHapticFeedbackManager(
    private val context: Context
) : HapticFeedbackManager {
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    override suspend fun performHapticFeedback(intensity: HapticIntensity) {
        when (intensity) {
            HapticIntensity.DISABLED -> return
            HapticIntensity.LIGHT -> performLightHaptic()
            HapticIntensity.MEDIUM -> performMediumHaptic()
            HapticIntensity.STRONG -> performStrongHaptic()
        }
    }
    
    override suspend fun performLightHaptic() {
        if (isSystemHapticEnabled() && isHapticFeedbackAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
    
    override suspend fun performMediumHaptic() {
        if (isSystemHapticEnabled() && isHapticFeedbackAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }
    
    override suspend fun performStrongHaptic() {
        if (isSystemHapticEnabled() && isHapticFeedbackAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }
    
    override suspend fun performSuccessHaptic() {
        if (isSystemHapticEnabled() && isHapticFeedbackAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 50, 50, 50)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        }
    }
    
    override suspend fun performErrorHaptic() {
        if (isSystemHapticEnabled() && isHapticFeedbackAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 100, 100, 100, 100, 100)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100), -1)
            }
        }
    }
    
    override suspend fun performWarningHaptic() {
        if (isSystemHapticEnabled() && isHapticFeedbackAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    }
    
    override fun isHapticFeedbackAvailable(): Boolean {
        return vibrator.hasVibrator()
    }
    
    override fun isSystemHapticEnabled(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 1
        } catch (e: Exception) {
            true // Default to enabled if we can't check
        }
    }
}