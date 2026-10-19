package com.example.kotlincalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The familiar calculator orange, used for the operation keys. */
val CalculatorOrange = Color(0xFFFF9500)

private val LightColors = lightColorScheme(
    primary = CalculatorOrange,
    onPrimary = Color.White,
    secondary = CalculatorOrange,
    background = Color(0xFFF3F3F6),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFF3F3F6),
    onSurface = Color(0xFF111114),
)

private val DarkColors = darkColorScheme(
    primary = CalculatorOrange,
    onPrimary = Color.White,
    secondary = CalculatorOrange,
    background = Color(0xFF0F0F11),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF0F0F11),
    onSurface = Color(0xFFF5F5F7),
)

@Composable
fun KotlinCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
