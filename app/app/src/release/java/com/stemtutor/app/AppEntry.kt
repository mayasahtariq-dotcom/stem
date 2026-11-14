package com.stemtutor.app

import androidx.compose.runtime.Composable
import com.stemtutor.core.designsystem.component.EmptyScreen

/** Release builds have no user features in M0; the gallery is debug-only. */
@Composable
fun AppEntry() {
    EmptyScreen(onRetry = {}, messageRes = R.string.release_placeholder)
}
