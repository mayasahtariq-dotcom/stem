package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/** Shown while data loads. The retry button appears only when [onRetry] is given. */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.ds_loading,
    onRetry: (() -> Unit)? = null,
) {
    StateScreen(modifier, messageRes, onRetry) {
        CircularProgressIndicator(color = StemColors.DeepBlue, modifier = Modifier.size(48.dp))
    }
}

@Composable
fun ErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.ds_error,
) {
    StateScreen(modifier, messageRes, onRetry) { Text("⚠️", fontSize = 40.sp) }
}

@Composable
fun EmptyScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.ds_empty,
) {
    StateScreen(modifier, messageRes, onRetry) { Text("📭", fontSize = 40.sp) }
}

@Composable
private fun StateScreen(
    modifier: Modifier,
    @StringRes messageRes: Int,
    onRetry: (() -> Unit)?,
    illustration: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        illustration()
        BilingualText(messageRes, size = BilingualTextSize.Large, textAlign = TextAlign.Center)
        if (onRetry != null) {
            ChunkyButton(R.string.ds_retry, onRetry, Modifier.fillMaxWidth())
        }
    }
}

@BilingualPreviews
@Composable
private fun LoadingScreenPreview() {
    StemTutorTheme { LoadingScreen(onRetry = {}) }
}

@BilingualPreviews
@Composable
private fun ErrorScreenPreview() {
    StemTutorTheme { ErrorScreen(onRetry = {}) }
}

@BilingualPreviews
@Composable
private fun EmptyScreenPreview() {
    StemTutorTheme { EmptyScreen(onRetry = {}) }
}
