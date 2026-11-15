package com.stemtutor.feature.student.doubt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.DoubtAnswer
import com.stemtutor.core.data.model.DoubtMode
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.SourceChip
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.component.StemTextField
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.bilingual
import com.stemtutor.feature.student.bilingualFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val DOUBT_COURSE_ARG = "courseId"
const val DOUBT_QUESTION_ARG = "question"
const val DOUBT_TERM_ARG = "term"
const val DOUBT_START_ARG = "start"

sealed interface ChatItem {
    data class Asked(val text: String) : ChatItem
    data class Answered(val answer: DoubtAnswer) : ChatItem
}

data class DoubtUiState(
    val items: List<ChatItem> = emptyList(),
    val sending: Boolean = false,
    val error: AppError? = null,
    val doubtsLeft: Int? = null,
    val aboutQuestion: Boolean = false,
    val aboutTerm: Tri? = null,
    val started: Boolean = false,
) {
    val lastAnswer: DoubtAnswer? get() = items.filterIsInstance<ChatItem.Answered>().lastOrNull()?.answer
}

/** Doubt chat (PRD M3): short bilingual answers from the teacher's notes, with the page. */
@HiltViewModel
class DoubtViewModel @Inject constructor(
    private val learning: LearningRepository,
    private val listen: ListenController,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val courseId: String = checkNotNull(savedState[DOUBT_COURSE_ARG])
    private val questionId: String? = savedState.get<String>(DOUBT_QUESTION_ARG)?.takeIf { it.isNotBlank() }
    private val termKey: String? = savedState.get<String>(DOUBT_TERM_ARG)?.takeIf { it.isNotBlank() }
    val start: String = savedState.get<String>(DOUBT_START_ARG).orEmpty()

    private val _state = MutableStateFlow(DoubtUiState(aboutQuestion = questionId != null))
    val state: StateFlow<DoubtUiState> = _state.asStateFlow()

    init {
        if (termKey != null) {
            viewModelScope.launch {
                learning.terms(courseId).onSuccess { list ->
                    list.firstOrNull { it.key == termKey }?.let { t -> _state.update { it.copy(aboutTerm = t.forms) } }
                }
            }
        }
    }

    /** Sends the opening message chosen on the feedback sheet ("Why?" or "Explain simpler"), once. */
    fun startWith(text: String, mode: DoubtMode) {
        if (_state.value.started) return
        _state.update { it.copy(started = true) }
        send(text, mode)
    }

    fun send(text: String, mode: DoubtMode = DoubtMode.ASK) {
        val t = text.trim()
        if (t.isEmpty() || _state.value.sending) return
        _state.update { it.copy(items = it.items + ChatItem.Asked(t), sending = true, error = null, started = true) }
        viewModelScope.launch {
            learning.askDoubt(courseId, t, mode, questionId, termKey).fold(
                onSuccess = { a -> _state.update { it.copy(items = it.items + ChatItem.Answered(a), sending = false, doubtsLeft = a.doubtsLeftToday) } },
                onFailure = { e -> _state.update { it.copy(sending = false, error = e.appError) } },
            )
        }
    }

    fun listenTo(answer: DoubtAnswer, mode: LanguageMode) {
        viewModelScope.launch { listen.speak(answer.answer, mode) }
    }
}

@Composable
fun DoubtRoute(onBack: () -> Unit, viewModel: DoubtViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode = currentLanguageMode()
    val why = bilingualStringResource(R.string.ln_auto_why).regional
    val simpler = bilingualStringResource(R.string.ln_auto_simpler).regional
    val example = bilingualStringResource(R.string.ln_auto_example).regional
    LaunchedEffect(viewModel.start) {
        when (viewModel.start) {
            "why" -> viewModel.startWith(why, DoubtMode.ASK)
            "simpler" -> viewModel.startWith(simpler, DoubtMode.SIMPLER)
        }
    }
    DoubtScreen(
        state = state,
        onBack = onBack,
        onSend = { viewModel.send(it) },
        onExample = { viewModel.send(example, DoubtMode.EXAMPLE) },
        onSimpler = { viewModel.send(simpler, DoubtMode.SIMPLER) },
        onListen = { state.lastAnswer?.let { viewModel.listenTo(it, mode) } },
    )
}

@Composable
fun DoubtScreen(
    state: DoubtUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onExample: () -> Unit,
    onSimpler: () -> Unit,
    onListen: () -> Unit,
    animate: Boolean = true,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(state.items.size, state.sending) {
        val count = state.items.size + if (state.sending) 1 else 0
        if (count > 0) list.animateScrollToItem(count)
    }
    val back = bilingualStringResource(R.string.ln_back)
    val mode = currentLanguageMode()
    Column(Modifier.fillMaxSize().background(StemColors.White).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "${back.regional} · ${back.english}", tint = StemColors.Ink)
            }
            Column {
                BilingualText(R.string.ln_doubt_title, size = BilingualTextSize.Large)
                val about = when {
                    state.aboutTerm != null -> bilingualFormat(R.string.ln_doubt_about_term, state.aboutTerm.regional(mode), state.aboutTerm.en)
                    state.aboutQuestion -> bilingualStringResource(R.string.ln_doubt_about_question)
                    else -> bilingualStringResource(R.string.ln_doubt_about_course)
                }
                BilingualText(about, size = BilingualTextSize.Small)
            }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            state = list,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CowMascot(CowPose.Wave, width = 72.dp, animate = animate)
                    SpeechBubble(R.string.ln_doubt_empty, Modifier.weight(1f))
                }
            }
            items(state.items.size) { i ->
                when (val item = state.items[i]) {
                    is ChatItem.Asked -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            item.text,
                            fontSize = 17.sp,
                            color = StemColors.Ink,
                            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(StemColors.BlueTint).padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    is ChatItem.Answered -> AnswerBubble(item.answer)
                }
            }
            if (state.sending) {
                item { BilingualText(R.string.ln_thinking, size = BilingualTextSize.Small) }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
            state.doubtsLeft?.let { BilingualText(bilingualStringResource(R.string.ln_doubts_left, it), size = BilingualTextSize.Small) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickButton(Sticker.Speaker, R.string.ln_listen, Modifier.weight(1f), enabled = state.lastAnswer != null, onClick = onListen)
                QuickButton(Sticker.Sparkle, R.string.ln_example, Modifier.weight(1f), enabled = !state.sending, onClick = onExample)
                QuickButton(Sticker.Sprout, R.string.ln_explain_simpler, Modifier.weight(1f), enabled = !state.sending, onClick = onSimpler)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StemTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = bilingualStringResource(R.string.ln_doubt_hint),
                    modifier = Modifier.weight(1f),
                    capitalization = KeyboardCapitalization.Sentences,
                    centered = false,
                    maxLength = 500,
                    onDone = { onSend(draft); draft = "" },
                )
                ChunkyButton(R.string.ln_send, { onSend(draft); draft = "" }, enabled = draft.isNotBlank() && !state.sending)
            }
        }
    }
}

@Composable
private fun AnswerBubble(a: DoubtAnswer) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StemColors.Soft).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!a.inNotes) {
            BilingualText(
                R.string.ln_not_in_notes,
                size = BilingualTextSize.Small,
                regionalColor = StemColors.Brown,
                englishColor = StemColors.Brown,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(StemColors.ChipFill).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        BilingualText(a.answer.bilingual())
        a.sourcePages.forEach { SourceChip(it) }
    }
}

@Composable
private fun QuickButton(icon: Sticker, label: Int, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) StemColors.Soft else StemColors.Line)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StickerIcon(icon, size = 22.dp)
        BilingualText(label, size = BilingualTextSize.Small, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@BilingualPreviews
@Composable
private fun DoubtPreview() {
    StemTutorTheme {
        DoubtScreen(
            DoubtUiState(
                items = listOf(
                    ChatItem.Asked("What do mitochondria do?"),
                    ChatItem.Answered(
                        DoubtAnswer(
                            Tri(
                                "Mitochondria make energy for the cell, like a generator powers a village.",
                                "माइटोकॉन्ड्रिया कोशिका के लिए ऊर्जा बनाते हैं, जैसे जनरेटर गाँव को बिजली देता है।",
                                "ಮೈಟೊಕಾಂಡ್ರಿಯಾ ಜೀವಕೋಶಕ್ಕೆ ಶಕ್ತಿ ಮಾಡುತ್ತವೆ, ಜನರೇಟರ್ ಊರಿಗೆ ವಿದ್ಯುತ್ ಕೊಡುವಂತೆ.",
                            ),
                            inNotes = true, sourcePages = listOf(1), doubtsLeftToday = 19,
                        ),
                    ),
                ),
                doubtsLeft = 19,
            ),
            {}, {}, {}, {}, {}, animate = false,
        )
    }
}
