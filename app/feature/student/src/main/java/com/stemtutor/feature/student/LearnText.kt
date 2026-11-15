package com.stemtutor.feature.student

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.regionalFontFamily
import com.stemtutor.feature.student.lesson.TermMatcher

/** Course content as the two lines the app shows: regional above English. */
fun Tri.bilingual(mode: LanguageMode) = BilingualString(regional(mode), en)

@Composable
fun Tri.bilingual() = bilingual(currentLanguageMode())

/** A string whose argument differs per line (e.g. a lesson title in each language). */
@Composable
fun bilingualFormat(@StringRes id: Int, regionalArg: String, englishArg: String): BilingualString =
    BilingualString(bilingualStringResource(id, regionalArg).regional, bilingualStringResource(id, englishArg).english)

/**
 * Bilingual text whose science words are underlined; tapping one opens its term card
 * (PRD M3). Looks like BilingualText: bold regional line above the muted English line.
 */
@Composable
fun TermText(
    text: Tri,
    terms: Collection<Term>,
    onTerm: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: BilingualTextSize = BilingualTextSize.Large,
    regionalColor: Color = StemColors.Ink,
    englishColor: Color = StemColors.Muted,
    textAlign: TextAlign = TextAlign.Start,
) {
    val mode = currentLanguageMode()
    Column(modifier) {
        Text(
            text = linked(text.regional(mode), terms, mode, english = false, onTerm),
            style = TextStyle(
                fontFamily = regionalFontFamily(mode),
                fontWeight = FontWeight.Bold,
                fontSize = size.regionalSize,
                lineHeight = size.regionalLineHeight,
                color = regionalColor,
                textAlign = textAlign,
            ),
        )
        Text(
            text = linked(text.en, terms, mode, english = true, onTerm),
            modifier = Modifier.padding(top = 2.dp),
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

private fun linked(text: String, terms: Collection<Term>, mode: LanguageMode, english: Boolean, onTerm: (String) -> Unit): AnnotatedString {
    val spans = TermMatcher.find(text, terms, mode, english)
    if (spans.isEmpty()) return AnnotatedString(text)
    val style = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline, color = StemColors.BlueDark))
    return buildAnnotatedString {
        var at = 0
        for (s in spans) {
            append(text.substring(at, s.start))
            pushLink(LinkAnnotation.Clickable(s.key, style) { onTerm(s.key) })
            append(text.substring(s.start, s.end))
            pop()
            at = s.end
        }
        append(text.substring(at))
    }
}
