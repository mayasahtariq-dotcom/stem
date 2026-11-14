package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/** Shown while data loads: the cow wonders. The retry button appears only when [onRetry] is given. */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.ds_loading,
    onRetry: (() -> Unit)? = null,
    animate: Boolean = true,
) {
    StateScreen(modifier, messageRes, onRetry, CowPose.Question, animate)
}

/** Something failed: a sad cow, the message and a retry button. Never a blank screen. */
@Composable
fun ErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.ds_error,
    animate: Boolean = true,
) {
    StateScreen(modifier, messageRes, onRetry, CowPose.Sad, animate)
}

/** Nothing here yet: a napping cow. */
@Composable
fun EmptyScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.ds_empty,
    animate: Boolean = true,
) {
    StateScreen(modifier, messageRes, onRetry, CowPose.Sleep, animate)
}

@Composable
private fun StateScreen(
    modifier: Modifier,
    @StringRes messageRes: Int,
    onRetry: (() -> Unit)?,
    pose: CowPose,
    animate: Boolean,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeechBubble(messageRes, Modifier.fillMaxWidth())
        CowMascot(pose, animate = animate)
        if (onRetry != null) {
            ChunkyButton(R.string.ds_retry, onRetry, Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@BilingualPreviews
@Composable
private fun LoadingScreenPreview() {
    StemTutorTheme { LoadingScreen(onRetry = {}, animate = false) }
}

@BilingualPreviews
@Composable
private fun ErrorScreenPreview() {
    StemTutorTheme { ErrorScreen(onRetry = {}, animate = false) }
}

@BilingualPreviews
@Composable
private fun EmptyScreenPreview() {
    StemTutorTheme { EmptyScreen(onRetry = {}, animate = false) }
}
