package com.python.localhost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Darcula-inspired palette (Android Studio look).
 * Panels #3C3F41/#45494A, editor background #2B2B2B, Darcula text #A9B7C6.
 */
private val DarculaScheme = darkColorScheme(
    primary = Color(0xFF61AFEF),          // Darcula light blue
    onPrimary = Color(0xFF0B1420),
    secondary = Color(0xFF499C54),        // IntelliJ green (run button)
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFCB6B),         // soft amber accent
    background = Color(0xFF2B2B2B),       // editor background
    onBackground = Color(0xFFA9B7C6),     // Darcula default text
    surface = Color(0xFF3C3F41),          // tool windows / panels
    onSurface = Color(0xFFBBBBBB),
    surfaceVariant = Color(0xFF45494A),   // toolbars / tabs
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainer = Color(0xFF323536),
    surfaceContainerHigh = Color(0xFF434748),
    error = Color(0xFFFF6B68),            // Darcula red
    onError = Color(0xFF1B1B1B),
    outline = Color(0xFF545A5D),
    primaryContainer = Color(0xFF2B4A6B),
    onPrimaryContainer = Color(0xFFB7D7F5),
    secondaryContainer = Color(0xFF2F5233),
    onSecondaryContainer = Color(0xFFB7E0BB),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E5E9E),
    secondary = Color(0xFF387F42),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
)

@Composable
fun PyMobileTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarculaScheme else LightScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
