package com.alvaro.pricewise.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),           // Azul corporativo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEAFF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF00695C),         // Verde para métricas positivas
    onSecondary = Color.White,
    tertiary = Color(0xFFE65100),          // Naranja para alertas WARNING
    error = Color(0xFFB00020),
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2F7),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF5A6172)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003064),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD3E3FF),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF0E1015),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF222529)
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
