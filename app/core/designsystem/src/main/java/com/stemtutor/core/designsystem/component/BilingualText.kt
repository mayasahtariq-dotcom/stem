package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.designsystem.theme.regionalFontFamily

/** Regional line 16–18 sp bold (line height ≥ 1.5×); English line 13–14 sp muted. */
enum class BilingualTextSize(
    val regionalSize: TextUnit,
    val regionalLineHeight: TextUnit,
    val englishSize: TextUnit,
    val englishLineHeight: TextUnit,
) {
    Small(16.sp, 24.sp, 13.sp, 18.sp),
    Medium(17.sp, 26.sp, 13.5.sp, 19.sp),
    Large(18.sp, 27.sp, 14.sp, 20.sp),
}

object BilingualTextTags {
    const val REGIONAL = "bilingual_regional"
    const val ENGLISH = "bilingual_english"
}

/** The shared bilingual text: the regional line in bold, the muted English line underneath. */
@Composable
fun BilingualText(
    text: BilingualString,
    modifier: Modifier = Modifier,
    size: BilingualTextSize = BilingualTextSize.Medium,
    regionalColor: Color = StemColors.Ink,
    englishColor: Color = StemColors.Muted,
    textAlign: TextAlign = TextAlign.Start,
) {
    val horizontal = when (textAlign) {
        TextAlign.Center -> Alignment.CenterHorizontally
        TextAlign.End, TextAlign.Right -> Alignment.End
        else -> Alignment.Start
    }
    Column(modifier = modifier, horizontalAlignment = horizontal) {
        Text(
            text = text.regional,
            modifier = Modifier.testTag(BilingualTextTags.REGIONAL),
            style = TextStyle(
                fontFamily = regionalFontFamily(currentLanguageMode()),
                fontWeight = FontWeight.Bold,
                fontSize = size.regionalSize,
                lineHeight = size.regionalLineHeight,
                color = regionalColor,
                textAlign = textAlign,
            ),
        )
        Text(
            text = text.english,
            modifier = Modifier.padding(top = 2.dp).testTag(BilingualTextTags.ENGLISH),
            style = TextStyle(
                fontFamily = Rubik,
                fontWeight = FontWeight.Medium,
                fontSize = size.englishSize,
                lineHeight = size.englishLineHeight,
                color = englishColor,
                textAlign = textAlign,
            ),
        )
    }
}

@Composable
fun BilingualText(
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
    size: BilingualTextSize = BilingualTextSize.Medium,
    regionalColor: Color = StemColors.Ink,
    englishColor: Color = StemColors.Muted,
    textAlign: TextAlign = TextAlign.Start,
) {
    BilingualText(
        text = bilingualStringResource(textRes),
        modifier = modifier,
        size = size,
        regionalColor = regionalColor,
        englishColor = englishColor,
        textAlign = textAlign,
    )
}

@BilingualPreviews
@Composable
private fun BilingualTextPreview() {
    StemTutorTheme {
        Column(Modifier.padding(16.dp)) {
            BilingualTextSize.entries.forEach { size ->
                BilingualText(
                    R.string.ds_sample_question,
                    size = size,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}
