package com.ondy.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

object OndyColors {
    // Fallback palette (Catppuccin-inspired) used before Android 12 / when
    // dynamic color is unavailable.
    val Primary = Color(0xFFB4BEFE)
    val PrimaryContainer = Color(0xFF313244)

    // Light-mode accent. The brand lavender is too pale to read on the light
    // surface, so light mode uses a deeper periwinkle with white content.
    val PrimaryLight = Color(0xFF5A5FD6)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val PrimaryContainerLight = Color(0xFFE3E2FF)
    val OnPrimaryContainerLight = Color(0xFF17194A)

    val SecondaryLight = Color(0xFF5C5B74)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val SecondaryContainerLight = Color(0xFFE6E1F0)
    val OnSecondaryContainerLight = Color(0xFF191826)

    val TertiaryLight = Color(0xFF7B5265)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val TertiaryContainerLight = Color(0xFFFFD8E8)
    val OnTertiaryContainerLight = Color(0xFF301221)

    val ErrorLight = Color(0xFFBA1A1A)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val OnErrorContainerLight = Color(0xFF410002)

    val Secondary = Color(0xFFFAB387)
    val SecondaryContainer = Color(0xFF45475A)

    val Tertiary = Color(0xFFF38BA8)
    val TertiaryContainer = Color(0xFF45475A)

    val Error = Color(0xFFF38BA8)
    val ErrorContainer = Color(0xFF45475A)

    val Background = Color(0xFF1E1E2E)
    val BackgroundLight = Color(0xFFEFF1F5)
    val Surface = Color(0xFF1E1E2E)
    val SurfaceLight = Color(0xFFEFF1F5)
    val SurfaceVariant = Color(0xFF313244)
    val SurfaceVariantLight = Color(0xFFE6E9EF)

    val OnPrimary = Color(0xFF1E1E2E)
    val OnSecondary = Color(0xFF1E1E2E)
    val OnSurface = Color(0xFFCDD6F4)
    val OnSurfaceLight = Color(0xFF4C4F69)
    val OnBackground = Color(0xFFCDD6F4)
    val OnBackgroundLight = Color(0xFF4C4F69)
    val OnSurfaceVariant = Color(0xFFA6ADC8)
    val OnSurfaceVariantLight = Color(0xFF5C5F77)
    val Outline = Color(0xFF6C7086)
    val OutlineLight = Color(0xFF7C7F8F)
}

private val DarkColorScheme = darkColorScheme(
    primary = OndyColors.Primary,
    onPrimary = OndyColors.OnPrimary,
    primaryContainer = OndyColors.PrimaryContainer,
    onPrimaryContainer = OndyColors.OnSurface,
    secondary = OndyColors.Secondary,
    onSecondary = OndyColors.OnSecondary,
    secondaryContainer = OndyColors.SecondaryContainer,
    onSecondaryContainer = OndyColors.OnSurface,
    tertiary = OndyColors.Tertiary,
    onTertiary = OndyColors.OnPrimary,
    tertiaryContainer = OndyColors.TertiaryContainer,
    onTertiaryContainer = OndyColors.OnSurface,
    error = OndyColors.Error,
    onError = OndyColors.OnPrimary,
    errorContainer = OndyColors.ErrorContainer,
    onErrorContainer = OndyColors.OnSurface,
    background = OndyColors.Background,
    onBackground = OndyColors.OnBackground,
    surface = OndyColors.Surface,
    onSurface = OndyColors.OnSurface,
    surfaceVariant = OndyColors.SurfaceVariant,
    onSurfaceVariant = OndyColors.OnSurfaceVariant,
    outline = OndyColors.Outline,
    outlineVariant = OndyColors.Outline.copy(alpha = 0.5f),
    scrim = Color.Black,
    inverseSurface = OndyColors.BackgroundLight,
    inverseOnSurface = OndyColors.OnBackgroundLight,
    inversePrimary = OndyColors.PrimaryLight
)

private val LightColorScheme = lightColorScheme(
    primary = OndyColors.PrimaryLight,
    onPrimary = OndyColors.OnPrimaryLight,
    primaryContainer = OndyColors.PrimaryContainerLight,
    onPrimaryContainer = OndyColors.OnPrimaryContainerLight,
    secondary = OndyColors.SecondaryLight,
    onSecondary = OndyColors.OnSecondaryLight,
    secondaryContainer = OndyColors.SecondaryContainerLight,
    onSecondaryContainer = OndyColors.OnSecondaryContainerLight,
    tertiary = OndyColors.TertiaryLight,
    onTertiary = OndyColors.OnTertiaryLight,
    tertiaryContainer = OndyColors.TertiaryContainerLight,
    onTertiaryContainer = OndyColors.OnTertiaryContainerLight,
    error = OndyColors.ErrorLight,
    onError = OndyColors.OnErrorLight,
    errorContainer = OndyColors.ErrorContainerLight,
    onErrorContainer = OndyColors.OnErrorContainerLight,
    background = OndyColors.BackgroundLight,
    onBackground = OndyColors.OnBackgroundLight,
    surface = OndyColors.SurfaceLight,
    onSurface = OndyColors.OnSurfaceLight,
    surfaceVariant = OndyColors.SurfaceVariantLight,
    onSurfaceVariant = OndyColors.OnSurfaceVariantLight,
    outline = OndyColors.OutlineLight,
    outlineVariant = OndyColors.OutlineLight.copy(alpha = 0.5f),
    scrim = Color.Black,
    inverseSurface = OndyColors.Background,
    inverseOnSurface = OndyColors.OnBackground,
    inversePrimary = OndyColors.Primary
)

val OndyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val OndyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun OndyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * When true and the device runs Android 12+, use the wallpaper-derived
     * Material You palette. Forced light/dark modes pass false so the built-in
     * Ondy palette is used consistently.
     */
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Dynamic color (Material You) on Android 12+, custom palette elsewhere.
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OndyTypography,
        shapes = OndyShapes,
        content = content
    )
}
