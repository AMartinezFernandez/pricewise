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

private val DarkColorScheme = darkColorScheme(
    // Naranja como accion principal
    primary = PwOrangeDark,
    onPrimary = PwDarkNavy,
    primaryContainer = PwOrangeDark,
    onPrimaryContainer = PwOrangeLight,

    // Cyan luminoso como secundario
    secondary = PwCyan,
    onSecondary = PwDarkNavy,
    secondaryContainer = PwCyanDark.copy(alpha = 0.35f),
    onSecondaryContainer = PwCyan,

    // Cyan medio como terciario
    tertiary =  PwOrangeDark,
    onTertiary = PwDarkNavy,
    tertiaryContainer = PwCyanDark.copy(alpha = 0.25f),
    onTertiaryContainer = PwCyanMedium,

    // Errores
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // Fondos y superficies
    background = PwDarkNavy,
    onBackground = Color(0xFFECEFF1),
    surface = PwSlateGray,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A4A55),
    onSurfaceVariant = Color(0xFFCFD8DC),

    // Contornos (claros para que los campos de texto se vean bien)
    outline = Color(0xFF90A4AE),
    outlineVariant = Color(0xFF607D8B),

    // Barra inferior / navegacion
    inverseSurface = Color(0xFFE1E3E6),
    inverseOnSurface = PwDarkNavy,
    inversePrimary = PwOrangeDark
)

@Composable
fun PriceWiseTheme(
    // Siempre oscuro
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar y navigation bar siempre oscuras
            window.statusBarColor = PwDarkNavy.toArgb()
            window.navigationBarColor = if (darkTheme) PwDarkNavy.toArgb() else PwSlateGray.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
