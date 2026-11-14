package com.stemtutor.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/** In-app banner when a course the user left generating is ready (push comes in M6). */
@Composable
fun ReadyBanner(onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .statusBarsPadding()
            .padding(12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StemColors.Ink)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StickerIcon(Sticker.Party, size = 28.dp)
        BilingualText(
            R.string.nt_banner_ready,
            size = BilingualTextSize.Small,
            regionalColor = Color.White,
            englishColor = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        ChunkyButton(R.string.nt_banner_open, onOpen, style = ChunkyButtonStyle.Primary)
        ChunkyButton(com.stemtutor.core.designsystem.R.string.ds_cancel, onDismiss, style = ChunkyButtonStyle.Ghost)
    }
}

@BilingualPreviews
@Composable
private fun ReadyBannerPreview() {
    StemTutorTheme { ReadyBanner({}, {}) }
}
