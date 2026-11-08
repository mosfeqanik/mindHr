package com.mindhr.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Same palette as the web app (frontend/src/styles/colors.css).
object MhColors {
    val Primary = Color(0xFF0298B6)
    val PrimaryDark = Color(0xFF026A80)
    val PrimarySubtle = Color(0xFFE6F5F8)
    val Navy = Color(0xFF1A2F4E)
    val Blue = Color(0xFF0685E6)
    val Green = Color(0xFF1BC48D)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFE5484D)
    val Muted = Color(0xFF6B7280)
    val Surface = Color(0xFFFFFFFF)
    val Background = Color(0xFFF8FAFB)
    val Border = Color(0xFFE5E7EB)
}

private val scheme = lightColorScheme(
    primary = MhColors.Primary,
    onPrimary = Color.White,
    primaryContainer = MhColors.PrimarySubtle,
    onPrimaryContainer = MhColors.PrimaryDark,
    secondary = MhColors.Navy,
    onSecondary = Color.White,
    tertiary = MhColors.Green,
    background = MhColors.Background,
    surface = MhColors.Surface,
    surfaceContainer = MhColors.Surface,
    surfaceContainerLow = MhColors.Surface,
    onSurfaceVariant = MhColors.Muted,
    outlineVariant = MhColors.Border,
    error = MhColors.Red,
)

@Composable
fun MindHrTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
