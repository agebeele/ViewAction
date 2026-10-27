package com.example.viewaction.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

object GoProColors {
    val Blue = Color(0xFF00AEEF)
    val Red = Color(0xFFFF3131)
    val DarkBackground = Color(0xFF080A0C)
    val Surface = Color(0xFF16191E)
    val Card = Color(0xFF20242C)
    val Yellow = Color(0xFFFFCC00)
    val Green = Color(0xFF34C759)
    val TextWhite = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF9A9FA8)
}

val GoProColorScheme = ColorScheme(
    primary = GoProColors.Blue,
    onPrimary = Color.Black,
    primaryContainer = GoProColors.Surface,
    onPrimaryContainer = GoProColors.TextWhite,
    secondary = GoProColors.Red,
    onSecondary = Color.White,
    secondaryContainer = GoProColors.Card,
    onSecondaryContainer = GoProColors.TextWhite,
    background = GoProColors.DarkBackground,
    onBackground = GoProColors.TextWhite,
    surfaceContainer = GoProColors.Surface,
    onSurface = GoProColors.TextWhite,
    onSurfaceVariant = GoProColors.TextMuted,
    error = GoProColors.Red,
    onError = Color.White
)

@Composable
fun GoProActionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GoProColorScheme,
        content = content
    )
}
