package com.ancata.prima_focus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ancata.prima_focus.data.prefs.ThemeMode
import com.ancata.prima_focus.data.prefs.ThemeSettings

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

private val LightColorScheme = lightColorScheme(
    primary = RoseAccent,
    secondary = PrimaryRose,
    tertiary = AccentSageDeep,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextMuted,
    error = ErrorRoseDeep
)

private val DarkGlows = PremiumGlowColors(
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

private val LightGlows = PremiumGlowColors(
    primaryGlow = PrimaryRose,
    primaryAccent = RoseAccent,
    coloredShadow = LightRoseShadow,
    glassSurface = LightGlassSurface,
    glassBorderStart = LightGlassBorderStart,
    glassBorderEnd = LightGlassBorderEnd,
    bottomNavBg = LightBottomNavBackground,
    backgroundCenter = LightSurface,
    backgroundEdge = LightBackground
)

/** Material You keeps the glass look but takes its accents and backgrounds from the wallpaper palette. */
private fun PremiumGlowColors.withDynamicScheme(scheme: ColorScheme) = copy(
    primaryGlow = scheme.primary,
    primaryAccent = scheme.primary,
    glassBorderStart = scheme.primary.copy(alpha = 0.2f),
    glassBorderEnd = scheme.primary.copy(alpha = 0.08f),
    backgroundCenter = scheme.surface,
    backgroundEdge = scheme.background
)

@Composable
fun PrimaFocusTheme(
    themeSettings: ThemeSettings = ThemeSettings(ThemeMode.SYSTEM, dynamicColor = false),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSettings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val useDynamicColor = themeSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val colorScheme = when {
        useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val baseGlows = if (darkTheme) DarkGlows else LightGlows
    val premiumGlows = if (useDynamicColor) baseGlows.withDynamicScheme(colorScheme) else baseGlows

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = premiumGlows.backgroundEdge.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
