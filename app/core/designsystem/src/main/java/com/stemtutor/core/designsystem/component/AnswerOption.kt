package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
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

private data class OptionColors(val fill: Color, val border: Color, val text: Color, val english: Color)

private fun colorsFor(state: AnswerOptionState) = when (state) {
    AnswerOptionState.Default ->
        OptionColors(StemColors.White, StemColors.NeutralBorder, StemColors.Ink, StemColors.Muted)
    AnswerOptionState.Selected ->
        OptionColors(StemColors.BabyBlueTint, StemColors.DeepBlue, StemColors.Ink, StemColors.Muted)
    AnswerOptionState.Correct ->
        OptionColors(StemColors.GreenTint, StemColors.Green, StemColors.Ink, StemColors.Muted)
    AnswerOptionState.Wrong ->
        OptionColors(StemColors.RedTint, StemColors.Red, StemColors.Ink, StemColors.Muted)
    AnswerOptionState.CorrectReveal ->
        OptionColors(StemColors.White, StemColors.Green, StemColors.Ink, StemColors.Muted)
    AnswerOptionState.Disabled ->
        OptionColors(StemColors.DisabledFill, StemColors.NeutralBorder, StemColors.Muted, StemColors.Muted)
}

@StringRes
private fun stateLabel(state: AnswerOptionState): Int? = when (state) {
    AnswerOptionState.Selected -> R.string.ds_option_selected
    AnswerOptionState.Correct -> R.string.ds_option_correct
    AnswerOptionState.Wrong -> R.string.ds_option_not_quite
    AnswerOptionState.CorrectReveal -> R.string.ds_option_correct_answer
    else -> null
}

private val OptionShape = RoundedCornerShape(14.dp)

/** A tappable answer card. Only Default and Selected accept taps. */
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
            .border(if (state == AnswerOptionState.Default) 2.dp else 3.dp, colors.border, OptionShape)
            .selectable(
                selected = state == AnswerOptionState.Selected,
                enabled = tappable,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { if (description != null) stateDescription = description }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BilingualText(
            text = text,
            modifier = Modifier.weight(1f),
            size = BilingualTextSize.Medium,
            regionalColor = colors.text,
            englishColor = colors.english,
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
