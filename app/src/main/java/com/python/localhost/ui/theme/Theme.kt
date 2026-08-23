package com.python.localhost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2F81F7),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3FB950),
    background = Color(0xFF0E1116),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    onBackground = Color(0xFFC9D1D9),
    onSurface = Color(0xFFC9D1D9),
    onSurfaceVariant = Color(0xFF8B949E),
    error = Color(0xFFF85149),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    secondary = Color(0xFF1A7F37),
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
)

@Composable
fun PyMobileTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
