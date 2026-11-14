package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.theme.StemColors

/**
 * Mooshala's onboarding layout: the cow's speech bubble, the cow, the screen's content, and the
 * screen's one primary action pinned at the bottom. Scrolls when the window is short, so the
 * button is always reachable.
 */
@Composable
fun PromptScreen(
    bubble: BilingualString?,
    pose: CowPose,
    modifier: Modifier = Modifier,
    background: Color = StemColors.White,
    animate: Boolean = true,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            if (bubble != null) SpeechBubble(bubble, Modifier.fillMaxWidth())
            CowMascot(pose, animate = animate)
            content()
        }
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            footer()
        }
    }
}

/** A bilingual error line under a form, in the alert colour. */
@Composable
fun ErrorLine(text: BilingualString, modifier: Modifier = Modifier) {
    BilingualText(
        text = text,
        size = BilingualTextSize.Small,
        regionalColor = StemColors.WrongText,
        englishColor = StemColors.WrongText,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
