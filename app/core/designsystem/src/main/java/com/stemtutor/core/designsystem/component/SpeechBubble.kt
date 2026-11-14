package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

private val TailSize = 14.dp

/** The cow's speech bubble: soft grey, rounded, with a tail pointing down at the mascot. */
@Composable
fun SpeechBubble(
    text: BilingualString,
    modifier: Modifier = Modifier,
    color: Color = StemColors.Soft,
    tailStart: Dp = 36.dp,
) {
    Box(
        modifier
            .padding(bottom = TailSize)
            .drawBehind {
                val tail = TailSize.toPx()
                val x = tailStart.toPx()
                val path = Path().apply {
                    moveTo(x - tail, size.height - 1f)
                    lineTo(x + tail, size.height - 1f)
                    lineTo(x, size.height + tail)
                    close()
                }
                drawPath(path, color)
            }
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        BilingualText(text = text, size = BilingualTextSize.Large)
    }
}

@Composable
fun SpeechBubble(@StringRes textRes: Int, modifier: Modifier = Modifier) {
    SpeechBubble(bilingualStringResource(textRes), modifier)
}

@BilingualPreviews
@Composable
private fun SpeechBubblePreview() {
    StemTutorTheme {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SpeechBubble(R.string.ds_sample_question, Modifier.fillMaxWidth())
            CowMascot(CowPose.Question, animate = false)
        }
    }
}
