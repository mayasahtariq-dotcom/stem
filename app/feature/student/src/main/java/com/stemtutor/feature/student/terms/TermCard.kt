package com.stemtutor.feature.student.terms

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.SourceChip
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.designsystem.theme.regionalFontFamily
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.bilingual
import com.stemtutor.feature.student.bilingualFormat

/** A term card in a bottom sheet (opened by tapping an underlined word or a glossary row). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermSheet(
    term: Term,
    related: List<Term>,
    onDismiss: () -> Unit,
    onListen: (Tri, Boolean) -> Unit,
    onToggleSave: () -> Unit,
    onAskDoubt: () -> Unit,
    onRelated: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = StemColors.White) {
        TermCardContent(term, related, onListen, onToggleSave, onAskDoubt, onRelated)
    }
}

/** The card itself: speaker, the big regional word with the English word, how to say it,
 * meaning, "think of it like", related terms and where it came from. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TermCardContent(
    term: Term,
    related: List<Term>,
    onListen: (Tri, Boolean) -> Unit,
    onToggleSave: () -> Unit,
    onAskDoubt: () -> Unit,
    onRelated: (String) -> Unit,
) {
    val mode = currentLanguageMode()
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BilingualText(
            R.string.ln_term,
            size = BilingualTextSize.Small,
            regionalColor = StemColors.Brown,
            englishColor = StemColors.Brown,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(StemColors.ChipFill).padding(horizontal = 10.dp, vertical = 2.dp),
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(StemColors.Cream).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val listen = bilingualStringResource(R.string.ln_listen)
                StickerIcon(
                    Sticker.Speaker,
                    size = 40.dp,
                    contentDescription = "${listen.regional} · ${listen.english}",
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { onListen(term.forms, false) },
                            onLongClick = { onListen(term.forms, true) },
                        ),
                )
                Column(Modifier.weight(1f)) {
                    Text(term.forms.regional(mode), fontFamily = regionalFontFamily(mode), fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 40.sp, color = StemColors.Ink)
                    Text(term.forms.en, fontFamily = Rubik, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = StemColors.Muted)
                }
            }
            val say = term.transliteration.regional(mode).ifBlank { term.transliteration.en }
            if (say.isNotBlank()) {
                BilingualText(bilingualFormat(R.string.ln_say_it, say, say), size = BilingualTextSize.Small)
            }
            if (term.formal.isComplete() && term.formal.regional(mode) != term.forms.regional(mode)) {
                BilingualText(bilingualFormat(R.string.ln_formal, term.formal.regional(mode), term.formal.en), size = BilingualTextSize.Small)
            }
            BilingualText(term.meaning.bilingual(mode), size = BilingualTextSize.Large)
            BilingualText(R.string.ln_think_of_it, size = BilingualTextSize.Small, regionalColor = StemColors.Brown, englishColor = StemColors.Brown)
            BilingualText(term.analogy.bilingual(mode))
            SourceChip(term.sourcePage)
        }
        if (related.isNotEmpty()) {
            BilingualText(R.string.ln_related, size = BilingualTextSize.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                related.forEach { r ->
                    BilingualText(
                        BilingualString(r.forms.regional(mode), r.forms.en),
                        size = BilingualTextSize.Small,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StemColors.Soft)
                            .combinedClickable(role = Role.Button, onClick = { onRelated(r.key) })
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton(
                if (term.saved) R.string.ln_saved else R.string.ln_save,
                onToggleSave,
                Modifier.weight(1f),
                if (term.saved) ChunkyButtonStyle.Cream else ChunkyButtonStyle.White,
            )
            ChunkyButton(R.string.ln_ask_doubt, onAskDoubt, Modifier.weight(1f), ChunkyButtonStyle.White)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.heightIn(min = 16.dp))
    }
}

internal val PREVIEW_TERM = Term(
    "mitochondria",
    Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"),
    Tri("maitokondriya", "maitokondriya", "maitokondriya"),
    Tri("Mitochondrion", "सूत्रकणिका", "ತಂತುಕಣ"),
    Tri("The part of the cell that makes energy.", "कोशिका का वह भाग जो ऊर्जा बनाता है।", "ಶಕ್ತಿ ಮಾಡುವ ಜೀವಕೋಶದ ಭಾಗ."),
    Tri("Like a generator that powers a village.", "जैसे गाँव को बिजली देने वाला जनरेटर।", "ಊರಿಗೆ ವಿದ್ಯುತ್ ಕೊಡುವ ಜನರೇಟರ್‌ನಂತೆ."),
    listOf("nucleus"),
    sourcePage = 1,
)

@BilingualPreviews
@Composable
private fun TermCardPreview() {
    StemTutorTheme {
        TermCardContent(PREVIEW_TERM, emptyList(), { _, _ -> }, {}, {}, {})
    }
}
