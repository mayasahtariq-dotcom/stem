package com.stemtutor.feature.student.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.data.model.Answer
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.designsystem.component.AnswerOption
import com.stemtutor.core.designsystem.component.AnswerOptionState
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.TermText
import com.stemtutor.feature.student.bilingual
import kotlin.math.roundToInt

/** The answer area for the current question, by type. */
@Composable
fun QuestionArea(
    body: QuestionBody,
    choice: Answer?,
    match: MatchState?,
    feedback: Feedback?,
    terms: Collection<Term>,
    onChoose: (Answer) -> Unit,
    onLeft: (Int) -> Unit,
    onRight: (Int) -> Unit,
    onTerm: (String) -> Unit,
) {
    when (body) {
        is QuestionBody.MultipleChoice -> MultipleChoiceQuestion(body, choice as? Answer.Choice, feedback, onChoose)
        is QuestionBody.TrueFalse -> TrueFalseQuestion(body, choice as? Answer.TrueFalse, feedback, terms, onChoose, onTerm)
        is QuestionBody.FillFormula -> FillFormulaQuestion(body, choice as? Answer.Chip, feedback, onChoose)
        is QuestionBody.MatchPairs -> if (match != null) MatchPairsQuestion(body, match, onLeft, onRight)
    }
}

private fun optionState(index: Int, picked: Int?, correctIndex: Int, checked: Boolean): AnswerOptionState = when {
    !checked -> if (index == picked) AnswerOptionState.Selected else AnswerOptionState.Default
    index == correctIndex && index == picked -> AnswerOptionState.Correct
    index == picked -> AnswerOptionState.Wrong
    index == correctIndex -> AnswerOptionState.CorrectReveal
    else -> AnswerOptionState.Disabled
}

/** Three or four bilingual answer cards. */
@Composable
fun MultipleChoiceQuestion(body: QuestionBody.MultipleChoice, picked: Answer.Choice?, feedback: Feedback?, onChoose: (Answer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        body.options.forEachIndexed { i, option ->
            AnswerOption(
                text = option.bilingual(),
                state = optionState(i, picked?.index, body.correctIndex, feedback != null),
                onClick = { onChoose(Answer.Choice(i)) },
            )
        }
    }
}

/** A statement with two large buttons. */
@Composable
fun TrueFalseQuestion(
    body: QuestionBody.TrueFalse,
    picked: Answer.TrueFalse?,
    feedback: Feedback?,
    terms: Collection<Term>,
    onChoose: (Answer) -> Unit,
    onTerm: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TermText(
            body.statement,
            terms,
            onTerm,
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.Cream).padding(16.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(true to R.string.ln_true, false to R.string.ln_false).forEach { (value, label) ->
                val index = if (value) 0 else 1
                AnswerOption(
                    text = bilingualStringResource(label),
                    state = optionState(index, picked?.let { if (it.value) 0 else 1 }, if (body.answer) 0 else 1, feedback != null),
                    onClick = { onChoose(Answer.TrueFalse(value)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A formula with a blank box and symbol chips to tap into it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillFormulaQuestion(body: QuestionBody.FillFormula, picked: Answer.Chip?, feedback: Feedback?, onChoose: (Answer) -> Unit) {
    val parts = body.formula.split("__", limit = 2)
    val filled = picked?.chip
    val boxColor = when {
        feedback == null -> if (filled == null) StemColors.Soft else StemColors.BlueTint
        feedback.correct -> StemColors.GreenTint
        else -> StemColors.RedTint
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BilingualText(R.string.ln_fill_hint, size = BilingualTextSize.Small)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.Cream).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(parts.first(), fontFamily = Rubik, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = StemColors.Ink)
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(boxColor)
                    .border(2.dp, StemColors.Line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(filled ?: "  ?  ", fontFamily = Rubik, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
            }
            Text(parts.getOrElse(1) { "" }, fontFamily = Rubik, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = StemColors.Ink)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            body.chips.forEach { chip ->
                val selected = chip == filled
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) StemColors.BlueTint else StemColors.White)
                        .border(2.dp, if (selected) StemColors.Blue else StemColors.Line, RoundedCornerShape(14.dp))
                        .clickable(enabled = feedback == null, role = Role.Button) { onChoose(Answer.Chip(chip)) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chip, fontFamily = Rubik, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
                }
            }
        }
    }
}

/** Two columns of tiles: tap one on the left, then its partner on the right. */
@Composable
fun MatchPairsQuestion(body: QuestionBody.MatchPairs, match: MatchState, onLeft: (Int) -> Unit, onRight: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BilingualText(R.string.ln_match_hint, size = BilingualTextSize.Small)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                body.pairs.forEachIndexed { i, (left, _) ->
                    MatchTile(
                        text = left.bilingual(),
                        matched = i in match.matched,
                        selected = match.selectedLeft == i,
                        shake = false,
                        onClick = { onLeft(i) },
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                match.rightOrder.forEachIndexed { slot, pair ->
                    MatchTile(
                        text = body.pairs[pair].second.bilingual(),
                        matched = pair in match.matched,
                        selected = false,
                        shake = match.wrongSlot == slot,
                        shakeKey = match.mistakes,
                        onClick = { onRight(slot) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchTile(
    text: com.stemtutor.core.designsystem.text.BilingualString,
    matched: Boolean,
    selected: Boolean,
    shake: Boolean,
    onClick: () -> Unit,
    shakeKey: Int = 0,
) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shake, shakeKey) {
        if (shake) {
            for (x in listOf(12f, -12f, 8f, -8f, 0f)) offset.animateTo(x, tween(50))
        }
    }
    AnimatedVisibility(visible = !matched, exit = fadeOut(tween(300))) {
        val border = when {
            shake -> StemColors.Red
            selected -> StemColors.Blue
            else -> StemColors.Line
        }
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (shake) StemColors.RedTint else if (selected) StemColors.BlueTint else StemColors.White)
                .border(2.dp, border, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BilingualText(text, size = BilingualTextSize.Small)
        }
    }
}
