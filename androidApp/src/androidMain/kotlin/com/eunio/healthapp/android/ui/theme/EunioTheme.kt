package com.eunio.healthapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Eunio Material 3 theme implementation with WCAG AA compliant colors.
 * All color combinations meet minimum 4.5:1 contrast ratio for normal text
 * and 3:1 for large text and UI components.
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors - main brand colors
    primary = EunioColors.Primary,
    onPrimary = Color.White, // Ensures high contrast
    primaryContainer = EunioColors.Primary.copy(alpha = 0.12f), // Subtle container
    onPrimaryContainer = EunioColors.Primary,
    
    // Secondary colors - supporting brand colors
    secondary = EunioColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = EunioColors.Secondary.copy(alpha = 0.12f),
    onSecondaryContainer = EunioColors.Secondary,
    
    // Tertiary colors - accent colors
    tertiary = EunioColors.Info,
    onTertiary = Color.White,
    tertiaryContainer = EunioColors.Info.copy(alpha = 0.12f),
    onTertiaryContainer = EunioColors.Info,
    
    // Surface colors - backgrounds and containers
    background = EunioColors.Background,
    onBackground = EunioColors.OnBackground,
    surface = EunioColors.Surface,
    onSurface = EunioColors.OnSurface,
    surfaceVariant = EunioColors.SurfaceVariant,
    onSurfaceVariant = EunioColors.OnSurfaceVariant,
    
    // Surface tints for elevated surfaces
    surfaceTint = EunioColors.Primary,
    
    // Error colors - high contrast for accessibility
    error = EunioColors.Error,
    onError = Color.White,
    errorContainer = EunioColors.Error.copy(alpha = 0.12f),
    onErrorContainer = EunioColors.Error,
    
    // Outline colors - borders and dividers
    outline = EunioColors.Gray300,
    outlineVariant = EunioColors.Gray200,
    
    // Utility colors
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = EunioColors.Gray800,
    inverseOnSurface = EunioColors.Gray100,
    inversePrimary = EunioColors.Primary.copy(alpha = 0.8f)
)

private val DarkColorScheme = darkColorScheme(
    // Primary colors - adjusted for dark theme accessibility
    primary = EunioColors.Primary.copy(alpha = 0.87f), // Higher alpha for better visibility
    onPrimary = Color.Black,
    primaryContainer = EunioColors.Primary.copy(alpha = 0.24f),
    onPrimaryContainer = EunioColors.Primary.copy(alpha = 0.87f),
    
    // Secondary colors
    secondary = EunioColors.Secondary.copy(alpha = 0.87f),
    onSecondary = Color.Black,
    secondaryContainer = EunioColors.Secondary.copy(alpha = 0.24f),
    onSecondaryContainer = EunioColors.Secondary.copy(alpha = 0.87f),
    
    // Tertiary colors
    tertiary = EunioColors.Info.copy(alpha = 0.87f),
    onTertiary = Color.Black,
    tertiaryContainer = EunioColors.Info.copy(alpha = 0.24f),
    onTertiaryContainer = EunioColors.Info.copy(alpha = 0.87f),
    
    // Surface colors - dark theme backgrounds
    background = EunioColors.Gray900,
    onBackground = EunioColors.Gray100,
    surface = EunioColors.Gray800,
    onSurface = EunioColors.Gray100,
    surfaceVariant = EunioColors.Gray700,
    onSurfaceVariant = EunioColors.Gray300,
    
    // Surface tints for dark theme
    surfaceTint = EunioColors.Primary,
    
    // Error colors - high contrast for dark theme
    error = EunioColors.Error.copy(alpha = 0.87f),
    onError = Color.Black,
    errorContainer = EunioColors.Error.copy(alpha = 0.24f),
    onErrorContainer = EunioColors.Error.copy(alpha = 0.87f),
    
    // Outline colors for dark theme
    outline = EunioColors.Gray600,
    outlineVariant = EunioColors.Gray700,
    
    // Utility colors
    scrim = Color.Black.copy(alpha = 0.32f),
    inverseSurface = EunioColors.Gray100,
    inverseOnSurface = EunioColors.Gray800,
    inversePrimary = EunioColors.Primary
)

@Composable
fun EunioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = EunioTypography,
        shapes = EunioShapes,
        content = content
    )
}