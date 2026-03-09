package com.alvaro.pricewise.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PwLightColorScheme = lightColorScheme(
    primary = PwOrangeDark,
    onPrimary = Color.White,
    primaryContainer = PwOrangeDark,
    onPrimaryContainer = Color.White,

    secondary = PwCyanDark,
    onSecondary = Color.White,
    secondaryContainer = PwCyanDark.copy(alpha = 0.15f),
    onSecondaryContainer = PwCyanDark,

    tertiary = PwOrangeDark,
    onTertiary = Color.White,
    tertiaryContainer = PwOrangeDark.copy(alpha = 0.15f),
    onTertiaryContainer = PwOrangeDark,

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4EC),
    onErrorContainer = Color(0xFFB3261E),

    background = PwLightBg,
    onBackground = PwDarkText,
    surface = Color.White,
    onSurface = PwDarkText,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = PwMutedText,

    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),

    inverseSurface = PwDarkNavy,
    inverseOnSurface = Color.White,
    inversePrimary = PwOrangeLight
)

private val PwDarkColorScheme = darkColorScheme(
    primary = PwOrangeLight,
    onPrimary = Color(0xFF1E1200),
    primaryContainer = PwOrangeDark,
    onPrimaryContainer = Color.White,

    secondary = PwCyan,
    onSecondary = Color(0xFF00232E),
    secondaryContainer = PwCyanDark,
    onSecondaryContainer = PwCyan,

    tertiary = PwOrangeLight,
    onTertiary = Color(0xFF1E1200),
    tertiaryContainer = PwOrangeDark,
    onTertiaryContainer = PwOrangeLight,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF121A1F),
    onBackground = Color(0xFFE1E3E5),
    surface = Color(0xFF1A2329),
    onSurface = Color(0xFFE1E3E5),
    surfaceVariant = PwSlateGray,
    onSurfaceVariant = Color(0xFFBFC8CE),

    outline = Color(0xFF596A73),
    outlineVariant = Color(0xFF3A4950),

    inverseSurface = Color(0xFFE1E3E5),
    inverseOnSurface = PwDarkNavy,
    inversePrimary = PwOrangeDark
)

@Composable
fun PriceWiseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PwDarkColorScheme else PwLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // En dark: iconos claros (false). En light: iconos claros (false) para PwDarkNavy TopAppBar.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
