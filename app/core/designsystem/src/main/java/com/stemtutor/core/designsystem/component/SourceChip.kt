package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

private val ChipShape = RoundedCornerShape(10.dp)

/** "Your notes, page 2": where an answer or question came from. */
@Composable
fun SourceChip(page: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(StemColors.Cream.copy(alpha = 0.45f))
            .border(1.dp, StemColors.DeepBlue.copy(alpha = 0.5f), ChipShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = StemColors.DeepBlue,
            modifier = Modifier.size(16.dp),
        )
        BilingualText(
            text = bilingualStringResource(R.string.ds_source_chip, page),
            size = BilingualTextSize.Small,
        )
    }
}

@BilingualPreviews
@Composable
private fun SourceChipPreview() {
    StemTutorTheme { SourceChip(page = 2, modifier = Modifier.padding(16.dp)) }
}
