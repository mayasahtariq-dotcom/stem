package com.stemtutor.feature.student.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stemtutor.core.data.model.LessonResult
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.StudentStats
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.ProgressBar
import com.stemtutor.core.designsystem.component.SourceChip
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.TermText
import com.stemtutor.feature.student.bilingual
import com.stemtutor.feature.student.bilingualFormat
import com.stemtutor.feature.student.terms.TermSheet

/** How the player asks for doubt chat: about a question, optionally sending a first message. */
enum class DoubtStart { NONE, WHY, SIMPLER }

@Composable
fun LessonPlayerRoute(
    onExit: () -> Unit,
    onAskDoubt: (courseId: String, questionId: String?, termKey: String?, start: DoubtStart) -> Unit,
    viewModel: LessonPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode = currentLanguageMode()
    val courseId = state.lesson?.courseId.orEmpty()
    LessonPlayerScreen(
        state = state,
        actions = PlayerActions(
            onClose = { if (state.result != null || state.loadError != null) onExit() else viewModel.askClose(true) },
            onConfirmClose = onExit,
            onCancelClose = { viewModel.askClose(false) },
            onChoose = viewModel::choose,
            onLeft = viewModel::tapLeft,
            onRight = viewModel::tapRight,
            onCheck = viewModel::check,
            onNext = viewModel::next,
            onRetryLoad = viewModel::load,
            onRetryComplete = viewModel::complete,
            onListen = { text, englishOnly -> viewModel.listen(text, mode, englishOnly) },
            onAskDoubt = { start -> onAskDoubt(courseId, state.question?.id, null, start) },
            onTerm = viewModel::openTerm,
            onToggleSave = viewModel::toggleSaved,
            onTermDoubt = { key -> viewModel.openTerm(null); onAskDoubt(courseId, null, key, DoubtStart.NONE) },
            onRefreshHearts = viewModel::refreshHearts,
            onDismissNotice = viewModel::dismissNotice,
            onDone = onExit,
        ),
    )
}

class PlayerActions(
    val onClose: () -> Unit = {},
    val onConfirmClose: () -> Unit = {},
    val onCancelClose: () -> Unit = {},
    val onChoose: (com.stemtutor.core.data.model.Answer) -> Unit = {},
    val onLeft: (Int) -> Unit = {},
    val onRight: (Int) -> Unit = {},
    val onCheck: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onRetryComplete: () -> Unit = {},
    val onListen: (Tri, Boolean) -> Unit = { _, _ -> },
    val onAskDoubt: (DoubtStart) -> Unit = {},
    val onTerm: (String?) -> Unit = {},
    val onToggleSave: (com.stemtutor.core.data.model.Term) -> Unit = {},
    val onTermDoubt: (String) -> Unit = {},
    val onRefreshHearts: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
    val onDone: () -> Unit = {},
)

@Composable
fun LessonPlayerScreen(state: PlayerUiState, actions: PlayerActions, animate: Boolean = true) {
    Box(Modifier.fillMaxSize().background(StemColors.White)) {
        when {
            state.loading -> LoadingScreen(animate = animate)
            state.loadError != null -> ErrorScreen(actions.onRetryLoad, messageRes = state.loadError.messageRes(), animate = animate)
            state.result != null -> LessonEnd(state.result, actions.onDone, animate)
            state.completing || state.queue.finished -> Finishing(state, actions, animate)
            state.heartsEmpty -> OutOfHearts(state.stats, actions, animate)
            else -> Playing(state, actions, animate)
        }
        state.openTerm?.let { term ->
            TermSheet(
                term = term,
                related = term.related.mapNotNull { state.terms[it] },
                onDismiss = { actions.onTerm(null) },
                onListen = actions.onListen,
                onToggleSave = { actions.onToggleSave(term) },
                onAskDoubt = { actions.onTermDoubt(term.key) },
                onRelated = { actions.onTerm(it) },
            )
        }
        if (state.confirmClose) {
            val title = bilingualStringResource(R.string.ln_close_title)
            val body = bilingualStringResource(R.string.ln_close_body)
            val leave = bilingualStringResource(R.string.ln_leave)
            val stay = bilingualStringResource(R.string.ln_stay)
            AlertDialog(
                onDismissRequest = actions.onCancelClose,
                title = { BilingualText(title, size = BilingualTextSize.Large) },
                text = { BilingualText(body) },
                confirmButton = { TextButton(onClick = actions.onConfirmClose) { BilingualText(leave, regionalColor = StemColors.Red) } },
                dismissButton = { TextButton(onClick = actions.onCancelClose) { BilingualText(stay) } },
                containerColor = StemColors.White,
            )
        }
    }
}

@Composable
private fun TopBar(state: PlayerUiState, onClose: () -> Unit) {
    val close = bilingualStringResource(R.string.ln_close)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "${close.regional} · ${close.english}", tint = StemColors.Muted)
        }
        ProgressBar(state.progress, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
            StickerIcon(Sticker.Heart, size = 24.dp)
            Text(" ${state.stats.hearts}", fontFamily = Rubik, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = StemColors.Red)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Playing(state: PlayerUiState, actions: PlayerActions, animate: Boolean) {
    val q = state.question ?: return
    val terms = state.terms.values
    val feedback = state.feedback
    val pose = when {
        feedback == null -> CowPose.Question
        feedback.correct -> CowPose.Heart
        else -> CowPose.Sad
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(state, actions.onClose)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.retrying) {
                BilingualText(R.string.ln_try_again_tag, size = BilingualTextSize.Small, regionalColor = StemColors.BlueDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CowMascot(pose, width = 84.dp, animate = animate)
                TermText(
                    q.prompt,
                    terms,
                    { actions.onTerm(it) },
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(StemColors.Soft).padding(16.dp),
                )
            }
            QuestionArea(q.body, state.choice, state.match, feedback, terms, actions.onChoose, actions.onLeft, actions.onRight) { actions.onTerm(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallAction(Sticker.Speaker, R.string.ln_listen, Modifier.weight(1f), onLongClick = { actions.onListen(listenText(q.prompt, q.body), true) }) {
                    actions.onListen(listenText(q.prompt, q.body), false)
                }
                SmallAction(Sticker.Chat, R.string.ln_ask_doubt, Modifier.weight(1f)) { actions.onAskDoubt(DoubtStart.NONE) }
            }
            state.notice?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
            Spacer(Modifier.heightIn(min = 8.dp))
        }
        AnimatedVisibility(visible = feedback != null, enter = slideInVertically { it }) {
            feedback?.let { FeedbackSheet(it, q.sourcePage, terms.toList(), actions) }
        }
        if (feedback == null) {
            ChunkyButton(R.string.ln_check, actions.onCheck, Modifier.fillMaxWidth().padding(20.dp), enabled = state.canCheck)
        }
    }
}

/** What Listen reads: the question, and for true/false the statement too. */
private fun listenText(prompt: Tri, body: QuestionBody): Tri =
    if (body is QuestionBody.TrueFalse) {
        Tri("${prompt.en} ${body.statement.en}", "${prompt.hi} ${body.statement.hi}", "${prompt.kn} ${body.statement.kn}")
    } else {
        prompt
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmallAction(icon: Sticker, label: Int, modifier: Modifier, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StemColors.Soft)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StickerIcon(icon, size = 24.dp)
        BilingualText(label, size = BilingualTextSize.Small)
    }
}

/** Slides up after Check: green "Great!" or red "Not quite", with the reason in both languages. */
@Composable
private fun FeedbackSheet(f: Feedback, sourcePage: Int, terms: List<com.stemtutor.core.data.model.Term>, actions: PlayerActions) {
    val mode = currentLanguageMode()
    val fill = if (f.correct) StemColors.GreenTint else StemColors.RedTint
    val ink = if (f.correct) StemColors.CorrectText else StemColors.WrongText
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(fill).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StickerIcon(if (f.correct) Sticker.Party else Sticker.Heart, size = 30.dp)
            BilingualText(if (f.correct) R.string.ln_great else R.string.ln_not_quite, size = BilingualTextSize.Large, regionalColor = ink, englishColor = ink)
            Spacer(Modifier.weight(1f))
            if (f.correct) {
                Text(bilingualStringResource(R.string.ln_xp_plus, f.xp).english, fontFamily = Rubik, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ink)
            }
        }
        if (f.heartLost) BilingualText(R.string.ln_heart_lost, size = BilingualTextSize.Small, regionalColor = ink, englishColor = ink)
        val trueText = bilingualTri(R.string.ln_true)
        val falseText = bilingualTri(R.string.ln_false)
        f.correctAnswer?.correctText(trueText, falseText)?.let { right ->
            BilingualText(bilingualFormat(R.string.ln_correct_answer, right.regional(mode), right.en), regionalColor = ink)
        }
        TermText(f.explanation, terms, { actions.onTerm(it) }, size = BilingualTextSize.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SourceChip(sourcePage)
            Text(
                bilingualStringResource(R.string.ln_explain_simpler).let { "${it.regional} · ${it.english}" },
                fontFamily = Rubik,
                fontSize = 14.sp,
                color = StemColors.BlueDark,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier.heightIn(min = 48.dp).padding(top = 14.dp).combinedClickableCompat { actions.onAskDoubt(DoubtStart.SIMPLER) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!f.correct) {
                ChunkyButton(R.string.ln_explain, { actions.onAskDoubt(DoubtStart.WHY) }, Modifier.weight(1f), ChunkyButtonStyle.White)
            }
            ChunkyButton(R.string.ln_next, actions.onNext, Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) = combinedClickable(role = Role.Button, onClick = onClick)

/** A bilingual string resource as a [Tri] (both regional languages from the current one). */
@Composable
private fun bilingualTri(id: Int): Tri {
    val b = bilingualStringResource(id)
    return Tri(b.english, b.regional, b.regional)
}

@Composable
private fun Finishing(state: PlayerUiState, actions: PlayerActions, animate: Boolean) {
    if (state.completeError != null) {
        ErrorScreen(actions.onRetryComplete, messageRes = state.completeError.messageRes(), animate = animate)
    } else {
        LoadingScreen(animate = animate)
    }
}

/** At zero hearts the lesson pauses and says when hearts refill (PRD M3). */
@Composable
private fun OutOfHearts(stats: StudentStats, actions: PlayerActions, animate: Boolean) {
    val minutes = stats.refillAt?.let(::minutesUntil)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CowMascot(CowPose.Sleep, animate = animate)
        BilingualText(R.string.ln_out_of_hearts, size = BilingualTextSize.Large, textAlign = TextAlign.Center)
        if (minutes != null) {
            BilingualText(bilingualStringResource(R.string.ln_refill_minutes, minutes), textAlign = TextAlign.Center)
        } else {
            BilingualText(R.string.ln_refill_soon, textAlign = TextAlign.Center)
        }
        ChunkyButton(R.string.ln_back_to_path, actions.onDone, Modifier.fillMaxWidth())
    }
}

/** Minutes until an ISO-8601 time (or epoch milliseconds, from the offline fake). */
internal fun minutesUntil(at: String, now: Long = System.currentTimeMillis()): Int? {
    val millis = at.toLongOrNull() ?: runCatching { java.time.OffsetDateTime.parse(at).toInstant().toEpochMilli() }.getOrNull() ?: return null
    return ((millis - now) / 60_000L).toInt().coerceAtLeast(1)
}

/** The end screen: XP, accuracy and the streak, with the gold celebration when it grew. */
@Composable
fun LessonEnd(result: LessonResult, onDone: () -> Unit, animate: Boolean = true) {
    val gold = Brush.verticalGradient(listOf(Color(0xFFFFE594), Color(0xFFFFBE4F), Color(0xFFF7973C)))
    val bg = if (result.streakExtended) Modifier.background(gold) else Modifier.background(StemColors.White)
    Column(
        Modifier.fillMaxSize().then(bg).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CowMascot(CowPose.Cool, animate = animate)
        if (result.streakExtended) {
            Text("${result.streak}", fontFamily = Rubik, fontWeight = FontWeight.Black, fontSize = 72.sp, color = Color(0xFF5A3410))
            BilingualText(bilingualStringResource(R.string.ln_streak_up, result.streak), size = BilingualTextSize.Large, textAlign = TextAlign.Center)
        } else {
            BilingualText(R.string.ln_lesson_done, size = BilingualTextSize.Large, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Sticker.Star, bilingualStringResource(R.string.ln_xp_earned, result.xpEarned), Modifier.weight(1f))
            StatCard(Sticker.Target, bilingualStringResource(R.string.ln_accuracy, (result.accuracy * 100).toInt()), Modifier.weight(1f))
        }
        ChunkyButton(R.string.ln_back_to_path, onDone, Modifier.fillMaxWidth(), if (result.streakExtended) ChunkyButtonStyle.White else ChunkyButtonStyle.Primary)
    }
}

@Composable
private fun StatCard(icon: Sticker, text: BilingualString, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(StemColors.White).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StickerIcon(icon, size = 32.dp)
        BilingualText(text, size = BilingualTextSize.Small, textAlign = TextAlign.Center)
    }
}

private val PREVIEW_Q = com.stemtutor.core.data.model.LessonQuestion(
    "q1", 0,
    Tri("Which part of the cell makes energy?", "कोशिका का कौन सा भाग ऊर्जा बनाता है?", "ಜೀವಕೋಶದ ಯಾವ ಭಾಗ ಶಕ್ತಿ ಮಾಡುತ್ತದೆ?"),
    Tri("Mitochondria make energy, like a stove.", "माइटोकॉन्ड्रिया ऊर्जा बनाता है, जैसे चूल्हा।", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ ಶಕ್ತಿ ಮಾಡುತ್ತದೆ, ಒಲೆಯಂತೆ."),
    QuestionBody.MultipleChoice(
        listOf(
            Tri("Nucleus", "न्यूक्लियस", "ನ್ಯೂಕ್ಲಿಯಸ್"),
            Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"),
            Tri("Ribosome", "राइबोसोम", "ರೈಬೋಸೋಮ್"),
        ),
        1,
    ),
    1, 0.9, null, false,
)

private fun previewState(feedback: Feedback? = null) = PlayerUiState(
    loading = false,
    lesson = com.stemtutor.core.data.model.LessonDetail(
        "l1", "c1", Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"),
        com.stemtutor.core.data.model.GenerationStatus.READY, true, listOf(PREVIEW_Q),
    ),
    queue = LessonQueue.of(4),
    choice = com.stemtutor.core.data.model.Answer.Choice(0),
    feedback = feedback,
)

@BilingualPreviews
@Composable
private fun PlayingPreview() {
    StemTutorTheme { LessonPlayerScreen(previewState(), PlayerActions(), animate = false) }
}

@BilingualPreviews
@Composable
private fun WrongFeedbackPreview() {
    StemTutorTheme {
        LessonPlayerScreen(
            previewState(Feedback(false, 0, true, PREVIEW_Q.body, PREVIEW_Q.explanation)),
            PlayerActions(),
            animate = false,
        )
    }
}

@BilingualPreviews
@Composable
private fun LessonEndPreview() {
    StemTutorTheme { LessonEnd(LessonResult(75, 0.83, 3, true, StudentStats.EMPTY), {}, animate = false) }
}
