package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.NotoSans
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/** Streak, XP and hearts. Numbers only, so no bilingual lines; screen readers get both. */
@Composable
fun TopStatsBar(
    streakDays: Int,
    xp: Int,
    hearts: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat(
            leading = { Text("🔥", fontSize = 20.sp) },
            value = streakDays,
            color = StemColors.Ink,
            description = bilingualStringResource(R.string.ds_stat_streak, streakDays).let {
                "${it.regional}. ${it.english}"
            },
        )
        Stat(
            leading = { StatIcon(Icons.Filled.Star, StemColors.DeepBlue) },
            value = xp,
            color = StemColors.DeepBlue,
            description = bilingualStringResource(R.string.ds_stat_xp, xp).english,
        )
        Stat(
            leading = { StatIcon(Icons.Filled.Favorite, StemColors.Red) },
            value = hearts,
            color = StemColors.Red,
            description = bilingualStringResource(R.string.ds_stat_hearts, hearts).let {
                "${it.regional}. ${it.english}"
            },
        )
    }
}

@Composable
private fun StatIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
}

@Composable
private fun Stat(
    leading: @Composable () -> Unit,
    value: Int,
    color: Color,
    description: String,
) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading()
        Text(
            text = value.toString(),
            style = TextStyle(
                fontFamily = NotoSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color,
            ),
        )
    }
}

@BilingualPreviews
@Composable
private fun TopStatsBarPreview() {
    StemTutorTheme { TopStatsBar(streakDays = 5, xp = 120, hearts = 4) }
}
