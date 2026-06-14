package com.primafocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = BackgroundColor,
    secondary = AccentGreen,
    onSecondary = TextPrimary,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = BackgroundColor
)

@Composable
fun PrimaFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        // typography = Typography, // Assume default Roboto is configured properly
        content = content
    )
}
