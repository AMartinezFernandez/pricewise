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

// ============================================================
// LIGHT — fondo claro, acentos oscuros y naranja/cyan vibrantes
// ============================================================
private val LightColorScheme = lightColorScheme(
    // Naranja como accion principal (botones, FAB, links)
    primary = PwOrangeDark,
    onPrimary = Color.White,
    primaryContainer = PwOrangeLight.copy(alpha = 0.3f),
    onPrimaryContainer = PwOrangeDark,

    // Cyan como secundario (badges, chips, indicadores)
    secondary = PwCyanDark,
    onSecondary = Color.White,
    secondaryContainer = PwCyan.copy(alpha = 0.25f),
    onSecondaryContainer = PwCyanDark,

    // Slate como terciario (elementos sutiles, iconos nav)
    tertiary = PwSlateGray,
    onTertiary = Color.White,
    tertiaryContainer = PwSlateGray.copy(alpha = 0.12f),
    onTertiaryContainer = PwDarkNavy,

    // Errores
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    // Fondos y superficies
    background = Color(0xFFF5F7FA),
    onBackground = PwDarkNavy,
    surface = Color.White,
    onSurface = PwDarkNavy,
    surfaceVariant = Color(0xFFE8ECF1),
    onSurfaceVariant = PwSlateGray,

    // Contornos
    outline = PwSlateGray.copy(alpha = 0.5f),
    outlineVariant = PwSlateGray.copy(alpha = 0.2f),

    // Barra inferior / navegacion
    inverseSurface = PwDarkNavy,
    inverseOnSurface = Color.White,
    inversePrimary = PwOrangeLight
)

// ============================================================
// DARK — fondo navy, acentos luminosos naranja/cyan
// ============================================================
private val DarkColorScheme = darkColorScheme(
    // Naranja claro como accion principal (contraste sobre fondo oscuro)
    primary = PwOrangeLight,
    onPrimary = PwDarkNavy,
    primaryContainer = PwOrangeDark.copy(alpha = 0.4f),
    onPrimaryContainer = PwOrangeLight,

    // Cyan luminoso como secundario
    secondary = PwCyan,
    onSecondary = PwDarkNavy,
    secondaryContainer = PwCyanDark.copy(alpha = 0.35f),
    onSecondaryContainer = PwCyan,

    // Cyan medio como terciario
    tertiary = PwCyanMedium,
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
    // Siempre oscuro: la app usa fondo DarkNavy en todas las pantallas
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar y navigation bar siempre oscuras (identidad de marca)
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
