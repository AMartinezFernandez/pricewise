package com.alvaro.pricewise.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PwColorScheme = lightColorScheme(
    // Naranja como accion principal
    primary = PwOrangeDark,
    onPrimary = Color.White,
    primaryContainer = PwOrangeDark,
    onPrimaryContainer = Color.White,

    // Cyan como secundario
    secondary = PwCyanDark,
    onSecondary = Color.White,
    secondaryContainer = PwCyanDark.copy(alpha = 0.15f),
    onSecondaryContainer = PwCyanDark,

    // Terciario
    tertiary = PwOrangeDark,
    onTertiary = Color.White,
    tertiaryContainer = PwOrangeDark.copy(alpha = 0.15f),
    onTertiaryContainer = PwOrangeDark,

    // Errores
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4EC),
    onErrorContainer = Color(0xFFB3261E),

    // Fondos y superficies — fondo claro para contenido
    background = PwLightBg,
    onBackground = PwDarkText,
    surface = Color.White,
    onSurface = PwDarkText,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = PwMutedText,

    // Contornos
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),

    // Inverse (para componentes que necesitan fondo oscuro)
    inverseSurface = PwDarkNavy,
    inverseOnSurface = Color.White,
    inversePrimary = PwOrangeLight
)

@Composable
fun PriceWiseTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PwColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
