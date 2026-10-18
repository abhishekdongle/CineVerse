package com.cineverse.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val TvColorScheme = darkColorScheme(
    primary = Color(0xFFB39DFF),
    secondary = Color(0xFFCCC2DC),
    background = Color(0xFF0E0B16),
    surface = Color(0xFF0E0B16),
)

/** TV is always dark: the 10-foot UI is viewed in dim rooms. */
@Composable
fun CineVerseTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TvColorScheme, content = content)
}
