package com.foxcode.foxweather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FoxColorScheme = darkColorScheme(
    primary = RainBlue,
    background = Night,
    surface = Cloud,
    onPrimary = Night,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextDim,
)

@Composable
fun FoxWeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoxColorScheme,
        content = content
    )
}