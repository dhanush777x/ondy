package com.ondy.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object Catppuccin {
    val Accent = Color(0xFFB4BEFE)
    val AccentDark = Color(0xFFCBA6F7)
    
    val MochaBase = Color(0xFF1E1E2E)
    val MochaMantle = Color(0xFF181825)
    val MochaText = Color(0xFFCDD6F4)
    val MochaSubtext = Color(0xFFA6ADC8)
    val MochaSurface = Color(0xFF313244)
    val MochaSurfaceVariant = Color(0xFF45475A)
    val MochaOverlay = Color(0xFF6C7086)
    val MochaRed = Color(0xFFF38BA8)
    val MochaPeach = Color(0xFFFAB387)
    
    val LightBase = Color(0xFFEFF1F5)
    val LightSurface = Color(0xFFE6E9EF)
    val LightText = Color(0xFF4C4F69)
    val LightSubtext = Color(0xFF5C5F77)
    val LightOverlay = Color(0xFF7C7F8F)
    val LightRed = Color(0xFFD20F39)
    val LightPeach = Color(0xFFFE640B)
}

private val DarkColorScheme = darkColorScheme(
    primary = Catppuccin.Accent,
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Catppuccin.MochaSurfaceVariant,
    onPrimaryContainer = Catppuccin.MochaText,
    secondary = Catppuccin.AccentDark,
    onSecondary = Catppuccin.MochaMantle,
    tertiary = Catppuccin.Accent,
    background = Catppuccin.MochaBase,
    onBackground = Catppuccin.MochaText,
    surface = Catppuccin.MochaBase,
    onSurface = Catppuccin.MochaText,
    surfaceVariant = Catppuccin.MochaSurface,
    onSurfaceVariant = Catppuccin.MochaSubtext,
    outline = Catppuccin.MochaOverlay,
    error = Catppuccin.MochaRed,
    onError = Catppuccin.MochaMantle,
    errorContainer = Catppuccin.MochaPeach,
    onErrorContainer = Catppuccin.MochaText
)

private val LightColorScheme = lightColorScheme(
    primary = Catppuccin.Accent,
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Catppuccin.LightSurface,
    onPrimaryContainer = Catppuccin.LightText,
    secondary = Catppuccin.LightOverlay,
    onSecondary = Color.White,
    tertiary = Catppuccin.Accent,
    background = Catppuccin.LightBase,
    onBackground = Catppuccin.LightText,
    surface = Catppuccin.LightBase,
    onSurface = Catppuccin.LightText,
    surfaceVariant = Catppuccin.LightSurface,
    onSurfaceVariant = Catppuccin.LightSubtext,
    outline = Catppuccin.LightOverlay,
    error = Catppuccin.LightRed,
    onError = Color.White,
    errorContainer = Catppuccin.LightPeach,
    onErrorContainer = Catppuccin.LightText
)

@Composable
fun OndyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
