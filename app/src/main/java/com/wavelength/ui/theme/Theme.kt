package com.wavelength.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WavelengthOrange = Color(0xFFFF6B35)
private val WavelengthOrangeLight = Color(0xFFFF8F66)
private val WavelengthBlue = Color(0xFF1E88E5)
private val WavelengthBlueLight = Color(0xFF64B5F6)

private val DarkColorScheme = darkColorScheme(
    primary = WavelengthOrangeLight,
    onPrimary = Color(0xFF2B0D00),
    primaryContainer = Color(0xFF5C1F00),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = WavelengthBlueLight,
    onSecondary = Color(0xFF002F5C),
    secondaryContainer = Color(0xFF004582),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1A1A1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2B30),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

private val LightColorScheme = lightColorScheme(
    primary = WavelengthOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = WavelengthBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Color(0xFF001C38),
    tertiary = Color(0xFFF57C00),
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0EFF4),
    onSurfaceVariant = Color(0xFF49454F),
)

@Composable
fun WavelengthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
