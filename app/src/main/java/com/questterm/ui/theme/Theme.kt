package com.questterm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TerminalBlue,
    secondary = TerminalGreen,
    tertiary = TerminalPurple,
    background = TerminalBlack,
    surface = TerminalSurface,
    surfaceVariant = TerminalSurfaceVariant,
    onPrimary = TerminalBlack,
    onSecondary = TerminalBlack,
    onTertiary = TerminalBlack,
    onBackground = TerminalForeground,
    onSurface = TerminalForeground,
    onSurfaceVariant = TerminalForegroundDim,
    error = TerminalRed,
    onError = TerminalBlack,
)

@Composable
fun QuestTermTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
