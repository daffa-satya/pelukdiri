package com.makhp.pelukdiri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PelukPrimary,
    onPrimary = PelukText,
    secondary = PelukPrimaryLight,
    onSecondary = PelukText,
    background = PelukText,
    onBackground = PelukSurface,
    surface = PelukText,
    onSurface = PelukSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PelukPrimary,
    onPrimary = PelukSurface,
    primaryContainer = PelukPrimaryLight,
    onPrimaryContainer = PelukPrimaryDark,
    secondary = PelukOrange,
    tertiary = PelukYellow,
    background = PelukBackground,
    onBackground = PelukText,
    surface = PelukSurface,
    onSurface = PelukText,
    surfaceVariant = PelukPrimaryLight,
    onSurfaceVariant = PelukSecondaryText,
    outlineVariant = PelukDivider,
    error = PelukDanger
)

@Composable
fun PELUKDIRITheme(
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
