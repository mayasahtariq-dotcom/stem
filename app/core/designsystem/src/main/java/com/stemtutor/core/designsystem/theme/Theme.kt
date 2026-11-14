package com.stemtutor.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StemColorScheme = lightColorScheme(
    primary = StemColors.Green,
    onPrimary = StemColors.White,
    primaryContainer = StemColors.GreenTint,
    onPrimaryContainer = StemColors.Ink,
    secondary = StemColors.Blue,
    onSecondary = StemColors.White,
    secondaryContainer = StemColors.BlueTint,
    onSecondaryContainer = StemColors.Ink,
    tertiary = StemColors.Gold,
    onTertiary = StemColors.Ink,
    background = StemColors.White,
    onBackground = StemColors.Ink,
    surface = StemColors.White,
    onSurface = StemColors.Ink,
    surfaceVariant = StemColors.Soft,
    onSurfaceVariant = StemColors.Muted,
    outline = StemColors.Line,
    outlineVariant = StemColors.Line,
    error = StemColors.Red,
    onError = StemColors.White,
)

/** The app's only theme: light, no dark variant. */
@Composable
fun StemTutorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StemColorScheme,
        typography = StemTypography,
        content = content,
    )
}
