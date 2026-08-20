package com.ancata.prima_focus.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class PremiumGlowColors(
    val primaryGlow: Color,
    val primaryAccent: Color,
    val coloredShadow: Color,
    val glassSurface: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val bottomNavBg: Color,
    val backgroundCenter: Color,
    val backgroundEdge: Color
)

val LocalPremiumGlows = staticCompositionLocalOf<PremiumGlowColors> {
    error("No PremiumGlowColors provided")
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryRose,
    secondary = RoseGlow,
    tertiary = AccentSage,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    error = ErrorRose
)

@Composable
fun PrimaFocusTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    
    val premiumGlows = PremiumGlowColors(
        primaryGlow = RoseGlow,
        primaryAccent = PrimaryRose,
        coloredShadow = RoseShadow,
        glassSurface = GlassSurface,
        glassBorderStart = GlassBorderStart,
        glassBorderEnd = GlassBorderEnd,
        bottomNavBg = BottomNavBackground,
        backgroundCenter = DeepPlumBackground,
        backgroundEdge = DarkBackground
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = premiumGlows.backgroundEdge.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalPremiumGlows provides premiumGlows,
            content = content
        )
    }
}