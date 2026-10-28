package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DynicTealPrimary,
    onPrimary = Color.White,
    primaryContainer = DynicTealContainer,
    onPrimaryContainer = DynicOnTealContainer,
    secondary = DynicSecondary,
    onSecondary = Color.White,
    secondaryContainer = DynicSecondaryContainer,
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = DynicTertiary,
    onTertiary = Color.White,
    background = DynicBackground,
    onBackground = Color(0xFF191C1B),
    surface = DynicSurface,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = DynicSurfaceVariant,
    onSurfaceVariant = Color(0xFF3F4946),
    outline = DynicOutline,
    outlineVariant = DynicOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DynicTealPrimary,
    onPrimary = Color.Black,
    primaryContainer = DynicTealDark,
    onPrimaryContainer = DynicTealLight,
    secondary = Color(0xFF4FD8EC),
    onSecondary = Color(0xFF00363D),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF202423),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBFC9C5),
    outline = Color(0xFF899390)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
