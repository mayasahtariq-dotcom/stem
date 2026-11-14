package com.stemtutor.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StemColorScheme = lightColorScheme(
    primary = StemColors.BabyBlue,
    onPrimary = StemColors.Ink,
    primaryContainer = StemColors.BabyBlueTint,
    onPrimaryContainer = StemColors.Ink,
    secondary = StemColors.Cream,
    onSecondary = StemColors.Ink,
    secondaryContainer = StemColors.Cream,
    onSecondaryContainer = StemColors.Ink,
    tertiary = StemColors.DeepBlue,
    onTertiary = StemColors.White,
    background = StemColors.White,
    onBackground = StemColors.Ink,
    surface = StemColors.White,
    onSurface = StemColors.Ink,
    onSurfaceVariant = StemColors.Muted,
    outline = StemColors.DeepBlue,
    outlineVariant = StemColors.NeutralBorder,
    error = StemColors.Red,
    onError = StemColors.White,
)

/** The app's only theme: light, no dark variant (PRD). */
@Composable
fun StemTutorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StemColorScheme,
        typography = StemTypography,
        content = content,
    )
}
