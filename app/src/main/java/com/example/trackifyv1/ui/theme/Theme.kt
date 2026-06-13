package com.example.trackifyv1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary   = Gold,
    secondary = TealAccent,
    tertiary  = Crimson
)

private val LightColorScheme = lightColorScheme(
    primary   = Color(0xFFB8860B),
    secondary = Color(0xFF00917A),
    tertiary  = Crimson
)

@Composable
fun Trackifyv1Theme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
