package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/** Mooshala HUD: flame streak, star XP and heart lives. Screen readers hear both languages. */
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
        val streak = bilingualStringResource(R.string.ds_stat_streak, streakDays)
        val hearts2 = bilingualStringResource(R.string.ds_stat_hearts, hearts)
        Stat(Sticker.Flame, streakDays.toString(), "${streak.regional}. ${streak.english}")
        Stat(Sticker.Star, "$xp XP", bilingualStringResource(R.string.ds_stat_xp, xp).english)
        Stat(Sticker.Heart, hearts.toString(), "${hearts2.regional}. ${hearts2.english}")
    }
}

@Composable
private fun Stat(icon: Sticker, value: String, description: String) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StickerIcon(icon, size = 24.dp)
        Text(
            text = value,
            style = TextStyle(
                fontFamily = Rubik,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = StemColors.Ink,
            ),
        )
    }
}

@BilingualPreviews
@Composable
private fun TopStatsBarPreview() {
    StemTutorTheme { TopStatsBar(streakDays = 5, xp = 120, hearts = 4) }
}
