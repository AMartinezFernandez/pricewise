package com.alvaro.pricewise.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PwOrange,
    onPrimary = Color.White,
    primaryContainer = PwOrangeLight,
    onPrimaryContainer = PwDarkNavy,
    secondary = PwCyanMedium,
    onSecondary = Color.White,
    secondaryContainer = PwCyan,
    onSecondaryContainer = PwDarkNavy,
    tertiary = PwCyanDark,
    onTertiary = Color.White,
    error = Color(0xFFB00020),
    background = Color(0xFFF8F9FA),
    onBackground = PwDarkNavy,
    surface = Color.White,
    onSurface = PwDarkNavy,
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = PwSlateGray
)

private val DarkColorScheme = darkColorScheme(
    primary = PwOrange,
    onPrimary = PwDarkNavy,
    primaryContainer = PwOrangeDark,
    onPrimaryContainer = PwOrangeLight,
    secondary = PwCyan,
    onSecondary = PwDarkNavy,
    secondaryContainer = PwCyanDark,
    onSecondaryContainer = PwCyan,
    tertiary = PwCyanMedium,
    onTertiary = PwDarkNavy,
    error = Color(0xFFCF6679),
    background = PwDarkNavy,
    onBackground = Color.White,
    surface = PwSlateGray,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF354350),
    onSurfaceVariant = Color(0xFFB0BEC5)
)

@Composable
fun PriceWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PwDarkNavy.toArgb()
            window.navigationBarColor = PwDarkNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
