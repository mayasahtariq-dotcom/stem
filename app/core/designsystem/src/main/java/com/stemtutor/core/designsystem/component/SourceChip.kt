package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

private val ChipShape = RoundedCornerShape(10.dp)

/** "Your notes, page 2": where an answer or question came from. Mooshala's brown chip. */
@Composable
fun SourceChip(page: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(StemColors.ChipFill)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StickerIcon(Sticker.Doc, size = 18.dp)
        BilingualText(
            text = bilingualStringResource(R.string.ds_source_chip, page),
            size = BilingualTextSize.Small,
            regionalColor = StemColors.Brown,
            englishColor = StemColors.Brown,
        )
    }
}

@BilingualPreviews
@Composable
private fun SourceChipPreview() {
    StemTutorTheme { SourceChip(page = 2, modifier = Modifier.padding(16.dp)) }
}
