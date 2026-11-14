package com.stemtutor.feature.teacher.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.QuestionType
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.feature.teacher.R

/** Course content in the user's language mode, English underneath. */
@Composable
@ReadOnlyComposable
fun Tri.bilingual(): BilingualString = BilingualString(regional(currentLanguageMode()), en)

fun GenerationStatus.labelRes(published: Boolean): Int = when {
    published -> R.string.tc_status_published
    this == GenerationStatus.READY -> R.string.tc_status_ready
    this == GenerationStatus.GENERATING -> R.string.tc_status_generating
    this == GenerationStatus.NEEDS_REVIEW -> R.string.tc_status_needs_review
    this == GenerationStatus.FAILED -> R.string.tc_status_failed
    else -> R.string.tc_status_not_started
}

fun QuestionType.labelRes(): Int = when (this) {
    QuestionType.MULTIPLE_CHOICE -> R.string.tc_type_mcq
    QuestionType.MATCH_PAIRS -> R.string.tc_type_match
    QuestionType.FILL_FORMULA -> R.string.tc_type_formula
    QuestionType.TRUE_FALSE -> R.string.tc_type_tf
}

/** A small rounded label (status, question type). */
@Composable
fun Pill(res: Int, color: Color = StemColors.Soft, text: Color = StemColors.Ink) {
    BilingualText(
        res,
        size = BilingualTextSize.Small,
        regionalColor = text,
        englishColor = text,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
