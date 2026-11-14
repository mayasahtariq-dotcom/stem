package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

enum class AnswerOptionState { Default, Selected, Correct, Wrong, CorrectReveal, Disabled }

private data class OptionColors(val fill: Color, val border: Color, val text: Color)

private fun colorsFor(state: AnswerOptionState) = when (state) {
    AnswerOptionState.Default -> OptionColors(StemColors.White, StemColors.Line, StemColors.Ink)
    AnswerOptionState.Selected -> OptionColors(StemColors.BlueTint, StemColors.Blue, StemColors.Ink)
    AnswerOptionState.Correct -> OptionColors(StemColors.GreenTint, StemColors.Green, StemColors.Ink)
    AnswerOptionState.Wrong -> OptionColors(StemColors.RedTint, StemColors.Red, StemColors.Ink)
    AnswerOptionState.CorrectReveal -> OptionColors(StemColors.White, StemColors.Green, StemColors.Ink)
    AnswerOptionState.Disabled -> OptionColors(StemColors.Soft, StemColors.Line, StemColors.Muted)
}

@StringRes
private fun stateLabel(state: AnswerOptionState): Int? = when (state) {
    AnswerOptionState.Selected -> R.string.ds_option_selected
    AnswerOptionState.Correct -> R.string.ds_option_correct
    AnswerOptionState.Wrong -> R.string.ds_option_not_quite
    AnswerOptionState.CorrectReveal -> R.string.ds_option_correct_answer
    else -> null
}

private val OptionRadius = 16.dp
private val OptionShape = RoundedCornerShape(OptionRadius)

/**
 * Mooshala answer card: 2 dp border with a thicker 4 dp bottom edge. Only Default and Selected
 * accept taps.
 */
@Composable
fun AnswerOption(
    text: BilingualString,
    state: AnswerOptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = colorsFor(state)
    val tappable = state == AnswerOptionState.Default || state == AnswerOptionState.Selected
    val description = stateLabel(state)?.let { bilingualStringResource(it).regional }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(OptionShape)
            .background(colors.fill)
            .drawBehind {
                val side = 2.dp.toPx()
                val bottom = 4.dp.toPx()
                val radius = CornerRadius(OptionRadius.toPx())
                // Bottom edge: a filled rounded rect peeking out below the outline.
                drawRoundRect(colors.border, cornerRadius = radius)
                drawRoundRect(
                    colors.fill,
                    topLeft = Offset(side, side),
                    size = Size(size.width - 2 * side, size.height - side - bottom),
                    cornerRadius = CornerRadius(radius.x - side),
                )
                drawRoundRect(colors.border, cornerRadius = radius, style = Stroke(side))
            }
            .selectable(
                selected = state == AnswerOptionState.Selected,
                enabled = tappable,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { if (description != null) stateDescription = description }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BilingualText(
            text = text,
            modifier = Modifier.weight(1f),
            size = BilingualTextSize.Large,
            regionalColor = colors.text,
        )
        StateBadge(state)
    }
}

@Composable
fun AnswerOption(
    @StringRes textRes: Int,
    state: AnswerOptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnswerOption(bilingualStringResource(textRes), state, onClick, modifier)
}

@Composable
private fun StateBadge(state: AnswerOptionState) {
    val (icon, tint) = when (state) {
        AnswerOptionState.Correct, AnswerOptionState.CorrectReveal -> Icons.Filled.Check to StemColors.Green
        AnswerOptionState.Wrong -> Icons.Filled.Close to StemColors.Red
        else -> return
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = StemColors.White, modifier = Modifier.size(18.dp))
    }
}

@BilingualPreviews
@Composable
private fun AnswerOptionPreview() {
    StemTutorTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnswerOptionState.entries.forEach { state ->
                AnswerOption(R.string.ds_sample_option_mitochondria, state, onClick = {})
            }
        }
    }
}
